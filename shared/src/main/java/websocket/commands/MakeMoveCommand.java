package websocket.commands;

import chess.ChessMove;

/**
 * Extends the base UserGameCommand to include a ChessMove,
 * for the MAKE_MOVE case.
 */
public class MakeMoveCommand extends UserGameCommand {

    private ChessMove move;  // or separate fields like startRow, startCol, etc.

    public MakeMoveCommand(String authToken, Integer gameID, ChessMove move) {
        super(CommandType.MAKE_MOVE, authToken, gameID);
        this.move = move;
    }

    // no-arg constructor for GSON
    public MakeMoveCommand() {
        super(CommandType.MAKE_MOVE, null, null);
    }

    public ChessMove getMove() {
        return move;
    }

    public void setMove(ChessMove move) {
        this.move = move;
    }
}