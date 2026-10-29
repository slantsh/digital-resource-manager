package database;

import model.User;

import java.sql.*;

/**
 * Data Access Object for User entity.
 * Handles authentication and registration.
 */
public class UserDAO {

    /**
     * Authenticates a user with the provided credentials.
     *
     * @param username user's username
     * @param password user's plaintext password
     * @return User object if authentication is successful; null otherwise
     */
    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        username = username.trim();
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }

        String hash = DatabaseConnection.hashPassword(password);
        String sql = "SELECT id, username FROM users WHERE username = ? AND password_hash = ?;";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hash);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication query error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Registers a new user with username and password.
     *
     * @param username desired username
     * @param password desired plaintext password
     * @return newly created User
     * @throws IllegalArgumentException if validation fails or username is already taken
     */
    public User register(String username, String password) throws IllegalArgumentException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        username = username.trim();
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
        if (password == null || password.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters long.");
        }

        if (userExists(username)) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }

        String hash = DatabaseConnection.hashPassword(password);
        String insertUserSql = "INSERT INTO users (username, password_hash) VALUES (?, ?);";
        String insertDefaultCatSql = "INSERT INTO categories (name, user_id) VALUES ('General', ?);";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            int newUserId = -1;

            try (PreparedStatement pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hash);
                int affected = pstmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            newUserId = keys.getInt(1);
                        }
                    }
                }
            }

            if (newUserId == -1) {
                conn.rollback();
                throw new SQLException("Failed to create user record.");
            }

            // Seed default 'General' category for the new user
            try (PreparedStatement pstmtCat = conn.prepareStatement(insertDefaultCatSql)) {
                pstmtCat.setInt(1, newUserId);
                pstmtCat.executeUpdate();
            }

            conn.commit();
            return new User(newUserId, username);

        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            throw new IllegalArgumentException("Failed to register user: " + e.getMessage());
        }
    }

    /**
     * Checks if a username already exists.
     */
    public boolean userExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT id FROM users WHERE username = ? COLLATE NOCASE;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("User exists check error: " + e.getMessage());
        }
        return false;
    }
}
