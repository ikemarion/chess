package chess;

import chess.piecemovement.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor shade;
    private final ChessPiece.PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.shade = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return shade;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {

        if (type == PieceType.BISHOP) {
            return BishopMovements.getPossibilities(myPosition, board);
        } else if (type == PieceType.ROOK) {
            return RookMovements.getPossibilities(myPosition, board);
        } else if (type == PieceType.QUEEN) {
            return QueenMovements.getPossibilities(myPosition, board);
        } else if (type == PieceType.KNIGHT) {
            return KnightMovements.getPossibilities(myPosition, board);
        } else if (type == PieceType.KING) {
            return KingMovements.getPossibilities(myPosition, board);
        } else if (type == PieceType.PAWN) {
            return PawnMovements.getPossibilities(myPosition, board);
        }
        return new HashSet<ChessMove>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece piece = (ChessPiece) o;
        return shade == piece.shade && type == piece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shade, type);
    }
}


