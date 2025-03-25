import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ServerFacade: Provides methods for the Chess client to call server endpoints:
 * - register
 * - login
 * - logout
 * - createGame
 * - listGames
 * - joinGame
 * - clearDB
 *
 * This version calls DELETE /db to clear the database.
 */
public class ServerFacade {

    private final String baseUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    /**
     * Calls DELETE /db, which should invoke ClearDAO on your server
     * to wipe out all tables. If your server uses a different endpoint name,
     * adjust accordingly.
     */
    public void clearDB() throws Exception {
        URL url = new URL(baseUrl + "/db");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");

        int status = conn.getResponseCode();
        if (status == 200) {
            conn.disconnect();
        } else {
            String err = readError(conn);
            conn.disconnect();
            throw new Exception("clearDB failed: " + err);
        }
    }

    // ========== REGISTER ==========
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

    // ========== LOGIN ==========
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

    // ========== LOGOUT ==========
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

    // ========== CREATE GAME ==========
    public void createGame(String authToken, String gameName) throws Exception {
        if (authToken == null) {
            throw new Exception("createGame: No authToken provided");
        }

        var body = new GameData(0, null, null, gameName, null);
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

    // ========== LIST GAMES ==========
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

    // ========== JOIN GAME ==========
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