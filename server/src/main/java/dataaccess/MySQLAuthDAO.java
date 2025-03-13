package dataaccess;

import model.AuthData;
import java.sql.*;

public class MySQLAuthDAO implements AuthDAO {

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO AuthTokens (authToken, username) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error in createAuth: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM AuthTokens WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error in getAuth: " + e.getMessage());
        }

        throw new DataAccessException("Auth token not found: " + authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM AuthTokens WHERE authToken = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new DataAccessException("No auth token found to delete: " + authToken);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error in deleteAuth: " + e.getMessage());
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM AuthTokens";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error clearing AuthTokens table: " + e.getMessage());
        }
    }
}