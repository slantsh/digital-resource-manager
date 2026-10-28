package ui;

import model.Resource;
import model.ResourceType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table model for displaying Resources in a JTable.
 */
public class ResourceTableModel extends AbstractTableModel {

    private final String[] columnNames = {"Fav", "Type", "Title", "Category", "Location"};
    private List<Resource> resources = new ArrayList<>();
    private Map<Integer, String> categoryNames = new HashMap<>();

    public interface FavoriteToggleListener {
        void onFavoriteToggled(Resource resource, boolean newFavorite);
    }

    private FavoriteToggleListener favoriteToggleListener;

    public void setFavoriteToggleListener(FavoriteToggleListener listener) {
        this.favoriteToggleListener = listener;
    }

    public void setData(List<Resource> resources, Map<Integer, String> categoryNames) {
        this.resources = resources != null ? resources : new ArrayList<>();
        this.categoryNames = categoryNames != null ? categoryNames : new HashMap<>();
        fireTableDataChanged();
    }

    public Resource getResourceAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < resources.size()) {
            return resources.get(rowIndex);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return resources.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        } else if (columnIndex == 1) {
            return ResourceType.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0; // Favorite is directly clickable in table
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Resource r = resources.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return r.favorite;
            case 1:
                return r.type != null ? r.type : ResourceType.PDF;
            case 2:
                return r.title;
            case 3:
                String catName = categoryNames.get(r.categoryId);
                return catName != null ? catName : "General";
            case 4:
                return r.location;
            default:
                return "";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && rowIndex >= 0 && rowIndex < resources.size()) {
            Resource r = resources.get(rowIndex);
            boolean newFav = (Boolean) aValue;
            r.favorite = newFav;
            fireTableCellUpdated(rowIndex, columnIndex);
            if (favoriteToggleListener != null) {
                favoriteToggleListener.onFavoriteToggled(r, newFav);
            }
        }
    }
}
