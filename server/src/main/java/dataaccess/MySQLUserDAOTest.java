package dataaccess;

import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLUserDAOTest {

    private MySQLUserDAO userDAO;

    @BeforeEach
    void setup() throws DataAccessException {
        DatabaseManager.initDB();

        userDAO = new MySQLUserDAO();
        userDAO.clear();
    }

    @Test
    void createUserPositive() throws DataAccessException {
        UserData user = new UserData("alice", BCrypt.hashpw("pw123", BCrypt.gensalt()), "alice@test.com");
        userDAO.createUser(user);

        UserData fetched = userDAO.getUser("alice");
        assertNotNull(fetched, "User should be inserted and retrievable.");
        assertEquals("alice", fetched.username(), "Username should match.");
        assertEquals("alice@test.com", fetched.email(), "Email should match.");
    }

    @Test
    void createUserNegativeDuplicate() throws DataAccessException {
        userDAO.createUser(new UserData("bob", BCrypt.hashpw("pw", BCrypt.gensalt()), "bob@test.com"));

        assertThrows(DataAccessException.class, () -> {
            userDAO.createUser(new UserData("bob", BCrypt.hashpw("other", BCrypt.gensalt()), "other@test.com"));
        }, "Creating a duplicate user should throw DataAccessException.");
    }

    @Test
    void getUserPositive() throws DataAccessException {
        UserData user = new UserData("charlie", "plainPW", "charlie@test.com");
        userDAO.createUser(user);

        UserData fetched = userDAO.getUser("charlie");
        assertNotNull(fetched, "Should retrieve newly inserted user.");
        assertEquals("charlie", fetched.username());
    }

    @Test
    void getUserNegativeNotFound() throws DataAccessException {
        UserData fetched = userDAO.getUser("missing");
        assertNull(fetched, "getUser on a non-existent user should return null (or handle how your DAO is designed).");
    }

    @Test
    void authenticateUserPositive() throws DataAccessException {
        userDAO.createUser(new UserData("dave", "secret", "dave@test.com"));

        boolean auth = userDAO.authenticateUser("dave", "secret");
        assertTrue(auth, "Should return true for correct credentials.");
    }

    @Test
    void authenticateUserNegativeWrongPassword() throws DataAccessException {
        userDAO.createUser(new UserData("eve", "abc123", "eve@test.com"));

        boolean auth = userDAO.authenticateUser("eve", "wrong");
        assertFalse(auth, "Should return false for wrong password.");
    }

    @Test
    void clearPositive() throws DataAccessException {
        userDAO.createUser(new UserData("frank", "pword", "frank@test.com"));
        userDAO.clear();

        UserData fetched = userDAO.getUser("frank");
        assertNull(fetched, "After clear(), user should no longer be found.");
    }
}