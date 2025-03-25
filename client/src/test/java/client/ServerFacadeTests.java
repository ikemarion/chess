package client;

import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example ServerFacadeTests demonstrating how to
 * have a positive & negative test for each facade method.
 */
public class ServerFacadeTests {

    private static Server server;
    private static client.ServerFacade facade;

    // We'll store a fresh unique user each time to avoid collisions:
    private static int uniqueCounter = 0;

    @BeforeAll
    public static void init() {
        // Start your server on a random port:
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        // Create a facade pointing to that port
        facade = new client.ServerFacade(port);
    }

    @AfterAll
    public static void stopServer() {
        // Shut down the server after all tests
        server.stop();
    }

    /**
     * Optionally, clear your DB between each test if your server
     * supports a "delete /db" or something. For example:
     */
    @BeforeEach
    public void beforeEachTest() throws Exception {
        // If you have an endpoint to clear the database, do it here:
        // e.g. facade.clearDatabase();
        // 
        // Otherwise, we can just rely on unique usernames to avoid collisions.
    }

    // ====================================================
    // register(String username, String password, String email)
    // ====================================================
    @Test
    public void registerPositive() throws Exception {
        String user = "testRegPos" + (++uniqueCounter);
        AuthData result = facade.register(user, "pass123", user + "@example.com");
        assertNotNull(result, "Should return an AuthData object on success");
        assertEquals(user, result.username(), "Usernames should match what we registered");
        assertTrue(result.authToken().length() > 5, "Token should be nontrivial");
    }

    @Test
    public void registerNegativeDuplicate() throws Exception {
        String user = "testRegDup" + (++uniqueCounter);

        // First time => success
        facade.register(user, "pw", user + "@ex.com");

        // Second time => should fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.register(user, "pw", user + "@ex.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("exists"),
                "Expected 'failed' or 'exists' in the error message");
    }

    // ====================================================
    // login(String username, String password)
    // ====================================================
    @Test
    public void loginPositive() throws Exception {
        String user = "testLoginPos" + (++uniqueCounter);
        // First register
        facade.register(user, "mypw", user + "@ex.com");

        // Now login
        AuthData data = facade.login(user, "mypw");
        assertEquals(user, data.username(), "Username should match");
        assertNotNull(data.authToken(), "Token should not be null");
    }

    @Test
    public void loginNegativeWrongPass() throws Exception {
        String user = "testLoginWrong" + (++uniqueCounter);
        facade.register(user, "correct", user + "@ex.com");

        Exception ex = assertThrows(Exception.class, () ->
                facade.login(user, "incorrect")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("unauthorized"),
                "Error message should indicate a bad password or failed login");
    }

    // ====================================================
    // logout(String authToken)
    // ====================================================
    @Test
    public void logoutPositive() throws Exception {
        String user = "testLogoutPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        // Should not throw an exception
        assertDoesNotThrow(() -> facade.logout(data.authToken()));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        // A random token that doesn't exist
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bogus-token-12345")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("invalid"),
                "Expected an error for invalid token");
    }

    // ====================================================
    // createGame(String authToken, String gameName)
    // ====================================================
    @Test
    public void createGamePositive() throws Exception {
        // Need a valid token first
        String user = "testCreateGamePos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        // Create game
        assertDoesNotThrow(() -> facade.createGame(data.authToken(), "GamePos" + uniqueCounter));
    }

    @Test
    public void createGameNegativeNoAuth() {
        // null token => should fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken") ||
                        ex.getMessage().toLowerCase().contains("failed"),
                "Expected error complaining about missing token");
    }

    // ====================================================
    // listGames(String authToken)
    // ====================================================
    @Test
    public void listGamesPositive() throws Exception {
        // Need a valid token
        String user = "testListGamesPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        // Create one game
        facade.createGame(data.authToken(), "ListPos" + uniqueCounter);

        // Now list
        List<GameData> games = facade.listGames(data.authToken());
        assertTrue(games.size() >= 1, "Should have at least 1 game after creation");
    }

    @Test
    public void listGamesNegativeNoAuth() {
        // null token => fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken") ||
                        ex.getMessage().toLowerCase().contains("failed"),
                "Expected error for missing token");
    }

    // ====================================================
    // joinGame(String authToken, int gameID, String color)
    // ====================================================
    @Test
    public void joinGamePositive() throws Exception {
        String user = "testJoinPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        // create a game
        facade.createGame(data.authToken(), "JoinGamePos" + uniqueCounter);

        // We need the actual gameID from the server. Let's list them:
        List<GameData> games = facade.listGames(data.authToken());
        assertTrue(games.size() > 0, "Should have at least one game");
        int gameID = games.get(games.size() - 1).gameID(); // The last created game

        // Join as WHITE, for example
        assertDoesNotThrow(() -> facade.joinGame(data.authToken(), gameID, "WHITE"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        // We'll create a game with a valid user
        String user = "testJoinNeg" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");
        facade.createGame(data.authToken(), "JoinNegGame" + uniqueCounter);

        // list to get an ID
        List<GameData> games = facade.listGames(data.authToken());
        int gameID = games.get(games.size() - 1).gameID();

        // now try join with a bogus token
        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("badToken123", gameID, "WHITE")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("invalid"),
                "Expected error for invalid token");
    }

}
