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
        // If hashing: String hashed = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        // Then store 'hashed' instead of user.password() below.

        String sql = "INSERT INTO Users (username, password, email) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, user.password());
            stmt.setString(3, user.email());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error in createUser: " + e.getMessage());
        }
    }

    @Override
    public boolean authenticateUser(String username, String password) throws DataAccessException {
        UserData user = getUser(username);
        if (user == null) {
            return false;
        }

        // If hashing:
        // return BCrypt.checkpw(password, user.password());
        // Otherwise (plain text):
        return user.password().equals(password);
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