package service;

import dataaccess.GameDAO;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import model.GameData;
import model.AuthData;
import java.util.List;

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    private AuthData validateAuth(String authToken) throws DataAccessException {
        AuthData authData = authDAO.getAuth(authToken);
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return authData;
    }

    public List<GameData> listGames(String authToken) throws DataAccessException {
        validateAuth(authToken);
        return gameDAO.listGames();
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        validateAuth(authToken);

        if (gameName == null || gameName.trim().isEmpty()) {
            throw new DataAccessException("Error: game name is required");
        }

        GameData newGame = new GameData(0, null, null, gameName, null);
        return gameDAO.createGame(newGame);
    }

    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        AuthData authData = validateAuth(authToken);
        GameData game = gameDAO.getGame(gameID);

        if (!playerColor.equalsIgnoreCase("WHITE") && !playerColor.equalsIgnoreCase("BLACK")) {
            throw new DataAccessException("Error: bad request");
        }

        if (playerColor.equalsIgnoreCase("WHITE") && game.whiteUsername() != null) {
            throw new DataAccessException("Error: already taken");
        }
        if (playerColor.equalsIgnoreCase("BLACK") && game.blackUsername() != null) {
            throw new DataAccessException("Error: already taken");
        }

        GameData updatedGame = new GameData(
                game.gameID(),
                playerColor.equalsIgnoreCase("WHITE") ? authData.username() : game.whiteUsername(),
                playerColor.equalsIgnoreCase("BLACK") ? authData.username() : game.blackUsername(),
                game.gameName(),
                game.game()
        );

        gameDAO.updateGame(updatedGame);
    }
}