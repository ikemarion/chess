package server.handlers;

import service.DatabaseService;
import spark.Request;
import spark.Response;
import spark.Route;

public class ClearHandler {
    private DatabaseService databaseService;

    // ✅ Ensure databaseService is assigned properly
    public ClearHandler(DatabaseService databaseService) {
        if (databaseService == null) {  // ✅ Prevents null reference issues
            throw new IllegalArgumentException("DatabaseService cannot be null");
        }
        this.databaseService = databaseService;
    }

    public Route clear = (Request req, Response res) -> {
        databaseService.clearDatabase();
        res.status(200);
        return "{}";
    };
}