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
        int port = server.run(0);
        System.out.println("Started test HTTP server on " + port);

        facade = new ServerFacade(port);
    }

    @BeforeEach
    public void setUp() throws Exception {
        facade.clearDB();
        authToken = null;
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    //Register
    @Test
    public void registerPositive() throws Exception {
        AuthData auth = facade.register("ikemarion", "pw", "ikemarion@example.com");
        assertNotNull(auth.authToken());
        assertEquals("ikemarion", auth.username());
    }

    @Test
    public void registerNegativeDuplicate() throws Exception {
        facade.register("so_air_maniac", "pw", "air@example.com");

        Exception ex = assertThrows(Exception.class, () ->
                facade.register("so_air_maniac", "pw", "air@example.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    //Login
    @Test
    public void loginPositive() throws Exception {
        facade.register("cosmo", "secret", "cosmo@example.com");

        AuthData auth = facade.login("cosmo", "secret");
        assertNotNull(auth.authToken());
        assertEquals("cosmo", auth.username());
    }

    @Test
    public void loginNegativeWrongPassword() throws Exception {
        facade.register("lebronjames", "rightPW", "lbj@example.com");

        Exception ex = assertThrows(Exception.class, () ->
                facade.login("lebronjames", "wrongPW")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    //Logout
    @Test
    public void logoutPositive() throws Exception {
        var auth = facade.register("isaac", "pw", "isaac@example.com");
        authToken = auth.authToken();

        assertDoesNotThrow(() -> facade.logout(authToken));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bad-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    //Create Game
    @Test
    public void createGamePositive() throws Exception {
        var auth = facade.register("testname1", "pw", "tn1@example.com");
        authToken = auth.authToken();

        assertDoesNotThrow(() -> facade.createGame(authToken, "MyChessGame"));
    }

    @Test
    public void createGameNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    //List games
    @Test
    public void listGamesPositive() throws Exception {
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

    //Join game
    @Test
    public void joinGamePositive() throws Exception {
        var auth = facade.register("testname3", "pw", "tn3@example.com");
        authToken = auth.authToken();

        facade.createGame(authToken, "JoinableX");

        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if ("JoinableX".equals(g.gameName())) {
                gameID = g.gameID();
                break;
            }
        }
        assertNotEquals(-1, gameID, "Should find the newly created game JoinableX");

        final int finalGameID = gameID;
        assertDoesNotThrow(() -> facade.joinGame(authToken, finalGameID, "WHITE"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        var auth = facade.register("testname4", "pw", "tn4@example.com");
        authToken = auth.authToken();

        facade.createGame(authToken, "InvalidJoinGame");

        var games = facade.listGames(authToken);
        int gameID = -1;
        for (GameData g : games) {
            if ("InvalidJoinGame".equals(g.gameName())) {
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