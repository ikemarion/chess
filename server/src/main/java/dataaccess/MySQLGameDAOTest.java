package dataaccess;

import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

public class MySQLGameDAOTest {

    private MySQLGameDAO gameDAO;

    @BeforeEach
    void setup() throws DataAccessException {
        DatabaseManager.initDB();
        gameDAO = new MySQLGameDAO();
        gameDAO.clear();
    }

    @Test
    void createGamePositive() throws DataAccessException {
        GameData game = new GameData(0, "whiteUser", "blackUser", "TestGame", null);
        int newID = gameDAO.createGame(game);

        assertTrue(newID > 0, "Should return generated ID > 0.");
        GameData fetched = gameDAO.getGame(newID);
        assertNotNull(fetched, "Game should be retrievable after creation.");
        assertEquals("TestGame", fetched.gameName());
    }

    @Test
    void createGameNegativeInvalidData() {
        GameData invalid = new GameData(0, null, null, null, null);
        assertThrows(DataAccessException.class, () -> {
            gameDAO.createGame(invalid);
        }, "Creating a game with invalid data should throw.");
    }

    @Test
    void getGamePositive() throws DataAccessException {
        int id = gameDAO.createGame(new GameData(0, "white", "black", "CoolGame", null));
        GameData fetched = gameDAO.getGame(id);
        assertNotNull(fetched);
        assertEquals("CoolGame", fetched.gameName());
    }

    @Test
    void getGameNegativeNotFound() {
        assertThrows(DataAccessException.class, () -> {
            gameDAO.getGame(9999);
        }, "Fetching a non-existent gameID should throw DataAccessException or your design's approach.");
    }

    @Test
    void listGamesPositive() throws DataAccessException {
        gameDAO.createGame(new GameData(0, "w1", "b1", "NameOne", null));
        gameDAO.createGame(new GameData(0, "w2", "b2", "NameTwo", null));

        List<GameData> all = gameDAO.listGames();
        assertEquals(2, all.size(), "Should list exactly 2 games.");
    }

    @Test
    void updateGamePositive() throws DataAccessException {
        int gameId = gameDAO.createGame(new GameData(0, "w1", "b1", "Original", null));
        GameData updated = new GameData(gameId, "w1", "b1", "RenamedGame", null);

        gameDAO.updateGame(updated);
        GameData fetched = gameDAO.getGame(gameId);
        assertEquals("RenamedGame", fetched.gameName(), "Game name should be updated.");
    }

    @Test
    void updateGameNegativeNoSuchGame() {
        GameData phantom = new GameData(9999, "w", "b", "Phantom", null);
        assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGame(phantom);
        }, "Updating a non-existent game should throw an exception.");
    }

    @Test
    void clearPositive() throws DataAccessException {
        gameDAO.createGame(new GameData(0, "w", "b", "WillClear", null));
        gameDAO.clear();

        List<GameData> all = gameDAO.listGames();
        assertEquals(0, all.size(), "After clear(), no games remain.");
    }
}
