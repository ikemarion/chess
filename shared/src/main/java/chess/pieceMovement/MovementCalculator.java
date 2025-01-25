package chess.pieceMovement;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;

public interface MovementCalculator {
    public final ChessPosition start;
    static void Movie(ChessBoard board, ChessPosition position, ChessPiece piece)
        this.start = position;
}
