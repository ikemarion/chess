package server.handlers;

import service.GameService;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import spark.Request;
import spark.Response;
import spark.Route;
import model.GameData;
import java.util.List;

public class GameHandler {
    private GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Route listGames = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401);
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }
        try {
            List<GameData> games = gameService.listGames(authToken);
            res.status(200);
            return gson.toJson(new GamesWrapper(games));
        } catch (DataAccessException e) {
            setProperStatus(res, e.getMessage());
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route createGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401);
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }
        String body = req.body();
        GameRequest gameRequest = gson.fromJson(body, GameRequest.class);
        if (gameRequest == null || gameRequest.getGameName() == null || gameRequest.getGameName().trim().isEmpty()) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: game name is required"));
        }
        try {
            int gameID = gameService.createGame(authToken, gameRequest.getGameName());
            res.status(200);
            return gson.toJson(new GameResponse(gameID));
        } catch (DataAccessException e) {
            setProperStatus(res, e.getMessage());
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route joinGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401);
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }
        String body = req.body();
        JoinGameRequest joinRequest = gson.fromJson(body, JoinGameRequest.class);
        if (joinRequest == null || joinRequest.playerColor == null || joinRequest.playerColor.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: playerColor is required"));
        }
        if (joinRequest.gameID <= 0) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: gameID is required"));
        }
        try {
            gameService.joinGame(authToken, joinRequest.playerColor, joinRequest.gameID);
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            setProperStatus(res, e.getMessage());
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    /**
     * Sets the correct status code based on the error message from DataAccessException.
     */
    private void setProperStatus(Response res, String msg) {
        if (msg == null) {
            res.status(400);
            return;
        }
        String lower = msg.toLowerCase();
        if (lower.contains("unauthorized") || lower.contains("invalid authentication token")) {
            res.status(401);
        } else if (lower.contains("already taken")) {
            res.status(403);
        } else {
            res.status(400);
        }
    }

    private static class GamesWrapper {
        private List<GameData> games;
        public GamesWrapper(List<GameData> games) {
            this.games = games;
        }
        public List<GameData> getGames() {
            return games;
        }
    }

    private static class GameRequest {
        private String gameName;
        public String getGameName() { return gameName; }
    }

    private static class JoinGameRequest {
        private String playerColor;
        private int gameID;
    }

    public static class ErrorResponse {
        private String message;
        public ErrorResponse(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }

    public static class GameResponse {
        private int gameID;
        public GameResponse(int gameID) {
            this.gameID = gameID;
        }
        public int getGameID() {
            return gameID;
        }
    }
}