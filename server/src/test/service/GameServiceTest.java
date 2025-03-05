package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests GameService methods directly (no HTTP).
 * Covers createGame, listGames, joinGame, with positive & negative scenarios.
 */
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

    /**
     * Positive: createGame with valid token & valid name
     */
    @Test
    void testCreateGame_Success() throws DataAccessException {
        // Insert a valid token
        String validToken = "token123";
        authDAO.createAuth(new AuthData(validToken, "userOne"));

        int gameID = gameService.createGame(validToken, "TestGame");
        assertTrue(gameID > 0, "Game ID should be a positive integer.");

        // Confirm in gameDAO
        var storedGame = gameDAO.getGame(gameID);
        assertNotNull(storedGame, "Game should be stored.");
        assertEquals("TestGame", storedGame.gameName(), "Game name should match.");
    }

    /**
     * Negative: createGame with invalid token
     */
    @Test
    void testCreateGame_Unauthorized() {
        assertThrows(DataAccessException.class, () -> {
            gameService.createGame("invalidToken", "GameX");
        }, "Invalid token => DataAccessException.");
    }

    /**
     * Positive: listGames with valid token
     */
    @Test
    void testListGames_Success() throws DataAccessException {
        // Valid token
        authDAO.createAuth(new AuthData("listToken", "lister"));

        // Create 2 games
        gameDAO.createGame(new GameData(1, "GameOne", null, null, null));
        gameDAO.createGame(new GameData(2, "GameTwo", null, null, null));

        List<GameData> games = gameService.listGames("listToken");
        assertEquals(2, games.size(), "Should list two games.");
    }

    /**
     * Negative: listGames with invalid token
     */
    @Test
    void testListGames_Unauthorized() {
        assertThrows(DataAccessException.class, () -> {
            gameService.listGames("nopeToken");
        });
    }

    /**
     * Positive: joinGame with valid token & open color
     */
    @Test
    void testJoinGame_Success() throws DataAccessException {
        authDAO.createAuth(new AuthData("joinToken", "joiner"));
        gameDAO.createGame(new GameData(1, "JoinableGame", null, null, null));

        gameService.joinGame("joinToken", "WHITE", 1);

        var updated = gameDAO.getGame(1);
        assertEquals("joiner", updated.whiteUsername(), "joiner should occupy WHITE");
    }

    /**
     * Negative: joinGame color already taken
     */
    @Test
    void testJoinGame_ColorTaken() throws DataAccessException {
        authDAO.createAuth(new AuthData("firstToken", "userOne"));
        authDAO.createAuth(new AuthData("secondToken", "userTwo"));

        gameDAO.createGame(new GameData(1, "ConflictGame", null, null, null));
        // userOne takes WHITE
        gameService.joinGame("firstToken", "WHITE", 1);

        // userTwo tries to take WHITE => should fail
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame("secondToken", "WHITE", 1);
        }, "Color already taken => exception.");
    }

    /**
     * Negative: joinGame invalid token
     */
    @Test
    void testJoinGame_InvalidToken() throws DataAccessException {
        gameDAO.createGame(new GameData(1, "InvalidTokenGame", null, null, null));
        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame("bogusToken", "WHITE", 1);
        }, "Invalid token => exception.");
    }

    /**
     * Negative: joinGame invalid color
     */
    @Test
    void testJoinGame_InvalidColor() throws DataAccessException {
        authDAO.createAuth(new AuthData("colorToken", "colorUser"));
        gameDAO.createGame(new GameData(1, "BadColorGame", null, null, null));

        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame("colorToken", "RED", 1);
        }, "Invalid color => exception.");
    }

    /**
     * Negative: joinGame nonexistent game
     */
    @Test
    void testJoinGame_NonexistentGame() throws DataAccessException {
        authDAO.createAuth(new AuthData("gameToken", "someUser"));
        // We do NOT create a game in the DAO => gameID=999 doesn't exist

        assertThrows(DataAccessException.class, () -> {
            gameService.joinGame("gameToken", "WHITE", 999);
        }, "Nonexistent game => exception.");
    }
}