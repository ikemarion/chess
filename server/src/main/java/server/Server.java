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
        // Set up in-memory DAOs
        UserDAO userDAO = new InMemoryUserDAO();
        GameDAO gameDAO = new InMemoryGameDAO();
        AuthDAO authDAO = new InMemoryAuthDAO();
        ClearDAO clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);

        // Initialize services
        this.userService = new UserService(userDAO, authDAO);
        this.gameService = new GameService(gameDAO, authDAO);
        this.databaseService = new DatabaseService(clearDAO);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // Register handlers
        UserHandler userHandler = new UserHandler(userService);
        GameHandler gameHandler = new GameHandler(gameService);
        ClearHandler clearHandler = new ClearHandler(databaseService);

        // Register API Endpoints
        Spark.delete("/db", clearHandler.clear);
        Spark.post("/user", userHandler.register);
        Spark.post("/session", userHandler.login);
        Spark.delete("/session", userHandler.logout);
        Spark.get("/game", gameHandler.listGames);
        Spark.post("/game", gameHandler.createGame);
        Spark.put("/game", gameHandler.joinGame);

        Spark.init(); // Keep this line to match your original structure

        Spark.awaitInitialization();
        System.out.println("Server running on port: " + Spark.port());
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
