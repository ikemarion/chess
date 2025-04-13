import client.ServerFacade;
import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessPosition;
import chess.ChessMove;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class ChessClient {

    private final Scanner inputScanner = new Scanner(System.in);
    private final ServerFacade facade = new ServerFacade(8080);

    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;
    // Maps the number shown on the game list to the gameID
    private HashMap<Integer, Integer> gameIndexToId = new HashMap<>();
    // NEW: Store the player's color when they join a game
    private String currentPlayerColor = null;

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

    // Pre-login commands.
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

    // Post-login commands.
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
                gameIndexToId.put(index, games.get(i).gameID());
                GameData g = games.get(i);
                String w = (g.whiteUsername() != null) ? g.whiteUsername() : "...";
                String b = (g.blackUsername() != null) ? g.blackUsername() : "...";
                System.out.printf("%d) %s (white=%s, black=%s)%n", index, g.gameName(), w, b);
            }
        } catch (Exception e) {
            System.out.println("List games failed");
        }
    }

    private void doPlayGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) { return; }

        System.out.print("What color? [white/black]: ");
        String color = inputScanner.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color. Must be 'white' or 'black'.");
            return;
        }
        try {
            facade.joinGame(authToken, gameId, color.toUpperCase());
            // Store the chosen color for later commands.
            currentPlayerColor = color.toUpperCase();
            drawUnicodeChessBoard(color.equals("white"));
            promptForMove(gameId, color.equals("white"));
        } catch (Exception e) {
            System.out.println("Join failed");
        }
    }

    private void doObserveGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) { return; }
        try {
            drawUnicodeChessBoard(true);
            promptForMove(gameId, true);
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

    // Draw a basic chess board (uses a fixed initial setup for demonstration).
    private void drawUnicodeChessBoard(boolean isWhitePerspective) {
        System.out.println("Drawing board from " + (isWhitePerspective ? "White" : "Black") + " perspective...");
        String[][] initialSetup = {
                {"R", "N", "B", "Q", "K", "B", "N", "R"},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {"r", "n", "b", "q", "k", "b", "n", "r"}
        };
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            int actualRow = isWhitePerspective ? rowIndex : (7 - rowIndex);
            int rankLabel = 8 - actualRow;
            System.out.printf("%2d ", rankLabel); // left label
            for (int colIndex = 0; colIndex < 8; colIndex++) {
                int actualCol = isWhitePerspective ? colIndex : (7 - colIndex);
                boolean isLightSquare = ((actualRow + actualCol) % 2 == 0);
                String bg = isLightSquare ? Ansi.BG_LIGHT : Ansi.BG_DARK;
                String letter = initialSetup[actualRow][actualCol];
                if (letter.equals(".")) {
                    letter = " ";
                } else {
                    letter = toUnicodeChessSymbol(letter);
                }
                System.out.print(bg);
                System.out.printf(" %s ", letter);
                System.out.print(Ansi.RESET);
            }
            System.out.printf(" %2d%n", rankLabel); // right label
        }
        System.out.print("   ");
        for (int colIndex = 0; colIndex < 8; colIndex++) {
            char fileLabel = (char) ('a' + (isWhitePerspective ? colIndex : (7 - colIndex)));
            System.out.printf(" %c ", fileLabel);
        }
        System.out.println();
    }

    // Overloaded version which highlights legal destination squares.
    private void drawUnicodeChessBoard(boolean isWhitePerspective, Set<chess.ChessPosition> legalDestinations) {
        System.out.println("Drawing board from " + (isWhitePerspective ? "White" : "Black") + " perspective with legal moves highlighted...");
        String[][] initialSetup = {
                {"R", "N", "B", "Q", "K", "B", "N", "R"},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {"r", "n", "b", "q", "k", "b", "n", "r"}
        };
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            int actualRow = isWhitePerspective ? rowIndex : (7 - rowIndex);
            int rankLabel = 8 - actualRow;
            System.out.printf("%2d ", rankLabel);
            for (int colIndex = 0; colIndex < 8; colIndex++) {
                int actualCol = isWhitePerspective ? colIndex : (7 - colIndex);
                boolean isLightSquare = ((actualRow + actualCol) % 2 == 0);
                String bg = isLightSquare ? Ansi.BG_LIGHT : Ansi.BG_DARK;
                String square = "" + (char)('a' + actualCol) + (8 - actualRow);
                if (legalDestinations != null && legalDestinations.contains(chess.ChessPosition.fromString(square))) {
                    // Highlight legal destination with a different background.
                    bg = Ansi.BG_HIGHLIGHT;
                }
                String letter = initialSetup[actualRow][actualCol];
                if (letter.equals(".")) {
                    letter = " ";
                } else {
                    letter = toUnicodeChessSymbol(letter);
                }
                System.out.print(bg);
                System.out.printf(" %s ", letter);
                System.out.print(Ansi.RESET);
            }
            System.out.printf(" %2d%n", rankLabel);
        }
        System.out.print("   ");
        for (int colIndex = 0; colIndex < 8; colIndex++) {
            char fileLabel = (char) ('a' + (isWhitePerspective ? colIndex : (7 - colIndex)));
            System.out.printf(" %c ", fileLabel);
        }
        System.out.println();
    }

    private String toUnicodeChessSymbol(String pieceLetter) {
        return switch (pieceLetter) {
            case "R" -> "♜";
            case "N" -> "♞";
            case "B" -> "♝";
            case "Q" -> "♛";
            case "K" -> "♚";
            case "P" -> "♟";
            // white pieces represented by lowercase in your setup
            case "r" -> "♖";
            case "n" -> "♘";
            case "b" -> "♗";
            case "q" -> "♕";
            case "k" -> "♔";
            case "p" -> "♙";
            default -> pieceLetter;
        };
    }

    // This method prompts for moves and supports commands such as leave, resign, legal, and move.
    private void promptForMove(int gameId, boolean isWhitePerspective) {
        System.out.println("In-game commands:");
        System.out.println("  move <start> <end> [promotion]   - Make a move (e.g., e2 e4 or e7 e8 q)");
        System.out.println("  legal <square>                   - Show legal moves from that square (e.g., legal e2)");
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
                System.out.println("  move <start> <end> [promotion]   - Example: e2 e4 or e7 e8 q");
                System.out.println("  legal <square>                   - Example: legal e2");
                System.out.println("  leave                            - Leave the game");
                System.out.println("  resign                           - Resign from the game");
                System.out.println("  redraw                           - Redraw the board");
                System.out.println("  quit                             - Exit move mode");
                continue;
            }
            if (commandLine.equals("leave")) {
                try {
                    facade.leaveGame(authToken, gameId, currentPlayerColor);
                    System.out.println("You have left the game.");
                    break;
                } catch (Exception e) {
                    System.out.println("Leave failed: " + e.getMessage());
                }
                continue;
            }
            if (commandLine.equals("resign")) {
                System.out.print("Are you sure you want to resign? (y/n): ");
                String response = inputScanner.nextLine().trim().toLowerCase();
                if (response.equals("y")) {
                    try {
                        facade.resignGame(authToken, gameId, currentPlayerColor);
                        System.out.println("You have resigned from the game.");
                    } catch (Exception e) {
                        System.out.println("Resign failed: " + e.getMessage());
                    }
                    break;
                } else {
                    System.out.println("Resignation canceled.");
                    continue;
                }
            }
            if (commandLine.equals("redraw")) {
                try {
                    GameData updatedGame = facade.getGame(authToken, gameId);
                    drawUnicodeChessBoard(isWhitePerspective);
                } catch (Exception e) {
                    System.out.println("Redraw failed: " + e.getMessage());
                }
                continue;
            }
            if (commandLine.startsWith("legal")) {
                String[] parts = commandLine.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Usage: legal <square>  (e.g., legal e2)");
                    continue;
                }
                String squareStr = parts[1];
                // Assumes a static method ChessPosition.fromString exists.
                chess.ChessPosition pos = chess.ChessPosition.fromString(squareStr);
                if (pos == null) {
                    System.out.println("Invalid square: " + squareStr);
                    continue;
                }
                try {
                    GameData currentGame = facade.getGame(authToken, gameId);
                    Set<ChessMove> legalMoves = (Set<ChessMove>) currentGame.game().validMoves(pos);
                    Set<chess.ChessPosition> legalDestinations = legalMoves.stream()
                            .map(ChessMove::getEndPosition)
                            .collect(Collectors.toSet());
                    drawUnicodeChessBoard(isWhitePerspective, legalDestinations);
                } catch (Exception e) {
                    System.out.println("Error retrieving legal moves: " + e.getMessage());
                }
                continue;
            }
            // Otherwise, assume it's a move command.
            String[] tokens = commandLine.split("\\s+");
            if (tokens.length < 2 || tokens.length > 3) {
                System.out.println("Invalid move format. Use: <start> <end> [promotion]");
                continue;
            }
            String startSquare = tokens[0];
            String endSquare = tokens[1];
            String promotion = (tokens.length == 3) ? tokens[2] : null;
            try {
                facade.makeMove(authToken, gameId, startSquare, endSquare, promotion);
                GameData updatedGame = facade.getGame(authToken, gameId);
                drawUnicodeChessBoard(isWhitePerspective);
            } catch (Exception e) {
                System.out.println("Move failed: " + e.getMessage());
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
        public static final String BG_DARK  = "\u001b[40m";
        public static final String BG_HIGHLIGHT = "\u001b[43m"; // Yellow background for highlighting
    }
}