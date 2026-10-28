package ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;

/**
 * Handles Drag-and-Drop operations for files and URLs.
 */
public class DropTargetHandler extends DropTargetAdapter {

    public interface DropListener {
        void onFilesDropped(List<File> files);
        void onTextOrUrlDropped(String text);
        default void onDragEnter() {}
        default void onDragExit() {}
    }

    private final DropListener listener;

    public DropTargetHandler(DropListener listener) {
        this.listener = listener;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (isSupported(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            if (listener != null) {
                listener.onDragEnter();
            }
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (isSupported(dtde)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        if (listener != null) {
            listener.onDragExit();
        }
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        if (listener != null) {
            listener.onDragExit();
        }

        try {
            Transferable transferable = dtde.getTransferable();

            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files != null && !files.isEmpty() && listener != null) {
                    listener.onFilesDropped(files);
                }
                dtde.dropComplete(true);
                return;
            }

            if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY);
                String text = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                if (text != null && !text.trim().isEmpty() && listener != null) {
                    listener.onTextOrUrlDropped(text.trim());
                }
                dtde.dropComplete(true);
                return;
            }

            dtde.rejectDrop();
        } catch (Exception ex) {
            System.err.println("Drop handling failed: " + ex.getMessage());
            dtde.dropComplete(false);
        }
    }

    private boolean isSupported(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || dtde.isDataFlavorSupported(DataFlavor.stringFlavor);
    }
}
