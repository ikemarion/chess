package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLAuthDAOTest {

    private MySQLAuthDAO authDAO;

    @BeforeEach
    void setup() throws DataAccessException {
        DatabaseManager.initDB();
        authDAO = new MySQLAuthDAO();
        authDAO.clear();
    }

    @Test
    void createAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("tokenXYZ", "alice");
        authDAO.createAuth(auth);

        AuthData fetched = authDAO.getAuth("tokenXYZ");
        assertNotNull(fetched, "Auth token should be inserted and retrievable.");
        assertEquals("alice", fetched.username());
    }

    @Test
    void createAuthNegativeDuplicate() throws DataAccessException {
        authDAO.createAuth(new AuthData("sameToken", "bob"));
        assertThrows(DataAccessException.class, () -> {
            authDAO.createAuth(new AuthData("sameToken", "anotherUser"));
        }, "Inserting a duplicate token should throw an exception.");
    }

    @Test
    void getAuthPositive() throws DataAccessException {
        authDAO.createAuth(new AuthData("t123", "charlie"));
        AuthData fetched = authDAO.getAuth("t123");
        assertNotNull(fetched);
        assertEquals("charlie", fetched.username());
    }

    @Test
    void getAuthNegativeNotFound() {
        assertThrows(DataAccessException.class, () -> {
            authDAO.getAuth("nopeToken");
        }, "Fetching a non-existent auth token should throw or return null (depending on your design).");
    }

    @Test
    void deleteAuthPositive() throws DataAccessException {
        authDAO.createAuth(new AuthData("toDelete", "dave"));
        authDAO.deleteAuth("toDelete");

        // If getAuth throws or returns null, handle accordingly
        assertThrows(DataAccessException.class, () -> {
            authDAO.getAuth("toDelete");
        }, "Should no longer find the token after deletion.");
    }

    @Test
    void deleteAuthNegativeNotFound() {
        assertThrows(DataAccessException.class, () -> {
            authDAO.deleteAuth("missing");
        }, "Deleting a non-existent token should throw an exception.");
    }

    @Test
    void clearPositive() throws DataAccessException {
        authDAO.createAuth(new AuthData("abc", "userX"));
        authDAO.clear();

        assertThrows(DataAccessException.class, () -> {
            authDAO.getAuth("abc");
        }, "After clear(), no tokens remain.");
    }
}