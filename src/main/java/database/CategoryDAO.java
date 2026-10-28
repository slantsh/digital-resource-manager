package database;

import model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Category entity.
 * Handles database operations for categories.
 */
public class CategoryDAO {

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name COLLATE NOCASE ASC;";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        return list;
    }

    public Category getCategoryById(int id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getInt("id"), rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding category by id: " + e.getMessage());
        }
        return null;
    }

    public Category addCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String sql = "INSERT INTO categories (name) VALUES (?);";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name.trim());
            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new Category(rs.getInt(1), name.trim());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding category: " + e.getMessage());
        }
        return null;
    }

    public boolean updateCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }
        String sql = "UPDATE categories SET name = ? WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName.trim());
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCategory(int id) {
        // Find a fallback category to reassign resources to, so we don't violate FK or lose resources
        String fallbackSql = "SELECT id FROM categories WHERE id != ? ORDER BY id ASC LIMIT 1;";
        int fallbackId = -1;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmtFallback = conn.prepareStatement(fallbackSql)) {
                pstmtFallback.setInt(1, id);
                try (ResultSet rs = pstmtFallback.executeQuery()) {
                    if (rs.next()) {
                        fallbackId = rs.getInt("id");
                    }
                }
            }

            if (fallbackId != -1) {
                String reassignSql = "UPDATE resources SET category_id = ? WHERE category_id = ?;";
                try (PreparedStatement pstmtReassign = conn.prepareStatement(reassignSql)) {
                    pstmtReassign.setInt(1, fallbackId);
                    pstmtReassign.setInt(2, id);
                    pstmtReassign.executeUpdate();
                }
            } else {
                // If this is the only category, delete its resources or prevent deletion
                String deleteResourcesSql = "DELETE FROM resources WHERE category_id = ?;";
                try (PreparedStatement pstmtDel = conn.prepareStatement(deleteResourcesSql)) {
                    pstmtDel.setInt(1, id);
                    pstmtDel.executeUpdate();
                }
            }

            String deleteCategorySql = "DELETE FROM categories WHERE id = ?;";
            try (PreparedStatement pstmtDelCat = conn.prepareStatement(deleteCategorySql)) {
                pstmtDelCat.setInt(1, id);
                pstmtDelCat.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }

    public Map<Integer, Integer> getResourceCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT category_id, COUNT(*) as cnt FROM resources GROUP BY category_id;";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getInt("category_id"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting resource counts: " + e.getMessage());
        }
        return counts;
    }

    public int getTotalResourceCount() {
        String sql = "SELECT COUNT(*) FROM resources;";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total count: " + e.getMessage());
        }
        return 0;
    }

    public int getFavoriteResourceCount() {
        String sql = "SELECT COUNT(*) FROM resources WHERE favorite = 1;";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting favorite count: " + e.getMessage());
        }
        return 0;
    }
}
