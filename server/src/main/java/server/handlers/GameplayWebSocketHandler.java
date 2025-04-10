package server.handlers;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import service.UserService;
import websocket.commands.UserGameCommand;
import websocket.commands.UserGameCommand.CommandType;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GameplayWebSocketHandler example:
 *   - On CONNECT, validates token, sets up a PlayerConnection
 *   - On MAKE_MOVE, attempts a move, updates and broadcasts
 *   - On LEAVE, removes from the game and notifies others
 *   - On RESIGN, marks game as ended and notifies others
 *
 *   Uses specialized classes for LOAD_GAME (LoadGameMessage)
 *   and NOTIFICATION (NotificationMessage), but sends a minimal
 *   JSON object for errors.
 */
@WebSocket
public class GameplayWebSocketHandler {

    private static UserService userService;
    private static GameService gameService;

    /**
     * Stores each connected session's info (username, token, color, gameID).
     * sessionMap is keyed by the Jetty Session object.
     */
    private static final Map<Session, PlayerConnection> sessionMap = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    /**
     * Provide this so you can inject your existing
     * UserService / GameService from outside.
     */
    public static void configureServices(UserService us, GameService gs) {
        userService = us;
        gameService = gs;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("New WebSocket connection: " + session);
        // Put a placeholder PlayerConnection in the map
        sessionMap.put(session, new PlayerConnection());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String rawJson) {
        System.out.println("Received WS message: " + rawJson);

        try {
            // Convert the raw JSON into a simple UserGameCommand
            UserGameCommand cmd = gson.fromJson(rawJson, UserGameCommand.class);
            if (cmd == null) {
                sendError(session, "Invalid or empty JSON in command");
                return;
            }

            switch (cmd.getCommandType()) {
                case CONNECT -> handleConnect(session, cmd);
                case MAKE_MOVE -> handleMakeMove(session, cmd);
                case LEAVE -> handleLeave(session, cmd);
                case RESIGN -> handleResign(session, cmd);
                default -> sendError(session, "Unknown commandType: " + cmd.getCommandType());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            sendError(session, "Exception: " + ex.getMessage());
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Socket closed: " + reason);

        // If the user didn't explicitly LEAVE, we do partial cleanup here
        PlayerConnection pc = sessionMap.remove(session);
        if (pc != null) {
            // Optionally broadcast that the user disconnected
            System.out.println("Session closed for user: " + pc.username);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error);
        // Optionally handle or log
    }

    // ------------------------------------------------------------------
    //                     COMMAND HANDLERS
    // ------------------------------------------------------------------

    private void handleConnect(Session session, UserGameCommand cmd) {
        String token = cmd.getAuthToken();
        Integer gameID = cmd.getGameID();

        if (token == null || token.isBlank() || gameID == null) {
            sendError(session, "CONNECT missing authToken or gameID");
            return;
        }

        // Validate token -> retrieve username
        String username;
        try {
            username = userService.getUsernameFromToken(token);
        } catch (DataAccessException e) {
            sendError(session, "Invalid auth token: " + e.getMessage());
            return;
        }
        if (username == null) {
            sendError(session, "Auth token not found");
            return;
        }

        // Retrieve or create a PlayerConnection for this session
        PlayerConnection pc = sessionMap.get(session);
        if (pc == null) {
            pc = new PlayerConnection();
            sessionMap.put(session, pc);
        }
        pc.authToken = token;
        pc.username = username;
        pc.gameID = gameID;

        // Determine if this user is WHITE, BLACK, or OBSERVER
        String color;
        try {
            // gameService.getPlayerColorIfExists returns "WHITE", "BLACK", or null if not a player
            color = gameService.getPlayerColorIfExists(gameID, username);
        } catch (DataAccessException e) {
            sendError(session, "Failed to retrieve player color: " + e.getMessage());
            return;
        }

        if (color == null) {
            pc.color = "OBSERVER";
        } else {
            pc.color = color;  // "WHITE" or "BLACK"
        }

        // Load the chess game
        ChessGame game;
        try {
            game = gameService.loadGame(gameID);
        } catch (Exception e) {
            sendError(session, "Could not load game from DB: " + e.getMessage());
            return;
        }

        // Send a LOAD_GAME message to the newly connected user
        sendLoadGame(session, game);

        // Broadcast a notification to everyone else in this game
        broadcastNotification(gameID, pc.username + " connected as " + pc.color);
    }

    private void handleMakeMove(Session session, UserGameCommand cmd) {
        PlayerConnection pc = sessionMap.get(session);
        if (pc == null) {
            sendError(session, "No PlayerConnection. Must CONNECT first.");
            return;
        }

        // TODO: Extract move from cmd (cmd.getMove()), then attempt to make the move
        try {
            // Example:
            // gameService.makeMove(pc.gameID, pc.username, cmd.getMove());
        } catch (Exception ex) {
            sendError(session, "Move failed: " + ex.getMessage());
            return;
        }

        // Reload the updated board from DB
        ChessGame updated;
        try {
            updated = gameService.loadGame(pc.gameID);
        } catch (Exception ex2) {
            sendError(session, "Could not load updated game: " + ex2.getMessage());
            return;
        }

        // Broadcast the new board & a notification
        broadcastLoadGame(pc.gameID, updated);
        broadcastNotification(pc.gameID, pc.username + " made a move!");
    }

    private void handleLeave(Session session, UserGameCommand cmd) {
        PlayerConnection pc = sessionMap.remove(session);
        if (pc == null) {
            // Not found in map
            return;
        }
        // Possibly call gameService.leaveGame(pc.gameID, pc.username);

        broadcastNotification(pc.gameID, pc.username + " left the game.");

        // Optionally close the session
        try {
            session.close();
        } catch (Exception ignore) {}
    }

    private void handleResign(Session session, UserGameCommand cmd) {
        PlayerConnection pc = sessionMap.get(session);
        if (pc == null) {
            sendError(session, "No PlayerConnection. Must CONNECT first.");
            return;
        }

        // Possibly call gameService.resignGame(pc.gameID, pc.username);
        // Mark the game as over

        broadcastNotification(pc.gameID, pc.username + " resigned!");
    }

    // ------------------------------------------------------------------
    //                          HELPER METHODS
    // ------------------------------------------------------------------

    private void sendLoadGame(Session session, ChessGame game) {
        LoadGameMessage msg = new LoadGameMessage(game);
        String json = gson.toJson(msg);
        try {
            session.getRemote().sendString(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastLoadGame(int gameID, ChessGame game) {
        LoadGameMessage msg = new LoadGameMessage(game);
        String json = gson.toJson(msg);
        broadcastToGame(gameID, json);
    }

    private void broadcastNotification(int gameID, String text) {
        NotificationMessage note = new NotificationMessage(text);
        String json = gson.toJson(note);
        broadcastToGame(gameID, json);
    }

    private void sendError(Session session, String errMsg) {
        // A minimal JSON for errors:
        String errorJson = String.format(
                "{\"serverMessageType\":\"ERROR\",\"errorMessage\":\"%s\"}",
                errMsg
        );
        try {
            session.getRemote().sendString(errorJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastToGame(int gameID, String json) {
        for (Session s : sessionMap.keySet()) {
            PlayerConnection pc = sessionMap.get(s);
            if (pc != null && pc.gameID == gameID && s.isOpen()) {
                try {
                    s.getRemote().sendString(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}