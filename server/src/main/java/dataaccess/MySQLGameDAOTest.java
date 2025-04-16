package dataaccess;

import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Final MySQLGameDAOTest that expects an exception
 * for duplicate gameName if 'UNIQUE' is enforced in the DB.
 */
public class MySQLGameDAOTest {

    private MySQLGameDAO gameDAO;

    @BeforeEach
    void setup() throws DataAccessException {
        DatabaseManager.initDB();  // Make sure 'gameName' is UNIQUE in the DB schema
        gameDAO = new MySQLGameDAO();
        gameDAO.clear(); // Clear the table before each test
    }

    @Test
    void createGamePositive() throws DataAccessException {
        GameData game = new GameData(0, "whiteUser", "blackUser", "TestGame", null);
        int newID = gameDAO.createGame(game);

        assertTrue(newID > 0, "Should return an auto-generated gameID > 0.");
        GameData fetched = gameDAO.getGame(newID);
        assertNotNull(fetched, "GameData should be retrievable.");
        assertEquals("TestGame", fetched.getGameName());
    }

    /**
     * Negative: Attempt to create a second game with the same name => expect DataAccessException.
     * Only valid if 'gameName' is UNIQUE in your DB table definition.
     */
    @Test
    void createGameNegativeDuplicateName() throws DataAccessException {
        // First insert
        gameDAO.createGame(new GameData(0, "white", "black", "UniqueName", null));

        // Second insert with same name => must throw DataAccessException
        assertThrows(DataAccessException.class, () -> {
            gameDAO.createGame(new GameData(0, "white2", "black2", "UniqueName", null));
        }, "Inserting a duplicate gameName should fail if the DB has UNIQUE constraint on gameName.");
    }

    @Test
    void getGamePositive() throws DataAccessException {
        int createdID = gameDAO.createGame(new GameData(0, "wPlayer", "bPlayer", "CoolGame", null));
        GameData fetched = gameDAO.getGame(createdID);

        assertNotNull(fetched, "Should retrieve the newly inserted game.");
        assertEquals("CoolGame", fetched.getGameName());
        assertEquals("wPlayer", fetched.getWhiteUsername());
    }

    @Test
    void getGameNegativeNotFound() {
        // 9999 presumably doesn't exist in DB
        assertThrows(DataAccessException.class, () -> {
            gameDAO.getGame(9999);
        }, "Fetching a non-existent gameID should throw DataAccessException.");
    }

    @Test
    void listGamesPositive() throws DataAccessException {
        // Insert multiple games
        gameDAO.createGame(new GameData(0, "w1", "b1", "GameOne", null));
        gameDAO.createGame(new GameData(0, "w2", "b2", "GameTwo", null));

        List<GameData> all = gameDAO.listGames();
        assertEquals(2, all.size(), "Should list exactly 2 games.");
    }

    @Test
    void updateGamePositive() throws DataAccessException {
        int gameId = gameDAO.createGame(new GameData(0, "whiteGuy", "blackGuy", "Original", null));
        GameData updatedGame = new GameData(gameId, "whiteGuy", "blackGuy", "Renamed", null);

        gameDAO.updateGame(updatedGame);
        GameData fetched = gameDAO.getGame(gameId);
        assertEquals("Renamed", fetched.getGameName(), "Game name should be updated in DB.");
    }

    @Test
    void updateGameNegativeNoSuchGame() {
        // Attempting to update a gameID that doesn't exist
        GameData phantom = new GameData(9999, "wTeam", "bTeam", "Phantom", null);
        assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGame(phantom);
        }, "Updating a non-existent gameID should throw an exception.");
    }

    @Test
    void clearPositive() throws DataAccessException {
        gameDAO.createGame(new GameData(0, "white", "black", "WillBeCleared", null));
        gameDAO.clear();

        List<GameData> all = gameDAO.listGames();
        assertEquals(0, all.size(), "After clear(), no games remain.");
    }
}