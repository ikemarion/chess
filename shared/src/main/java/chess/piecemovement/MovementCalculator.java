package chess.piecemovement;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.HashSet;

public interface MovementCalculator {
    static void getMovement(ChessBoard board, ChessPosition startPosition, ChessPiece piece, int[][] direction, HashSet<ChessMove> validMoves, boolean repeatable){
        for (int[] i : direction) {

            int newrow = startPosition.getRow();
            int newcol = startPosition.getColumn();

            while(true) {
                newrow = newrow + i[0];
                newcol = newcol + i[1];
                ChessPosition endPostion = new ChessPosition(newrow, newcol);
                if (boundaryCheck(endPostion)) {
                    if (board.getPiece(endPostion) == null) {
                        validMoves.add(new ChessMove(startPosition, endPostion, null));
                    } else if (piece.getTeamColor() != board.getPiece(endPostion).getTeamColor()) {
                        validMoves.add(new ChessMove(startPosition, endPostion, null));
                        break;
                    }
                    else{break;}
                }
                else{break;}
                if (!repeatable){
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