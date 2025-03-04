package server.handlers;

import service.DatabaseService;
import spark.Request;
import spark.Response;
import spark.Route;

public class ClearHandler {
    private DatabaseService databaseService;

    public ClearHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public Route clear = (Request req, Response res) -> {
        databaseService.clearDatabase();
        res.status(200);
        return "{}";
    };
}