package server.handlers;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import service.UserService;
import model.AuthData;
import spark.Request;
import spark.Response;
import spark.Route;

public class UserHandler {
    private final UserService userService;
    private final Gson gson = new Gson();

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    public Route register = (Request req, Response res) -> {
        try {
            var requestData = gson.fromJson(req.body(), RegisterRequest.class);
            AuthData authData = userService.register(requestData.username(), requestData.password(), requestData.email());
            res.status(200);
            return gson.toJson(authData);
        } catch (DataAccessException e) {
            res.status(400);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    public Route login = (Request req, Response res) -> {
        try {
            var requestData = gson.fromJson(req.body(), LoginRequest.class);
            AuthData authData = userService.login(requestData.username(), requestData.password());
            res.status(200);
            return gson.toJson(authData);
        } catch (DataAccessException e) {
            res.status(401);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    public Route logout = (Request req, Response res) -> {
        try {
            String authToken = req.headers("authorization");
            userService.logout(authToken);
            res.status(200);
            return "{}";
        } catch (DataAccessException e) {
            res.status(401);
            return gson.toJson(new ErrorResponse(e.getMessage()));
        }
    };

    private record RegisterRequest(String username, String password, String email) {}
    private record LoginRequest(String username, String password) {}
    private record ErrorResponse(String message) {}
}