package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.GameService;
import model.GameData;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

public class GameHandler {
    private final GameService gameService;
    private final Gson gson = new Gson();

    public GameHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public Route listGames = (Request req, Response res) -> {
        try {
            String authToken = req.headers("authorization");
            List<GameData> games = gameService.listGames(authToken);
            res.status(200);
            return gson.toJson(new GameListResponse(games));
        } catch (DataAccessException e) {
            res.status(401);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    public Route createGame = (Request req, Response res) -> {
        try {
            String authToken = req.headers("authorization");
            var requestData = gson.fromJson(req.body(), CreateGameRequest.class);
            int gameID = gameService.createGame(authToken, requestData.gameName());
            res.status(200);
            return gson.toJson(new CreateGameResponse(gameID));
        } catch (DataAccessException e) {
            res.status(400);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    public Route joinGame = (Request req, Response res) -> {
        try {
            String authToken = req.headers("authorization");
            var requestData = gson.fromJson(req.body(), JoinGameRequest.class);
            gameService.joinGame(authToken, requestData.playerColor(), requestData.gameID());
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(400);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    private record GameListResponse(List<GameData> games) {}
    private record CreateGameRequest(String gameName) {}
    private record CreateGameResponse(int gameID) {}
    private record JoinGameRequest(String playerColor, int gameID) {}
    private record ErrorResponse(String message) {}
}