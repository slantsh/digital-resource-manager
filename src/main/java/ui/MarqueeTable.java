package ui;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * JTable with file-explorer style selection: multi-select via Ctrl/Shift click,
 * plus a rubber-band (marquee) box when dragging from empty space below the rows.
 */
public class MarqueeTable extends JTable {

    private Point marqueeAnchor;
    private Rectangle marqueeRect;
    private int[] baseSelection = new int[0];

    public MarqueeTable(TableModel model) {
        super(model);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setFillsViewportHeight(true);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)
                && rowAtPoint(e.getPoint()) == -1) {
            requestFocusInWindow();
            marqueeAnchor = e.getPoint();
            marqueeRect = new Rectangle(marqueeAnchor);
            boolean ctrl = (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0;
            if (!ctrl) {
                clearSelection();
            }
            baseSelection = getSelectedRows();
            return;
        }
        if (e.getID() == MouseEvent.MOUSE_RELEASED && marqueeAnchor != null) {
            marqueeAnchor = null;
            marqueeRect = null;
            repaint();
            return;
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && marqueeAnchor != null) {
            updateMarquee(e.getPoint());
            return;
        }
        super.processMouseMotionEvent(e);
    }

    private void updateMarquee(Point p) {
        int x = Math.min(marqueeAnchor.x, p.x);
        int y = Math.min(marqueeAnchor.y, p.y);
        marqueeRect = new Rectangle(x, y, Math.abs(p.x - marqueeAnchor.x), Math.abs(p.y - marqueeAnchor.y));
        scrollRectToVisible(new Rectangle(p.x, p.y, 1, 1));

        ListSelectionModel sm = getSelectionModel();
        sm.setValueIsAdjusting(true);
        sm.clearSelection();
        for (int row : baseSelection) {
            sm.addSelectionInterval(row, row);
        }
        if (getRowCount() > 0) {
            int top = rowAtPoint(new Point(0, marqueeRect.y));
            int bottom = rowAtPoint(new Point(0, marqueeRect.y + marqueeRect.height));
            // Points past the last row map to -1; clamp them to the edges of the table
            if (top == -1 && marqueeRect.y < 0) top = 0;
            if (bottom == -1) bottom = getRowCount() - 1;
            if (top != -1 && top <= bottom) {
                sm.addSelectionInterval(top, bottom);
            }
        }
        sm.setValueIsAdjusting(false);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (marqueeRect != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color c = getSelectionBackground() != null ? getSelectionBackground() : new Color(0, 120, 215);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g2.fill(marqueeRect);
            g2.setColor(c);
            g2.draw(marqueeRect);
            g2.dispose();
        }
    }
}
