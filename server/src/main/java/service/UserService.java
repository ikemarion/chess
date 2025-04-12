package service;

import dataaccess.AuthDAO;
import dataaccess.UserDAO;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import java.util.UUID;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public AuthData register(String username, String password, String email) throws DataAccessException {
        if (username == null || password == null || email == null ||
                username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }
        if (userDAO.getUser(username) != null) {
            throw new DataAccessException("Error: User already exists");
        }

        UserData newUser = new UserData(username, password, email);
        userDAO.createUser(newUser);

        String authToken = UUID.randomUUID().toString();
        AuthData authData = new AuthData(authToken, username);
        authDAO.createAuth(authData);

        return authData;
    }

    public AuthData login(String username, String password) throws DataAccessException {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            throw new DataAccessException("Error: bad request");
        }
        if (!userDAO.authenticateUser(username, password)) {
            throw new DataAccessException("Error: unauthorized");
        }

        String authToken = UUID.randomUUID().toString();
        AuthData authData = new AuthData(authToken, username);
        authDAO.createAuth(authData);

        return authData;
    }

    public void logout(String authToken) throws DataAccessException {
        AuthData authData = authDAO.getAuth(authToken);
        if (authData == null) {
            throw new DataAccessException("Invalid authentication token");
        }
        authDAO.deleteAuth(authToken);
    }
}