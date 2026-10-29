package model;

/**
 * Simple Resource class without getters and setters.
 * Uses public fields directly.
 */
public class Resource {
    public int id;
    public String title;
    public String description;
    public ResourceType type;
    public String location;
    public int categoryId;
    public boolean favorite;
    public int userId;

    public Resource(String title, String description, ResourceType type, String location, int categoryId, boolean favorite) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
    }

    public Resource(int id, String title, String description, ResourceType type, String location, int categoryId, boolean favorite) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
    }

    public Resource(String title, String description, ResourceType type, String location, int categoryId, boolean favorite, int userId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
        this.userId = userId;
    }

    public Resource(int id, String title, String description, ResourceType type, String location, int categoryId, boolean favorite, int userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.categoryId = categoryId;
        this.favorite = favorite;
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Resource [id=" + id 
                + ", title=" + title 
                + ", type=" + type 
                + ", categoryId=" + categoryId 
                + ", favorite=" + favorite 
                + ", location=" + location 
                + ", userId=" + userId + "]";
    }
}
