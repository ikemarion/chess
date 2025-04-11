package server.handlers;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import websocket.messages.ServerMessage.ServerMessageType;
import javax.websocket.Endpoint;


import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class GameplayWebSocketHandler {

    private static final Gson gson = new Gson();
    private final GameService gameService;
    private static final Map<Integer, Set<Session>> gameSessions = new ConcurrentHashMap<>();

    public GameplayWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("WebSocket connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            UserGameCommand cmd = gson.fromJson(message, UserGameCommand.class);
            if (cmd == null || cmd.getCommandType() == null) {
                send(session, new ServerMessage(ServerMessageType.ERROR, "Error: Invalid or missing commandType"));
                return;
            }

            Integer gameID = cmd.getGameID();
            String token = cmd.getAuthToken();

            switch (cmd.getCommandType()) {
                case CONNECT -> {
                    if (gameID == null || gameID <= 0) {
                        send(session, new ServerMessage(ServerMessageType.ERROR, "Error: CONNECT missing valid gameID"));
                        return;
                    }

                    ChessGame game = gameService.loadGame(gameID);
                    gameSessions.computeIfAbsent(gameID, k -> ConcurrentHashMap.newKeySet()).add(session);

                    GameData gameData = gameService.getGameData(gameID);
                    send(session, new ServerMessage(ServerMessageType.LOAD_GAME, gameData));
                    broadcastToGame(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A user connected to game " + gameID));
                }

                case MAKE_MOVE -> {
                    ChessMove move = cmd.move();
                    if (gameID == null || move == null) {
                        send(session, new ServerMessage(ServerMessageType.ERROR, "Error: MAKE_MOVE missing gameID or move"));
                        return;
                    }

                    gameService.makeMove(gameID, move);
                    GameData updated = gameService.getGameData(gameID);

                    broadcastToGame(gameID, new ServerMessage(ServerMessageType.LOAD_GAME, updated));
                    broadcastToGame(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A move was made in game " + gameID));
                }

                case LEAVE -> {
                    if (gameID == null || token == null) {
                        send(session, new ServerMessage(ServerMessageType.ERROR, "Error: LEAVE missing gameID or authToken"));
                        return;
                    }

                    gameService.leaveGame(gameID, token);
                    removeSessionFromGame(gameID, session);
                    broadcastToGame(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A player has left game " + gameID));
                }

                case RESIGN -> {
                    if (gameID == null || token == null) {
                        send(session, new ServerMessage(ServerMessageType.ERROR, "Error: RESIGN missing gameID or authToken"));
                        return;
                    }

                    gameService.resignGame(gameID, token);
                    broadcastToGame(gameID, new ServerMessage(ServerMessageType.NOTIFICATION, "A player has resigned from game " + gameID));
                }
            }

        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
            send(session, new ServerMessage(ServerMessageType.ERROR, "Error: " + e.getMessage()));
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("WebSocket closed: " + reason);
        gameSessions.values().forEach(set -> set.remove(session));
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    private void send(Session session, ServerMessage msg) {
        try {
            session.getRemote().sendString(gson.toJson(msg));
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        }
    }

    private void broadcastToGame(int gameID, ServerMessage msg) {
        Set<Session> sessions = gameSessions.get(gameID);
        if (sessions == null) return;

        String json = gson.toJson(msg);
        for (Session s : sessions) {
            try {
                s.getRemote().sendString(json);
            } catch (IOException e) {
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
    }
}