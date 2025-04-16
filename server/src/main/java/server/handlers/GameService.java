package server.handlers;

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

    // Lists all games after verifying authentication
    public List<GameData> listGames(String authToken) throws DataAccessException {
        validateAuth(authToken);
        return gameDAO.listGames();
    }

    // Creates a new game; sets the game name and fresh board using GameData's functions
    public int createGame(String authToken, String gameName) throws DataAccessException {
        validateAuth(authToken);

        if (gameName == null || gameName.trim().isEmpty()) {
            throw new DataAccessException("Error: game name is required");
        }
        ChessGame freshBoard = new ChessGame();
        // Assume a gameID of 0 indicates a new game that will be assigned an ID by the DAO
        GameData newGame = new GameData(0);
        newGame.setGameName(gameName);
        newGame.setChessGame(freshBoard);
        return gameDAO.createGame(newGame);
    }

    // Lets an authenticated player join a game as WHITE or BLACK
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

        if (playerColor.equalsIgnoreCase("WHITE")) {
            game.setWhiteUsername(authData.username());
        } else {  // playerColor is BLACK
            game.setBlackUsername(authData.username());
        }

        gameDAO.updateGame(game);
    }

    // Loads the current state of a game
    public ChessGame loadGame(int gameID) throws DataAccessException {
        // Fetch game data from the DB
        GameData data = gameDAO.getGame(gameID);
        if (data == null) {
            throw new DataAccessException("Game not found: " + gameID);
        }
        // Return a fresh board if no chess game is stored, otherwise return the current game
        return (data.getChessGame() == null) ? new ChessGame() : data.getChessGame();
    }

    // Makes a move in the game and updates the state using GameData's setters
    public void makeMove(int gameID, ChessMove move) throws DataAccessException {
        // Retrieve and update the chess game instance
        ChessGame cg = loadGame(gameID);
        try {
            cg.makeMove(move);
        } catch (Exception e) {
            throw new DataAccessException("Invalid move: " + e.getMessage());
        }

        // Retrieve the GameData instance and update its chess board
        GameData existing = gameDAO.getGame(gameID);
        if (existing == null) {
            throw new DataAccessException("Game not found after move: " + gameID);
        }

        existing.setChessGame(cg);
        gameDAO.updateGame(existing);
    }
}
