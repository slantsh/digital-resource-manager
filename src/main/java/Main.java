import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Basic entry point: connects to SQLite and sets up the database tables.
 * No mutating, reading, or CRUD demo.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Connecting to SQLite database...");

        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null) {
                System.out.println("Connected to SQLite successfully!");
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return;
        }

        System.out.println("Setting up database tables...");
        DatabaseConnection.setupDatabase();
        System.out.println("Database setup complete!");
    }
}
