package service;

import dataaccess.*;
import model.UserData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseServiceTest {

    private DatabaseService databaseService;
    private UserDAO userDAO;
    private GameDAO gameDAO;
    private AuthDAO authDAO;
    private ClearDAO clearDAO;

    @BeforeEach
    void setUp() {
        userDAO = new InMemoryUserDAO();
        gameDAO = new InMemoryGameDAO();
        authDAO = new InMemoryAuthDAO();
        clearDAO = new ClearDAO(userDAO, gameDAO, authDAO);
        databaseService = new DatabaseService(clearDAO);
    }

    @Test
    void testClearDatabase() throws DataAccessException {
        userDAO.createUser(new UserData("user1", "pass", "user1@example.com"));
        gameDAO.createGame(new GameData(10, "TestGame", null, null, null));

        databaseService.clear();

        assertNull(userDAO.getUser("user1"), "Users should be cleared.");
        assertEquals(0, gameDAO.listGames().size(), "Games should be cleared.");
    }
}