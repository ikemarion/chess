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
        // 1) Make sure your database & tables are set up
        try {
            DatabaseManager.initDB();  // calls createDatabase() + CREATE TABLE IF NOT EXISTS statements
        } catch (DataAccessException e) {
            // If DB initialization fails, we can’t continue.
            e.printStackTrace();
            throw new RuntimeException("Could not initialize database. Server cannot start.", e);
        }

        // 2) Use the MySQL DAOs
        UserDAO userDAO = new MySQLUserDAO();
        GameDAO gameDAO = new MySQLGameDAO();
        AuthDAO authDAO = new MySQLAuthDAO();

        // 3) ClearDAO with MySQL DAOs
        ClearDAO clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);

        // 4) Create services using the new MySQL-based DAOs
        this.databaseService = new DatabaseService(clearDAO);
        this.userService = new UserService(userDAO, authDAO);
        this.gameService = new GameService(gameDAO, authDAO);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("web");

        // 5) Handlers remain the same, but they now indirectly use MySQL
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
