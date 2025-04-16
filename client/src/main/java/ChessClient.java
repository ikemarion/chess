import chess.*;
import client.ServerFacade;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class ChessClient {

    private final Scanner inputScanner = new Scanner(System.in);
    private final ServerFacade facade = new ServerFacade(8080);

    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;
    // Maps game list numbers to game IDs
    private HashMap<Integer, Integer> gameIndexToId = new HashMap<>();
    // Stores the player's chosen color when joining a game (e.g., "WHITE" or "BLACK")
    private String currentPlayerColor = null;

    // New fields for WebSocket usage
    private GameWebSocketClient gameSocket;
    private int currentGameId = -1;

    public void start() {
        System.out.println("Welcome to Chess Client! Type 'help' for commands.");
        while (true) {
            try {
                if (!loggedIn) {
                    handlePrelogin();
                } else {
                    handlePostlogin();
                }
            } catch (Exception e) {
                System.out.println("Error: " + hideJson(e.getMessage()));
            }
        }
    }

    // --- Pre-login Section ---
    private void handlePrelogin() {
        System.out.print("prelogin> ");
        String commandLine = inputScanner.nextLine().trim().toLowerCase();
        switch (commandLine) {
            case "help" -> showPreloginHelp();
            case "login" -> doLogin();
            case "register" -> doRegister();
            case "quit" -> quit();
            default -> System.out.println("Unknown command: " + commandLine);
        }
    }

    private void showPreloginHelp() {
        System.out.println("Prelogin commands:");
        System.out.println("  help     - Show this help menu");
        System.out.println("  login    - Log in with your username/password");
        System.out.println("  register - Create a new account");
        System.out.println("  quit     - Exit the program");
    }

    private void doLogin() {
        System.out.print("Username: ");
        String user = inputScanner.nextLine().trim();
        System.out.print("Password: ");
        String pass = inputScanner.nextLine().trim();
        try {
            AuthData data = facade.login(user, pass);
            this.authToken = data.authToken();
            this.username = data.username();
            this.loggedIn = true;
            System.out.println("Logged in as " + username);
        } catch (Exception e) {
            System.out.println("Login failed");
        }
    }

    private void doRegister() {
        System.out.print("Username: ");
        String user = inputScanner.nextLine().trim();
        System.out.print("Password: ");
        String pass = inputScanner.nextLine().trim();
        System.out.print("Email: ");
        String email = inputScanner.nextLine().trim();
        try {
            AuthData data = facade.register(user, pass, email);
            this.authToken = data.authToken();
            this.username = data.username();
            this.loggedIn = true;
            System.out.println("Registered and logged in as " + username);
        } catch (Exception e) {
            System.out.println("Register failed");
        }
    }

    // --- Post-login Section ---
    private void handlePostlogin() {
        System.out.print("postlogin> ");
        String commandLine = inputScanner.nextLine().trim().toLowerCase();
        switch (commandLine) {
            case "help" -> showPostloginHelp();
            case "logout" -> doLogout();
            case "create game" -> doCreateGame();
            case "list games" -> doListGames();
            case "play game" -> doPlayGame();
            case "observe game" -> doObserveGame();
            case "quit" -> quit();
            default -> System.out.println("Unknown command: " + commandLine);
        }
    }

    private void showPostloginHelp() {
        System.out.println("Postlogin commands:");
        System.out.println("  help          - Show this menu");
        System.out.println("  logout        - Logout of your current account");
        System.out.println("  create game   - Create a new game on the server");
        System.out.println("  list games    - List existing games on the server");
        System.out.println("  play game     - Join a game as a player (by number and color)");
        System.out.println("  observe game  - Observe a game (white perspective)");
        System.out.println("  quit          - Exit the program");
    }

    private void doLogout() {
        try {
            facade.logout(authToken);
            authToken = null;
            username = null;
            loggedIn = false;
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed");
        }
    }

    private void doCreateGame() {
        System.out.print("Enter a name for the new game: ");
        String gameName = inputScanner.nextLine().trim();
        try {
            facade.createGame(authToken, gameName);
            System.out.println("Game created: " + gameName);
        } catch (Exception e) {
            System.out.println("Create game failed");
        }
    }

    private void doListGames() {
        try {
            List<GameData> games = facade.listGames(authToken);
            gameIndexToId.clear();
            if (games.isEmpty()) {
                System.out.println("No games found.");
                return;
            }
            for (int i = 0; i < games.size(); i++) {
                int index = i + 1;
                gameIndexToId.put(index, games.get(i).getGameID());
                GameData g = games.get(i);
                String w = (g.getWhiteUsername() != null) ? g.getWhiteUsername() : "...";
                String b = (g.getBlackUsername() != null) ? g.getBlackUsername() : "...";
                System.out.printf("%d) %s (white=%s, black=%s)%n", index, g.getGameName(), w, b);
            }
        } catch (Exception e) {
            System.out.println("List games failed");
        }
    }

    private void doPlayGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) {
            return;
        }

        System.out.print("What color? [white/black]: ");
        String color = inputScanner.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color. Must be 'white' or 'black'.");
            return;
        }

        try {
            // Join the game via HTTP.
            facade.joinGame(authToken, gameId, color.toUpperCase());
            currentPlayerColor = color.toUpperCase();  // Store the player's chosen color.

            // Connect to the game's WebSocket for in-game actions.
            connectToGameWebSocket(gameId);

            // Draw the current board from the server
            GameData updatedGame = facade.getGame(authToken, gameId);
            drawUnicodeChessBoard(updatedGame, color.equals("white"));

            // Prompt for moves
            promptForMove(gameId, color.equals("white"));
        } catch (Exception e) {
            System.out.println("Join failed");
        }
    }

    private void doObserveGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) {
            return;
        }

        try {
            // For observers, mark the player's color as OBSERVER.
            currentPlayerColor = "OBSERVER";
            connectToGameWebSocket(gameId);

            GameData updatedGame = facade.getGame(authToken, gameId);
            drawUnicodeChessBoard(updatedGame, true); // Observers see White's perspective

            promptForMove(gameId, true); // Observers can still highlight or leave
        } catch (Exception e) {
            System.out.println("Observe game failed");
        }
    }

    private int promptGameNumber() {
        System.out.print("Game number: ");
        String input = inputScanner.nextLine().trim();
        try {
            int choice = Integer.parseInt(input);
            if (!gameIndexToId.containsKey(choice)) {
                System.out.println("Invalid number.");
                return -1;
            }
            return gameIndexToId.get(choice);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    private void quit() {
        System.out.println("Goodbye!");
        System.exit(0);
    }

    // --- Board Drawing Methods (UPDATED for actual server data) ---

    /**
     * Draws the board from the server’s current state.
     * If legalDestinations is not null, those squares will be highlighted.
     */
    private void drawUnicodeChessBoard(GameData gameData, boolean isWhitePerspective) {
        ChessGame chessGame = gameData.getChessGame();
        ChessBoard board = chessGame.getBoard();

        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            int actualRow = isWhitePerspective ? rowIndex : 7 - rowIndex;
            int rankLabel = 8 - actualRow;
            System.out.printf("%2d ", rankLabel);

            for (int colIndex = 0; colIndex < 8; colIndex++) {
                int actualCol = isWhitePerspective ? colIndex : 7 - colIndex;

                // Choose background color (light/dark)
                boolean isLightSquare = ((actualRow + actualCol) % 2 == 0);
                String bg = isLightSquare ? Ansi.BG_LIGHT : Ansi.BG_DARK;

                // The board uses 1-based row/column, so +1:
                ChessPosition pos = new ChessPosition(actualRow + 1, actualCol + 1);
                ChessPiece piece = board.getPiece(pos);

                // Convert piece to a single Unicode char
                String letter = toUnicodeChessSymbol(piece);

                // Print the square
                System.out.print(bg);
                System.out.printf(" %s ", letter);
                System.out.print(Ansi.RESET);
            }
            System.out.printf(" %2d%n", rankLabel);
        }

        // file labels
        System.out.print("   ");
        for (int colIndex = 0; colIndex < 8; colIndex++) {
            char fileLabel = (char) ('a' + (isWhitePerspective ? colIndex : 7 - colIndex));
            System.out.printf(" %c ", fileLabel);
        }
        System.out.println();
    }

    /** Overloaded method that takes a ChessPiece. */
    private String toUnicodeChessSymbol(ChessPiece piece) {
        if (piece == null) return " ";

        boolean black = (piece.getTeamColor() == ChessGame.TeamColor.BLACK);
        return switch (piece.getPieceType()) {
            case PAWN   -> black ? "♟" : "♙";
            case ROOK   -> black ? "♜" : "♖";
            case KNIGHT -> black ? "♞" : "♘";
            case BISHOP -> black ? "♝" : "♗";
            case QUEEN  -> black ? "♛" : "♕";
            case KING   -> black ? "♚" : "♔";
        };
    }

    // Overload without highlights
    private void drawUnicodeChessBoard(boolean isWhitePerspective) {
        // Minimal change: just fetch the current game from the server (if we have a valid game ID)
        // then call the method with null for legalDestinations.
        if (currentGameId < 0) {
            System.out.println("No current game to draw.");
            return;
        }
        try {
            GameData gameData = facade.getGame(authToken, currentGameId);
            drawUnicodeChessBoard(gameData, isWhitePerspective);
        } catch (Exception e) {
            System.out.println("Could not fetch game data: " + e.getMessage());
        }
    }

    private void drawUnicodeChessBoard(boolean isWhitePerspective, Set<ChessPosition> legalDestinations) {
        if (currentGameId < 0) {
            System.out.println("No current game to draw.");
            return;
        }
        try {
            GameData gameData = facade.getGame(authToken, currentGameId);
            drawUnicodeChessBoard(gameData, isWhitePerspective);
        } catch (Exception e) {
            System.out.println("Could not fetch game data: " + e.getMessage());
        }
    }

    /**
     * Converts a piece letter like 'R', 'n', 'K', etc. to a Unicode chess symbol.
     */
    private String toUnicodeChessSymbol(String pieceLetter) {
        return switch (pieceLetter) {
            case "R" -> "♜"; // black rook
            case "N" -> "♞"; // black knight
            case "B" -> "♝"; // black bishop
            case "Q" -> "♛"; // black queen
            case "K" -> "♚"; // black king
            case "P" -> "♟"; // black pawn
            case "r" -> "♖"; // white rook
            case "n" -> "♘"; // white knight
            case "b" -> "♗"; // white bishop
            case "q" -> "♕"; // white queen
            case "k" -> "♔"; // white king
            case "p" -> "♙"; // white pawn
            default -> pieceLetter; // fallback if something else
        };
    }

    // --- WebSocket Integration Methods ---
    private void connectToGameWebSocket(int gameId) {
        try {
            currentGameId = gameId;
            // Change endpoint URI from "/gameplay" to "/ws"
            URI endpointURI = new URI("ws://localhost:8080/ws");
            gameSocket = new GameWebSocketClient(endpointURI);
            gameSocket.addMessageHandler(message -> {
                // Process server messages (e.g., update board state or display notifications)
                System.out.println("WebSocket message received: " + message);

                // Optionally auto-refresh the board after a move
                // so we see other players' moves in real time
                try {
                    GameData updatedGame = facade.getGame(authToken, currentGameId);
                    boolean isWhitePerspective =
                            "WHITE".equalsIgnoreCase(currentPlayerColor)
                                    || "OBSERVER".equalsIgnoreCase(currentPlayerColor);
                    drawUnicodeChessBoard(updatedGame, isWhitePerspective);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            // Send an initial CONNECT command.
            String connectMsg = String.format(
                    "{\"commandType\": \"CONNECT\", \"authToken\": \"%s\", \"gameID\": %d}",
                    authToken, gameId
            );
            gameSocket.sendMessage(connectMsg);
            System.out.println("WebSocket CONNECT message sent.");
        } catch (Exception e) {
            System.err.println("WebSocket connection error: " + e.getMessage());
        }
    }

    private void sendMove(String startSquare, String endSquare, String promotion) {
        ChessPosition startPosition = convertSquareToPosition(startSquare);
        ChessPosition endPosition = convertSquareToPosition(endSquare);

        String moveMsg = String.format(
                "{" +
                        "\"commandType\": \"MAKE_MOVE\", " +
                        "\"authToken\": \"%s\", " +
                        "\"gameID\": %d, " +
                        "\"move\": {" +
                        "\"startPosition\": {\"row\": %d, \"column\": %d}, " +
                        "\"endPosition\": {\"row\": %d, \"column\": %d}, " +
                        "\"promotionPiece\": %s" +
                        "}" +
                        "}",
                authToken, currentGameId,
                startPosition.getRow(), startPosition.getColumn(),
                endPosition.getRow(), endPosition.getColumn(),
                (promotion != null && !promotion.isEmpty()) ? "\"" + promotion + "\"" : "null"
        );

        System.out.println("Sending move JSON: " + moveMsg);
        gameSocket.sendMessage(moveMsg);
    }

    private ChessPosition convertSquareToPosition(String square) {
        // e.g. "e2" -> (row=2, col=5) if your server indexing uses 1-based row/col
        char file = square.charAt(0);              // 'e'
        int rank = Character.getNumericValue(square.charAt(1)); // 2
        int column = file - 'a' + 1;               // e -> 5
        // If your server uses (row=1 at bottom = White side) then you might do  rank=2
        // and row=8-rank+1 if needed. We'll assume row=rank directly if that's how your code works.
        return new ChessPosition(rank, column);
    }

    private void sendResign() {
        String resignMsg = String.format(
                "{\"commandType\": \"RESIGN\", \"authToken\": \"%s\", \"gameID\": %d, \"playerColor\": \"%s\"}",
                authToken, currentGameId, currentPlayerColor
        );
        gameSocket.sendMessage(resignMsg);
    }

    private void sendLeave() {
        String leaveMsg = String.format(
                "{\"commandType\": \"LEAVE\", \"authToken\": \"%s\", \"gameID\": %d, \"playerColor\": \"%s\"}",
                authToken, currentGameId, currentPlayerColor
        );
        gameSocket.sendMessage(leaveMsg);
    }

    // --- Updated In-Game Prompt ---
    private void promptForMove(int gameId, boolean isWhitePerspective) {
        System.out.println("In-game commands:");
        System.out.println("  move <start> <end> [promotion]   - Make a move (e.g. move e2 e4 or move e7 e8 q)");
        System.out.println("  legal <square>                   - Show & highlight legal moves from that square (e.g. legal e2)");
        System.out.println("  leave                            - Leave the game");
        System.out.println("  resign                           - Resign from the game");
        System.out.println("  redraw                           - Redraw the board");
        System.out.println("  help                             - Show this command list");
        System.out.println("  quit                             - Exit move mode");

        while (true) {
            System.out.print("move> ");
            String commandLine = inputScanner.nextLine().trim().toLowerCase();
            if (commandLine.equals("quit")) {
                System.out.println("Exiting move prompt.");
                break;
            }
            if (commandLine.equals("help")) {
                System.out.println("Available in-game commands:");
                System.out.println("  move <start> <end> [promotion]   - Example: move e2 e4 or move e7 e8 q");
                System.out.println("  legal <square>                   - Example: legal e2");
                System.out.println("  leave                            - Leave the game");
                System.out.println("  resign                           - Resign from the game");
                System.out.println("  redraw                           - Redraw the board");
                System.out.println("  quit                             - Exit move mode");
                continue;
            }
            if (commandLine.equals("leave")) {
                sendLeave();
                System.out.println("Leave command sent over WebSocket. Returning to postlogin menu...");
                break;
            }
            if (commandLine.equals("resign")) {
                System.out.print("Are you sure you want to resign? (y/n): ");
                String response = inputScanner.nextLine().trim().toLowerCase();
                if (response.equals("y")) {
                    sendResign();
                    System.out.println("Resign command sent over WebSocket. You remain in the game as a spectator until you leave.");
                    // We do NOT break here if you want them to remain in the move prompt,
                    // but the requirements say: “Does not cause the user to leave the game.”
                    // If you prefer to automatically exit, you can do so.
                } else {
                    System.out.println("Resignation canceled.");
                }
                continue;
            }
            if (commandLine.equals("redraw")) {
                try {
                    GameData updatedGame = facade.getGame(authToken, gameId);
                    drawUnicodeChessBoard(updatedGame, isWhitePerspective);
                } catch (Exception e) {
                    System.out.println("Redraw failed: " + e.getMessage());
                }
                continue;
            }
            if (commandLine.startsWith("legal")) {
                String[] parts = commandLine.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Usage: legal <square> (e.g., legal e2)");
                    continue;
                }
                String squareStr = parts[1];
                ChessPosition pos = ChessPosition.fromString(squareStr);
                if (pos == null) {
                    System.out.println("Invalid square: " + squareStr);
                    continue;
                }
                try {
                    GameData currentGame = facade.getGame(authToken, gameId);
                    @SuppressWarnings("unchecked")
                    Set<ChessMove> legalMoves = (Set<ChessMove>) currentGame.getChessGame().validMoves(pos);
                    Set<ChessPosition> legalDestinations = legalMoves.stream()
                            .map(ChessMove::getEndPosition)
                            .collect(Collectors.toSet());

                    // We can also add the piece's own position to highlight it
                    legalDestinations = new HashSet<>(legalDestinations);
                    legalDestinations.add(pos);

                    drawUnicodeChessBoard(currentGame, isWhitePerspective);
                } catch (Exception e) {
                    System.out.println("Error retrieving legal moves: " + e.getMessage());
                }
                continue;
            }
            // Handle move commands
            if (commandLine.startsWith("move ")) {
                String[] tokens = commandLine.split("\\s+");
                if (tokens.length < 3 || tokens.length > 4) {
                    System.out.println("Invalid move format. Use: move <start> <end> [promotion]");
                    continue;
                }
                // tokens[0] is "move"
                String startSquare = tokens[1];
                String endSquare = tokens[2];
                String promotion = (tokens.length == 4) ? tokens[3] : null;

                sendMove(startSquare, endSquare, promotion);

                // After sending move, we can fetch the new board state to reflect changes
                try {
                    // Let the server process the move. Possibly add a small sleep or wait for WebSocket update.
                    Thread.sleep(200);
                    GameData updatedGame = facade.getGame(authToken, gameId);
                    drawUnicodeChessBoard(updatedGame, isWhitePerspective);
                } catch (Exception e) {
                    System.out.println("Could not refresh board after move: " + e.getMessage());
                }
                continue;
            } else {
                // Fallback: if user typed something like "e2 e4" directly (no "move" prefix).
                // We'll treat it as a move with optional promotion.
                String[] tokens = commandLine.split("\\s+");
                if (tokens.length < 2 || tokens.length > 3) {
                    System.out.println("Invalid move format. Use: move e2 e4 [promotion] or 'move <start> <end> [promotion]'");
                    continue;
                }
                String startSquare = tokens[0];
                String endSquare = tokens[1];
                String promotion = (tokens.length == 3) ? tokens[2] : null;

                sendMove(startSquare, endSquare, promotion);

                // After sending move, get the new board
                try {
                    Thread.sleep(200);
                    GameData updatedGame = facade.getGame(authToken, gameId);
                    drawUnicodeChessBoard(updatedGame, isWhitePerspective);
                } catch (Exception e) {
                    System.out.println("Could not refresh board after move: " + e.getMessage());
                }
            }
        }
    }

    private String hideJson(String msg) {
        if (msg == null) {
            return "Unknown error.";
        }
        int braceIdx = msg.indexOf('{');
        if (braceIdx >= 0) {
            msg = msg.substring(0, braceIdx);
        }
        return msg.trim();
    }

    private static class Ansi {
        public static final String RESET = "\u001b[0m";
        public static final String BG_LIGHT = "\u001b[47m";
        public static final String BG_DARK = "\u001b[40m";
        public static final String BG_HIGHLIGHT = "\u001b[43m"; // Yellow for highlighting
    }
}