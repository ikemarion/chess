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
        try {
            DatabaseManager.initDB();
        } catch (DataAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize database. Server cannot start.", e);
        }

        UserDAO userDAO = new MySQLUserDAO();
        GameDAO gameDAO = new MySQLGameDAO();
        AuthDAO authDAO = new MySQLAuthDAO();

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

        // HTTP endpoints
        Spark.delete("/db", clearHandler.clear);
        Spark.post("/user", userHandler.register);
        Spark.post("/session", userHandler.login);
        Spark.delete("/session", userHandler.logout);
        Spark.get("/game", gameHandler.listGames);
        Spark.post("/game", gameHandler.createGame);
        Spark.put("/game", gameHandler.joinGame);

        // WebSocket endpoint for gameplay
        Spark.webSocket("/ws", GameplayWebSocketHandler.class);

        // Configure the WebSocket handler so it can use userService/gameService
        GameplayWebSocketHandler.configureServices(userService, gameService);

        Spark.init();
        Spark.awaitInitialization();
        System.out.println("Server running on port: " + Spark.port());
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}