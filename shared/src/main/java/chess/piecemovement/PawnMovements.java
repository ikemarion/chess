package chess.piecemovement;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public class PawnMovements implements MovementCalculator {

    public static HashSet<ChessMove> getPossibilities(ChessPosition position, ChessBoard board) {
        HashSet<ChessMove> output = new HashSet<>();
        ChessPiece piece = board.getPiece(position);
        if (piece == null) {
            return output;
        }

        ChessGame.TeamColor color = piece.getTeamColor();

        // Pawns move "up" for WHITE, "down" for BLACK
        int pawnDir = (color == ChessGame.TeamColor.WHITE) ? 1 : -1;

        // 1) Straight forward by 1
        int newRow = position.getRow() + pawnDir;
        int newCol = position.getColumn();
        ChessPosition endPos = new ChessPosition(newRow, newCol);

        if (MovementCalculator.boundaryCheck(endPos)
                && board.getPiece(endPos) == null) {

            // Promotion if we reach last rank
            if ((pawnDir == 1 && newRow == 8) || (pawnDir == -1 && newRow == 1)) {
                addPromotionMoves(position, endPos, output);
            } else {
                output.add(new ChessMove(position, endPos, null));
            }
        }

        // 2) Move 2 squares if on starting rank
        if ((pawnDir == 1 && position.getRow() == 2)
                || (pawnDir == -1 && position.getRow() == 7)) {

            int rowAhead1 = position.getRow() + pawnDir;
            int rowAhead2 = position.getRow() + (pawnDir * 2);
            ChessPosition pos1 = new ChessPosition(rowAhead1, newCol);
            ChessPosition pos2 = new ChessPosition(rowAhead2, newCol);

            if (MovementCalculator.boundaryCheck(pos1)
                    && MovementCalculator.boundaryCheck(pos2)
                    && board.getPiece(pos1) == null
                    && board.getPiece(pos2) == null) {

                output.add(new ChessMove(position, pos2, null));
            }
        }

        // 3) Capture diagonally (left)
        newRow = position.getRow() + pawnDir;
        newCol = position.getColumn() - 1;
        endPos = new ChessPosition(newRow, newCol);

        if (MovementCalculator.boundaryCheck(endPos)
                && board.getPiece(endPos) != null
                && board.getPiece(endPos).getTeamColor() != color) {

            // If promotion
            if ((pawnDir == 1 && newRow == 8) || (pawnDir == -1 && newRow == 1)) {
                addPromotionMoves(position, endPos, output);
            } else {
                output.add(new ChessMove(position, endPos, null));
            }
        }

        // 4) Capture diagonally (right)
        newRow = position.getRow() + pawnDir;
        newCol = position.getColumn() + 1;
        endPos = new ChessPosition(newRow, newCol);

        if (MovementCalculator.boundaryCheck(endPos)
                && board.getPiece(endPos) != null
                && board.getPiece(endPos).getTeamColor() != color) {

            if ((pawnDir == 1 && newRow == 8) || (pawnDir == -1 && newRow == 1)) {
                addPromotionMoves(position, endPos, output);
            } else {
                output.add(new ChessMove(position, endPos, null));
            }
        }

        return output;
    }

    /**
     * Helper to add promotion moves for Pawn -> Knight/Rook/Bishop/Queen
     */
    private static void addPromotionMoves(ChessPosition start,
                                          ChessPosition end,
                                          HashSet<ChessMove> out) {
        out.add(new ChessMove(start, end, ChessPiece.PieceType.KNIGHT));
        out.add(new ChessMove(start, end, ChessPiece.PieceType.ROOK));
        out.add(new ChessMove(start, end, ChessPiece.PieceType.BISHOP));
        out.add(new ChessMove(start, end, ChessPiece.PieceType.QUEEN));
    }
}
