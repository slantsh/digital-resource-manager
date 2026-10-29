package database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

/**
 * Handles connecting to the SQLite database and creating/migrating tables.
 */
public class DatabaseConnection {

    // Database file location
    private static final String URL = "jdbc:sqlite:resources.db";

    /**
     * Connects to the SQLite database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Helper to compute SHA-256 password hash.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            password = "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Sets up the database tables and runs schema migrations if needed.
     */
    public static void setupDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = OFF;");

            // 1. Create users table
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE COLLATE NOCASE, "
                    + "password_hash TEXT NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ");";
            stmt.execute(createUsersTable);

            // Seed default admin account if no users exist
            boolean hasUsers = false;
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM users LIMIT 1;")) {
                if (rs.next()) {
                    hasUsers = true;
                }
            }
            if (!hasUsers) {
                String adminHash = hashPassword("admin");
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT OR IGNORE INTO users (id, username, password_hash) VALUES (1, 'admin', ?);")) {
                    pstmt.setString(1, adminHash);
                    pstmt.executeUpdate();
                }
            }

            // 2. Setup / Migrate categories table
            boolean categoriesTableExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='categories';")) {
                if (rs.next()) categoriesTableExists = true;
            }

            if (!categoriesTableExists) {
                String createCategoriesTable = "CREATE TABLE IF NOT EXISTS categories ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "name TEXT NOT NULL, "
                        + "user_id INTEGER, "
                        + "FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, "
                        + "UNIQUE (name, user_id)"
                        + ");";
                stmt.execute(createCategoriesTable);
                stmt.execute("INSERT OR IGNORE INTO categories (name, user_id) VALUES ('General', 1);");
            } else {
                // Check if user_id column exists
                boolean catHasUserId = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(categories);")) {
                    while (rs.next()) {
                        if ("user_id".equalsIgnoreCase(rs.getString("name"))) {
                            catHasUserId = true;
                            break;
                        }
                    }
                }
                if (!catHasUserId) {
                    stmt.execute("CREATE TABLE categories_new ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "name TEXT NOT NULL, "
                            + "user_id INTEGER, "
                            + "FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE, "
                            + "UNIQUE (name, user_id)"
                            + ");");
                    stmt.execute("INSERT INTO categories_new (id, name, user_id) SELECT id, name, 1 FROM categories;");
                    stmt.execute("DROP TABLE categories;");
                    stmt.execute("ALTER TABLE categories_new RENAME TO categories;");
                }
            }

            // 3. Setup / Migrate resources table
            boolean resourcesTableExists = false;
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='resources';")) {
                if (rs.next()) resourcesTableExists = true;
            }

            if (!resourcesTableExists) {
                String createResourcesTable = "CREATE TABLE IF NOT EXISTS resources ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "title TEXT NOT NULL, "
                        + "description TEXT, "
                        + "type TEXT NOT NULL, "
                        + "location TEXT NOT NULL, "
                        + "category_id INTEGER, "
                        + "favorite INTEGER DEFAULT 0, "
                        + "user_id INTEGER, "
                        + "FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE, "
                        + "FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE"
                        + ");";
                stmt.execute(createResourcesTable);
            } else {
                boolean resHasUserId = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(resources);")) {
                    while (rs.next()) {
                        if ("user_id".equalsIgnoreCase(rs.getString("name"))) {
                            resHasUserId = true;
                            break;
                        }
                    }
                }
                if (!resHasUserId) {
                    stmt.execute("ALTER TABLE resources ADD COLUMN user_id INTEGER REFERENCES users (id) ON DELETE CASCADE;");
                    stmt.execute("UPDATE resources SET user_id = 1 WHERE user_id IS NULL;");
                }
            }

            // Ensure category General exists for default admin
            stmt.execute("INSERT OR IGNORE INTO categories (name, user_id) VALUES ('General', 1);");

            // Re-enable foreign key support
            stmt.execute("PRAGMA foreign_keys = ON;");

            System.out.println("Database tables created/verified successfully.");
        } catch (SQLException e) {
            System.err.println("Error setting up database: " + e.getMessage());
        }
    }
}
