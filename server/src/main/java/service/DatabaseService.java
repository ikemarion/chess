package service;

import dataaccess.ClearDAO;
import dataaccess.DataAccessException;

public class DatabaseService {
    private final ClearDAO clearDAO;

    public DatabaseService(ClearDAO clearDAO) {
        this.clearDAO = clearDAO;
    }

    public void clear() throws DataAccessException {
        clearDAO.clear();
    }
}