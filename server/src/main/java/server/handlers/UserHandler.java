package server.handlers;

import service.UserService;
import model.UserData;
import spark.Request;
import spark.Response;
import spark.Route;
import com.google.gson.Gson;
import dataaccess.DataAccessException;

public class UserHandler {
    private UserService userService;
    private final Gson gson = new Gson(); // Add Gson for JSON parsing

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Route register = (Request req, Response res) -> {
        UserData userData = gson.fromJson(req.body(), UserData.class); // Parse JSON request body

        if (userData.username() == null || userData.password() == null || userData.email() == null) {
            res.status(400);  // Return 400 for missing fields
            return gson.toJson(new ErrorResponse("Error: Missing required fields"));
        }

        try {
            var authData = userService.register(userData.username(), userData.password(), userData.email());
            res.status(200);  // Return 200 on successful registration
            return gson.toJson(authData);
        } catch (DataAccessException e) {
            if (e.getMessage().contains("User already exists")) {
                res.status(403);  // Return 403 Forbidden when user already exists
            } else {
                res.status(400);  // Return 400 Bad Request for other errors
            }
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));  // Include the error message
        }
    };

    public Route login = (Request req, Response res) -> {
        UserData userData = gson.fromJson(req.body(), UserData.class); // Parse JSON request body

        if (userData.username() == null || userData.password() == null) {
            res.status(400);
            return gson.toJson(new ErrorResponse("Error: Missing required fields"));
        }

        try {
            var authData = userService.login(userData.username(), userData.password());
            res.status(200);
            return gson.toJson(authData);
        } catch (DataAccessException e) {
            res.status(401); // Unauthorized
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
        }
    };

    public Route logout = (Request req, Response res) -> {
        var authToken = req.headers("authorization");

        try {
            userService.logout(authToken);
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(401);  // Return 401 Unauthorized for invalid token
            return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));  // Include error message in response
        }
    };

    // ErrorResponse class for consistent error responses
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}