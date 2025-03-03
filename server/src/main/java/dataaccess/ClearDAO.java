package dataaccess;

public class ClearDAO {
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    private final AuthDAO authDAO;

    public ClearDAO(UserDAO userDAO, gameDAO gameDAO, AuthDAO authDAO){
        this.userDAO = userDAO;
        this.gameDAO = gameDAO;
        this.authDAO = authDAO;
    }

    public void clear(){
        userDAO.clear();
        gameDAO.clear();
        authDAO.clear();
    }
}
