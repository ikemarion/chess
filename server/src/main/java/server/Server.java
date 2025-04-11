package server;

import dataaccess.*;
import service.*;
import server.handlers.GameplayWebSocketHandler;
import server.handlers.ClearHandler;
import server.handlers.UserHandler;
import server.handlers.GameHandler;
import spark.Spark;

public class Server {
    private final UserService userService;
    private final GameService gameService;
    private final DatabaseService databaseService;

    // DAO instances created once and reused
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public Server() {
        try {
            DatabaseManager.initDB();
        } catch (DataAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize database. Server cannot start.", e);
        }

        userDAO = new MySQLUserDAO();
        gameDAO = new MySQLGameDAO();
        authDAO = new MySQLAuthDAO();
        ClearDAO clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);

        this.databaseService = new DatabaseService(clearDAO);
        this.userService = new UserService(userDAO, authDAO);
        this.gameService = new GameService(gameDAO, authDAO);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);

        // Inject the SAME DAO instances into the WebSocket handler.
        GameplayWebSocketHandler.initialize(authDAO, gameDAO, userDAO);
        GameplayWebSocketHandler webSocketHandler = new GameplayWebSocketHandler();
        Spark.webSocket("/ws", webSocketHandler);

        Spark.staticFiles.location("web");

        ClearHandler clearHandler = new ClearHandler(databaseService);
        Spark.delete("/db", clearHandler.clear);

        UserHandler userHandler = new UserHandler(userService);
        Spark.post("/user", userHandler.register);
        Spark.post("/session", userHandler.login);
        Spark.delete("/session", userHandler.logout);

        GameHandler gameHandler = new GameHandler(gameService);
        Spark.get("/game", gameHandler.listGames);
        Spark.post("/game", gameHandler.createGame);
        Spark.put("/game", gameHandler.joinGame);

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