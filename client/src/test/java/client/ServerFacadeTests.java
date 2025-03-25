import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {
    private static Server server;
    private static ServerFacade facade;
    private static String authToken;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Each test starts by registering a brand-new user (unique name)
        String uniqueUser = "testUser_" + System.nanoTime();
        authToken = facade.register(uniqueUser, "pw", uniqueUser + "@site.com").authToken();
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    // ==========================
    // REGISTER
    // ==========================
    @Test
    public void registerPositive() throws Exception {
        String user = "uniqueReg_" + System.nanoTime();
        var auth = facade.register(user, "pw", user + "@email.com");
        assertNotNull(auth.authToken());
        assertEquals(user, auth.username());
    }

    @Test
    public void registerNegativeDuplicate() throws Exception {
        facade.register("dupUser", "pw", "a@b.com");
        Exception ex = assertThrows(Exception.class, () ->
                facade.register("dupUser", "pw", "a@b.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ==========================
    // LOGIN
    // ==========================
    @Test
    public void loginPositive() throws Exception {
        // unique user
        String user = "logPos_" + System.nanoTime();
        facade.register(user, "secret", user + "@test.com");

        var auth = facade.login(user, "secret");
        assertNotNull(auth.authToken());
        assertEquals(user, auth.username());
    }

    @Test
    public void loginNegativeWrongPassword() throws Exception {
        facade.register("pwUser", "correctPW", "pw@site.com");
        Exception ex = assertThrows(Exception.class, () ->
                facade.login("pwUser", "wrongPW")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ==========================
    // LOGOUT
    // ==========================
    @Test
    public void logoutPositive() {
        assertDoesNotThrow(() -> facade.logout(authToken));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bad-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ==========================
    // CREATE GAME
    // ==========================
    @Test
    public void createGamePositive() {
        String gameName = "GamePos_" + System.nanoTime();
        // Use final local var for lambda
        final String finalGameName = gameName;
        assertDoesNotThrow(() -> facade.createGame(authToken, finalGameName));
    }

    @Test
    public void createGameNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    // ==========================
    // LIST GAMES
    // ==========================
    @Test
    public void listGamesPositive() throws Exception {
        String gameName = "Game_" + System.nanoTime();
        facade.createGame(authToken, gameName);

        List<GameData> games = facade.listGames(authToken);
        assertTrue(games.size() >= 1, "Should have at least 1 game");
    }

    @Test
    public void listGamesNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    // ==========================
    // JOIN GAME
    // ==========================
    @Test
    public void joinGamePositive() throws Exception {
        String gameName = "Joinable_" + System.nanoTime();
        facade.createGame(authToken, gameName);

        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if (gameName.equals(g.gameName())) {
                gameID = g.gameID();
                break;
            }
        }
        assertNotEquals(-1, gameID, "Should find the new game we created.");

        // Must store it in a final local var for the lambda
        final int finalGameID = gameID;
        assertDoesNotThrow(() -> facade.joinGame(authToken, finalGameID, "WHITE"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        String gameName = "BadToken_" + System.nanoTime();
        facade.createGame(authToken, gameName);

        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if (gameName.equals(g.gameName())) {
                gameID = g.gameID();
                break;
            }
        }
        assertNotEquals(-1, gameID);

        final int finalGameID = gameID;
        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("fakeTokenXYZ", finalGameID, "WHITE")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }
}