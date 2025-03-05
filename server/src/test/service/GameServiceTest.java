package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import dataaccess.InMemoryAuthDAO;
import dataaccess.InMemoryGameDAO;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private GameService gameService;
    private GameDAO gameDAO;
    private AuthDAO authDAO;

    @BeforeEach
    void setUp() {
        gameDAO = new InMemoryGameDAO();
        authDAO = new InMemoryAuthDAO();
        gameService = new GameService(gameDAO, authDAO);
    }

    @Test
    void testCreateGame_Success() throws DataAccessException {
        String validToken = "token123";
        authDAO.createAuth(new AuthData(validToken, "userOne"));

        int gameID = gameService.createGame(validToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be > 0 for a newly created game.");

        var gameData = gameDAO.getGame(gameID);
        assertNotNull(gameData, "Created game should exist in DAO.");
        assertEquals("TestGame", gameData.gameName(), "Game name should match.");
    }

    @Test
    void testCreateGame_Unauthorized() {
        String invalidToken = "badToken";

        assertThrows(DataAccessException.class, () -> {
            gameService.createGame(invalidToken, "NoGame");
        }, "Invalid token should throw DataAccessException.");
    }

    @Test
    void testListGames_Success() throws DataAccessException {
        String validToken = "tokenABC";
        authDAO.createAuth(new AuthData(validToken, "someUser"));

        gameDAO.createGame(new GameData(1, "GameOne", null, null, null));
        gameDAO.createGame(new GameData(2, "GameTwo", null, null, null));

        List<GameData> games = gameService.listGames(validToken);
        assertEquals(2, games.size(), "Should return two games.");
    }

    @Test
    void testListGames_Unauthorized() {
        String invalidToken = "zzzToken";
        assertThrows(DataAccessException.class, () -> {
            gameService.listGames(invalidToken);
        }, "Listing games with invalid token should throw an error.");
    }

    @Test
    void testJoinGame_ColorTaken() throws DataAccessException {
        String token1 = "firstToken";
        authDAO.createAuth(new AuthData(token1, "userOne"));

        String token2 = "secondToken";
        authDAO.createAuth(new AuthData(token2, "userTwo"));

        gameDAO.createGame(new GameData(1, "ConflictGame", null, null, null));

        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame(token2, "WHITE", 1);
        }, "Joining a color that's already taken should throw DataAccessException.");
    }
}