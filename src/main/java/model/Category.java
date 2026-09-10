package model;

/**
 * Simple Category class without getters and setters.
 * Uses public fields directly.
 */
public class Category {
    public int id;
    public String name;

    public Category(String name) {
        this.name = name;
    }

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Category [id=" + id + ", name=" + name + "]";
    }
}
