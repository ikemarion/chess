package chess;

import java.util.Objects;

/**
 * Represents a single square position on a chess board.
 */
public class ChessPosition {

    private final int row;
    private final int col;

    public ChessPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * @return which row this position is in (1-indexed, where 1 is the bottom row)
     */
    public int getRow() {
        return row;
    }

    /**
     * @return which column this position is in (1-indexed, where 1 corresponds to file 'a')
     */
    public int getColumn() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            System.out.println("ChessPosition.equals: Other object is null or not a ChessPosition");
            return false;
        }
        ChessPosition that = (ChessPosition) o;
        boolean rowEquals = this.row == that.row;
        boolean colEquals = this.col == that.col;
        if (!rowEquals || !colEquals) {
            System.out.println("ChessPosition.equals: Mismatch detected:");
            System.out.println("  This position: " + this);
            System.out.println("  That position: " + that);
            System.out.println("  rowEquals: " + rowEquals + ", colEquals: " + colEquals);
        }
        return rowEquals && colEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    /**
     * Creates a ChessPosition from a string.
     * Expects a two-character string (e.g., "e2") where the first character is a file (a-h)
     * and the second character is a rank (1-8).
     *
     * @param posStr the input string.
     * @return a ChessPosition or null if invalid.
     */
    public static ChessPosition fromString(String posStr) {
        if (posStr == null || posStr.length() != 2) {
            return null;
        }
        char fileChar = posStr.charAt(0);
        char rankChar = posStr.charAt(1);
        int col = fileChar - 'a' + 1;
        int row = Character.getNumericValue(rankChar);
        if (col < 1 || col > 8 || row < 1 || row > 8) {
            return null;
        }
        return new ChessPosition(row, col);
    }

    @Override
    public String toString() {
        return "ChessPosition{" + "row=" + row + ", col=" + col + '}';
    }
}