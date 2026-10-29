import database.DatabaseConnection;
import ui.LoginFrame;

import javax.swing.*;
import java.awt.GraphicsEnvironment;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Application entry point: initializes SQLite database and launches the Login GUI.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Initializing Digital Resource Manager...");

        // 1. Verify database connection
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("Connected to SQLite successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return;
        }

        // 2. Set up database schema and run migrations
        System.out.println("Setting up database tables...");
        DatabaseConnection.setupDatabase();
        System.out.println("Database setup complete!");

        // 3. Check for headless environment
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless mode. GUI cannot be displayed.");
            return;
        }

        // 4. Launch Login GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Apply system look and feel for a clean native UI without heavy themes
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
