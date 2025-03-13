package dataaccess;

import model.UserData;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt; // If you want to do password hashing, otherwise remove

public class MySQLUserDAO implements UserDAO {

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password, email FROM Users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error in getUser: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        // 1) Generate a salted hash of the user’s plain-text password
        String hashedPW = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        // 2) Insert into the database
        String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, hashedPW); // store hashed, not plain text
            stmt.setString(3, user.email());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error in createUser: " + e.getMessage());
        }
    }

    @Override
    public boolean authenticateUser(String username, String plainPassword) throws DataAccessException {
        // Get the user from DB:
        UserData storedUser = getUser(username);
        if (storedUser == null) {
            return false; // no such user
        }

        // storedUser.password() is the hashed password read from DB
        String hashedPW = storedUser.password();
        return BCrypt.checkpw(plainPassword, hashedPW);
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM Users";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error clearing Users table: " + e.getMessage());
        }
    }
}