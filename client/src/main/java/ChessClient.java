import client.ServerFacade;
import model.AuthData;
import model.GameData;
import model.UserData;
import chess.ChessPosition;
import chess.ChessMove;

import java.net.URI;
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
            // Join the game via HTTP.
            facade.joinGame(authToken, gameId, color.toUpperCase());
            currentPlayerColor = color.toUpperCase();  // Store the player's chosen color.

            // Connect to the game's WebSocket for in-game actions.
            connectToGameWebSocket(gameId);

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
            // For observers, mark the player's color as OBSERVER.
            currentPlayerColor = "OBSERVER";
            connectToGameWebSocket(gameId);

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

    // --- Board Drawing Methods ---
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
            System.out.printf("%2d ", rankLabel);
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
            System.out.printf(" %2d%n", rankLabel);
        }

        System.out.print("   ");
        for (int colIndex = 0; colIndex < 8; colIndex++) {
            char fileLabel = (char) ('a' + (isWhitePerspective ? colIndex : (7 - colIndex)));
            System.out.printf(" %c ", fileLabel);
        }
        System.out.println();
    }

    // Overloaded version to highlight legal destination squares.
    private void drawUnicodeChessBoard(boolean isWhitePerspective, Set<chess.ChessPosition> legalDestinations) {
        System.out.println("Drawing board from " + (isWhitePerspective ? "White" : "Black") +
                " perspective with legal moves highlighted...");
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
                String square = "" + (char) ('a' + actualCol) + (8 - actualRow);
                if (legalDestinations != null && legalDestinations.contains(chess.ChessPosition.fromString(square))) {
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
            case "r" -> "♖";
            case "n" -> "♘";
            case "b" -> "♗";
            case "q" -> "♕";
            case "k" -> "♔";
            case "p" -> "♙";
            default -> pieceLetter;
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
            });
            // Send an initial CONNECT command.
            String connectMsg = String.format("{\"commandType\": \"CONNECT\", \"authToken\": \"%s\", \"gameID\": %d}",
                    authToken, gameId);
            gameSocket.sendMessage(connectMsg);
            System.out.println("WebSocket CONNECT message sent.");
        } catch (Exception e) {
            System.err.println("WebSocket connection error: " + e.getMessage());
        }
    }

    private void sendMove(String startSquare, String endSquare, String promotion) {
        String moveMsg = String.format(
                "{\"commandType\": \"MAKE_MOVE\", \"authToken\": \"%s\", \"gameID\": %d, " +
                        "\"move\": {\"start\": \"%s\", \"end\": \"%s\", \"promotion\": \"%s\"}}",
                authToken, currentGameId, startSquare, endSquare, (promotion != null ? promotion : "")
        );
        gameSocket.sendMessage(moveMsg);
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
        System.out.println("  move <start> <end> [promotion]   - Make a move (e.g., move e2 e4 or move e7 e8 q)");
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
                System.out.println("Leave command sent over WebSocket.");
                break;
            }
            if (commandLine.equals("resign")) {
                System.out.print("Are you sure you want to resign? (y/n): ");
                String response = inputScanner.nextLine().trim().toLowerCase();
                if (response.equals("y")) {
                    sendResign();
                    System.out.println("Resign command sent over WebSocket.");
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
                    Set<ChessMove> legalMoves = (Set<ChessMove>) currentGame.game().validMoves(pos);
                    Set<ChessPosition> legalDestinations = legalMoves.stream()
                            .map(ChessMove::getEndPosition)
                            .collect(Collectors.toSet());
                    drawUnicodeChessBoard(isWhitePerspective, legalDestinations);
                } catch (Exception e) {
                    System.out.println("Error retrieving legal moves: " + e.getMessage());
                }
                continue;
            }
            // Handle move commands.
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
                continue;
            } else {
                // Fallback: if no "move" prefix, treat the command as a move command.
                String[] tokens = commandLine.split("\\s+");
                if (tokens.length < 2 || tokens.length > 3) {
                    System.out.println("Invalid move format. Use: move <start> <end> [promotion]");
                    continue;
                }
                String startSquare = tokens[0];
                String endSquare = tokens[1];
                String promotion = (tokens.length == 3) ? tokens[2] : null;
                sendMove(startSquare, endSquare, promotion);
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