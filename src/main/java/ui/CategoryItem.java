package ui;

public class CategoryItem {
    public static final int ID_ALL = -1;
    public static final int ID_FAVORITES = -2;

    public final int id;
    public final String name;
    public final int count;

    public CategoryItem(int id, String name, int count) {
        this.id = id;
        this.name = name;
        this.count = count;
    }

    @Override
    public String toString() {
        if (id == ID_ALL) {
            return "📁 All Resources (" + count + ")";
        } else if (id == ID_FAVORITES) {
            return "⭐ Favorites (" + count + ")";
        } else {
            return "📂 " + name + " (" + count + ")";
        }
    }

    public boolean isSpecial() {
        return id < 0;
    }
}
