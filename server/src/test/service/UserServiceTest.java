package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.InMemoryAuthDAO;
import dataaccess.InMemoryUserDAO;
import dataaccess.UserDAO;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private UserService userService;
    private UserDAO userDAO;
    private AuthDAO authDAO;

    @BeforeEach
    void setUp() {
        userDAO = new InMemoryUserDAO();
        authDAO = new InMemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    void testRegister_Success() throws DataAccessException {
        var authData = userService.register("newUser", "password", "new@example.com");
        assertNotNull(authData, "AuthData should not be null when registering a new user.");
        assertEquals("newUser", authData.username(), "Usernames should match.");

        var storedUser = userDAO.getUser("newUser");
        assertNotNull(storedUser, "User should now exist in the DAO.");
        assertEquals("new@example.com", storedUser.email(), "Emails should match.");
    }

    @Test
    void testRegister_UserAlreadyExists() throws DataAccessException {
        userDAO.createUser(new UserData("existingUser", "pass", "ex@example.com"));

        assertThrows(DataAccessException.class, () -> {
            userService.register("existingUser", "newpass", "again@example.com");
        }, "Registering an existing user should throw DataAccessException");
    }

    @Test
    void testLogin_Success() throws DataAccessException {
        userService.register("loginUser", "pass123", "login@example.com");

        var authData = userService.login("loginUser", "pass123");
        assertNotNull(authData, "AuthData should not be null on successful login.");
        assertEquals("loginUser", authData.username(), "Usernames should match.");
    }

    @Test
    void testLogin_InvalidCredentials() throws DataAccessException {
        userService.register("wrongUser", "rightPass", "some@example.com");

        assertThrows(DataAccessException.class, () -> {
            userService.login("wrongUser", "badPass");
        }, "Invalid credentials should throw DataAccessException");
    }

    @Test
    void testLogout_InvalidToken() {
        assertThrows(DataAccessException.class, () -> {
            userService.logout("nonExistentToken");
        }, "Logging out with non-existent token should throw DataAccessException.");
    }
}