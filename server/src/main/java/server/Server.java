package server;

import dataaccess.*;
import service.*;
import server.handlers.*;
import spark.Spark;

public class Server {
    private final UserService userService;
    private final GameService gameService;
    private final DatabaseService databaseService;

    public Server() {
        UserDAO userDAO = new InMemoryUserDAO();
        GameDAO gameDAO = new InMemoryGameDAO();
        AuthDAO authDAO = new InMemoryAuthDAO();
        ClearDAO clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);

        this.databaseService = new DatabaseService(clearDAO);
        this.userService = new UserService(userDAO, authDAO);
        this.gameService = new GameService(gameDAO, authDAO);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        ClearHandler clearHandler = new ClearHandler(databaseService);
        UserHandler userHandler = new UserHandler(userService);
        GameHandler gameHandler = new GameHandler(gameService);

        Spark.delete("/db", clearHandler.clear);
        Spark.post("/user", userHandler.register);
        Spark.post("/session", userHandler.login);
        Spark.delete("/session", userHandler.logout);
        Spark.get("/game", gameHandler.listGames);
        Spark.post("/game", gameHandler.createGame);
        Spark.put("/game", gameHandler.joinGame);

        Spark.init(); // Keeps structure matching original
        Spark.awaitInitialization();

        System.out.println("Server running on port: " + Spark.port());
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}