package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final String DATABASE_NAME;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    /*
     * Load the database information for the db.properties file.
     */
    static {
        try {
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
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
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
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
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DbInfo.getConnection(databaseName)) {
     * // execute SQL statements.
     * }
     * </code>
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

    public static void initDB() throws DataAccessException {
        createDatabase(); // Already in your code

        try (Connection conn = getConnection()) {
            // Users table
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

            // Games table
            String createGames = """
            CREATE TABLE IF NOT EXISTS Games (
              gameID INT AUTO_INCREMENT PRIMARY KEY,
              whiteUsername VARCHAR(50),
              blackUsername VARCHAR(50),
              gameName VARCHAR(100),
              gameJSON TEXT
            );
        """;
            try (PreparedStatement stmt = conn.prepareStatement(createGames)) {
                stmt.executeUpdate();
            }

            // AuthTokens table
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
