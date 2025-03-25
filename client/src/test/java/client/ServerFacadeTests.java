package client;

import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static client.ServerFacade facade;

    private static int uniqueCounter = 0;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        facade = new client.ServerFacade(port);
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void beforeEachTest() throws Exception {
    }

    //register
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

        facade.register(user, "pw", user + "@ex.com");

        Exception ex = assertThrows(Exception.class, () ->
                facade.register(user, "pw", user + "@ex.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("exists"),
                "Expected 'failed' or 'exists' in the error message");
    }

    //login
    @Test
    public void loginPositive() throws Exception {
        String user = "testLoginPos" + (++uniqueCounter);
        facade.register(user, "mypw", user + "@ex.com");

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

    //logout
    @Test
    public void logoutPositive() throws Exception {
        String user = "testLogoutPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        assertDoesNotThrow(() -> facade.logout(data.authToken()));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bogus-token-12345")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("invalid"),
                "Expected an error for invalid token");
    }

    //createGame
    @Test
    public void createGamePositive() throws Exception {
        String user = "testCreateGamePos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        assertDoesNotThrow(() -> facade.createGame(data.authToken(), "GamePos" + uniqueCounter));
    }

    @Test
    public void createGameNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken") ||
                        ex.getMessage().toLowerCase().contains("failed"),
                "Expected error complaining about missing token");
    }

    //listGames
    @Test
    public void listGamesPositive() throws Exception {
        String user = "testListGamesPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        facade.createGame(data.authToken(), "ListPos" + uniqueCounter);

        List<GameData> games = facade.listGames(data.authToken());
        assertTrue(games.size() >= 1, "Should have at least 1 game after creation");
    }

    @Test
    public void listGamesNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken") ||
                        ex.getMessage().toLowerCase().contains("failed"),
                "Expected error for missing token");
    }

    //joinGame
    @Test
    public void joinGamePositive() throws Exception {
        String user = "testJoinPos" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");

        facade.createGame(data.authToken(), "JoinGamePos" + uniqueCounter);

        List<GameData> games = facade.listGames(data.authToken());
        assertTrue(games.size() > 0, "Should have at least one game");
        int gameID = games.get(games.size() - 1).gameID();

        assertDoesNotThrow(() -> facade.joinGame(data.authToken(), gameID, "WHITE"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        String user = "testJoinNeg" + (++uniqueCounter);
        AuthData data = facade.register(user, "pw", user + "@ex.com");
        facade.createGame(data.authToken(), "JoinNegGame" + uniqueCounter);

        List<GameData> games = facade.listGames(data.authToken());
        int gameID = games.get(games.size() - 1).gameID();

        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("badToken123", gameID, "WHITE")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed") ||
                        ex.getMessage().toLowerCase().contains("invalid"),
                "Expected error for invalid token");
    }
}