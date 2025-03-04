package service;

import dataaccess.AuthDAO;
import dataaccess.GameDAO;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;
import java.util.List;  // ✅ Add this import for List

public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public List<GameData> listGames(String authToken) throws DataAccessException {
        validateAuth(authToken);
        return gameDAO.listGames();
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {
        validateAuth(authToken);

        if (gameName == null || gameName.isEmpty()) {
            throw new DataAccessException("Error: game name required");
        }

        GameData newGame = new GameData(0, null, null, gameName, null);
        return gameDAO.createGame(newGame);
    }

    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        AuthData authData = validateAuth(authToken); // Validate token
        GameData game = gameDAO.getGame(gameID); // Get the game

        if (game == null) {
            throw new DataAccessException("Error: game not found");
        }

        if (playerColor.equalsIgnoreCase("WHITE") && game.whiteUsername() != null) {
            throw new DataAccessException("Error: WHITE already taken");
        }

        if (playerColor.equalsIgnoreCase("BLACK") && game.blackUsername() != null) {
            throw new DataAccessException("Error: BLACK already taken");
        }

        // Update the game with the new player's color
        GameData updatedGame = new GameData(
                game.gameID(),
                playerColor.equalsIgnoreCase("WHITE") ? authData.username() : game.whiteUsername(),
                playerColor.equalsIgnoreCase("BLACK") ? authData.username() : game.blackUsername(),
                game.gameName(),
                game.game()
        );

        gameDAO.updateGame(updatedGame);
    }

    private AuthData validateAuth(String authToken) throws DataAccessException {
        AuthData authData = authDAO.getAuth(authToken);
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return authData;
    }
}