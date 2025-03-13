package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests UserService methods directly (no HTTP).
 * Covers register, login, logout, and checks that DAOs are exercised.
 */
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

    /**
     * Positive test: Register a new user successfully.
     */
    @Test
    void testRegisterSuccess() throws DataAccessException {
        var authData = userService.register("newUser", "password", "new@example.com");
        assertNotNull(authData, "AuthData should not be null on successful registration.");
        assertEquals("newUser", authData.username(), "Username should match.");

        // Confirm user is in userDAO
        UserData storedUser = userDAO.getUser("newUser");
        assertNotNull(storedUser, "User should be created in the DAO.");
        assertEquals("new@example.com", storedUser.email(), "Email should match stored user.");
    }

    /**
     * Negative test: Register a user who already exists.
     */
    @Test
    void testRegisterAlreadyExists() throws DataAccessException {
        userDAO.createUser(new UserData("existingUser", "pw", "ex@example.com"));
        // Attempt to register same username
        assertThrows(DataAccessException.class, () -> {
            userService.register("existingUser", "anotherPass", "again@example.com");
        }, "Registering a duplicate user should throw an exception.");
    }

    /**
     * Positive test: Login with correct credentials.
     */
    @Test
    void testLoginSuccess() throws DataAccessException {
        userService.register("loginUser", "pass123", "login@example.com");
        var authData = userService.login("loginUser", "pass123");
        assertNotNull(authData, "Should return AuthData on successful login.");
        assertEquals("loginUser", authData.username(), "Username should match.");
    }

    /**
     * Negative test: Login with bad credentials.
     */
    @Test
    void testLoginInvalidCredentials() throws DataAccessException {
        userService.register("userX", "secret", "x@example.com");
        // Wrong password
        assertThrows(DataAccessException.class, () -> {
            userService.login("userX", "wrong");
        }, "Logging in with wrong password should fail.");
    }

    /**
     * Positive test: Logout with valid token.
     */
    @Test
    void testLogoutSuccess() throws DataAccessException {
        var authData = userService.register("logoutUser", "pword", "mail@example.com");
        String token = authData.authToken();

        userService.logout(token);

        // Confirm token is removed
        assertNull(authDAO.getAuth(token), "Auth token should be removed after logout.");
    }

    /**
     * Negative test: Logout with invalid token.
     */
    @Test
    void testLogoutInvalidToken() {
        assertThrows(DataAccessException.class, () -> {
            userService.logout("fakeToken");
        }, "Logging out with non-existent token should throw DataAccessException.");
    }
}