package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AllServiceTests {

    private UserService userService;
    private GameService gameService;
    private DatabaseService databaseService;
    private UserDAO userDAO;
    private GameDAO gameDAO;
    private AuthDAO authDAO;

    @BeforeEach
    void setUp() {
        userDAO = new InMemoryUserDAO();
        gameDAO = new InMemoryGameDAO();
        authDAO = new InMemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
        gameService = new GameService(gameDAO, authDAO);
        databaseService = new DatabaseService(new ClearDAO(userDAO, gameDAO, authDAO));
    }

    private AuthData registerAndLogin(String username, String password) throws DataAccessException {
        userService.register(username, password, username + "@test.com");
        return userService.login(username, password);
    }

    @Test
    void testRegisterSuccess() throws DataAccessException {
        var authData = registerAndLogin("newUser", "password");
        assertNotNull(authData);
        assertEquals("newUser", authData.username());
        assertNotNull(userDAO.getUser("newUser"));
    }

    @Test
    void testRegisterAlreadyExists() throws DataAccessException {
        registerAndLogin("existingUser", "pw");
        assertThrows(DataAccessException.class, () -> {
            userService.register("existingUser", "newpass", "again@example.com");
        });
    }

    @Test
    void testCreateGameSuccess() throws DataAccessException {
        AuthData auth = registerAndLogin("userOne", "password");
        int gameID = gameService.createGame(auth.authToken(), "TestGame");
        assertTrue(gameID > 0);
        assertNotNull(gameDAO.getGame(gameID));
    }

    @Test
    void testClearDatabase() throws DataAccessException {
        registerAndLogin("user1", "pw");
        gameDAO.createGame(new GameData(10, "TestGame", null, null, null));
        authDAO.createAuth(new AuthData("tokenABC", "user1"));

        databaseService.clear();
        assertNull(userDAO.getUser("user1"));
        assertEquals(0, gameDAO.listGames().size());
        assertNull(authDAO.getAuth("tokenABC"));
    }
}