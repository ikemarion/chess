package chess.pieceMovement;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public class KnightMovements implements MovementCalculator{
    public static HashSet<ChessMove> getPossibilities(ChessPosition position, ChessBoard board){
        HashSet<ChessMove> output = new HashSet<>();
        ChessPiece piece = board.getPiece(position);
        boolean repeatable = false;
        int[][] direction = new int[][] {{2,1},{2,-1},{-2,1},{-2,-1},{-1,2},{-1,-2},{1,2},{1,-2}};
        MovementCalculator.getMovement(board,position,piece,direction,output, repeatable);
        return output;
    }
}