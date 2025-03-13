package dataaccess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database creation & connections.
 * Make sure you call initDB() once when your server starts up.
 */
public class DatabaseManager {
    private static final String DATABASE_NAME;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    throw new Exception("Unable to load db.properties");
                }
                Properties props = new Properties();
                props.load(propStream);

                DATABASE_NAME = props.getProperty("db.name");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                var host = props.getProperty("db.host");
                var port = Integer.parseInt(props.getProperty("db.port"));
                CONNECTION_URL = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Unable to process db.properties. " + ex.getMessage());
        }
    }

    /**
     * Creates the database if it does not already exist.
     */
    static void createDatabase() throws DataAccessException {
        try {
            String statement = "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME;
            try (var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
                 var preparedStatement = conn.prepareStatement(statement)) {

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * Obtain a connection to the database, setting the catalog.
     */
    static Connection getConnection() throws DataAccessException {
        try {
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            conn.setCatalog(DATABASE_NAME);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * Initializes the DB & tables if they don't exist,
     * and tries to add a UNIQUE constraint for gameName
     * without dropping existing data.
     */
    public static void initDB() throws DataAccessException {
        createDatabase();

        try (Connection conn = getConnection()) {
            String createUsers = """
                CREATE TABLE IF NOT EXISTS Users (
                  username VARCHAR(50) NOT NULL PRIMARY KEY,
                  password VARCHAR(60) NOT NULL,
                  email VARCHAR(100) DEFAULT NULL
                );
            """;
            try (PreparedStatement stmt = conn.prepareStatement(createUsers)) {
                stmt.executeUpdate();
            }

            String createGames = """
                CREATE TABLE IF NOT EXISTS Games (
                  gameID INT AUTO_INCREMENT PRIMARY KEY,
                  whiteUsername VARCHAR(50),
                  blackUsername VARCHAR(50),
                  gameName VARCHAR(100) NOT NULL,
                  gameJSON TEXT
                );
            """;
            try (PreparedStatement stmt = conn.prepareStatement(createGames)) {
                stmt.executeUpdate();
            }

            try (PreparedStatement alterStmt = conn.prepareStatement(
                    "ALTER TABLE Games ADD UNIQUE (gameName)"
            )) {
                alterStmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Note: Could not add UNIQUE constraint on gameName (possibly already exists). " + e.getMessage());
            }

            // AUTH TOKENS
            String createAuthTokens = """
                CREATE TABLE IF NOT EXISTS AuthTokens (
                  authToken VARCHAR(255) NOT NULL PRIMARY KEY,
                  username VARCHAR(50) NOT NULL
                );
            """;
            try (PreparedStatement stmt = conn.prepareStatement(createAuthTokens)) {
                stmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error creating tables: " + e.getMessage());
        }
    }
}