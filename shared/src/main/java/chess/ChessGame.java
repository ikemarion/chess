package chess;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * Manages a chess game, making moves on a board.
 *
 * Note: You can add to this class, but you may not alter
 * the signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor turn;

    public ChessGame() {
        board = new ChessBoard();
        setTeamTurn(TeamColor.WHITE);
        board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return turn;
    }

    /**
     * Sets which team's turn it is.
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        turn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves, or empty if no piece at startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return new HashSet<>();
        }
        HashSet<ChessMove> valid = new HashSet<>();
        HashSet<ChessMove> allMoves = new HashSet<>(piece.pieceMoves(board, startPosition));

        for (ChessMove move : allMoves) {
            ChessPiece startPiece = board.getPiece(startPosition);
            ChessPiece endPiece = board.getPiece(move.getEndPosition());

            // Temporarily make the move
            board.addPiece(move.getEndPosition(), startPiece);
            board.addPiece(move.getStartPosition(), null);

            // Check if this leaves the mover in check
            if (!isInCheck(startPiece.getTeamColor())) {
                valid.add(move);
            }

            // Undo the move
            board.addPiece(move.getStartPosition(), startPiece);
            board.addPiece(move.getEndPosition(), endPiece);
        }
        return valid;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPiece piece = board.getPiece(move.getStartPosition());
        boolean isValid = validMoves(move.getStartPosition()).contains(move);
        boolean correctTurn = (piece != null && piece.getTeamColor() == turn);

        if (isValid && correctTurn) {
            if (move.getPromotionPiece() != null) {
                board.addPiece(move.getEndPosition(),
                        new ChessPiece(turn, move.getPromotionPiece()));
            } else {
                board.addPiece(move.getEndPosition(), piece);
            }
            board.addPiece(move.getStartPosition(), null);

            // Toggle turn
            if (turn == TeamColor.BLACK) {
                setTeamTurn(TeamColor.WHITE);
            } else {
                setTeamTurn(TeamColor.BLACK);
            }
        } else {
            throw new InvalidMoveException();
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check
     * @return True if the specified team is in check, otherwise false
     */
    public boolean isInCheck(TeamColor teamColor) {
        // For each opposing piece, see if it can capture that team's king
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece == null || piece.getTeamColor() == teamColor) {
                    continue;
                }
                for (ChessMove move
                        : piece.pieceMoves(board, new ChessPosition(x, y))) {
                    ChessPosition kingPos = findKing(teamColor);
                    if (kingPos != null && move.getEndPosition().equals(kingPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }
        // If there's any valid move for this team, it's not checkmate
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }
                if (!validMoves(new ChessPosition(x, y)).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate
     *
     * @param teamColor which team to check
     * @return True if the specified team is in stalemate (no valid moves
     * and not in check)
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        // If there's any valid move for this team, it's not stalemate
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }
                if (!validMoves(new ChessPosition(x, y)).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public ChessPosition findKing(TeamColor color) {
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece p = board.getPiece(new ChessPosition(x, y));
                if (p != null
                        && p.getTeamColor() == color
                        && p.getPieceType() == ChessPiece.PieceType.KING) {
                    return new ChessPosition(x, y);
                }
            }
        }
        return null;
    }

    /**
     * Sets this game's chessboard
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame other = (ChessGame) o;
        return Objects.equals(board, other.board)
                && turn == other.turn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, turn);
    }

    @Override
    public String toString() {
        return "ChessGame{board=" + board + ", turn=" + turn + "}";
    }
}