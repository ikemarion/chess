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
            var statement = "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME;
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * Obtain a connection to the database, setting the catalog.
     * Make sure to close this connection (try-with-resources).
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
     * Initializes the DB & tables if they don't exist.
     * Call this once at the beginning of your server (e.g., in `main`).
     */
    public static void initDB() throws DataAccessException {
        createDatabase();

        try (Connection conn = getConnection()) {
            // USERS table
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

            // GAMES table, with UNIQUE constraint on gameName
            String createGames = """
                CREATE TABLE IF NOT EXISTS Games (
                  gameID INT AUTO_INCREMENT PRIMARY KEY,
                  whiteUsername VARCHAR(50),
                  blackUsername VARCHAR(50),
                  gameName VARCHAR(100) NOT NULL UNIQUE,  -- unique constraint here
                  gameJSON TEXT
                );
            """;
            try (PreparedStatement stmt = conn.prepareStatement(createGames)) {
                stmt.executeUpdate();
            }

            // AUTHTOKENS table
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