package service;

import chess.ChessGame;
import chess.ChessMove;
import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.GameDAO;
import model.AuthData;
import model.GameData;

import java.util.List;

/**
 * GameService handles creating/joining games, plus
 * storing/reading a ChessGame object in the GameData.
 *
 * It provides methods for:
 *  - listGames (requires valid auth)
 *  - createGame (stores a new ChessGame)
 *  - joinGame (assigns white/black seats)
 *  - loadGame (fetch ChessGame from DB)
 *  - makeMove (actually move a piece on that game)
 *  - leaveGame (optionally remove them from seat)
 *  - resignGame (mark the game as finished, if you want)
 */
public class GameService {
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public GameService(GameDAO gameDAO, AuthDAO authDAO) {
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    /**
     * Simple helper to verify the auth token belongs to a user
     */
    private AuthData validateAuth(String authToken) throws DataAccessException {
        AuthData authData = authDAO.getAuth(authToken);
        if (authData == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        return authData;
    }

    // -----------------------------
    // 1) List Games
    // -----------------------------
    public List<GameData> listGames(String authToken) throws DataAccessException {
        validateAuth(authToken);
        return gameDAO.listGames();
    }

    // -----------------------------
    // 2) Create Game
    // -----------------------------
    public int createGame(String authToken, String gameName) throws DataAccessException {
        validateAuth(authToken);

        if (gameName == null || gameName.trim().isEmpty()) {
            throw new DataAccessException("Error: game name is required");
        }
        // Store a brand-new ChessGame for the initial board
        ChessGame freshBoard = new ChessGame();
        GameData newGame = new GameData(0, null, null, gameName, freshBoard);
        return gameDAO.createGame(newGame);
    }

    // -----------------------------
    // 3) Join Game (white or black)
    // -----------------------------
    public void joinGame(String authToken, String playerColor, int gameID) throws DataAccessException {
        // 1) Validate auth
        AuthData authData = validateAuth(authToken);

        // 2) Get the existing game
        GameData game = gameDAO.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("Game not found, id=" + gameID);
        }

        // 3) Check color validity
        if (!playerColor.equalsIgnoreCase("WHITE") && !playerColor.equalsIgnoreCase("BLACK")) {
            throw new DataAccessException("Error: bad request. Must join as WHITE or BLACK.");
        }

        // 4) Check seat availability
        if (playerColor.equalsIgnoreCase("WHITE") && game.whiteUsername() != null) {
            throw new DataAccessException("Error: WHITE seat is already taken");
        }
        if (playerColor.equalsIgnoreCase("BLACK") && game.blackUsername() != null) {
            throw new DataAccessException("Error: BLACK seat is already taken");
        }

        // 5) Update the seats
        String newWhite = game.whiteUsername();
        String newBlack = game.blackUsername();
        if (playerColor.equalsIgnoreCase("WHITE")) {
            newWhite = authData.username();
        } else {
            newBlack = authData.username();
        }

        // 6) Store back in DB
        GameData updated = new GameData(
                game.gameID(),
                newWhite,
                newBlack,
                game.gameName(),
                game.game()  // Keep same ChessGame object
        );
        gameDAO.updateGame(updated);
    }

    // -----------------------------
    // 4) Load the current ChessGame
    // -----------------------------
    public ChessGame loadGame(int gameID) throws DataAccessException {
        // fetch from DB
        GameData data = gameDAO.getGame(gameID);
        if (data == null) {
            throw new DataAccessException("Game not found: " + gameID);
        }
        // If game() is null for some reason, create a fresh one
        return (data.game() == null) ? new ChessGame() : data.game();
    }

    // -----------------------------
    // 5) Make a Move
    // -----------------------------
    public void makeMove(int gameID, ChessMove move) throws DataAccessException {
        // 1) Load the ChessGame
        ChessGame cg = loadGame(gameID);

        // 2) Attempt the move (may throw if invalid)
        try {
            cg.makeMove(move);
        } catch (Exception e) {
            throw new DataAccessException("Invalid move: " + e.getMessage());
        }

        // 3) Save back to DB
        GameData existing = gameDAO.getGame(gameID);
        if (existing == null) {
            throw new DataAccessException("Game not found after move: " + gameID);
        }
        // same players, same gameName, updated board
        GameData updated = new GameData(
                existing.gameID(),
                existing.whiteUsername(),
                existing.blackUsername(),
                existing.gameName(),
                cg
        );
        gameDAO.updateGame(updated);
    }

    // -----------------------------
    // 6) Leave Game
    // -----------------------------
    public void leaveGame(int gameID, String authToken) throws DataAccessException {
        AuthData authData = validateAuth(authToken);
        String username = authData.username();

        GameData existing = gameDAO.getGame(gameID);
        if (existing == null) {
            // Up to you: throw or ignore
            throw new DataAccessException("Game not found: " + gameID);
        }

        String newWhite = existing.whiteUsername();
        String newBlack = existing.blackUsername();

        // If they were white, remove them
        if (username.equals(newWhite)) {
            newWhite = null;
        }
        // If they were black, remove them
        if (username.equals(newBlack)) {
            newBlack = null;
        }
        // If they were neither, that's okay.

        // Save updated
        GameData updated = new GameData(
                existing.gameID(),
                newWhite,
                newBlack,
                existing.gameName(),
                existing.game()
        );
        gameDAO.updateGame(updated);
    }

    // -----------------------------
    // 7) Resign Game
    // -----------------------------
    public void resignGame(int gameID, String authToken) throws DataAccessException {
        // Possibly mark game as ended. Here’s an example that simply does nothing
        // or you could store a "winner" or "status" in the DB
        AuthData authData = validateAuth(authToken);

        // e.g. do something with the DB
        // For instance, add a "Game Over" or store "resigner" somewhere
        // Not mandatory unless your design requires it
    }
    // ... [rest of the imports and class declaration unchanged]

    // ... [your existing methods]

    // -----------------------------
    // ✅ 8) Get username from auth
    // -----------------------------
    public String getUsernameFromAuth(String authToken) throws DataAccessException {
        return validateAuth(authToken).username();
    }

    // -----------------------------
    // ✅ 9) Get full GameData
    // -----------------------------
    public GameData getGameData(int gameID) throws DataAccessException {
        GameData data = gameDAO.getGame(gameID);
        if (data == null) {
            throw new DataAccessException("Game not found: " + gameID);
        }
        return data;
    }
}