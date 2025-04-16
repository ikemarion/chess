package chess;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Represents moving a chess piece on a chessboard.
 */
public class ChessMove {
    // Bind "startPosition" from JSON to the internal 'start' field.
    @SerializedName("startPosition")
    private final ChessPosition start;

    // Bind "endPosition" from JSON to the internal 'end' field.
    @SerializedName("endPosition")
    private final ChessPosition end;

    // Bind "promotionPiece" from JSON to 'promo'
    @SerializedName("promotionPiece")
    private final ChessPiece.PieceType promo;

    // Constructor used by GSON.
    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.start = startPosition;
        this.end = endPosition;
        this.promo = promotionPiece;
    }

    /**
     * @return ChessPosition of starting location.
     */
    public ChessPosition getStartPosition() {
        return start;
    }

    /**
     * @return ChessPosition of ending location.
     */
    public ChessPosition getEndPosition() {
        return end;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this move.
     *
     * @return Type of piece for promotion, or null if no promotion.
     */
    public ChessPiece.PieceType getPromotionPiece() {
        return promo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            System.out.println("ChessMove.equals: Other object is null or not a ChessMove");
            return false;
        }
        ChessMove that = (ChessMove) o;
        boolean startEquals = Objects.equals(this.start, that.start);
        boolean endEquals = Objects.equals(this.end, that.end);
        boolean promoEquals = this.promo == that.promo;
        if (!startEquals || !endEquals || !promoEquals) {
            System.out.println("ChessMove.equals: Mismatch detected:");
            System.out.println("  This move: " + this);
            System.out.println("  That move: " + that);
            System.out.println("  startEquals: " + startEquals + ", endEquals: " + endEquals + ", promoEquals: " + promoEquals);
        }
        return startEquals && endEquals && promoEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, promo);
    }

    @Override
    public String toString() {
        return "ChessMove{start=" + getStartPosition() +
                ", end=" + getEndPosition() +
                ", promo=" + getPromotionPiece() + "}";
    }
}