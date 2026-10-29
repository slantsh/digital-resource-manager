package ui;

import database.CategoryDAO;
import database.ResourceDAO;
import database.TagDAO;
import model.Category;
import model.Resource;
import model.ResourceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistics dashboard: headline numbers plus breakdowns by type, collection and tag.
 */
public class StatsDialog extends JDialog {

    private static final Color SURFACE = new Color(0xfcfcfb);
    private static final Color CARD = Color.WHITE;
    private static final Color CARD_BORDER = new Color(0xe4e3df);
    private static final Color TEXT_PRIMARY = new Color(0x0b0b0b);
    private static final Color TEXT_SECONDARY = new Color(0x52514e);
    private static final Color TEXT_MUTED = new Color(0x8a8984);
    private static final Color SERIES = new Color(0x2a78d6);
    private static final Color SERIES_HOVER = new Color(0x1d5fae);
    private static final Color WARNING = new Color(0xb36b00);

    public StatsDialog(Frame parent, ResourceDAO resourceDAO, CategoryDAO categoryDAO, TagDAO tagDAO) {
        super(parent, "Statistics", true);

        List<Resource> resources = resourceDAO.getAllResources();
        List<Category> categories = categoryDAO.getAllCategories();
        Map<Integer, Integer> categoryCounts = categoryDAO.getResourceCounts();
        Map<String, Integer> tagCounts = tagDAO.getTagCounts();

        int favorites = 0;
        int missing = 0;
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (ResourceType t : ResourceType.values()) {
            byType.put(t.name(), 0);
        }
        for (Resource r : resources) {
            if (r.favorite) favorites++;
            if (isMissingFile(r)) missing++;
            byType.merge(r.type != null ? r.type.name() : ResourceType.PDF.name(), 1, Integer::sum);
        }

        Map<String, Integer> byCollection = new LinkedHashMap<>();
        for (Category c : categories) {
            byCollection.put(c.name, categoryCounts.getOrDefault(c.id, 0));
        }

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(SURFACE);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Headline numbers
        JPanel tiles = new JPanel(new GridLayout(1, 5, 10, 0));
        tiles.setOpaque(false);
        tiles.add(statTile(String.valueOf(resources.size()), "Resources", TEXT_PRIMARY));
        tiles.add(statTile(String.valueOf(favorites), "Favorites", TEXT_PRIMARY));
        tiles.add(statTile(String.valueOf(categories.size()), "Collections", TEXT_PRIMARY));
        tiles.add(statTile(String.valueOf(tagCounts.size()), "Tags", TEXT_PRIMARY));
        tiles.add(missing > 0
                ? statTile("⚠ " + missing, "Missing files", WARNING)
                : statTile("0", "Missing files", TEXT_PRIMARY));
        root.add(tiles, BorderLayout.NORTH);

        // Breakdown charts
        JPanel charts = new JPanel(new GridLayout(1, 3, 10, 0));
        charts.setOpaque(false);
        charts.add(chartCard("By type", new HBarChart(byType, 8, "resources")));
        charts.add(chartCard("By collection", new HBarChart(sortDescending(byCollection), 8, "resources")));
        charts.add(chartCard("Top tags", new HBarChart(tagCounts.isEmpty() ? tagCounts : sortDescending(tagCounts), 8, "resources")));
        root.add(charts, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(closeBtn);
        root.add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(closeBtn);

        setContentPane(root);
        setSize(900, 460);
        setMinimumSize(new Dimension(700, 380));
        setLocationRelativeTo(parent);
    }

    private static boolean isMissingFile(Resource r) {
        if (r.type == ResourceType.LINK || r.location == null) return false;
        String loc = r.location.trim();
        if (loc.isEmpty() || loc.startsWith("http://") || loc.startsWith("https://")) return false;
        return !new File(loc).exists();
    }

    private static Map<String, Integer> sortDescending(Map<String, Integer> data) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(data.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        Map<String, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    private static JPanel card() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        return p;
    }

    private static JPanel statTile(String value, String label, Color valueColor) {
        JPanel p = card();
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 26f));
        v.setForeground(valueColor);
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(12f));
        l.setForeground(TEXT_SECONDARY);
        p.add(v, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);
        return p;
    }

    private static JPanel chartCard(String title, JComponent chart) {
        JPanel p = card();
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 13f));
        t.setForeground(TEXT_PRIMARY);
        p.add(t, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(chart);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    /**
     * Single-series horizontal bar chart with direct value labels and hover tooltips.
     * Rows past maxBars are folded into an "Other" bar.
     */
    private static class HBarChart extends JComponent {
        private static final int ROW_HEIGHT = 26;
        private static final int BAR_THICKNESS = 14;
        private static final int LABEL_MAX_WIDTH = 110;
        private static final int GAP = 8;

        private final List<String> labels = new ArrayList<>();
        private final List<Integer> values = new ArrayList<>();
        private final String unit;
        private int total;
        private int hoverIndex = -1;

        HBarChart(Map<String, Integer> data, int maxBars, String unit) {
            this.unit = unit;
            int i = 0;
            int other = 0;
            int otherCount = 0;
            for (Map.Entry<String, Integer> e : data.entrySet()) {
                total += e.getValue();
                if (i < maxBars - 1 || data.size() <= maxBars) {
                    labels.add(e.getKey());
                    values.add(e.getValue());
                } else {
                    other += e.getValue();
                    otherCount++;
                }
                i++;
            }
            if (otherCount > 0) {
                labels.add("Other (" + otherCount + ")");
                values.add(other);
            }

            setToolTipText("");
            MouseAdapter hover = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = indexAt(e.getPoint());
                    if (idx != hoverIndex) {
                        hoverIndex = idx;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }
            };
            addMouseListener(hover);
            addMouseMotionListener(hover);
        }

        private int indexAt(Point p) {
            // The whole row is the hit target, not just the bar
            int idx = (p.y - 4) / ROW_HEIGHT;
            return (p.y >= 4 && idx >= 0 && idx < labels.size()) ? idx : -1;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int idx = indexAt(e.getPoint());
            if (idx < 0) return null;
            int v = values.get(idx);
            String pct = total > 0 ? String.format("%.0f%%", v * 100.0 / total) : "0%";
            return "<html><b>" + escape(labels.get(idx)) + "</b><br>" + v + " " + unit + " · " + pct + "</html>";
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, Math.max(60, labels.size() * ROW_HEIGHT + 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(CARD);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Font font = getFont() != null ? getFont() : UIManager.getFont("Label.font");
            g2.setFont(font.deriveFont(12f));
            FontMetrics fm = g2.getFontMetrics();

            if (labels.isEmpty() || total == 0) {
                g2.setColor(TEXT_MUTED);
                g2.drawString("No data yet", 0, fm.getAscent() + 6);
                g2.dispose();
                return;
            }

            int labelWidth = 0;
            for (String l : labels) {
                labelWidth = Math.max(labelWidth, Math.min(LABEL_MAX_WIDTH, fm.stringWidth(l)));
            }
            int max = 1;
            for (int v : values) max = Math.max(max, v);
            int valueWidth = fm.stringWidth(String.valueOf(max)) + GAP;
            int barX = labelWidth + GAP;
            int barMaxWidth = Math.max(10, getWidth() - barX - valueWidth - 4);

            for (int i = 0; i < labels.size(); i++) {
                int rowY = 4 + i * ROW_HEIGHT;
                int textY = rowY + (ROW_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
                int v = values.get(i);

                g2.setColor(TEXT_SECONDARY);
                g2.drawString(truncate(labels.get(i), fm, LABEL_MAX_WIDTH), 0, textY);

                int w = (int) Math.round((double) v / max * barMaxWidth);
                if (v > 0) {
                    w = Math.max(w, 3);
                    int barY = rowY + (ROW_HEIGHT - BAR_THICKNESS) / 2;
                    // Rounded data-end, square end at the baseline
                    Area bar = new Area(new RoundRectangle2D.Double(barX, barY, w, BAR_THICKNESS, 8, 8));
                    bar.add(new Area(new Rectangle(barX, barY, Math.min(w, 4), BAR_THICKNESS)));
                    g2.setColor(i == hoverIndex ? SERIES_HOVER : SERIES);
                    g2.fill(bar);
                }

                g2.setColor(TEXT_PRIMARY);
                g2.drawString(String.valueOf(v), barX + w + GAP / 2 + 2, textY);
            }

            // Recessive baseline
            g2.setColor(CARD_BORDER);
            g2.drawLine(barX, 4, barX, 4 + labels.size() * ROW_HEIGHT);
            g2.dispose();
        }

        private static String truncate(String s, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(s) <= maxWidth) return s;
            String ellipsis = "…";
            int end = s.length();
            while (end > 0 && fm.stringWidth(s.substring(0, end) + ellipsis) > maxWidth) end--;
            return s.substring(0, end) + ellipsis;
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
