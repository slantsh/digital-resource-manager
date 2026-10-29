package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for tags.
 * Tags are free-form labels scoped to a user and linked many-to-many with resources.
 * Tags that no longer label any resource are removed automatically.
 */
public class TagDAO {

    public static final int MAX_TAG_LENGTH = 30;

    private final int userId;

    public TagDAO(int userId) {
        this.userId = userId;
    }

    /**
     * Cleans up a user-entered tag: trims it, drops a leading '#', collapses whitespace
     * and strips commas. Returns null if nothing usable is left.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String name = raw.trim();
        while (name.startsWith("#")) {
            name = name.substring(1).trim();
        }
        name = name.replace(",", " ").replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) return null;
        if (name.length() > MAX_TAG_LENGTH) {
            name = name.substring(0, MAX_TAG_LENGTH).trim();
        }
        return name;
    }

    /**
     * Parses a comma-separated tag list (e.g. "exam, #lecture, week 3"), ignoring
     * blanks and case-insensitive duplicates.
     */
    public static List<String> parseList(String csv) {
        Map<String, String> unique = new LinkedHashMap<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String name = normalize(part);
                if (name != null) {
                    unique.putIfAbsent(name.toLowerCase(), name);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Returns every tag in use with the number of resources it labels, ordered by name.
     */
    public Map<String, Integer> getTagCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        String sql = "SELECT t.name, COUNT(rt.resource_id) AS cnt FROM tags t "
                + "JOIN resource_tags rt ON rt.tag_id = t.id "
                + "WHERE t.user_id = ? GROUP BY t.id ORDER BY t.name COLLATE NOCASE ASC;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("name"), rs.getInt("cnt"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tag counts: " + e.getMessage());
        }
        return counts;
    }

    /**
     * Returns the tags of every resource belonging to this user, keyed by resource id.
     */
    public Map<Integer, List<String>> getTagsByResource() {
        Map<Integer, List<String>> map = new HashMap<>();
        String sql = "SELECT rt.resource_id, t.name FROM resource_tags rt "
                + "JOIN tags t ON t.id = rt.tag_id "
                + "WHERE t.user_id = ? ORDER BY t.name COLLATE NOCASE ASC;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.computeIfAbsent(rs.getInt("resource_id"), k -> new ArrayList<>()).add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching resource tags: " + e.getMessage());
        }
        return map;
    }

    /**
     * Replaces the full tag list of one resource.
     */
    public boolean setTags(int resourceId, List<String> names) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM resource_tags WHERE resource_id = ?;")) {
                del.setInt(1, resourceId);
                del.executeUpdate();
            }
            for (String raw : names) {
                String name = normalize(raw);
                if (name != null) {
                    link(conn, resourceId, getOrCreateTagId(conn, name));
                }
            }
            removeUnusedTags(conn);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error setting tags: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adds a tag to each of the given resources.
     */
    public boolean addTag(Collection<Integer> resourceIds, String rawName) {
        String name = normalize(rawName);
        if (name == null) return false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            int tagId = getOrCreateTagId(conn, name);
            for (int resourceId : resourceIds) {
                link(conn, resourceId, tagId);
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding tag: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a tag from each of the given resources.
     */
    public boolean removeTag(Collection<Integer> resourceIds, String name) {
        String sql = "DELETE FROM resource_tags WHERE resource_id = ? "
                + "AND tag_id = (SELECT id FROM tags WHERE name = ? AND user_id = ?);";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int resourceId : resourceIds) {
                    pstmt.setInt(1, resourceId);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, userId);
                    pstmt.executeUpdate();
                }
            }
            removeUnusedTags(conn);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error removing tag: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes every tag from each of the given resources.
     */
    public boolean clearTags(Collection<Integer> resourceIds) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM resource_tags WHERE resource_id = ?;")) {
                for (int resourceId : resourceIds) {
                    pstmt.setInt(1, resourceId);
                    pstmt.executeUpdate();
                }
            }
            removeUnusedTags(conn);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error clearing tags: " + e.getMessage());
            return false;
        }
    }

    /**
     * Renames a tag. If another tag already has the new name, the two are merged.
     */
    public boolean renameTag(String oldName, String rawNewName) {
        String newName = normalize(rawNewName);
        if (newName == null) return false;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            Integer oldId = findTagId(conn, oldName);
            if (oldId == null) return false;
            Integer existingId = findTagId(conn, newName);

            if (existingId == null || existingId.equals(oldId)) {
                // Plain rename (also covers changing only the capitalisation)
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE tags SET name = ? WHERE id = ?;")) {
                    pstmt.setString(1, newName);
                    pstmt.setInt(2, oldId);
                    pstmt.executeUpdate();
                }
            } else {
                // Merge into the existing tag
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT OR IGNORE INTO resource_tags (resource_id, tag_id) SELECT resource_id, ? FROM resource_tags WHERE tag_id = ?;")) {
                    pstmt.setInt(1, existingId);
                    pstmt.setInt(2, oldId);
                    pstmt.executeUpdate();
                }
                deleteTagById(conn, oldId);
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error renaming tag: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a tag and removes it from every resource. The resources themselves are kept.
     */
    public boolean deleteTag(String name) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            Integer id = findTagId(conn, name);
            if (id == null) return false;
            deleteTagById(conn, id);
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting tag: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes links to resources that no longer exist, then tags that label nothing.
     */
    public void removeOrphans() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM resource_tags WHERE resource_id NOT IN (SELECT id FROM resources);");
            }
            removeUnusedTags(conn);
        } catch (SQLException e) {
            System.err.println("Error cleaning up tags: " + e.getMessage());
        }
    }

    // --- internal helpers (run inside the caller's connection/transaction) ---

    private Integer findTagId(Connection conn, String name) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM tags WHERE name = ? AND user_id = ?;")) {
            pstmt.setString(1, name);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private int getOrCreateTagId(Connection conn, String name) throws SQLException {
        Integer id = findTagId(conn, name);
        if (id != null) return id;
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO tags (name, user_id) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Could not create tag: " + name);
    }

    private void link(Connection conn, int resourceId, int tagId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT OR IGNORE INTO resource_tags (resource_id, tag_id) VALUES (?, ?);")) {
            pstmt.setInt(1, resourceId);
            pstmt.setInt(2, tagId);
            pstmt.executeUpdate();
        }
    }

    private void deleteTagById(Connection conn, int id) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM resource_tags WHERE tag_id = ?;")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
        try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM tags WHERE id = ? AND user_id = ?;")) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    private void removeUnusedTags(Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "DELETE FROM tags WHERE user_id = ? AND id NOT IN (SELECT tag_id FROM resource_tags);")) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
}
