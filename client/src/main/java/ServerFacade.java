package client;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ServerFacade {

    private final String baseUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    // Register
    public AuthData register(String username, String password, String email) throws Exception {
        UserData body = new UserData(username, password, email);
        String jsonBody = gson.toJson(body);

        URL url = new URL(baseUrl + "/user");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();
        if (status == 200) {
            String respBody = new String(conn.getInputStream().readAllBytes(), "UTF-8");
            conn.disconnect();
            return gson.fromJson(respBody, AuthData.class);
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("Register failed: " + errMsg);
        }
    }

    // Login
    public AuthData login(String username, String password) throws Exception {
        UserData body = new UserData(username, password, null);
        String jsonBody = gson.toJson(body);

        URL url = new URL(baseUrl + "/session");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();
        if (status == 200) {
            String resp = new String(conn.getInputStream().readAllBytes(), "UTF-8");
            conn.disconnect();
            return gson.fromJson(resp, AuthData.class);
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("Login failed: " + errMsg);
        }
    }

    // Logout
    public void logout(String authToken) throws Exception {
        if (authToken == null) {
            throw new Exception("logout: No authToken provided");
        }

        URL url = new URL(baseUrl + "/session");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", authToken);

        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("Logout failed: " + errMsg);
        }
    }

    // Create game
    public void createGame(String authToken, String gameName) throws Exception {
        if (authToken == null) {
            throw new Exception("createGame: No authToken provided");
        }
        GameData body = new GameData(0, null, null, gameName, null);
        String jsonBody = gson.toJson(body);

        URL url = new URL(baseUrl + "/game");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", authToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("createGame failed: " + errMsg);
        }
    }

    // List games
    private static class GamesWrapper {
        List<GameData> games;
    }

    public List<GameData> listGames(String authToken) throws Exception {
        if (authToken == null) {
            throw new Exception("listGames: No authToken provided");
        }
        URL url = new URL(baseUrl + "/game");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", authToken);

        int status = conn.getResponseCode();
        if (status == 200) {
            String resp = new String(conn.getInputStream().readAllBytes(), "UTF-8");
            conn.disconnect();
            GamesWrapper w = gson.fromJson(resp, GamesWrapper.class);
            return (w != null && w.games != null) ? w.games : new ArrayList<>();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("listGames failed: " + errMsg);
        }
    }

    // Join game
    private static class JoinRequest {
        int gameID;
        String playerColor;
        JoinRequest(int gameID, String playerColor) {
            this.gameID = gameID;
            this.playerColor = playerColor;
        }
    }

    public void joinGame(String authToken, int gameID, String color) throws Exception {
        if (authToken == null) {
            throw new Exception("joinGame: No authToken provided");
        }
        JoinRequest body = new JoinRequest(gameID, color);
        String jsonBody = gson.toJson(body);
        URL url = new URL(baseUrl + "/game");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", authToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("joinGame failed: " + errMsg);
        }
    }

    // getGame: Since there is no dedicated endpoint for a single game,
    // we list all games and return the one matching gameID.
    public GameData getGame(String authToken, int gameID) throws Exception {
        List<GameData> games = listGames(authToken);
        for (GameData g : games) {
            if (g.gameID() == gameID) {
                return g;
            }
        }
        throw new Exception("Game not found for ID: " + gameID);
    }

    // New Method: makeMove
    private static class MoveCommandPayload {
        String commandType = "MAKE_MOVE";
        String authToken;
        int gameID;
        Move move;
    }
    private static class Move {
        String startPosition;
        String endPosition;
        String promotionPiece;
        Move(String start, String end, String promotion) {
            this.startPosition = start;
            this.endPosition = end;
            this.promotionPiece = promotion;
        }
    }
    public void makeMove(String authToken, int gameID, String start, String end, String promotion) throws Exception {
        if (authToken == null) {
            throw new Exception("makeMove: No authToken provided");
        }
        MoveCommandPayload payload = new MoveCommandPayload();
        payload.authToken = authToken;
        payload.gameID = gameID;
        payload.move = new Move(start, end, promotion);
        String jsonBody = gson.toJson(payload);

        URL url = new URL(baseUrl + "/game/move");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");  // Using PUT as required.
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", authToken);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("makeMove failed: " + errMsg);
        }
    }

    // New Methods: leaveGame and resignGame
    // We now use dedicated endpoints for these actions.
    private static class GameCommandPayload {
        String commandType; // "LEAVE" or "RESIGN"
        String authToken;
        int gameID;
        String playerColor;
        GameCommandPayload(String commandType, String authToken, int gameID, String playerColor) {
            this.commandType = commandType;
            this.authToken = authToken;
            this.gameID = gameID;
            this.playerColor = playerColor;
        }
    }

    public void leaveGame(String authToken, int gameID, String playerColor) throws Exception {
        if (authToken == null) {
            throw new Exception("leaveGame: No authToken provided");
        }
        GameCommandPayload payload = new GameCommandPayload("LEAVE", authToken, gameID, playerColor);
        String jsonBody = gson.toJson(payload);
        URL url = new URL(baseUrl + "/game/leave"); // Dedicated endpoint for leaving.
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", authToken);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("leaveGame failed: " + errMsg);
        }
    }

    public void resignGame(String authToken, int gameID, String playerColor) throws Exception {
        if (authToken == null) {
            throw new Exception("resignGame: No authToken provided");
        }
        GameCommandPayload payload = new GameCommandPayload("RESIGN", authToken, gameID, playerColor);
        String jsonBody = gson.toJson(payload);
        URL url = new URL(baseUrl + "/game/resign"); // Dedicated endpoint for resigning.
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", authToken);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String errMsg = readError(conn);
            conn.disconnect();
            throw new Exception("resignGame failed: " + errMsg);
        }
    }

    private String readError(HttpURLConnection conn) {
        try {
            var err = conn.getErrorStream();
            if (err != null) {
                return new String(err.readAllBytes(), "UTF-8");
            }
        } catch (Exception e) {
        }
        return "Unknown error";
    }
}