package server.handlers;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import service.UserService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessage.ServerMessageType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@WebSocket
public class GameplayWebSocketHandler {

    private static final Gson gson = new Gson();

    private static GameService gameService;
    private static UserService userService;

    private static final Map<Integer, Set<Session>> gameSessions = new ConcurrentHashMap<>();
    private static final Map<Session, Set<Integer>> loadGameSent = new ConcurrentHashMap<>();

    public static void initialize(GameService gameSvc, UserService userSvc) {
        gameService = gameSvc;
        userService = userSvc;
    }

    public GameplayWebSocketHandler() {}

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            UserGameCommand cmd = gson.fromJson(message, UserGameCommand.class);
            if (cmd == null || cmd.getCommandType() == null) {
                sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: Invalid or missing commandType"));
                return;
            }

            Integer gameID = cmd.getGameID();
            String token = cmd.getAuthToken();

            switch (cmd.getCommandType()) {

                case CONNECT -> {
                    if (gameID == null || gameID <= 0) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: CONNECT missing valid gameID"));
                        return;
                    }
                    if (token == null || token.isEmpty()) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: CONNECT missing auth token"));
                        return;
                    }

                    String username = userService.getUsernameFromToken(token);
                    if (username == null) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: Invalid auth token"));
                        return;
                    }

                    ChessGame game = gameService.loadGame(gameID);
                    if (game == null) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: Game not found for ID: " + gameID));
                        return;
                    }

                    gameSessions.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(session);
                    Set<Integer> sentGames = loadGameSent.computeIfAbsent(session, s -> ConcurrentHashMap.newKeySet());

                    if (!sentGames.contains(gameID)) {
                        GameData gameData = gameService.getGameData(gameID);
                        System.out.println("DEBUG: Handling CONNECT for user: " + username + " gameID: " + gameID);
                        System.out.println("DEBUG: gameService.getGameData returned: " + gameData);
                        System.out.println("DEBUG: Sending LOAD_GAME to session: " + session);

                        sendBlocking(session, new ServerMessage(ServerMessageType.LOAD_GAME, gameData));
                        sentGames.add(gameID);
                    }
                }

                case MAKE_MOVE -> {
                    if (gameID == null || cmd.move() == null) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: MAKE_MOVE missing gameID or move"));
                        return;
                    }

                    gameService.makeMove(gameID, cmd.move());
                    GameData updated = gameService.getGameData(gameID);

                    broadcastBlocking(gameID, new ServerMessage(ServerMessageType.LOAD_GAME, updated));
                    broadcastBlocking(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A move was made in game " + gameID));
                }

                case LEAVE -> {
                    if (gameID == null || token == null) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: LEAVE missing gameID or authToken"));
                        return;
                    }

                    gameService.leaveGame(gameID, token);
                    removeSessionFromGame(gameID, session);
                    broadcastBlocking(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A player has left game " + gameID));
                }

                case RESIGN -> {
                    if (gameID == null || token == null) {
                        sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: RESIGN missing gameID or authToken"));
                        return;
                    }

                    gameService.resignGame(gameID, token);
                    broadcastBlocking(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A player has resigned from game " + gameID));
                }
            }

        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
            sendBlocking(session, new ServerMessage(ServerMessageType.ERROR, "Error: " + e.getMessage()));
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
        gameSessions.values().forEach(sessions -> sessions.remove(session));
        loadGameSent.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    private void sendBlocking(Session session, ServerMessage msg) {
        try {
            String json = gson.toJson(msg);
            Future<Void> future = session.getRemote().sendStringByFuture(json);
            future.get();
            try {
                session.getRemote().flush();
            } catch (IOException e) {
                System.err.println("Flush failed: " + e.getMessage());
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    private void broadcastBlocking(int gameID, ServerMessage msg) {
        Set<Session> sessions = gameSessions.get(gameID);
        if (sessions == null) return;

        String json = gson.toJson(msg);
        for (Session s : sessions) {
            try {
                Future<Void> future = s.getRemote().sendStringByFuture(json);
                future.get();
                try {
                    s.getRemote().flush();
                } catch (IOException e) {
                    System.err.println("Flush failed: " + e.getMessage());
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Broadcast failed: " + e.getMessage());
            }
        }
    }

    private void removeSessionFromGame(int gameID, Session session) {
        Set<Session> sessions = gameSessions.get(gameID);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameSessions.remove(gameID);
            }
        }

        loadGameSent.computeIfPresent(session, (sess, games) -> {
            games.remove(gameID);
            return games.isEmpty() ? null : games;
        });
    }
}