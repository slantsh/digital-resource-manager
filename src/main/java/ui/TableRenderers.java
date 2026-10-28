package ui;

import model.ResourceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Custom table cell renderers for clean, readable Swing UI.
 */
public class TableRenderers {

    public static class FavoriteRenderer extends DefaultTableCellRenderer {
        public FavoriteRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean fav = Boolean.TRUE.equals(value);
            setText(fav ? "★" : "☆");
            setFont(getFont().deriveFont(Font.BOLD, 14f));
            if (!isSelected) {
                setForeground(fav ? new Color(220, 150, 0) : Color.LIGHT_GRAY);
            }
            return this;
        }
    }

    public static class TypeBadgeRenderer extends DefaultTableCellRenderer {
        public TypeBadgeRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof ResourceType) {
                ResourceType type = (ResourceType) value;
                setText(type.name());
                setFont(getFont().deriveFont(Font.BOLD, 11f));
                if (!isSelected) {
                    switch (type) {
                        case PDF:
                            setForeground(new Color(180, 40, 40));
                            break;
                        case IMAGE:
                            setForeground(new Color(30, 130, 60));
                            break;
                        case VIDEO:
                            setForeground(new Color(140, 50, 160));
                            break;
                        case LINK:
                            setForeground(new Color(20, 100, 180));
                            break;
                    }
                }
            }
            return this;
        }
    }

    public static class PaddedTextRenderer extends DefaultTableCellRenderer {
        public PaddedTextRenderer() {
            setBorder(new EmptyBorder(4, 6, 4, 6));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(4, 6, 4, 6));
            return c;
        }
    }
}
