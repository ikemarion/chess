package dataaccess;

import model.UserData;
import java.util.HashMap;
import java.util.Map;

public class InMemoryUserDAO implements UserDAO {
    private Map<String, UserData> users = new HashMap<>();

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("User already exists");  // Handle existing user case
        }
        users.put(user.username(), user);
    }

    @Override
    public boolean authenticateUser(String username, String password) throws DataAccessException {
        UserData user = users.get(username);
        return user != null && user.password().equals(password);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        return users.get(username);
    }

    // Implement the clear() method to clear all users from the in-memory map
    @Override
    public void clear() {
        users.clear();  // Clears the in-memory user data
    }
}