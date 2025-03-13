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

    public ChessGame() {
        board = new ChessBoard();
        setTeamTurn(TeamColor.WHITE);
        board.resetBoard();
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
            toggleTurn();
        } else {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame other = (ChessGame) o;
        return Objects.equals(board, other.board) && turn == other.turn;
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