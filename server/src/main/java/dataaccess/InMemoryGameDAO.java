package dataaccess;

import model.GameData;
import java.util.*;

public class InMemoryGameDAO implements GameDAO {
    private final Map<Integer, GameData> games = new HashMap<>();
    private int nextGameID = 1;

    @Override
    public int createGame(GameData game) {
        int gameID = nextGameID++;
        GameData newGame = new GameData(gameID);
        games.put(gameID, newGame);
        return gameID;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        if (!games.containsKey(gameID)) {
            throw new DataAccessException("Game not found");
        }
        return games.get(gameID);
    }

    @Override
    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        if (!games.containsKey(game.getGameID())) {
            throw new DataAccessException("Game not found");
        }
        games.put(game.getGameID(), game);
    }

    @Override
    public void clear() {
        games.clear();
        nextGameID = 1;
    }
}