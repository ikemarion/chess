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
    public void reset() throws Exception {
        authToken = facade.register("user_" + System.nanoTime(), "pass", "email@x.com").authToken();
    }

    @AfterAll
    public static void stopServer() {
        server.stop();
    }

    @Test
    public void registerPositive() throws Exception {
        var auth = facade.register("newUser", "pw", "email@e.com");
        assertNotNull(auth.authToken());
        assertEquals("newUser", auth.username());
    }

    @Test
    public void registerNegativeDuplicate() throws Exception {
        facade.register("dupUser", "pw", "a@b.com");
        Exception ex = assertThrows(Exception.class, () ->
                facade.register("dupUser", "pw", "a@b.com")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    public void loginPositive() throws Exception {
        facade.register("logUser", "pass123", "log@example.com");
        var auth = facade.login("logUser", "pass123");
        assertNotNull(auth.authToken());
        assertEquals("logUser", auth.username());
    }

    @Test
    public void loginNegativeWrongPassword() throws Exception {
        facade.register("wrongPw", "right", "em@x.com");
        Exception ex = assertThrows(Exception.class, () ->
                facade.login("wrongPw", "wrong")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    public void logoutPositive() throws Exception {
        assertDoesNotThrow(() -> facade.logout(authToken));
    }

    @Test
    public void logoutNegativeInvalidToken() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bad-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }

    @Test
    public void createGamePositive() throws Exception {
        assertDoesNotThrow(() -> facade.createGame(authToken, "MyChessGame"));
    }

    @Test
    public void createGameNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame(null, "NoAuthGame")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    @Test
    public void listGamesPositive() throws Exception {
        facade.createGame(authToken, "Game1");
        List<GameData> games = facade.listGames(authToken);
        assertTrue(games.size() >= 1);
    }

    @Test
    public void listGamesNegativeNoAuth() {
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames(null)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("no authtoken"));
    }

    @Test
    public void joinGamePositive() throws Exception {
        facade.createGame(authToken, "JoinableGame");
        List<GameData> games = facade.listGames(authToken);
        int id = games.get(0).gameID();
        assertDoesNotThrow(() -> facade.joinGame(authToken, id, "white"));
    }

    @Test
    public void joinGameNegativeInvalidToken() throws Exception {
        facade.createGame(authToken, "InvalidJoinGame");
        List<GameData> games = facade.listGames(authToken);
        int id = games.get(0).gameID();
        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("bad-token", id, "white")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("failed"));
    }
}