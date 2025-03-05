package server.handlers;

import service.GameService;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import spark.Request;
import spark.Response;
import spark.Route;
import model.GameRequest;

public class GameHandler {
    private GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Route listGames = (Request req, Response res) -> {
        var authToken = req.headers("authorization");

        try {
            var games = gameService.listGames(authToken);
            res.status(200);
            return gson.toJson(games);
        } catch (DataAccessException e) {
            res.status(401);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route createGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");

        // Parse the request body as JSON to extract the gameName
        String body = req.body();
        GameRequest gameRequest = gson.fromJson(body, GameRequest.class);
        String gameName = gameRequest.getGameName();

        // Check if the gameName is null or empty
        if (gameName == null || gameName.trim().isEmpty()) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: game name is required"));
        }

        try {
            int gameID = gameService.createGame(authToken, gameName);
            res.status(200);
            return gson.toJson(new GameResponse(gameID));
        } catch (DataAccessException e) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route joinGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        String playerColor = req.queryParams("playerColor");
        int gameID = Integer.parseInt(req.queryParams("gameID"));

        try {
            gameService.joinGame(authToken, playerColor, gameID);
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

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