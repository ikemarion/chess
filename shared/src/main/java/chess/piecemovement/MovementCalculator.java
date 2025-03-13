package chess.piecemovement;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public interface MovementCalculator {

    static void getMovement(
            ChessBoard board,
            ChessPosition startPosition,
            ChessPiece piece,
            int[][] direction,
            HashSet<ChessMove> validMoves,
            boolean repeatable
    ) {
        for (int[] dir : direction) {
            int newRow = startPosition.getRow();
            int newCol = startPosition.getColumn();

            while (true) {
                newRow += dir[0];
                newCol += dir[1];
                ChessPosition endPos = new ChessPosition(newRow, newCol);

                if (boundaryCheck(endPos)) {
                    if (board.getPiece(endPos) == null) {
                        validMoves.add(new ChessMove(startPosition, endPos, null));
                    } else if (piece.getTeamColor() != board.getPiece(endPos).getTeamColor()) {
                        validMoves.add(new ChessMove(startPosition, endPos, null));
                        break;
                    } else {
                        break;
                    }
                } else {
                    break;
                }

                if (!repeatable) {
                    break;
                }
            }
        }
    }

    static boolean boundaryCheck(ChessPosition position) {
        if (position.getRow() > 8) {
            return false;
        }
        if (position.getColumn() > 8) {
            return false;
        }
        if (position.getRow() < 1) {
            return false;
        }
        if (position.getColumn() < 1) {
            return false;
        }
        return true;
    }
}