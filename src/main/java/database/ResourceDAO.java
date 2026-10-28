package database;

import model.Resource;
import model.ResourceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Resource entity.
 * Handles database operations for resources.
 */
public class ResourceDAO {

    public List<Resource> getAllResources() {
        return filterResources(null, null, null, false);
    }

    public List<Resource> filterResources(Integer categoryId, ResourceType type, String query, boolean favoritesOnly) {
        List<Resource> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, title, description, type, location, category_id, favorite FROM resources WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }

        if (favoritesOnly) {
            sql.append(" AND favorite = 1");
        }

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (title LIKE ? OR description LIKE ? OR location LIKE ?)");
            String pattern = "%" + query.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY id DESC;");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    String typeStr = rs.getString("type");
                    ResourceType resType = ResourceType.valueOf(typeStr);
                    String location = rs.getString("location");
                    int catId = rs.getInt("category_id");
                    boolean favorite = rs.getInt("favorite") == 1;

                    list.add(new Resource(id, title, description, resType, location, catId, favorite));
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying resources: " + e.getMessage());
        }

        return list;
    }

    public Resource getResourceById(int id) {
        String sql = "SELECT id, title, description, type, location, category_id, favorite FROM resources WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    ResourceType resType = ResourceType.valueOf(rs.getString("type"));
                    String location = rs.getString("location");
                    int catId = rs.getInt("category_id");
                    boolean favorite = rs.getInt("favorite") == 1;
                    return new Resource(id, title, description, resType, location, catId, favorite);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching resource: " + e.getMessage());
        }
        return null;
    }

    public boolean addResource(Resource res) {
        String sql = "INSERT INTO resources (title, description, type, location, category_id, favorite) VALUES (?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, res.title != null ? res.title : "Untitled");
            pstmt.setString(2, res.description != null ? res.description : "");
            pstmt.setString(3, res.type != null ? res.type.name() : ResourceType.PDF.name());
            pstmt.setString(4, res.location != null ? res.location : "");
            pstmt.setInt(5, res.categoryId);
            pstmt.setInt(6, res.favorite ? 1 : 0);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        res.id = keys.getInt(1);
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding resource: " + e.getMessage());
        }
        return false;
    }

    public boolean updateResource(Resource res) {
        String sql = "UPDATE resources SET title = ?, description = ?, type = ?, location = ?, category_id = ?, favorite = ? WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, res.title);
            pstmt.setString(2, res.description);
            pstmt.setString(3, res.type.name());
            pstmt.setString(4, res.location);
            pstmt.setInt(5, res.categoryId);
            pstmt.setInt(6, res.favorite ? 1 : 0);
            pstmt.setInt(7, res.id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating resource: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteResource(int id) {
        String sql = "DELETE FROM resources WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting resource: " + e.getMessage());
            return false;
        }
    }

    public boolean setFavorite(int id, boolean favorite) {
        String sql = "UPDATE resources SET favorite = ? WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, favorite ? 1 : 0);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling favorite: " + e.getMessage());
            return false;
        }
    }
}
