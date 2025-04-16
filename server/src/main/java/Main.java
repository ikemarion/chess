import chess.*;
import dataaccess.DatabaseManager;  // <-- Import the DatabaseManager class
import dataaccess.DataAccessException;
import server.Server;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.initDB();
        } catch (DataAccessException e) {
            e.printStackTrace();
            return;
        }

        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Server: " + piece);

        Server server = new Server();
        server.run(9090);
    }
}