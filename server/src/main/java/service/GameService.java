package service;

import chess.ChessGame;
import chess.ChessMove;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;

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
        ChessGame freshBoard = new ChessGame();
        GameData newGame = new GameData(0);
        newGame.setGameName(gameName);
        newGame.setChessGame(freshBoard);
        return gameDAO.createGame(newGame);
    }

    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        AuthData authData = validateAuth(authToken);

        GameData game = gameDAO.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found, id=" + gameID);
        }

        if (!playerColor.equalsIgnoreCase("WHITE") && !playerColor.equalsIgnoreCase("BLACK")) {
            throw new DataAccessException("Error: bad request. Must join as WHITE or BLACK.");
        }

        if (playerColor.equalsIgnoreCase("WHITE") && game.getWhiteUsername() != null) {
            throw new DataAccessException("Error: WHITE seat is already taken");
        }
        if (playerColor.equalsIgnoreCase("BLACK") && game.getBlackUsername() != null) {
            throw new DataAccessException("Error: BLACK seat is already taken");
        }

        // Update the game object directly with the player's username.
        if (playerColor.equalsIgnoreCase("WHITE")) {
            game.setWhiteUsername(authData.username());
        } else {  // playerColor is BLACK
            game.setBlackUsername(authData.username());
        }

        // Update the game in the database.
        gameDAO.updateGame(game);
    }

    public ChessGame loadGame(int gameID) throws DataAccessException {
        // Fetch the game data from the database.
        GameData data = gameDAO.getGame(gameID);
        if (data == null) {
            throw new DataAccessException("Game not found: " + gameID);
        }
        return (data.getChessGame() == null) ? new ChessGame() : data.getChessGame();
    }

    public void makeMove(int gameID, ChessMove move) throws DataAccessException {
        ChessGame cg = loadGame(gameID);

        try {
            cg.makeMove(move);
        } catch (Exception e) {
            throw new DataAccessException("Invalid move: " + e.getMessage());
        }

        // Retrieve the existing game data.
        GameData existing = gameDAO.getGame(gameID);
        if (existing == null) {
            throw new DataAccessException("Game not found after move: " + gameID);
        }

        // Update the chess game state in the existing game data.
        existing.setChessGame(cg);
        gameDAO.updateGame(existing);
    }
}