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
        // Start your server on random port
        server = new Server();
        int port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        // Create the facade pointing to that port
        facade = new ServerFacade(port);
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Clear the database so each test starts fresh
        facade.clearDB();

        // We'll set authToken to null each time so we can register new users
        authToken = null;
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    // ======================
    // REGISTER
    // ======================
    @Test
    public void registerPositive() throws Exception {
        // user: "ikemarion" (fresh DB => no collision)
        AuthData auth = facade.register("ikemarion", "pw", "ikemarion@example.com");
        assertNotNull(auth.authToken());
        assertEquals("ikemarion", auth.username());
    }

    @Test
    public void registerNegativeDuplicate() throws Exception {
        // user: "so_air_maniac"
        facade.register("so_air_maniac", "pw", "air@example.com");

        // second register => duplicate => fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.register("so_air_maniac", "pw", "air@example.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ======================
    // LOGIN
    // ======================
    @Test
    public void loginPositive() throws Exception {
        // user: "cosmo"
        facade.register("cosmo", "secret", "cosmo@example.com");

        AuthData auth = facade.login("cosmo", "secret");
        assertNotNull(auth.authToken());
        assertEquals("cosmo", auth.username());
    }

    @Test
    public void loginNegativeWrongPassword() throws Exception {
        // user: "lebronjames"
        facade.register("lebronjames", "rightPW", "lbj@example.com");

        // attempt wrong PW => fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.login("lebronjames", "wrongPW")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ======================
    // LOGOUT
    // ======================
    @Test
    public void logoutPositive() throws Exception {
        // user: "isaac"
        var auth = facade.register("isaac", "pw", "isaac@example.com");
        authToken = auth.authToken();

        assertDoesNotThrow(() -> facade.logout(authToken));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        // no user => pass a bogus token => fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bad-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    // ======================
    // CREATE GAME
    // ======================
    @Test
    public void createGamePositive() throws Exception {
        // user: "testname1"
        var auth = facade.register("testname1", "pw", "tn1@example.com");
        authToken = auth.authToken();

        // create game with a fixed name
        assertDoesNotThrow(() -> facade.createGame(authToken, "MyChessGame"));
    }

    @Test
    public void createGameNegativeNoAuth() {
        // try create w/out token => fail
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    // ======================
    // LIST GAMES
    // ======================
    @Test
    public void listGamesPositive() throws Exception {
        // user: "testname2"
        var auth = facade.register("testname2", "pw", "tn2@example.com");
        authToken = auth.authToken();

        facade.createGame(authToken, "GameA");
        List<GameData> games = facade.listGames(authToken);
        assertTrue(games.size() >= 1, "Should have at least 1 game in DB");
    }

    @Test
    public void listGamesNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    // ======================
    // JOIN GAME
    // ======================
    @Test
    public void joinGamePositive() throws Exception {
        // user: "testname3"
        var auth = facade.register("testname3", "pw", "tn3@example.com");
        authToken = auth.authToken();

        // create a game "JoinableX"
        facade.createGame(authToken, "JoinableX");

        // find that game
        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if ("JoinableX".equals(g.gameName())) {
                gameID = g.gameID();
                break;
            }
        }
        assertNotEquals(-1, gameID, "Should find the newly created game JoinableX");

        // join with color=WHITE
        final int finalGameID = gameID;
        assertDoesNotThrow(() -> facade.joinGame(authToken, finalGameID, "WHITE"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        // user: "testname4"
        var auth = facade.register("testname4", "pw", "tn4@example.com");
        authToken = auth.authToken();

        // create "InvalidJoinGame"
        facade.createGame(authToken, "InvalidJoinGame");

        // find that game
        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if ("InvalidJoinGame".equals(g.gameName())) {
                gameID = g.gameID();
                break;
            }
        }
        assertNotEquals(-1, gameID);

        // pass bogus token => fail
        final int finalGameID = gameID;
        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("fakeTokenXYZ", finalGameID, "WHITE")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }
}