package service;

import dataaccess.ClearDAO;
import dataaccess.DataAccessException;

public class DatabaseService {
    private final ClearDAO clearDAO;

    public DatabaseService(ClearDAO clearDAO) {
        this.clearDAO = clearDAO;
    }

    public void clearDatabase() throws DataAccessException {
        try {
            clearDAO.clear();
        } catch (Exception e) {
            throw new DataAccessException("Error: failed to clear database");
        }
    }
}