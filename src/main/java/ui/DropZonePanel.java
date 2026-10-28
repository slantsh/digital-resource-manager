package ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * A clean, dedicated drop area for importing files or links.
 * Clicking the panel also triggers the file chooser.
 */
public class DropZonePanel extends JPanel {

    private final JLabel label;
    private final Border normalBorder;
    private final Border hoverBorder;
    private Runnable clickAction;

    public DropZonePanel(DropTargetHandler.DropListener dropListener, Runnable clickAction) {
        this.clickAction = clickAction;
        setLayout(new BorderLayout());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label = new JLabel("📥 Drag & drop files or web links here to add them (or click to browse)", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12f));

        normalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground") : Color.GRAY, 1.5f, 5f, 3f, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        );

        hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(new Color(0, 120, 215), 2.0f, 4f, 2f, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        );

        setBorder(normalBorder);
        add(label, BorderLayout.CENTER);

        // Click to browse
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (DropZonePanel.this.clickAction != null) {
                    DropZonePanel.this.clickAction.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorder(normalBorder);
            }
        });

        // Setup drop target
        new DropTarget(this, new DropTargetHandler(new DropTargetHandler.DropListener() {
            @Override
            public void onFilesDropped(List<File> files) {
                setBorder(normalBorder);
                if (dropListener != null) {
                    dropListener.onFilesDropped(files);
                }
            }

            @Override
            public void onTextOrUrlDropped(String text) {
                setBorder(normalBorder);
                if (dropListener != null) {
                    dropListener.onTextOrUrlDropped(text);
                }
            }

            @Override
            public void onDragEnter() {
                setBorder(hoverBorder);
                if (dropListener != null) {
                    dropListener.onDragEnter();
                }
            }

            @Override
            public void onDragExit() {
                setBorder(normalBorder);
                if (dropListener != null) {
                    dropListener.onDragExit();
                }
            }
        }));
    }

    public void setClickAction(Runnable clickAction) {
        this.clickAction = clickAction;
    }
}
