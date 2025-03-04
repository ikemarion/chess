package service;

import dataaccess.ClearDAO;

public class DatabaseService {
    private final ClearDAO clearDAO;

    public DatabaseService(ClearDAO clearDAO) {
        this.clearDAO = clearDAO;
    }

    public void clearDatabase() {
        clearDAO.clear();
    }
}
