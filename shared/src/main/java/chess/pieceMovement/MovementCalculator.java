package chess.pieceMovement;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public interface MovementCalculator {
    static void getMovement(ChessBoard board, ChessPosition startposition, ChessPiece piece, int[][] direction, HashSet<ChessMove> validMoves, boolean something){
        for (int[] i : direction) {

            int newrow = startposition.getRow();
            int newcol = startposition.getColumn();

            while(true) {
                newrow = newrow + i[0];
                newcol = newcol + i[1];
                ChessPosition endpostion = new ChessPosition(newrow, newcol);
                if (boundaryCheck(endpostion)) {
                    if (board.getPiece(endpostion) == null) {
                        validMoves.add(new ChessMove(startposition, endpostion, null));
                    } else if (piece.getTeamColor() != board.getPiece(endpostion).getTeamColor()) {
                        validMoves.add(new ChessMove(startposition, endpostion, null));
                        break;
                    }
                    else{break;}
                }
                else{break;}
                if (!something){
                    break;
                }
            }
        }
    }

    static boolean boundaryCheck(ChessPosition position){
        if(position.getRow() > 8){
            return false;
        }
        if(position.getColumn() > 8){
            return false;
        }
        if(position.getRow() < 1){
            return false;
        }
        if(position.getColumn() < 1){
            return false;
        }
        return true;
    }

}