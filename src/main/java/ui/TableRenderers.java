package ui;

import model.ResourceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Draws a resource's tags as rounded coloured chips.
     */
    public static class TagChipRenderer extends JComponent implements TableCellRenderer {
        private static final int PAD_X = 7;
        private static final int GAP = 4;
        private static final int CELL_PAD = 6;

        private List<String> tags = List.of();
        private boolean selected;
        private JTable table;

        @Override
        @SuppressWarnings("unchecked")
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            this.table = table;
            this.tags = value instanceof List ? (List<String>) value : List.of();
            this.selected = isSelected;
            setFont(table.getFont().deriveFont(11f));
            setToolTipText(tags.isEmpty() ? null : "#" + String.join("  #", tags));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(selected ? table.getSelectionBackground() : table.getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            List<Rectangle> chips = layoutChips(fm, tags, getHeight());
            for (int i = 0; i < chips.size(); i++) {
                Rectangle r = chips.get(i);
                if (r.x + r.width > getWidth() - CELL_PAD) {
                    // Out of room: show how many tags are hidden
                    g2.setColor(selected ? table.getSelectionForeground() : Color.GRAY);
                    g2.drawString("+" + (chips.size() - i), r.x, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    break;
                }
                Color hue = colorFor(tags.get(i));
                g2.setColor(mix(hue, Color.WHITE, 0.82));
                g2.fillRoundRect(r.x, r.y, r.width, r.height, r.height, r.height);
                g2.setColor(mix(hue, Color.WHITE, 0.35));
                g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, r.height, r.height);
                g2.setColor(new Color(30, 30, 30));
                g2.drawString(tags.get(i), r.x + PAD_X, r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }

        private static List<Rectangle> layoutChips(FontMetrics fm, List<String> tags, int cellHeight) {
            List<Rectangle> chips = new ArrayList<>();
            int chipHeight = fm.getHeight() + 2;
            int y = (cellHeight - chipHeight) / 2;
            int x = CELL_PAD;
            for (String tag : tags) {
                int w = fm.stringWidth(tag) + PAD_X * 2;
                chips.add(new Rectangle(x, y, w, chipHeight));
                x += w + GAP;
            }
            return chips;
        }

        /**
         * Returns the tag whose chip is under the given point of the table, or null.
         */
        public static String tagAt(JTable table, Point p) {
            int row = table.rowAtPoint(p);
            int col = table.columnAtPoint(p);
            if (row < 0 || col < 0 || table.convertColumnIndexToModel(col) != ResourceTableModel.COL_TAGS) {
                return null;
            }
            Object value = table.getValueAt(row, col);
            if (!(value instanceof List)) return null;
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) value;
            Rectangle cell = table.getCellRect(row, col, false);
            FontMetrics fm = table.getFontMetrics(table.getFont().deriveFont(11f));
            List<Rectangle> chips = layoutChips(fm, tags, cell.height);
            for (int i = 0; i < chips.size(); i++) {
                Rectangle r = chips.get(i);
                if (r.x + r.width > cell.width - CELL_PAD) break;
                r.translate(cell.x, cell.y);
                if (r.contains(p)) return tags.get(i);
            }
            return null;
        }
    }

    // Fixed hue set for tags; each tag keeps the same colour everywhere based on its name
    private static final Color[] TAG_HUES = {
            new Color(0x2a78d6), new Color(0xeb6834), new Color(0x1baf7a), new Color(0xeda100),
            new Color(0xe87ba4), new Color(0x008300), new Color(0x6250d6), new Color(0xe34948)
    };

    public static Color colorFor(String tag) {
        int h = tag.toLowerCase().hashCode();
        return TAG_HUES[Math.floorMod(h, TAG_HUES.length)];
    }

    /**
     * Blends color a toward b; amount 0 = a, 1 = b.
     */
    public static Color mix(Color a, Color b, double amount) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
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
