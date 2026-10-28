package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles connecting to the SQLite database and creating tables.
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
     * Sets up the database tables if they do not already exist.
     */
    public static void setupDatabase() {
        String createCategoriesTable = "CREATE TABLE IF NOT EXISTS categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE"
                + ");";

        String createResourcesTable = "CREATE TABLE IF NOT EXISTS resources ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT NOT NULL, "
                + "description TEXT, "
                + "type TEXT NOT NULL, "
                + "location TEXT NOT NULL, "
                + "category_id INTEGER, "
                + "favorite INTEGER DEFAULT 0, "
                + "FOREIGN KEY (category_id) REFERENCES categories (id)"
                + ");";

        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();

            // Enable foreign key support in SQLite
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Create tables
            stmt.execute(createCategoriesTable);
            stmt.execute(createResourcesTable);

            // Seed default category if empty
            stmt.execute("INSERT OR IGNORE INTO categories (id, name) VALUES (1, 'General');");

            stmt.close();
            conn.close();
            System.out.println("Database tables created/verified successfully.");
        } catch (SQLException e) {
            System.out.println("Error setting up database: " + e.getMessage());
        }
    }
}
