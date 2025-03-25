import chess.*;

public class Main {
    public static void main(String[] args) {
        // Just a splash message at startup
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♔ 240 Chess Client: " + piece);

        // The port your server is running on (commonly 8080)
        int port = 8080;

        // Create a ServerFacade pointing to that port, then pass it to ChessClient
        ServerFacade facade = new ServerFacade(port);
        ChessClient client = new ChessClient(facade);

        // Start the console UI
        client.start();
    }
}
