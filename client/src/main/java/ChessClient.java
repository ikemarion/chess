import model.AuthData;
import model.GameData;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * ChessClient:
 * - Provides pre-login and post-login menus
 * - Calls ServerFacade for register/login/logout/etc.
 * - Draws an advanced ASCII chessboard in the console
 *   using ANSI color codes for squares and pieces.
 *
 * Phase 5 note:
 *   We do NOT do real gameplay. We just show a standard
 *   initial board from White or Black viewpoint.
 */
public class ChessClient {
    private final Scanner inputScanner = new Scanner(System.in);
    private final ServerFacade facade = new ServerFacade(8080); // Adjust port if needed

    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;

    // Maps the "list index" (1..n) to actual gameID
    private HashMap<Integer, Integer> gameIndexToId = new HashMap<>();

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
                // Catch all exceptions so the client doesn't crash
                System.out.println("Error: " + e.getMessage());
                // For debugging: e.printStackTrace();
            }
        }
    }

    // =====================
    // PRE-LOGIN MENU
    // =====================
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
            System.out.println("Login failed: " + e.getMessage());
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
            System.out.println("Register failed: " + e.getMessage());
        }
    }

    // =====================
    // POST-LOGIN MENU
    // =====================
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
        System.out.println("  create game   - Create a new game in the server");
        System.out.println("  list games    - List existing games on the server");
        System.out.println("  play game     - Join a game by number + color");
        System.out.println("  observe game  - Observe a game by number (white perspective)");
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
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private void doCreateGame() {
        System.out.print("Enter a name for the new game: ");
        String gameName = inputScanner.nextLine().trim();
        try {
            facade.createGame(authToken, gameName);
            System.out.println("Game created: " + gameName);
        } catch (Exception e) {
            System.out.println("Create game failed: " + e.getMessage());
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
            System.out.println("List games failed: " + e.getMessage());
        }
    }

    private void doPlayGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) return;

        System.out.print("What color? [white/black]: ");
        String color = inputScanner.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color. Must be 'white' or 'black'.");
            return;
        }

        try {
            facade.joinGame(authToken, gameId, color.toUpperCase());
            // Draw the fancy ASCII board from the perspective chosen
            boolean isWhite = color.equals("white");
            drawAdvancedBoard(isWhite);
        } catch (Exception e) {
            System.out.println("Join failed: " + e.getMessage());
        }
    }

    private void doObserveGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) return;

        // Observers see from White's perspective
        drawAdvancedBoard(true);
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

    // =======================================
    // ADVANCED ASCII BOARD DRAWING (static)
    // =======================================
    /**
     * Draws a standard chess opening position in ASCII with ANSI color codes,
     * oriented for White or Black.
     */
    private void drawAdvancedBoard(boolean isWhitePerspective) {
        if (isWhitePerspective) {
            System.out.println("Drawing board from White's perspective...");
        } else {
            System.out.println("Drawing board from Black's perspective...");
        }

        // Hardcoded standard arrangement from White's viewpoint:
        // row=0 => rank8 (Black major pieces), row=7 => rank1 (White major)
        String[][] initialSetup = {
                {"R","N","B","Q","K","B","N","R"}, // black major (rank 8)
                {"P","P","P","P","P","P","P","P"}, // black pawns (rank 7)
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {".",".",".",".",".",".",".","."},
                {"p","p","p","p","p","p","p","p"}, // white pawns (rank 2)
                {"r","n","b","q","k","b","n","r"}  // white major (rank 1)
        };

        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            // actual row if White => rowIndex, if Black => 7-rowIndex
            int actualRow = isWhitePerspective ? rowIndex : (7 - rowIndex);
            // rank label (0 => rank 8, 7 => rank 1)
            int rankLabel = 8 - actualRow;

            // Print rank # on left
            System.out.printf("%2d ", rankLabel);

            for (int colIndex = 0; colIndex < 8; colIndex++) {
                // actual col if White => colIndex, if Black => 7-colIndex
                int actualCol = isWhitePerspective ? colIndex : (7 - colIndex);

                boolean isLightSquare = ((actualRow + actualCol) % 2 == 0);
                String piece = initialSetup[actualRow][actualCol];
                if (piece.equals(".")) piece = " ";

                // We'll color squares:
                String bg = isLightSquare ? Ansi.BG_LIGHT : Ansi.BG_DARK;

                // Decide piece color: uppercase is black, lowercase is white
                // We'll interpret uppercase => black => use black text
                // We'll interpret lowercase => white => use white text
                boolean isBlackPiece = piece.matches("[A-Z]"); // e.g. "R" "N" etc.
                String textColor = isBlackPiece ? Ansi.FG_BLACK : Ansi.FG_WHITE;

                // If it's just " " => no piece, we won't add text color
                if (piece.trim().isEmpty()) {
                    textColor = "";
                }

                // Print [square background] + text color + " piece " + reset
                System.out.print(bg + textColor);

                // If piece is single char, pad with spaces => " P "
                String cell = " " + piece + " ";
                System.out.print(cell);

                // reset after each square
                System.out.print(Ansi.RESET);
            }

            // rank label on right
            System.out.printf(" %2d\n", rankLabel);
        }

        // file labels
        System.out.print("   ");
        for (int colIndex = 0; colIndex < 8; colIndex++) {
            char fileLabel = (char) ('a' + (isWhitePerspective ? colIndex : (7 - colIndex)));
            System.out.print(" " + fileLabel + " ");
        }
        System.out.println();
    }

    // =====================
    // ANSI HELPER
    // =====================
    private static class Ansi {
        public static final String RESET = "\u001b[0m";

        // You can pick fancier bright/dim backgrounds
        public static final String BG_LIGHT = "\u001b[47m"; // white background
        public static final String BG_DARK  = "\u001b[40m"; // black background

        public static final String FG_WHITE = "\u001b[37m";
        public static final String FG_BLACK = "\u001b[30m";
    }
}