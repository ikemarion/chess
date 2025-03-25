import model.AuthData;
import model.GameData;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class ChessClient {
    private final Scanner inputScanner = new Scanner(System.in);
    private final ServerFacade facade = new ServerFacade(8080);

    private boolean loggedIn = false;
    private String authToken = null;
    private String username = null;

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
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

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
                GameData g = games.get(i);
                int index = i + 1;
                gameIndexToId.put(index, g.gameID());

                String white = g.whiteUsername() != null ? g.whiteUsername() : "...";
                String black = g.blackUsername() != null ? g.blackUsername() : "...";
                System.out.printf("%d) %s (white=%s, black=%s)%n", index, g.gameName(), white, black);
            }
        } catch (Exception e) {
            System.out.println("List games failed: " + e.getMessage());
        }
    }

    private void doPlayGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) return;

        System.out.print("What color? [white/black]: ");
        String color = inputScanner.nextLine().trim().toUpperCase();

        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            System.out.println("Invalid color.");
            return;
        }

        try {
            facade.joinGame(authToken, gameId, color);
            drawBoard(color.equals("WHITE"));
        } catch (Exception e) {
            System.out.println("Join failed: " + e.getMessage());
        }
    }

    private void doObserveGame() {
        int gameId = promptGameNumber();
        if (gameId == -1) return;

        drawBoard(true);
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

    private void drawBoard(boolean white) {
        System.out.println("Drawing board from " + (white ? "White's" : "Black's") + " perspective...");
        for (int row = 0; row < 8; row++) {
            int realRow = white ? 8 - row : row + 1;
            System.out.print(realRow + " ");
            for (int col = 0; col < 8; col++) {
                int realCol = white ? col + 1 : 8 - col;
                boolean light = (realRow + realCol) % 2 == 0;
                System.out.print(light ? "[ ]" : "[#]");
            }
            System.out.println();
        }
        System.out.print("   ");
        for (int col = 0; col < 8; col++) {
            char c = (char) ('a' + (white ? col : 7 - col));
            System.out.print(" " + c + " ");
        }
        System.out.println();
    }

    private void quit() {
        System.out.println("Goodbye!");
        System.exit(0);
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
}