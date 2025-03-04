package dataaccess;

import model.UserData;
import java.util.HashMap;
import java.util.Map;

public class InMemoryUserDAO implements UserDAO {
    private final Map<String, UserData> users = new HashMap<>();

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (users.containsKey(user.username())) {
            throw new DataAccessException("User already exists");
        }
        users.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (!users.containsKey(username)) {
            throw new DataAccessException("User not found");
        }
        return users.get(username);
    }

    @Override
    public boolean authenticateUser(String username, String password) throws DataAccessException {
        UserData user = getUser(username);
        return user.password().equals(password);
    }

    @Override
    public void clear() {
        users.clear();
    }
}
