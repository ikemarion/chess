package chess.pieceMovement;

import chess.*;

import java.util.HashSet;

public class PawnMovements implements MovementCalculator{
    public static HashSet<ChessMove> getPossibilities(ChessPosition position, ChessBoard board){
        HashSet<ChessMove> output = new HashSet<>();
        ChessPiece piece = board.getPiece(position);
        ChessGame.TeamColor color = piece.getTeamColor();

        int pawnDirection = -1;
        if(color == ChessGame.TeamColor.WHITE){pawnDirection = 1;}

        int[][] direction = new int[][] {{0, pawnDirection}};
        int newrow = position.getRow() + pawnDirection;
        int newcol = position.getColumn();
            ChessPosition endPosition = new ChessPosition(newrow, newcol);
            if (MovementCalculator.boundaryCheck(endPosition)) {
                if (board.getPiece(endPosition) == null) {
                    if(pawnDirection == 1 && newrow == 8 || pawnDirection == -1 && newrow == 1){
                        output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.KNIGHT));
                        output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.ROOK));
                        output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.BISHOP));
                        output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.QUEEN));
                    }
                    else {
                        output.add(new ChessMove(position, endPosition, null));
                    }
                    }
            }

        newrow = position.getRow();
        if(pawnDirection == 1 && newrow == 2 || pawnDirection == -1 && newrow == 7) {
            newrow = position.getRow() + pawnDirection;
            int newRow2 = position.getRow() + pawnDirection * 2;
            endPosition = new ChessPosition(newrow, newcol);
            ChessPosition endPosition2 = new ChessPosition(newRow2, newcol);
            if (MovementCalculator.boundaryCheck(endPosition) && MovementCalculator.boundaryCheck(endPosition2)) {
                if (board.getPiece(endPosition) == null && board.getPiece(endPosition2) == null) {
                    output.add(new ChessMove(position, endPosition2, null));
                }
            }
        }

        newrow = position.getRow() + pawnDirection;
        newcol = position.getColumn() - 1;
        endPosition = new ChessPosition(newrow, newcol);
        if (MovementCalculator.boundaryCheck(endPosition) && board.getPiece(endPosition) != null) {
            if(piece.getTeamColor() != board.getPiece(endPosition).getTeamColor()){
                if(pawnDirection == 1 && newrow == 8 || pawnDirection == -1 && newrow == 1){
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.KNIGHT));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.ROOK));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.BISHOP));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.QUEEN));
                }
                else {
                    output.add(new ChessMove(position, endPosition, null));
                }

            }
        }

        newrow = position.getRow() + pawnDirection;
        newcol = position.getColumn() + 1;
        endPosition = new ChessPosition(newrow, newcol);
        if (MovementCalculator.boundaryCheck(endPosition) && board.getPiece(endPosition) != null) {
            if(piece.getTeamColor() != board.getPiece(endPosition).getTeamColor()){
                if(pawnDirection == 1 && newrow == 8 || pawnDirection == -1 && newrow == 1){
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.KNIGHT));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.ROOK));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.BISHOP));
                    output.add(new ChessMove(position, endPosition, ChessPiece.PieceType.QUEEN));
                }
                else {
                    output.add(new ChessMove(position, endPosition, null));
                }
            }
        }


        return output;
    }
}