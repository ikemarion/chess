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

        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401); // Unauthorized
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }

        try {
            var games = gameService.listGames(authToken);
            res.status(200);
            return gson.toJson(games);
        } catch (DataAccessException e) {
            res.status(401); // Unauthorized
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route createGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");

        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401); // Unauthorized
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }

        String body = req.body();
        GameRequest gameRequest = gson.fromJson(body, GameRequest.class);
        String gameName = gameRequest.getGameName();

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

        if (authToken == null || authToken.trim().isEmpty()) {
            res.status(401); // Unauthorized
            return gson.toJson(new ErrorResponse("Error: Invalid authentication token"));
        }

        String playerColor = req.queryParams("playerColor");

        // Ensure gameID is not null or empty before parsing
        String gameIDParam = req.queryParams("gameID");
        if (gameIDParam == null || gameIDParam.trim().isEmpty()) {
            res.status(400); // Bad request
            return gson.toJson(new ErrorResponse("Error: gameID is required"));
        }

        int gameID;
        try {
            gameID = Integer.parseInt(gameIDParam);
        } catch (NumberFormatException e) {
            res.status(400); // Bad request
            return gson.toJson(new ErrorResponse("Error: Invalid gameID format"));
        }

        try {
            gameService.joinGame(authToken, playerColor, gameID);
            res.status(200);
            return "{}"; // Empty JSON object for successful join
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