package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Manages a chess game, making moves on a board.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor turn;
    private boolean resigned;  // new field to track if a player resigned

    public ChessGame() {
        board = new ChessBoard();
        setTeamTurn(TeamColor.WHITE);
        board.resetBoard();
        resigned = false;
    }

    public TeamColor getTeamTurn() {
        return turn;
    }

    public void setTeamTurn(TeamColor team) {
        turn = team;
    }

    public enum TeamColor {
        WHITE, BLACK
    }

    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return new HashSet<>();
        }
        HashSet<ChessMove> valid = new HashSet<>();
        for (ChessMove move : piece.pieceMoves(board, startPosition)) {
            if (isSafeMove(move)) {
                valid.add(move);
            }
        }
        return valid;
    }

    private boolean isSafeMove(ChessMove move) {
        ChessPiece startPiece = board.getPiece(move.getStartPosition());
        ChessPiece endPiece = board.getPiece(move.getEndPosition());

        board.addPiece(move.getEndPosition(), startPiece);
        board.addPiece(move.getStartPosition(), null);

        boolean safe = !isInCheck(startPiece.getTeamColor());

        board.addPiece(move.getStartPosition(), startPiece);
        board.addPiece(move.getEndPosition(), endPiece);
        return safe;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());
        // Use the Collection returned by validMoves instead of casting to a List.
        Collection<ChessMove> validMovesFromPosition = validMoves(move.getStartPosition());
        System.out.println("Number of valid moves from " + move.getStartPosition() + ": " + validMovesFromPosition.size());
        boolean isValid = validMovesFromPosition.contains(move);
        boolean correctTurn = (piece != null && piece.getTeamColor() == turn);

        // Logging diagnostic information
        System.out.println("=== Attempting move: " + move);
        System.out.println("Piece at start (" + move.getStartPosition() + "): " + piece);
        System.out.println("Current turn: " + turn);
        System.out.println("Submitted move: " + move);
        System.out.println("Valid moves from " + move.getStartPosition() + ":");
        for (ChessMove validMove : validMovesFromPosition) {
            System.out.println("  " + validMove + " -> equals submitted? " + validMove.equals(move));
        }
        System.out.println("Is move valid? " + isValid);
        System.out.println("Is correct turn? " + correctTurn);

        if (isValid && correctTurn) {
            if (move.getPromotionPiece() != null) {
                board.addPiece(move.getEndPosition(), new ChessPiece(turn, move.getPromotionPiece()));
            } else {
                board.addPiece(move.getEndPosition(), piece);
            }
            board.addPiece(move.getStartPosition(), null);
            toggleTurn();
        } else {
            System.err.println("Move rejected. isValid: " + isValid + ", correctTurn: " + correctTurn);
            throw new InvalidMoveException();
        }
    }

    private void toggleTurn() {
        if (turn == TeamColor.BLACK) {
            setTeamTurn(TeamColor.WHITE);
        } else {
            setTeamTurn(TeamColor.BLACK);
        }
    }

    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPos = findKing(teamColor);
        if (kingPos == null) {
            return false;
        }

        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece == null || piece.getTeamColor() == teamColor) {
                    continue;
                }
                for (ChessMove move : piece.pieceMoves(board, new ChessPosition(x, y))) {
                    if (move.getEndPosition().equals(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }
        return !hasValidMoves(teamColor);
    }

    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        return !hasValidMoves(teamColor);
    }

    private boolean hasValidMoves(TeamColor teamColor) {
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece != null && piece.getTeamColor() == teamColor
                        && !validMoves(new ChessPosition(x, y)).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public ChessPosition findKing(TeamColor color) {
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece p = board.getPiece(new ChessPosition(x, y));
                if (p != null && p.getTeamColor() == color
                        && p.getPieceType() == ChessPiece.PieceType.KING) {
                    return new ChessPosition(x, y);
                }
            }
        }
        return null;
    }

    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    public ChessBoard getBoard() {
        return board;
    }

    /**
     * Returns true if the game has ended due to resignation, checkmate, or stalemate.
     */
    public boolean isEndGame() {
        return resigned ||
                isInCheckmate(TeamColor.WHITE) || isInCheckmate(TeamColor.BLACK) ||
                isInStalemate(TeamColor.WHITE) || isInStalemate(TeamColor.BLACK);
    }

    /**
     * Marks the game as resigned, effectively ending the game.
     */
    public void setResigned() {
        resigned = true;
    }

    /**
     * Returns true if the game has been marked as resigned.
     */
    public boolean isResigned() {
        return resigned;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame other = (ChessGame) o;
        return Objects.equals(board, other.board) &&
                turn == other.turn &&
                resigned == other.resigned;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, turn, resigned);
    }

    @Override
    public String toString() {
        return "ChessGame{board=" + board + ", turn=" + turn + ", resigned=" + resigned + "}";
    }
}