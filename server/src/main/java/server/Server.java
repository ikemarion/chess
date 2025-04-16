package server;

import com.google.gson.Gson;
import dataaccess.AuthDAO;
import dataaccess.ClearDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.MySQLAuthDAO;
import dataaccess.MySQLGameDAO;
import dataaccess.MySQLUserDAO;
import dataaccess.UserDAO;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import service.DatabaseService;
import service.GameService;
import service.UserService;
import spark.Spark;
import server.handlers.ClearHandler;
import server.handlers.GameHandler;
import server.handlers.UserHandler;

import java.util.Map;

public class Server {

    // Service fields
    private final UserService userService;
    private final GameService gameService;
    private final DatabaseService databaseService;

    // DAO instances
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    // Handler instances
    private final UserHandler userHandler;
    private final GameHandler gameHandler;
    private final ClearHandler clearHandler;

    public Server() {
        try {
            // Initialize the database
            DatabaseManager.initDB();
        } catch (DataAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize database. Server cannot start.", e);
        }

        // Initialize DAO instances
        this.userDAO = new MySQLUserDAO();
        this.gameDAO = new MySQLGameDAO();
        this.authDAO = new MySQLAuthDAO();

        // Create a ClearDAO instance
        ClearDAO clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);

        // Initialize services
        this.databaseService = new DatabaseService(clearDAO);
        this.userService = new UserService(userDAO, authDAO);
        this.gameService = new GameService(gameDAO, authDAO);

        // Initialize handlers
        this.userHandler = new UserHandler(userService);
        this.gameHandler = new GameHandler(gameService);
        this.clearHandler = new ClearHandler(databaseService);
    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFiles.location("resources/web");

        // 1) Define a WebSocket at /ws using MyWebSocketHandler
        Spark.webSocket("/ws", server.MyWebSocketHandler.class);

        // 2) Global exception handler for DataAccessException
        Spark.exception(DataAccessException.class, (exception, req, res) -> {
            res.status(403);
            res.type("application/json");
            res.body(new Gson().toJson(Map.of("error", exception.getMessage())));
        });

        // 3) Map HTTP routes
        Spark.delete("/db", clearHandler.clear);
        Spark.post("/user", userHandler.register);
        Spark.post("/session", userHandler.login);
        Spark.delete("/session", userHandler.logout);
        Spark.post("/game", gameHandler.createGame);
        Spark.put("/game", gameHandler.joinGame);
        Spark.get("/game", gameHandler.listGames);

        // 4) Start Spark
        Spark.init();
        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }

    private static class DatabaseManager {
        public static void initDB() throws DataAccessException {
            // DB init code here
        }
    }
}
