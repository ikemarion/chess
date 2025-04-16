package server.handlers;

import chess.ChessGame;
import chess.InvalidMoveException;
import com.google.gson.Gson;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.UserDAO;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessage.ServerMessageType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@WebSocket
public class GameplayWebSocketHandler {

    // Constants must be in UPPER_SNAKE_CASE.
    private static final Gson GSON = new Gson();
    private static AuthDAO authDAO;
    private static GameDAO gameDAO;
    private static UserDAO userDAO;

    private static final Map<Integer, Set<Session>> GAME_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Session, String> SESSION_USER_MAP = new ConcurrentHashMap<>();

    /**
     * Dependency injection method.
     * The handler expects three DAO objects.
     */
    public static void initialize(AuthDAO auth, GameDAO game, UserDAO user) {
        authDAO = auth;
        gameDAO = game;
        userDAO = user;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            UserGameCommand cmd = GSON.fromJson(message, UserGameCommand.class);
            if (cmd == null || cmd.getCommandType() == null) {
                sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                        "Invalid or missing commandType", null));
                return;
            }

            // 1) Verify the authentication token.
            String token = cmd.getAuthToken();
            AuthData authData = authDAO.getAuth(token);
            if (authData == null) {
                sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                        "Unauthenticated request", null));
                return;
            }
            String username = authData.username();
            SESSION_USER_MAP.put(session, username);

            // 2) Dispatch based on command type.
            switch (cmd.getCommandType()) {
                case CONNECT: {
                    handleConnect(session, cmd, username);
                    break;
                }
                case MAKE_MOVE: {
                    handleMakeMove(session, cmd, username);
                    break;
                }
                case LEAVE: {
                    handleLeave(session, cmd, username);
                    break;
                }
                case RESIGN: {
                    handleResign(session, cmd, username);
                    break;
                }
                default: {
                    sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                            "Unknown command type", username));
                    break;
                }
            }

        } catch (DataAccessException dae) {
            System.err.println("Data access error: " + dae.getMessage());
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "DataAccess error: " + dae.getMessage(), null));
        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Error: " + e.getMessage(), null));
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
        for (Set<Session> sessions : GAME_SESSIONS.values()) {
            if (sessions != null) {
                sessions.remove(session);
            }
        }
        SESSION_USER_MAP.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket encountered an error: " + error.getMessage());
    }

    // ------------------------------------------------------------------------
    // Command Handlers
    // ------------------------------------------------------------------------

    private void handleConnect(Session session, UserGameCommand cmd, String username)
            throws DataAccessException, IOException {

        Integer gameID = cmd.getGameID();
        if (gameID == null || gameID <= 0) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "CONNECT missing valid gameID", username));
            return;
        }
        GameData game = gameDAO.getGame(gameID);
        ChessGame chess = game.game();

        String playerColor = determinePlayerColor(game, username);
        GAME_SESSIONS.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(session);
        sendBlocking(session, new ServerMessage(ServerMessageType.LOAD_GAME, game, username));

        // Only if the game is active, broadcast the join notification.
        if ((chess != null) && (!chess.isEndGame())) {
            String notifyMsg = username + " joined as " + playerColor;
            broadcastExcluding(gameID, session, new ServerMessage(ServerMessageType.NOTIFICATION, notifyMsg, null));
        }
    }

    private void handleMakeMove(Session session, UserGameCommand cmd, String username)
            throws DataAccessException {

        Integer gameID = cmd.getGameID();
        if (gameID == null || cmd.move() == null) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "MAKE_MOVE missing gameID or move", username));
            return;
        }

        GameData game = gameDAO.getGame(gameID);
        ChessGame chess = game.game();
        if (chess == null || chess.isEndGame()) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Game is over", username));
            return;
        }

        String playerColor = determinePlayerColor(game, username);
        if (playerColor.equals("OBSERVER")) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Observers cannot move", username));
            return;
        }

        ChessGame.TeamColor turn = chess.getTeamTurn();
        if ((turn == ChessGame.TeamColor.WHITE && !playerColor.equals("WHITE")) ||
                (turn == ChessGame.TeamColor.BLACK && !playerColor.equals("BLACK"))) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Not your turn", username));
            return;
        }

        if (!chess.validMoves(cmd.move().getStartPosition()).contains(cmd.move())) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Invalid move", username));
            return;
        }

        try {
            chess.makeMove(cmd.move());
        } catch (InvalidMoveException ex) {
                    sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Move error: " + ex.getMessage(), username));
            return;
        }

        GameData updated = new GameData(
                game.gameID(),
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                chess
        );
        gameDAO.updateGame(updated);
        broadcastBlocking(gameID, new ServerMessage(ServerMessageType.LOAD_GAME, updated, null));
        broadcastExcluding(gameID, session, new ServerMessage(ServerMessageType.NOTIFICATION, username + " made a move", null));
    }

    private void handleLeave(Session session, UserGameCommand cmd, String username)
            throws DataAccessException {

        Integer gameID = cmd.getGameID();
        if (gameID == null) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "LEAVE missing gameID", username));
            return;
        }

        GameData game = gameDAO.getGame(gameID);
        String newWhite = game.whiteUsername();
        String newBlack = game.blackUsername();
        if (username.equals(newWhite)) {
            newWhite = null;
        } else if (username.equals(newBlack)) {
            newBlack = null;
        }
        GameData updated = new GameData(
                game.gameID(),
                newWhite,
                newBlack,
                game.gameName(),
                game.game()
        );
        gameDAO.updateGame(updated);
        removeSessionFromGame(gameID, session);
        broadcastBlocking(gameID, new ServerMessage(ServerMessageType.NOTIFICATION,
                username + " left the game", null));
    }

    private void handleResign(Session session, UserGameCommand cmd, String username)
            throws DataAccessException {

        Integer gameID = cmd.getGameID();
        if (gameID == null) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "RESIGN missing gameID", username));
            return;
        }

        GameData game = gameDAO.getGame(gameID);
        ChessGame chess = game.game();
        if (chess == null || chess.isEndGame()) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Game already ended", username));
            return;
        }

        if (!username.equals(game.whiteUsername()) && !username.equals(game.blackUsername())) {
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR,
                    "Observers cannot resign", username));
            return;
        }

        chess.setResigned();
        GameData updated = new GameData(
                game.gameID(),
                game.whiteUsername(),
                game.blackUsername(),
                game.gameName(),
                chess
        );
        gameDAO.updateGame(updated);
        // Broadcast exactly one NOTIFICATION message.
        broadcastBlocking(gameID, new ServerMessage(ServerMessageType.NOTIFICATION,
                username + " resigned from the game", null));
        removeSessionFromGame(gameID, session);
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    private String determinePlayerColor(GameData game, String username) throws DataAccessException {
        if (username.equals(game.whiteUsername())) {
            return "WHITE";
        } else if (username.equals(game.blackUsername())) {
            return "BLACK";
        }
        if (game.whiteUsername() == null || game.whiteUsername().isEmpty()) {
            GameData updated = new GameData(
                    game.gameID(),
                    username,
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            );
            gameDAO.updateGame(updated);
            return "WHITE";
        } else if (game.blackUsername() == null || game.blackUsername().isEmpty()) {
            GameData updated = new GameData(
                    game.gameID(),
                    game.whiteUsername(),
                    username,
                    game.gameName(),
                    game.game()
            );
            gameDAO.updateGame(updated);
            return "BLACK";
        }
        return "OBSERVER";
    }

    private void sendMessage(Session s, ServerMessage msg) throws InterruptedException, ExecutionException, IOException {
        String recipient = SESSION_USER_MAP.get(s);
        ServerMessage copy = copyMessageForRecipient(msg, recipient);
        Future<Void> future = s.getRemote().sendStringByFuture(GSON.toJson(copy));
        future.get();
        s.getRemote().flush();
        Thread.sleep(50);
    }

    private void sendBlocking(Session session, ServerMessage msg) {
        try {
            sendMessage(session, msg);
        } catch (InterruptedException | ExecutionException | IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    private void broadcastBlocking(int gameID, ServerMessage msg) {
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions == null) {
            return;
        }
        for (Session s : sessions) {
            try {
                sendMessage(s, msg);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.err.println("Broadcast failed: " + e.getMessage());
            }
        }
    }

    private void broadcastExcluding(int gameID, Session excludeSession, ServerMessage msg) {
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions == null) {
            return;
        }
        for (Session s : sessions) {
            if (s == excludeSession) {
                continue;
            }
            try {
                sendMessage(s, msg);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.err.println("BroadcastExcluding failed: " + e.getMessage());
            }
        }
    }

    private void removeSessionFromGame(int gameID, Session session) {
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                GAME_SESSIONS.remove(gameID);
            }
        }
        SESSION_USER_MAP.remove(session);
    }

    private ServerMessage copyMessageForRecipient(ServerMessage original, String recipient) {
        if (original.getGame() != null) {
            return new ServerMessage(original.getServerMessageType(), original.getGame(), recipient);
        } else if (original.getErrorMessage() != null) {
            return new ServerMessage(original.getServerMessageType(), original.getErrorMessage(), recipient);
        } else {
            return new ServerMessage(original.getServerMessageType(), original.getMessage(), recipient);
        }
    }
}