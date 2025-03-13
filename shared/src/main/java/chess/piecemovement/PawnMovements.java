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

        // 1) Move forward by 1
        int newRow = position.getRow() + pawnDir;
        int newCol = position.getColumn();
        ChessPosition endPos = new ChessPosition(newRow, newCol);

        if (MovementCalculator.boundaryCheck(endPos) && board.getPiece(endPos) == null) {
            // Promotion if we reach last rank
            if (reachedPromotionRank(pawnDir, newRow)) {
                addPromotionMoves(position, endPos, output);
            } else {
                output.add(new ChessMove(position, endPos, null));
            }
        }

        // 2) Move forward by 2 if on starting rank
        maybeMoveTwoSquares(position, board, pawnDir, output);

        // 3) Capture diagonally left
        tryPawnCapture(position, board, pawnDir,
                position.getRow() + pawnDir, position.getColumn() - 1, output);

        // 4) Capture diagonally right
        tryPawnCapture(position, board, pawnDir,
                position.getRow() + pawnDir, position.getColumn() + 1, output);

        return output;
    }

    /**
     * Checks if row is last rank for the given pawnDir.
     */
    private static boolean reachedPromotionRank(int pawnDir, int row) {
        return (pawnDir == 1 && row == 8) || (pawnDir == -1 && row == 1);
    }

    /**
     * Attempt to move forward by 2 squares if on the starting rank.
     */
    private static void maybeMoveTwoSquares(ChessPosition position,
                                            ChessBoard board,
                                            int pawnDir,
                                            HashSet<ChessMove> output) {
        int row = position.getRow();
        int col = position.getColumn();

        // White can move 2 from row = 2, Black can move 2 from row = 7
        if ((pawnDir == 1 && row == 2) || (pawnDir == -1 && row == 7)) {
            int rowAhead1 = row + pawnDir;
            int rowAhead2 = row + (pawnDir * 2);
            ChessPosition pos1 = new ChessPosition(rowAhead1, col);
            ChessPosition pos2 = new ChessPosition(rowAhead2, col);

            if (MovementCalculator.boundaryCheck(pos1)
                    && MovementCalculator.boundaryCheck(pos2)
                    && board.getPiece(pos1) == null
                    && board.getPiece(pos2) == null) {

                output.add(new ChessMove(position, pos2, null));
            }
        }
    }

    /**
     * Attempt to capture diagonally. If there's an opponent piece, add capture or promotion.
     */
    private static void tryPawnCapture(ChessPosition startPos,
                                       ChessBoard board,
                                       int pawnDir,
                                       int targetRow,
                                       int targetCol,
                                       HashSet<ChessMove> output) {

        if (!MovementCalculator.boundaryCheck(new ChessPosition(targetRow, targetCol))) {
            return;
        }
        ChessPiece piece = board.getPiece(startPos);
        ChessPiece target = board.getPiece(new ChessPosition(targetRow, targetCol));
        if (piece == null || target == null) {
            return;
        }
        // Capture if opposing color
        if (target.getTeamColor() != piece.getTeamColor()) {
            ChessPosition endPos = new ChessPosition(targetRow, targetCol);
            if (reachedPromotionRank(pawnDir, targetRow)) {
                addPromotionMoves(startPos, endPos, output);
            } else {
                output.add(new ChessMove(startPos, endPos, null));
            }
        }
    }

    /**
     * Helper to add promotion moves for Pawn -> Knight/Rook/Bishop/Queen.
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