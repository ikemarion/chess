package chess;

import chess.pieceMovement.BishopMovements;
import chess.pieceMovement.MovementCalculator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {
    private ChessBoard board;
    private TeamColor turn;

    public ChessGame() {
        board = new ChessBoard();
        setTeamTurn(TeamColor.WHITE);
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return turn;
    }

    /**
     * Set's which teams turn it is
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
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);

        if(piece == null){
            return new HashSet<>();
        }
        HashSet<ChessMove> validMoves = new HashSet<>();
        HashSet<ChessMove> allMoves = new HashSet<>(piece.pieceMoves(board, startPosition));

        for(ChessMove move : allMoves){
            ChessPiece placeholder = board.getPiece(startPosition);
            ChessPiece placeholder2 = board.getPiece(move.getEndPosition());

            board.addPiece(move.getEndPosition(), placeholder);
            board.addPiece(move.getStartPosition(), null);
            if(!isInCheck(placeholder.getTeamColor())){
                validMoves.add(move);
            }
            board.addPiece(move.getStartPosition(), placeholder);
            board.addPiece(move.getEndPosition(), placeholder2);
        }
        return validMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to preform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if(validMoves(move.getStartPosition()).contains(move) && turn == board.getPiece(move.getStartPosition()).getTeamColor()){
            if(move.getPromotionPiece() != null){
                board.addPiece(move.getEndPosition(), new ChessPiece(getTeamTurn(), move.getPromotionPiece()));
            }
            else{
                board.addPiece(move.getEndPosition(), board.getPiece(move.getStartPosition()));
            }
            board.addPiece(move.getStartPosition(), null);
            if(turn == TeamColor.BLACK){
                setTeamTurn(TeamColor.WHITE);
            }
            else {
                setTeamTurn(TeamColor.BLACK);
            }
        }
        else {
            throw new InvalidMoveException();
        }
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
//        for (int y = 1; y <= 8; y++) {
//            for (int x = 1; x <= 8; x++) {
//                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
//                if (piece == null || piece.getTeamColor() == teamColor) {
//                    continue;
//                }
//                for (ChessMove move : piece.pieceMoves(board, new ChessPosition(x, y))) {
//                    if (move.getEndPosition().equals(findKing(teamColor))) {
//                        return true;
//                    }
//                }
//            }
//        }

        int pawnCheck = 1;
        if(turn == TeamColor.BLACK){
            pawnCheck = -1;
        }

        ChessPosition throne = findKing(teamColor);

        //Check Pawns
        //If the pieces there are pawns and the other team
        if(MovementCalculator.boundaryCheck(new ChessPosition((throne).getRow()+1,throne.getColumn()+pawnCheck))){
            if(board.getPiece(new ChessPosition(throne.getRow()+1,throne.getColumn()+pawnCheck)).getPieceType() == ChessPiece.PieceType.PAWN && board.getPiece(throne).getTeamColor() != turn){
                return true;
            }
        }

        if(MovementCalculator.boundaryCheck(new ChessPosition(throne.getRow()-1,throne.getColumn()+pawnCheck))){
            if(board.getPiece(new ChessPosition(throne.getRow()-1,throne.getColumn()+pawnCheck)).getPieceType() == ChessPiece.PieceType.PAWN && board.getPiece(throne).getTeamColor() != turn){
                return true;
            }
        }

        //Check Bishop/Queen
        board.addPiece(throne, new ChessPiece(teamColor, ChessPiece.PieceType.BISHOP));
        for(ChessMove move : BishopMovements.getPossibilities(throne, board)){
            if(board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.BISHOP || board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.QUEEN && board.getPiece(move.getEndPosition()).getTeamColor() != turn){
                return true;
            }
        }
        //Check Rook/Queen
        board.addPiece(throne, new ChessPiece(teamColor, ChessPiece.PieceType.ROOK));
        for(ChessMove move : BishopMovements.getPossibilities(throne, board)){
            if(board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.BISHOP || board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.QUEEN && board.getPiece(move.getEndPosition()).getTeamColor() != turn){
                return true;
            }
        }
        //Check Knight
        board.addPiece(throne, new ChessPiece(teamColor, ChessPiece.PieceType.KNIGHT));
        for(ChessMove move : BishopMovements.getPossibilities(throne, board)){
            if(board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.BISHOP || board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.QUEEN && board.getPiece(move.getEndPosition()).getTeamColor() != turn){
                return true;
            }
        }
        //Check King
        board.addPiece(throne, new ChessPiece(teamColor, ChessPiece.PieceType.KING));
        for(ChessMove move : BishopMovements.getPossibilities(throne, board)){
            if(board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.BISHOP || board.getPiece(move.getEndPosition()).getPieceType() == ChessPiece.PieceType.QUEEN && board.getPiece(move.getEndPosition()).getTeamColor() != turn){
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if(!isInCheck(teamColor)) {
            return false;
        }
            for (int y = 1; y <= 8; y++) {
                for (int x = 1; x <= 8; x++) {
                    ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                    if (piece == null || piece.getTeamColor() != teamColor) {
                        continue;
                    }
                    Collection<ChessMove> test = validMoves(new ChessPosition(x, y));
                    if(!test.isEmpty()){
                        return false;
                    }
                }
            }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }
        for (int y = 1; y <= 8; y++) {
            for (int x = 1; x <= 8; x++) {
                ChessPiece piece = board.getPiece(new ChessPosition(x, y));
                if (piece == null || piece.getTeamColor() != teamColor) {
                    continue;
                }
                Collection<ChessMove> test = validMoves(new ChessPosition(x, y));
                if(!test.isEmpty()){
                    return false;
                }
            }
        }
        return true;
    }

    public ChessPosition findKing(TeamColor color){
        ChessPosition king = null;
        for (int y = 1; y <= 8 && king == null; y++) {
            for (int x = 1; x <= 8  && king == null; x++) {
                if(board.getPiece(new ChessPosition(x, y)) != null){
                    if (board.getPiece(new ChessPosition(x, y)).getTeamColor() == color && board.getPiece(new ChessPosition(x, y)).getPieceType() == ChessPiece.PieceType.KING) {
                        king = new ChessPosition(x, y);
                    }
                }
            }
        }
        return king;
    }

    /**
     * Sets this game's chessboard with a given board
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
        return board;    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return Objects.equals(board, chessGame.board) && turn == chessGame.turn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(board, turn);
    }

    @Override
    public String toString() {
        return "ChessGame{" +
                "board=" + board +
                ", turn=" + turn +
                '}';
    }
}
