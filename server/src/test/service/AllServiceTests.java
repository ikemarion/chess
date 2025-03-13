package service;

import dataaccess.*;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AllServiceTests {

    @Nested
    class UserServiceTest {
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
        void testRegisterSuccess() throws DataAccessException {
            var authData = userService.register("newUser", "password", "new@example.com");
            assertNotNull(authData, "AuthData should not be null on successful registration.");
            assertEquals("newUser", authData.username(), "Username should match.");

            UserData stored = userDAO.getUser("newUser");
            assertNotNull(stored, "User should be created in the DAO.");
            assertEquals("new@example.com", stored.email(), "Email should match stored user.");
        }

        @Test
        void testRegisterAlreadyExists() throws DataAccessException {
            userDAO.createUser(new UserData("existingUser", "pw", "ex@example.com"));
            assertThrows(DataAccessException.class, () -> {
                userService.register("existingUser", "newpass", "again@example.com");
            }, "Registering a duplicate user should throw an exception.");
        }

        @Test
        void testLoginSuccess() throws DataAccessException {
            userService.register("loginUser", "pass123", "login@example.com");
            var authData = userService.login("loginUser", "pass123");
            assertNotNull(authData, "Should return AuthData on successful login.");
            assertEquals("loginUser", authData.username(), "Username should match.");
        }

        @Test
        void testLoginInvalidCredentials() throws DataAccessException {
            userService.register("userX", "secret", "x@example.com");
            assertThrows(DataAccessException.class, () -> {
                userService.login("userX", "wrong");
            }, "Logging in with wrong password should fail.");
        }

        @Test
        void testLogoutSuccess() throws DataAccessException {
            var authData = userService.register("logoutUser", "pword", "mail@example.com");
            String token = authData.authToken();
            userService.logout(token);
            assertNull(authDAO.getAuth(token), "Auth token should be removed after logout.");
        }

        @Test
        void testLogoutInvalidToken() {
            assertThrows(DataAccessException.class, () -> {
                userService.logout("fakeToken");
            }, "Logging out with non-existent token should throw DataAccessException.");
        }
    }

    @Nested
    class GameServiceTest {
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
        void testCreateGameSuccess() throws DataAccessException {
            String validToken = "token123";
            authDAO.createAuth(new AuthData(validToken, "userOne"));
            int gameID = gameService.createGame(validToken, "TestGame");
            assertTrue(gameID > 0, "Game ID should be a positive integer.");

            GameData storedGame = gameDAO.getGame(gameID);
            assertNotNull(storedGame, "Game should be stored.");
            assertEquals("TestGame", storedGame.gameName(), "Game name should match.");
        }

        @Test
        void testCreateGameUnauthorized() {
            assertThrows(DataAccessException.class, () -> {
                gameService.createGame("invalidToken", "ChessGame");
            }, "Creating a game with an invalid token should throw DataAccessException.");
        }

        @Test
        void testListGamesSuccess() throws DataAccessException {
            authDAO.createAuth(new AuthData("listToken", "lister"));
            gameDAO.createGame(new GameData(1, "GameOne", null, null, null));
            gameDAO.createGame(new GameData(2, "GameTwo", null, null, null));
            List<GameData> games = gameService.listGames("listToken");
            assertEquals(2, games.size(), "Should list two games.");
        }

        @Test
        void testListGamesUnauthorized() {
            assertThrows(DataAccessException.class, () -> {
                gameService.listGames("nopeToken");
            }, "Listing games with an invalid token should throw an exception.");
        }

        @Test
        void testJoinGameSuccess() throws DataAccessException {
            authDAO.createAuth(new AuthData("joinToken", "joiner"));
            gameDAO.createGame(new GameData(1, "JoinableGame", null, null, null));
            gameService.joinGame("joinToken", "WHITE", 1);

            GameData updated = gameDAO.getGame(1);
            assertEquals("joiner", updated.whiteUsername(), "joiner should occupy WHITE spot.");
        }

        @Test
        void testJoinGameColorTaken() throws DataAccessException {
            authDAO.createAuth(new AuthData("firstToken", "userOne"));
            authDAO.createAuth(new AuthData("secondToken", "userTwo"));
            gameDAO.createGame(new GameData(1, "ConflictGame", null, null, null));
            gameService.joinGame("firstToken", "WHITE", 1);

            assertThrows(DataAccessException.class, () -> {
                gameService.joinGame("secondToken", "WHITE", 1);
            }, "Joining a color that's already taken should throw an exception.");
        }

        @Test
        void testJoinGameInvalidToken() throws DataAccessException {
            gameDAO.createGame(new GameData(1, "InvalidTokenGame", null, null, null));
            assertThrows(DataAccessException.class, () -> {
                gameService.joinGame("bogusToken", "WHITE", 1);
            }, "Joining with an invalid token should throw an exception.");
        }

        @Test
        void testJoinGameInvalidColor() throws DataAccessException {
            authDAO.createAuth(new AuthData("colorToken", "colorUser"));
            gameDAO.createGame(new GameData(1, "BadColorGame", null, null, null));

            assertThrows(DataAccessException.class, () -> {
                gameService.joinGame("colorToken", "RED", 1);
            }, "Joining with an invalid color should throw an exception.");
        }

        @Test
        void testJoinGameNonexistentGame() throws DataAccessException {
            authDAO.createAuth(new AuthData("gameToken", "someUser"));
            assertThrows(DataAccessException.class, () -> {
                gameService.joinGame("gameToken", "WHITE", 999);
            }, "Joining a nonexistent game should throw an exception.");
        }
    }

    @Nested
    class DatabaseServiceTest {
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
            userDAO.createUser(new UserData("user1", "pw", "u1@example.com"));
            gameDAO.createGame(new GameData(10, "TestGame", null, null, null));
            authDAO.createAuth(new AuthData("tokenABC", "user1"));

            databaseService.clear();

            assertNull(userDAO.getUser("user1"), "Users should be cleared.");
            assertEquals(0, gameDAO.listGames().size(), "Games should be cleared.");
            assertNull(authDAO.getAuth("tokenABC"), "Auth tokens should be cleared.");
        }
    }
}