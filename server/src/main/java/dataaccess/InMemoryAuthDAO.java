package dataaccess;

import model.AuthData;
import java.util.HashMap;
import java.util.Map;

public class InMemoryAuthDAO implements AuthDAO {
    private final Map<String, AuthData> authTokens = new HashMap<>();

    @Override
    public void createAuth(AuthData auth) {
        authTokens.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        // Return null if the token doesn't exist
        return authTokens.get(authToken);
        // This automatically returns null if the key isn't in the map
    }


    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (!authTokens.containsKey(authToken)) {
            throw new DataAccessException("Invalid authentication token");
        }
        authTokens.remove(authToken);
    }

    @Override
    public void clear() {
        authTokens.clear();
    }
}