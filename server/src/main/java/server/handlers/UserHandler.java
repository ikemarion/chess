package server.handlers;

import service.UserService;
import model.UserData;
import spark.Request;
import spark.Response;
import spark.Route;
import com.google.gson.Gson;

public class UserHandler {
    private UserService userService;
    private final Gson gson = new Gson(); // Add Gson for JSON parsing

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Route register = (Request req, Response res) -> {
        UserData userData = gson.fromJson(req.body(), UserData.class); // Parse JSON request body

        if (userData.username() == null || userData.password() == null || userData.email() == null) {
            res.status(400);
            return "{ \"message\": \"Error: Missing required fields\" }";
        }

        var authData = userService.register(userData.username(), userData.password(), userData.email()); // Correct call
        res.status(200);
        return gson.toJson(authData);
    };

    public Route login = (Request req, Response res) -> {
        UserData userData = gson.fromJson(req.body(), UserData.class); // Parse JSON request body

        if (userData.username() == null || userData.password() == null) {
            res.status(400);
            return "{ \"message\": \"Error: Missing required fields\" }";
        }

        var authData = userService.login(userData.username(), userData.password()); // Correct call
        res.status(200);
        return gson.toJson(authData);
    };

    public Route logout = (Request req, Response res) -> {
        var authToken = req.headers("authorization");
        userService.logout(authToken);
        res.status(200);
        return "{}";
    };
}