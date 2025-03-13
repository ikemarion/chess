package chess.piecemovement;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public class BishopMovements implements MovementCalculator{
    public static HashSet<ChessMove> getPossibilities(ChessPosition position, ChessBoard board){
        HashSet<ChessMove> output = new HashSet<>();
        ChessPiece piece = board.getPiece(position);
        boolean repeatable = true;
        int[][] direction = new int[][] {{1, 1},{1, -1},{-1,1},{-1,-1}};
        MovementCalculator.getMovement(board,position,piece,direction,output, repeatable);
        return output;
    }
}