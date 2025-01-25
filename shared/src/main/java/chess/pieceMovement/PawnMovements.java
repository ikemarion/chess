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
        boolean repeatable = false;
        int[][] direction = new int[][] {{0, pawnDirection}};
        int newrow = position.getRow();
        int newcol = position.getColumn() + pawnDirection;
            ChessPosition endPostion = new ChessPosition(newrow, newcol);
            if (MovementCalculator.boundaryCheck(endPostion)) {
                if (board.getPiece(endPostion) == null) {
                    output.add(new ChessMove(position, endPostion, null));
                } else if (piece.getTeamColor() != board.getPiece(endPostion).getTeamColor()) {
                    output.add(new ChessMove(position, endPostion, null));
                }
            }
        newrow = position.getRow() - 1;
        newcol = position.getColumn() + pawnDirection;
        endPostion = new ChessPosition(newrow, newcol);
        if (MovementCalculator.boundaryCheck(endPostion) && board.getPiece(endPostion) != null) {
            if(piece.getTeamColor() != board.getPiece(endPostion).getTeamColor()){
                output.add(new ChessMove(position, endPostion, null));
            }
        }

        newrow = position.getRow() + 1;
        newcol = position.getColumn() + pawnDirection;
        endPostion = new ChessPosition(newrow, newcol);
        if (MovementCalculator.boundaryCheck(endPostion) && board.getPiece(endPostion) != null) {
            if(piece.getTeamColor() != board.getPiece(endPostion).getTeamColor()){
                output.add(new ChessMove(position, endPostion, null));
            }
        }


        return output;
    }
}