package database;

import model.Resource;
import model.ResourceType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Resource entity.
 * Handles database operations for resources scoped to a specific user.
 */
public class ResourceDAO {

    private final int userId;

    public ResourceDAO() {
        this(1);
    }

    public ResourceDAO(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public List<Resource> getAllResources() {
        return filterResources(null, null, null, false);
    }

    public List<Resource> filterResources(Integer categoryId, ResourceType type, String query, boolean favoritesOnly) {
        return filterResources(categoryId, type, query, favoritesOnly, null);
    }

    /**
     * @param tag only include resources carrying this tag (null = any)
     */
    public List<Resource> filterResources(Integer categoryId, ResourceType type, String query, boolean favoritesOnly, String tag) {
        List<Resource> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, title, description, type, location, category_id, favorite, user_id FROM resources WHERE user_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(userId);

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

        if (tag != null) {
            sql.append(" AND id IN (SELECT rt.resource_id FROM resource_tags rt JOIN tags t ON t.id = rt.tag_id"
                    + " WHERE t.user_id = ? AND t.name = ?)");
            params.add(userId);
            params.add(tag);
        }

        if (query != null && !query.trim().isEmpty()) {
            // Search also matches tag names, so typing "exam" finds everything tagged #exam
            sql.append(" AND (title LIKE ? OR description LIKE ? OR location LIKE ?"
                    + " OR id IN (SELECT rt.resource_id FROM resource_tags rt JOIN tags t ON t.id = rt.tag_id"
                    + " WHERE t.user_id = ? AND t.name LIKE ?))");
            String q = query.trim();
            String pattern = "%" + q + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(userId);
            // A leading '#' searches tags only by exact-prefix, e.g. "#ex" matches #exam
            params.add(q.startsWith("#") ? q.substring(1) + "%" : pattern);
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
                    int resUserId = rs.getInt("user_id");

                    list.add(new Resource(id, title, description, resType, location, catId, favorite, resUserId));
                }
            }
        } catch (Exception e) {
            System.err.println("Error querying resources: " + e.getMessage());
        }

        Map<Integer, List<String>> tagsByResource = new TagDAO(userId).getTagsByResource();
        for (Resource r : list) {
            r.tags = tagsByResource.getOrDefault(r.id, new ArrayList<>());
        }

        return list;
    }

    public Resource getResourceById(int id) {
        String sql = "SELECT id, title, description, type, location, category_id, favorite, user_id FROM resources WHERE id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String description = rs.getString("description");
                    ResourceType resType = ResourceType.valueOf(rs.getString("type"));
                    String location = rs.getString("location");
                    int catId = rs.getInt("category_id");
                    boolean favorite = rs.getInt("favorite") == 1;
                    int resUserId = rs.getInt("user_id");
                    return new Resource(id, title, description, resType, location, catId, favorite, resUserId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching resource: " + e.getMessage());
        }
        return null;
    }

    public boolean addResource(Resource res) {
        String sql = "INSERT INTO resources (title, description, type, location, category_id, favorite, user_id) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int targetUserId = res.userId > 0 ? res.userId : this.userId;
            pstmt.setString(1, res.title != null ? res.title : "Untitled");
            pstmt.setString(2, res.description != null ? res.description : "");
            pstmt.setString(3, res.type != null ? res.type.name() : ResourceType.PDF.name());
            pstmt.setString(4, res.location != null ? res.location : "");
            pstmt.setInt(5, res.categoryId);
            pstmt.setInt(6, res.favorite ? 1 : 0);
            pstmt.setInt(7, targetUserId);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                res.userId = targetUserId;
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
        String sql = "UPDATE resources SET title = ?, description = ?, type = ?, location = ?, category_id = ?, favorite = ? WHERE id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, res.title);
            pstmt.setString(2, res.description);
            pstmt.setString(3, res.type.name());
            pstmt.setString(4, res.location);
            pstmt.setInt(5, res.categoryId);
            pstmt.setInt(6, res.favorite ? 1 : 0);
            pstmt.setInt(7, res.id);
            pstmt.setInt(8, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating resource: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteResource(int id) {
        String sql = "DELETE FROM resources WHERE id = ? AND user_id = ?;";
        boolean deleted;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            deleted = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting resource: " + e.getMessage());
            return false;
        }
        if (deleted) {
            new TagDAO(userId).removeOrphans();
        }
        return deleted;
    }

    public boolean setCategory(int id, int categoryId) {
        String sql = "UPDATE resources SET category_id = ? WHERE id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            pstmt.setInt(2, id);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error moving resource: " + e.getMessage());
            return false;
        }
    }

    public boolean setFavorite(int id, boolean favorite) {
        String sql = "UPDATE resources SET favorite = ? WHERE id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, favorite ? 1 : 0);
            pstmt.setInt(2, id);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling favorite: " + e.getMessage());
            return false;
        }
    }
}
