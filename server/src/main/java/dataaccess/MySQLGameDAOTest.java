package dataaccess;

import model.GameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        assertTrue(newID > 0, "Should return a generated gameID > 0.");

        GameData fetched = gameDAO.getGame(newID);
        assertNotNull(fetched, "GameData should be retrievable after creation.");
        assertEquals("TestGame", fetched.gameName(), "Game name should match inserted value.");
    }


    @Test
    void createGameNegativeNullName() {
        GameData invalidGame = new GameData(0, "white", "black", null, null);

        assertThrows(DataAccessException.class, () -> {
            gameDAO.createGame(invalidGame);
        }, "Creating a game with a null name (NOT NULL in DB) should throw an exception.");
    }

    @Test
    void getGamePositive() throws DataAccessException {
        int createdID = gameDAO.createGame(new GameData(0, "wPlayer", "bPlayer", "CoolGame", null));
        GameData fetched = gameDAO.getGame(createdID);

        assertNotNull(fetched, "Should retrieve the newly inserted game.");
        assertEquals("CoolGame", fetched.gameName());
        assertEquals("wPlayer", fetched.whiteUsername());
    }

    @Test
    void getGameNegativeNotFound() {
        assertThrows(DataAccessException.class, () -> {
            gameDAO.getGame(9999);
        }, "Fetching a non-existent gameID should throw an exception.");
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
        int gameId = gameDAO.createGame(new GameData(0, "whiteGuy", "blackGuy", "OriginalName", null));
        GameData updatedGame = new GameData(gameId, "whiteGuy", "blackGuy", "RenamedGame", null);

        gameDAO.updateGame(updatedGame);
        GameData fetched = gameDAO.getGame(gameId);
        assertEquals("RenamedGame", fetched.gameName(), "Game name should be updated in DB.");
    }

    @Test
    void updateGameNegativeNoSuchGame() {
        GameData phantom = new GameData(9999, "wTeam", "bTeam", "PhantomGame", null);
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