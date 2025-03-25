import java.util.Scanner;

/**
 * A console-based client that:
 * - Prelogin: help, login, register, quit
 * - Postlogin: help, logout, create game, list games, play game, observe game
 */
public class ChessClient {
    private final Scanner inputScanner = new Scanner(System.in);
    private final ServerFacade facade;

    // Track login state
    private boolean loggedIn = false;
    // Track current auth token for server calls
    private String authToken = null;

    public ChessClient(ServerFacade facade) {
        this.facade = facade;
    }

    /**
     * Main loop: if loggedOut => handlePrelogin, else handlePostlogin.
     */
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
                // Catch any unexpected exceptions so client doesn't crash
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ============================
    // PRELOGIN UI
    // ============================
    private void handlePrelogin() {
        System.out.print("prelogin> ");
        String commandLine = inputScanner.nextLine().trim().toLowerCase();
        switch (commandLine) {
            case "help":
                showPreloginHelp();
                break;
            case "login":
                doLogin();
                break;
            case "register":
                doRegister();
                break;
            case "quit":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command: " + commandLine);
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
        String username = inputScanner.nextLine().trim();
        System.out.print("Password: ");
        String password = inputScanner.nextLine().trim();

        try {
            var auth = facade.login(username, password);
            authToken = auth.authToken();
            loggedIn = true;
            System.out.println("Logged in as: " + auth.username());
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void doRegister() {
        System.out.print("Username: ");
        String username = inputScanner.nextLine().trim();
        System.out.print("Password: ");
        String password = inputScanner.nextLine().trim();
        System.out.print("Email: ");
        String email = inputScanner.nextLine().trim();

        try {
            var auth = facade.register(username, password, email);
            authToken = auth.authToken();
            loggedIn = true;
            System.out.println("Registered & logged in as: " + auth.username());
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    // ============================
    // POSTLOGIN UI
    // ============================
    private void handlePostlogin() {
        System.out.print("postlogin> ");
        String commandLine = inputScanner.nextLine().trim().toLowerCase();
        switch (commandLine) {
            case "help":
                showPostloginHelp();
                break;
            case "logout":
                doLogout();
                break;
            case "create game":
                doCreateGame();
                break;
            case "list games":
                doListGames();
                break;
            case "play game":
                doPlayGame();
                break;
            case "observe game":
                doObserveGame();
                break;
            case "quit":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Unknown command: " + commandLine);
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
            loggedIn = false;
            authToken = null;
            System.out.println("Logged out successfully.");
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
            var games = facade.listGames(authToken);
            if (games.isEmpty()) {
                System.out.println("No games found on server.");
            } else {
                System.out.println("Games on server:");
                // We'll number them for the user
                int index = 1;
                for (var g : games) {
                    String white = g.whiteUsername() == null ? "-" : g.whiteUsername();
                    String black = g.blackUsername() == null ? "-" : g.blackUsername();
                    System.out.println(index + ") " + g.gameName()
                            + " (white=" + white + ", black=" + black + ")");
                    index++;
                }
            }
        } catch (Exception e) {
            System.out.println("List games failed: " + e.getMessage());
        }
    }

    private void doPlayGame() {
        System.out.print("Which game number do you want to join? ");
        String choice = inputScanner.nextLine().trim();
        int gameNumber;
        try {
            gameNumber = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Try again.");
            return;
        }
        System.out.print("What color? [white/black]: ");
        String color = inputScanner.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color. Must be 'white' or 'black'.");
            return;
        }

        // We'll need to map the gameNumber the user typed to the actual gameID
        // so we must do facade.listGames(...) again or keep a cached list
        try {
            var games = facade.listGames(authToken);
            if (gameNumber < 1 || gameNumber > games.size()) {
                System.out.println("No such game index: " + gameNumber);
                return;
            }
            var chosenGame = games.get(gameNumber - 1);
            int gameID = chosenGame.gameID();

            facade.joinGame(authToken, gameID, color);
            System.out.println("Joined game #" + gameNumber + " (" + chosenGame.gameName()
                    + ") as " + color + ".");
            // Now draw the board from that perspective
            drawBoard(color.equals("white"));
        } catch (Exception e) {
            System.out.println("Play game failed: " + e.getMessage());
        }
    }

    private void doObserveGame() {
        System.out.print("Which game number to observe? ");
        String choice = inputScanner.nextLine().trim();
        int gameNumber;
        try {
            gameNumber = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number. Try again.");
            return;
        }

        // We'll not actually call a separate endpoint,
        // just treat it as "observer sees from white perspective"
        // but you might do facade.observeGame(...) if you have it

        try {
            var games = facade.listGames(authToken);
            if (gameNumber < 1 || gameNumber > games.size()) {
                System.out.println("No such game index: " + gameNumber);
                return;
            }
            var chosenGame = games.get(gameNumber - 1);

            System.out.println("Observing game #" + gameNumber + " (" + chosenGame.gameName()
                    + ") as an observer (white perspective).");
            drawBoard(true);
        } catch (Exception e) {
            System.out.println("Observe game failed: " + e.getMessage());
        }
    }

    /**
     * Phase 5 requirement: Draw a dummy board with correct orientation
     */
    private void drawBoard(boolean whitePerspective) {
        if (whitePerspective) {
            System.out.println("Drawing board from White's perspective...");
            for (int row = 8; row >= 1; row--) {
                System.out.print(row + " ");
                for (char col = 'a'; col <= 'h'; col++) {
                    boolean isLightSquare = ((row + col) % 2 == 0);
                    if (isLightSquare) {
                        System.out.print("[ ]");
                    } else {
                        System.out.print("[#]");
                    }
                }
                System.out.println();
            }
            System.out.println("   a  b  c  d  e  f  g  h ");
        } else {
            System.out.println("Drawing board from Black's perspective...");
            for (int row = 1; row <= 8; row++) {
                System.out.print(row + " ");
                for (char col = 'h'; col >= 'a'; col--) {
                    boolean isLightSquare = ((row + col) % 2 == 0);
                    if (isLightSquare) {
                        System.out.print("[ ]");
                    } else {
                        System.out.print("[#]");
                    }
                }
                System.out.println();
            }
            System.out.println("   h  g  f  e  d  c  b  a ");
        }
    }
}
