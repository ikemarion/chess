package server.handlers;

import service.GameService;
import spark.Request;
import spark.Response;
import spark.Route;
import com.google.gson.Gson;
import java.util.List;
import model.GameData;

public class GameHandler {
    private GameService gameService;
    private final Gson gson = new Gson(); // Add Gson for JSON handling

    public GameHandler(GameService gameService) {
        if (gameService == null) {
            throw new IllegalArgumentException("GameService cannot be null");
        }
        this.gameService = gameService;
    }

    public Route listGames = (Request req, Response res) -> {
        var authToken = req.headers("authorization");

        List<GameData> games = gameService.listGames(authToken); // Correct method now exists
        res.status(200);
        return gson.toJson(games);
    };

    public Route createGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        var gameName = req.queryParams("gameName");

        if (gameName == null || gameName.isEmpty()) {
            res.status(400);
            return "{ \"message\": \"Error: game name is required\" }";
        }

        int gameID = gameService.createGame(authToken, gameName); // Correct method now exists
        res.status(200);
        return "{ \"gameID\": " + gameID + " }";
    };

    public Route joinGame = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        var playerColor = req.queryParams("playerColor");
        var gameIDString = req.queryParams("gameID");

        int gameID;
        try {
            gameID = Integer.parseInt(gameIDString);
        } catch (NumberFormatException e) {
            res.status(400);
            return "{ \"message\": \"Error: invalid game ID\" }";
        }

        gameService.joinGame(authToken, playerColor, gameID);  // This should now work with correct parameters
        res.status(200);
        return "{}";
    };
}