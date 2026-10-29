package ui;

import database.CategoryDAO;
import database.ResourceDAO;
import database.TagDAO;
import model.Category;
import model.Resource;
import model.ResourceType;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DropTarget;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window for Digital Resource Manager.
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private final CategoryDAO categoryDAO;
    private final ResourceDAO resourceDAO;
    private final TagDAO tagDAO;

    // UI Components
    private DefaultListModel<CategoryItem> categoryListModel;
    private JList<CategoryItem> categoryList;
    private DefaultListModel<String> tagListModel;
    private JList<String> tagList;
    private Map<String, Integer> tagCounts = new HashMap<>();
    private boolean suppressTagEvents;
    private ResourceTableModel tableModel;
    private JTable resourceTable;
    private DropZonePanel dropZonePanel;
    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JCheckBox favoritesFilterCheck;
    private JLabel statusCountLabel;
    private JLabel statusActivityLabel;
    private int totalResourceCount;

    public MainFrame() {
        this(new User(1, "admin"));
    }

    public MainFrame(User currentUser) {
        super("Digital Resource Manager - " + (currentUser != null ? currentUser.username : "Default"));
        this.currentUser = currentUser != null ? currentUser : new User(1, "admin");
        this.categoryDAO = new CategoryDAO(this.currentUser.id);
        this.resourceDAO = new ResourceDAO(this.currentUser.id);
        this.tagDAO = new TagDAO(this.currentUser.id);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1060, 680);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);

        initMenuBar();
        initUI();
        initGlobalDragAndDrop();

        // Initial data load
        refreshCategories();
        refreshResources();
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem addResItem = new JMenuItem("New Resource...", KeyEvent.VK_N);
        addResItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        addResItem.addActionListener(e -> showAddResourceDialog());
        fileMenu.add(addResItem);

        JMenuItem importFilesItem = new JMenuItem("Import Files...", KeyEvent.VK_O);
        importFilesItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        importFilesItem.addActionListener(e -> chooseAndImportFiles());
        fileMenu.add(importFilesItem);

        JMenuItem screenshotItem = new JMenuItem("Save Screenshot (F12)", KeyEvent.VK_S);
        screenshotItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        screenshotItem.addActionListener(e -> saveScreenshot());
        fileMenu.add(screenshotItem);

        fileMenu.addSeparator();

        JMenuItem logoutItem = new JMenuItem("Log Out...", KeyEvent.VK_L);
        logoutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        JMenuItem editItem = new JMenuItem("Edit Selected Resource", KeyEvent.VK_E);
        editItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        editItem.addActionListener(e -> editSelectedResource());
        editMenu.add(editItem);

        JMenuItem deleteItem = new JMenuItem("Delete Selected", KeyEvent.VK_D);
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteItem.addActionListener(e -> deleteSelectedResource());
        editMenu.add(deleteItem);

        JMenuItem favToggleItem = new JMenuItem("Toggle Favorite", KeyEvent.VK_T);
        favToggleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        favToggleItem.addActionListener(e -> toggleSelectedFavorite());
        editMenu.add(favToggleItem);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem refreshItem = new JMenuItem("Refresh", KeyEvent.VK_R);
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshItem.addActionListener(e -> {
            refreshCategories();
            refreshResources();
        });
        viewMenu.add(refreshItem);

        JMenuItem statsItem = new JMenuItem("Statistics...", KeyEvent.VK_S);
        statsItem.addActionListener(e -> showStatistics());
        viewMenu.add(statsItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About Digital Resource Manager");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 0));

        // 1. Top Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground") : Color.LIGHT_GRAY),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JButton addBtn = new JButton("➕ Add Resource");
        addBtn.addActionListener(e -> showAddResourceDialog());
        toolbar.add(addBtn);

        toolbar.add(Box.createHorizontalStrut(5));

        JButton importBtn = new JButton("📂 Import Files");
        importBtn.addActionListener(e -> chooseAndImportFiles());
        toolbar.add(importBtn);

        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalStrut(15));

        toolbar.add(new JLabel("Type: "));
        typeFilterCombo = new JComboBox<>(new String[]{"All Types", "PDF", "IMAGE", "VIDEO", "LINK"});
        typeFilterCombo.setMaximumSize(new Dimension(110, 28));
        typeFilterCombo.addActionListener(e -> refreshResources());
        toolbar.add(typeFilterCombo);

        toolbar.add(Box.createHorizontalStrut(15));

        favoritesFilterCheck = new JCheckBox("★ Favorites Only");
        favoritesFilterCheck.addActionListener(e -> refreshResources());
        toolbar.add(favoritesFilterCheck);

        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalStrut(15));

        toolbar.add(new JLabel("🔍 Search: "));
        searchField = new JTextField(16);
        searchField.setMaximumSize(new Dimension(200, 28));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refreshResources(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refreshResources(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refreshResources(); }
        });
        toolbar.add(searchField);

        JButton clearSearchBtn = new JButton("✖");
        clearSearchBtn.setToolTipText("Clear search");
        clearSearchBtn.setMargin(new Insets(2, 6, 2, 6));
        clearSearchBtn.addActionListener(e -> searchField.setText(""));
        toolbar.add(clearSearchBtn);

        toolbar.add(Box.createHorizontalGlue());

        JButton statsBtn = new JButton("📊");
        statsBtn.setToolTipText("Statistics");
        statsBtn.setMargin(new Insets(2, 6, 2, 6));
        statsBtn.setFocusable(false);
        statsBtn.addActionListener(e -> showStatistics());
        toolbar.add(statsBtn);

        toolbar.add(Box.createHorizontalStrut(5));

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            refreshCategories();
            refreshResources();
        });
        toolbar.add(refreshBtn);

        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalStrut(12));

        JLabel userBadge = new JLabel("👤 " + currentUser.username);
        userBadge.setFont(userBadge.getFont().deriveFont(Font.BOLD));
        userBadge.setToolTipText("Active user account");
        toolbar.add(userBadge);

        toolbar.add(Box.createHorizontalStrut(8));

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.setToolTipText("Log out and switch account");
        logoutBtn.addActionListener(e -> logout());
        toolbar.add(logoutBtn);

        contentPane.add(toolbar, BorderLayout.NORTH);

        // 2. Center SplitPane (Categories Sidebar + Resources View)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(230);
        splitPane.setContinuousLayout(true);

        // --- Sidebar (Categories) ---
        JPanel sidebarPanel = new JPanel(new BorderLayout(5, 5));
        sidebarPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel catHeaderLabel = new JLabel("Collections & Categories");
        catHeaderLabel.setFont(catHeaderLabel.getFont().deriveFont(Font.BOLD, 12f));
        sidebarPanel.add(catHeaderLabel, BorderLayout.NORTH);

        categoryListModel = new DefaultListModel<>();
        categoryList = new JList<>(categoryListModel);
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshResources();
            }
        });

        // Sidebar right-click popup
        JPopupMenu catPopupMenu = new JPopupMenu();
        JMenuItem renameCatItem = new JMenuItem("Rename Category");
        renameCatItem.addActionListener(e -> renameSelectedCategory());
        JMenuItem deleteCatItem = new JMenuItem("Delete Category");
        deleteCatItem.addActionListener(e -> deleteSelectedCategory());
        catPopupMenu.add(renameCatItem);
        catPopupMenu.add(deleteCatItem);

        categoryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = categoryList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        categoryList.setSelectedIndex(index);
                        CategoryItem item = categoryListModel.get(index);
                        renameCatItem.setEnabled(!item.isSpecial());
                        deleteCatItem.setEnabled(!item.isSpecial());
                        catPopupMenu.show(categoryList, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane catScrollPane = new JScrollPane(categoryList);
        sidebarPanel.add(catScrollPane, BorderLayout.CENTER);

        // Category Actions Toolbar
        JPanel catButtonsPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        JButton addCatBtn = new JButton("+ New");
        addCatBtn.setToolTipText("Create a new category");
        addCatBtn.addActionListener(e -> promptNewCategory());

        JButton renameCatBtn = new JButton("Rename");
        renameCatBtn.setToolTipText("Rename selected category");
        renameCatBtn.addActionListener(e -> renameSelectedCategory());

        JButton deleteCatBtn = new JButton("Delete");
        deleteCatBtn.setToolTipText("Delete selected category");
        deleteCatBtn.addActionListener(e -> deleteSelectedCategory());

        catButtonsPanel.add(addCatBtn);
        catButtonsPanel.add(renameCatBtn);
        catButtonsPanel.add(deleteCatBtn);
        sidebarPanel.add(catButtonsPanel, BorderLayout.SOUTH);

        JSplitPane sidebarSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sidebarPanel, initTagsPanel());
        sidebarSplit.setResizeWeight(0.65);
        sidebarSplit.setBorder(null);
        sidebarSplit.setContinuousLayout(true);

        splitPane.setLeftComponent(sidebarSplit);

        // --- Main Content (DropZone + Table) ---
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 8));
        mainContentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Drop Zone Panel at top of main view
        dropZonePanel = new DropZonePanel(new DropTargetHandler.DropListener() {
            @Override
            public void onFilesDropped(List<File> files) {
                importDroppedFiles(files);
            }

            @Override
            public void onTextOrUrlDropped(String text) {
                handleDroppedTextOrUrl(text);
            }
        }, this::chooseAndImportFiles);
        mainContentPanel.add(dropZonePanel, BorderLayout.NORTH);

        // Table
        tableModel = new ResourceTableModel();
        tableModel.setFavoriteToggleListener((res, newFav) -> {
            resourceDAO.setFavorite(res.id, newFav);
            refreshCategories();
            setStatusActivity((newFav ? "Added to" : "Removed from") + " favorites: " + res.title);
        });

        resourceTable = new MarqueeTable(tableModel);
        resourceTable.setRowHeight(26);
        resourceTable.setAutoCreateRowSorter(true);
        resourceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateStatusCount();
        });
        resourceTable.getTableHeader().setReorderingAllowed(false);

        // Column settings
        resourceTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        resourceTable.getColumnModel().getColumn(0).setMaxWidth(55);
        resourceTable.getColumnModel().getColumn(0).setCellRenderer(new TableRenderers.FavoriteRenderer());

        resourceTable.getColumnModel().getColumn(1).setPreferredWidth(75);
        resourceTable.getColumnModel().getColumn(1).setMaxWidth(90);
        resourceTable.getColumnModel().getColumn(1).setCellRenderer(new TableRenderers.TypeBadgeRenderer());

        resourceTable.getColumnModel().getColumn(2).setPreferredWidth(230);
        resourceTable.getColumnModel().getColumn(2).setCellRenderer(new TableRenderers.PaddedTextRenderer());

        resourceTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        resourceTable.getColumnModel().getColumn(3).setMaxWidth(160);
        resourceTable.getColumnModel().getColumn(3).setCellRenderer(new TableRenderers.PaddedTextRenderer());

        resourceTable.getColumnModel().getColumn(ResourceTableModel.COL_TAGS).setPreferredWidth(170);
        resourceTable.getColumnModel().getColumn(ResourceTableModel.COL_TAGS).setCellRenderer(new TableRenderers.TagChipRenderer());

        resourceTable.getColumnModel().getColumn(5).setPreferredWidth(260);
        resourceTable.getColumnModel().getColumn(5).setCellRenderer(new TableRenderers.PaddedTextRenderer());

        // Table interaction: Double click to open
        resourceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)
                        && (e.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)) == 0) {
                    String tag = TableRenderers.TagChipRenderer.tagAt(resourceTable, e.getPoint());
                    if (tag != null) {
                        tagList.setSelectedValue(tag, true);
                        return;
                    }
                }
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = resourceTable.rowAtPoint(e.getPoint());
                    if (viewRow >= 0 && resourceTable.columnAtPoint(e.getPoint()) != 0) {
                        int modelRow = resourceTable.convertRowIndexToModel(viewRow);
                        Resource r = tableModel.getResourceAt(modelRow);
                        if (r != null) {
                            openResource(r);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showTableContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showTableContextMenu(e);
            }
        });

        // Esc clears the selection, like a file explorer
        resourceTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSelection");
        resourceTable.getActionMap().put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resourceTable.clearSelection();
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(resourceTable);
        mainContentPanel.add(tableScrollPane, BorderLayout.CENTER);

        splitPane.setRightComponent(mainContentPanel);
        contentPane.add(splitPane, BorderLayout.CENTER);

        // 3. Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground") != null
                        ? UIManager.getColor("Separator.foreground") : Color.LIGHT_GRAY),
                new EmptyBorder(5, 10, 5, 10)
        ));

        statusCountLabel = new JLabel("Loading...");
        statusActivityLabel = new JLabel("Ready");
        statusActivityLabel.setForeground(Color.GRAY);

        statusBar.add(statusCountLabel, BorderLayout.WEST);
        statusBar.add(statusActivityLabel, BorderLayout.EAST);

        contentPane.add(statusBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    /**
     * Builds the right-click menu for the current selection. Right-clicking an unselected row
     * selects just that row; right-clicking inside the selection keeps it (file-explorer style).
     */
    private void showTableContextMenu(MouseEvent e) {
        int row = resourceTable.rowAtPoint(e.getPoint());
        if (row == -1) {
            resourceTable.clearSelection();
        } else if (!resourceTable.isRowSelected(row)) {
            resourceTable.setRowSelectionInterval(row, row);
        }

        List<Resource> selected = getSelectedResources();
        JPopupMenu popupMenu = new JPopupMenu();

        if (selected.isEmpty()) {
            JMenuItem addItem = new JMenuItem("New Resource...");
            addItem.addActionListener(ev -> showAddResourceDialog());
            JMenuItem importItem = new JMenuItem("Import Files...");
            importItem.addActionListener(ev -> chooseAndImportFiles());
            JMenuItem selectAllItem = new JMenuItem("Select All");
            selectAllItem.setEnabled(resourceTable.getRowCount() > 0);
            selectAllItem.addActionListener(ev -> resourceTable.selectAll());
            popupMenu.add(addItem);
            popupMenu.add(importItem);
            popupMenu.addSeparator();
            popupMenu.add(selectAllItem);
            popupMenu.show(resourceTable, e.getX(), e.getY());
            return;
        }

        int n = selected.size();
        boolean single = n == 1;
        String suffix = single ? "" : " (" + n + ")";

        JMenuItem openItem = new JMenuItem(single ? "Open Resource" : "Open " + n + " Resources");
        openItem.setFont(openItem.getFont().deriveFont(Font.BOLD));
        openItem.addActionListener(ev -> openResources(selected));

        JMenuItem openFolderItem = new JMenuItem("Show Containing Folder");
        openFolderItem.setEnabled(single);
        openFolderItem.addActionListener(ev -> openContainingFolder(selected.get(0)));

        boolean allFavorite = selected.stream().allMatch(r -> r.favorite);
        JMenuItem favItem = new JMenuItem((allFavorite ? "Remove from Favorites" : "Add to Favorites") + suffix);
        favItem.addActionListener(ev -> setFavorite(selected, !allFavorite));

        JMenu collectionMenu = new JMenu("Add to Collection" + suffix);
        for (Category cat : categoryDAO.getAllCategories()) {
            JMenuItem catItem = new JMenuItem(cat.name);
            catItem.setEnabled(!selected.stream().allMatch(r -> r.categoryId == cat.id));
            catItem.addActionListener(ev -> moveToCategory(selected, cat));
            collectionMenu.add(catItem);
        }
        collectionMenu.addSeparator();
        JMenuItem newCollectionItem = new JMenuItem("New Collection...");
        newCollectionItem.addActionListener(ev -> {
            String name = JOptionPane.showInputDialog(this, "Enter name for new collection:", "New Collection", JOptionPane.PLAIN_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                Category created = categoryDAO.addCategory(name.trim());
                if (created != null) {
                    moveToCategory(selected, created);
                } else {
                    JOptionPane.showMessageDialog(this, "A category with that name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        collectionMenu.add(newCollectionItem);

        JMenu tagsMenu = new JMenu("Tags" + suffix);
        List<Integer> ids = idsOf(selected);
        for (String tag : tagCounts.keySet()) {
            // Checked only when every selected resource has the tag; clicking adds it to all or removes it from all
            boolean all = selected.stream().allMatch(r -> r.tags.stream().anyMatch(tag::equalsIgnoreCase));
            JCheckBoxMenuItem tagItem = new JCheckBoxMenuItem(tag, tagDot(tag), all);
            tagItem.addActionListener(ev -> {
                if (all) {
                    tagDAO.removeTag(ids, tag);
                } else {
                    tagDAO.addTag(ids, tag);
                }
                afterTagChange(ids, (all ? "Removed tag #" : "Added tag #") + tag + (single ? "" : " (" + n + " resources)"));
            });
            tagsMenu.add(tagItem);
        }
        if (!tagCounts.isEmpty()) {
            tagsMenu.addSeparator();
        }
        JMenuItem newTagItem = new JMenuItem("New Tag...");
        newTagItem.addActionListener(ev -> {
            String input = JOptionPane.showInputDialog(this,
                    "Tag name (separate several with commas):", "New Tag", JOptionPane.PLAIN_MESSAGE);
            List<String> names = TagDAO.parseList(input);
            if (!names.isEmpty()) {
                for (String name : names) {
                    tagDAO.addTag(ids, name);
                }
                afterTagChange(ids, "Added tag " + "#" + String.join(", #", names));
            }
        });
        tagsMenu.add(newTagItem);
        JMenuItem clearTagsItem = new JMenuItem("Remove All Tags");
        clearTagsItem.setEnabled(selected.stream().anyMatch(r -> !r.tags.isEmpty()));
        clearTagsItem.addActionListener(ev -> {
            tagDAO.clearTags(ids);
            afterTagChange(ids, "Removed all tags" + (single ? "" : " from " + n + " resources"));
        });
        tagsMenu.add(clearTagsItem);

        JMenuItem editItem = new JMenuItem("Edit Details...");
        editItem.setEnabled(single);
        editItem.addActionListener(ev -> editSelectedResource());

        JMenuItem copyPathItem = new JMenuItem(single ? "Copy Path / URL" : "Copy Paths / URLs" + suffix);
        copyPathItem.addActionListener(ev -> {
            StringBuilder sb = new StringBuilder();
            for (Resource r : selected) {
                if (sb.length() > 0) sb.append(System.lineSeparator());
                sb.append(r.location);
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            setStatusActivity(single ? "Copied location to clipboard: " + selected.get(0).location
                    : "Copied " + n + " locations to clipboard.");
        });

        JMenuItem deleteItem = new JMenuItem(single ? "Delete Resource" : "Delete " + n + " Resources");
        deleteItem.addActionListener(ev -> deleteSelectedResource());

        popupMenu.add(openItem);
        popupMenu.add(openFolderItem);
        popupMenu.addSeparator();
        popupMenu.add(favItem);
        popupMenu.add(collectionMenu);
        popupMenu.add(tagsMenu);
        popupMenu.add(editItem);
        popupMenu.add(copyPathItem);
        popupMenu.addSeparator();
        popupMenu.add(deleteItem);

        popupMenu.show(resourceTable, e.getX(), e.getY());
    }

    private JPanel initTagsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel tagHeaderLabel = new JLabel("Tags");
        tagHeaderLabel.setFont(tagHeaderLabel.getFont().deriveFont(Font.BOLD, 12f));
        JButton clearTagBtn = new JButton("Clear");
        clearTagBtn.setToolTipText("Stop filtering by tag (Esc)");
        clearTagBtn.setMargin(new Insets(0, 6, 0, 6));
        clearTagBtn.setFocusable(false);
        clearTagBtn.setEnabled(false);

        JPanel header = new JPanel(new BorderLayout());
        header.add(tagHeaderLabel, BorderLayout.WEST);
        header.add(clearTagBtn, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        tagListModel = new DefaultListModel<>();
        tagList = new JList<>(tagListModel);
        tagList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tagList.setToolTipText("Click a tag to show only resources with it. Right-click resources to tag them.");
        tagList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String tag = (String) value;
                setText("#" + tag + "  (" + tagCounts.getOrDefault(tag, 0) + ")");
                setIcon(tagDot(tag));
                setIconTextGap(6);
                return this;
            }
        });
        tagList.addListSelectionListener(e -> {
            clearTagBtn.setEnabled(tagList.getSelectedValue() != null);
            if (!e.getValueIsAdjusting() && !suppressTagEvents) {
                refreshResources();
            }
        });
        clearTagBtn.addActionListener(e -> tagList.clearSelection());

        tagList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSelection");
        tagList.getActionMap().put("clearSelection", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tagList.clearSelection();
            }
        });

        JPopupMenu tagPopupMenu = new JPopupMenu();
        JMenuItem renameTagItem = new JMenuItem("Rename Tag...");
        renameTagItem.addActionListener(e -> renameSelectedTag());
        JMenuItem deleteTagItem = new JMenuItem("Delete Tag");
        deleteTagItem.addActionListener(e -> deleteSelectedTag());
        tagPopupMenu.add(renameTagItem);
        tagPopupMenu.add(deleteTagItem);

        tagList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = tagList.locationToIndex(e.getPoint());
                    if (index >= 0 && tagList.getCellBounds(index, index).contains(e.getPoint())) {
                        tagList.setSelectedIndex(index);
                        tagPopupMenu.show(tagList, e.getX(), e.getY());
                    }
                }
            }
        });

        panel.add(new JScrollPane(tagList), BorderLayout.CENTER);
        return panel;
    }

    /**
     * A small filled circle in the tag's colour, used beside tag names in lists and menus.
     */
    private static Icon tagDot(String tag) {
        Color color = TableRenderers.colorFor(tag);
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(x + 1, y + 1, 8, 8);
                g2.dispose();
            }

            @Override
            public int getIconWidth() { return 10; }

            @Override
            public int getIconHeight() { return 10; }
        };
    }

    private void initGlobalDragAndDrop() {
        DropTargetHandler.DropListener listener = new DropTargetHandler.DropListener() {
            @Override
            public void onFilesDropped(List<File> files) {
                importDroppedFiles(files);
            }

            @Override
            public void onTextOrUrlDropped(String text) {
                handleDroppedTextOrUrl(text);
            }
        };

        // Enable dropping directly onto the table and this window
        new DropTarget(resourceTable, new DropTargetHandler(listener));
        new DropTarget(this, new DropTargetHandler(listener));
    }

    // --- Data loading & refresh ---

    public void refreshCategories() {
        refreshTags();

        int selectedId = CategoryItem.ID_ALL;
        CategoryItem currentSelection = categoryList.getSelectedValue();
        if (currentSelection != null) {
            selectedId = currentSelection.id;
        }

        categoryListModel.clear();
        int totalCount = categoryDAO.getTotalResourceCount();
        int favCount = categoryDAO.getFavoriteResourceCount();
        Map<Integer, Integer> counts = categoryDAO.getResourceCounts();

        categoryListModel.addElement(new CategoryItem(CategoryItem.ID_ALL, "All Resources", totalCount));
        categoryListModel.addElement(new CategoryItem(CategoryItem.ID_FAVORITES, "Favorites", favCount));

        List<Category> categories = categoryDAO.getAllCategories();
        int indexToSelect = 0;
        int i = 2;
        for (Category cat : categories) {
            int cnt = counts.getOrDefault(cat.id, 0);
            CategoryItem item = new CategoryItem(cat.id, cat.name, cnt);
            categoryListModel.addElement(item);
            if (cat.id == selectedId) {
                indexToSelect = i;
            }
            i++;
        }

        if (selectedId == CategoryItem.ID_FAVORITES) {
            indexToSelect = 1;
        }

        categoryList.setSelectedIndex(indexToSelect);
    }

    private void refreshTags() {
        String selected = tagList.getSelectedValue();
        tagCounts = tagDAO.getTagCounts();

        // Rebuilding the list would fire selection events; the caller refreshes resources itself
        suppressTagEvents = true;
        tagListModel.clear();
        for (String tag : tagCounts.keySet()) {
            tagListModel.addElement(tag);
            if (tag.equalsIgnoreCase(selected)) {
                tagList.setSelectedIndex(tagListModel.size() - 1);
            }
        }
        suppressTagEvents = false;
    }

    public void refreshResources() {
        CategoryItem selectedCatItem = categoryList.getSelectedValue();
        Integer categoryIdFilter = null;
        boolean favoritesOnly = favoritesFilterCheck.isSelected();

        if (selectedCatItem != null) {
            if (selectedCatItem.id == CategoryItem.ID_FAVORITES) {
                favoritesOnly = true;
            } else if (selectedCatItem.id > 0) {
                categoryIdFilter = selectedCatItem.id;
            }
        }

        ResourceType typeFilter = null;
        int typeIdx = typeFilterCombo.getSelectedIndex();
        if (typeIdx > 0) {
            typeFilter = ResourceType.valueOf((String) typeFilterCombo.getSelectedItem());
        }

        String query = searchField.getText().trim();

        String tagFilter = tagList.getSelectedValue();

        List<Resource> resources = resourceDAO.filterResources(categoryIdFilter, typeFilter, query, favoritesOnly, tagFilter);

        // Fetch category map
        List<Category> allCategories = categoryDAO.getAllCategories();
        Map<Integer, String> catMap = new HashMap<>();
        for (Category c : allCategories) {
            catMap.put(c.id, c.name);
        }

        tableModel.setData(resources, catMap);
        totalResourceCount = categoryDAO.getTotalResourceCount();
        updateStatusCount();
    }

    private void updateStatusCount() {
        String text = "Total: " + totalResourceCount + " resources | Showing: " + tableModel.getRowCount();
        int selectedCount = resourceTable.getSelectedRowCount();
        if (selectedCount > 1) {
            text += " | Selected: " + selectedCount;
        }
        statusCountLabel.setText(text);
    }

    private Resource getSelectedResource() {
        int viewRow = resourceTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = resourceTable.convertRowIndexToModel(viewRow);
            return tableModel.getResourceAt(modelRow);
        }
        return null;
    }

    private static List<Integer> idsOf(List<Resource> resources) {
        List<Integer> ids = new ArrayList<>();
        for (Resource r : resources) ids.add(r.id);
        return ids;
    }

    /**
     * Re-selects the given resources after the table has been reloaded (those still visible).
     */
    private void reselectResources(Collection<Integer> ids) {
        ListSelectionModel sm = resourceTable.getSelectionModel();
        sm.setValueIsAdjusting(true);
        sm.clearSelection();
        for (int viewRow = 0; viewRow < resourceTable.getRowCount(); viewRow++) {
            Resource r = tableModel.getResourceAt(resourceTable.convertRowIndexToModel(viewRow));
            if (r != null && ids.contains(r.id)) {
                sm.addSelectionInterval(viewRow, viewRow);
            }
        }
        sm.setValueIsAdjusting(false);
    }

    private List<Resource> getSelectedResources() {
        List<Resource> list = new ArrayList<>();
        for (int viewRow : resourceTable.getSelectedRows()) {
            Resource r = tableModel.getResourceAt(resourceTable.convertRowIndexToModel(viewRow));
            if (r != null) list.add(r);
        }
        return list;
    }

    private Integer getCurrentCategoryId() {
        CategoryItem item = categoryList.getSelectedValue();
        if (item != null && item.id > 0) {
            return item.id;
        }
        List<Category> all = categoryDAO.getAllCategories();
        return all.isEmpty() ? 1 : all.get(0).id;
    }

    // --- Actions ---

    private void showAddResourceDialog() {
        ResourceDialog dialog = new ResourceDialog(this, resourceDAO, categoryDAO, null, getCurrentCategoryId());
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshCategories();
            refreshResources();
            setStatusActivity("Resource added successfully.");
        }
    }

    private void editSelectedResource() {
        Resource r = getSelectedResource();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a resource to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ResourceDialog dialog = new ResourceDialog(this, resourceDAO, categoryDAO, r, r.categoryId);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshCategories();
            refreshResources();
            setStatusActivity("Updated resource: " + r.title);
        }
    }

    private void deleteSelectedResource() {
        List<Resource> selected = getSelectedResources();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a resource to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String prompt = selected.size() == 1
                ? "Are you sure you want to delete '" + selected.get(0).title + "' from the manager?"
                : "Are you sure you want to delete these " + selected.size() + " resources from the manager?";
        int opt = JOptionPane.showConfirmDialog(this,
                prompt,
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            int deleted = 0;
            for (Resource r : selected) {
                if (resourceDAO.deleteResource(r.id)) deleted++;
            }
            refreshCategories();
            refreshResources();
            if (deleted == selected.size()) {
                setStatusActivity(deleted == 1 ? "Deleted: " + selected.get(0).title : "Deleted " + deleted + " resources.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete " + (selected.size() - deleted) + " resource(s).", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleSelectedFavorite() {
        List<Resource> selected = getSelectedResources();
        if (!selected.isEmpty()) {
            // Like a file explorer: if any item isn't a favorite, favorite them all; otherwise unfavorite all
            boolean allFavorite = selected.stream().allMatch(r -> r.favorite);
            setFavorite(selected, !allFavorite);
        }
    }

    private void setFavorite(List<Resource> resources, boolean favorite) {
        for (Resource r : resources) {
            r.favorite = favorite;
            resourceDAO.setFavorite(r.id, favorite);
        }
        refreshCategories();
        reselectResources(idsOf(resources));
        String what = resources.size() == 1 ? resources.get(0).title : resources.size() + " resources";
        setStatusActivity((favorite ? "Added to" : "Removed from") + " favorites: " + what);
    }

    private void afterTagChange(List<Integer> ids, String message) {
        refreshCategories();
        reselectResources(ids);
        setStatusActivity(message);
    }

    private void renameSelectedTag() {
        String tag = tagList.getSelectedValue();
        if (tag == null) return;
        String input = (String) JOptionPane.showInputDialog(this, "Rename tag #" + tag + " to:",
                "Rename Tag", JOptionPane.PLAIN_MESSAGE, null, null, tag);
        String newName = TagDAO.normalize(input);
        if (newName == null || newName.equals(tag)) return;

        boolean merging = tagCounts.keySet().stream().anyMatch(t -> t.equalsIgnoreCase(newName) && !t.equalsIgnoreCase(tag));
        if (merging) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "A tag named #" + newName + " already exists.\nMerge #" + tag + " into it?",
                    "Merge Tags", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) return;
        }

        if (tagDAO.renameTag(tag, newName)) {
            suppressTagEvents = true;
            tagList.clearSelection();
            suppressTagEvents = false;
            refreshCategories();
            for (int i = 0; i < tagListModel.size(); i++) {
                if (tagListModel.get(i).equalsIgnoreCase(newName)) {
                    tagList.setSelectedIndex(i);
                    break;
                }
            }
            setStatusActivity((merging ? "Merged #" + tag + " into #" : "Renamed tag #" + tag + " to #") + newName);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to rename tag.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedTag() {
        String tag = tagList.getSelectedValue();
        if (tag == null) return;
        int count = tagCounts.getOrDefault(tag, 0);
        int opt = JOptionPane.showConfirmDialog(this,
                "Remove tag #" + tag + " from " + count + " resource(s)?\nThe resources themselves will be kept.",
                "Delete Tag", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            if (tagDAO.deleteTag(tag)) {
                refreshCategories();
                setStatusActivity("Deleted tag #" + tag);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete tag.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStatistics() {
        new StatsDialog(this, resourceDAO, categoryDAO, tagDAO).setVisible(true);
    }

    private void moveToCategory(List<Resource> resources, Category category) {
        int moved = 0;
        for (Resource r : resources) {
            if (resourceDAO.setCategory(r.id, category.id)) moved++;
        }
        refreshCategories();
        refreshResources();
        setStatusActivity("Added " + moved + " resource(s) to collection: " + category.name);
    }

    private void openResources(List<Resource> resources) {
        if (resources.size() > 5) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Open all " + resources.size() + " selected resources?",
                    "Open Multiple",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) return;
        }
        for (Resource r : resources) {
            openResource(r);
        }
    }

    private void chooseAndImportFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Files to Import");
        chooser.setMultiSelectionEnabled(true);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            if (files != null && files.length > 0) {
                importDroppedFiles(List.of(files));
            }
        }
    }

    public void importDroppedFiles(List<File> files) {
        if (files == null || files.isEmpty()) return;

        int catId = getCurrentCategoryId();
        int importedCount = 0;

        for (File f : files) {
            if (f.isDirectory()) {
                // If directory, optionally skip or import files within
                File[] children = f.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.isFile()) {
                            if (addSingleFile(child, catId)) importedCount++;
                        }
                    }
                }
            } else {
                if (addSingleFile(f, catId)) importedCount++;
            }
        }

        refreshCategories();
        refreshResources();
        setStatusActivity("Imported " + importedCount + " file(s) into category.");
    }

    private boolean addSingleFile(File f, int categoryId) {
        String name = f.getName();
        String title = name;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            title = name.substring(0, dot);
        }
        ResourceType type = ResourceType.fromFileName(name);
        String location = f.getAbsolutePath();

        Resource res = new Resource(title, "Imported file (" + (f.length() / 1024) + " KB)", type, location, categoryId, false);
        return resourceDAO.addResource(res);
    }

    public void handleDroppedTextOrUrl(String text) {
        if (text == null || text.trim().isEmpty()) return;
        text = text.trim();

        // Check if it's a file path
        File testFile = new File(text);
        if (testFile.exists()) {
            importDroppedFiles(List.of(testFile));
            return;
        }

        // Open Add Resource Dialog pre-populated with URL/Link
        ResourceDialog dialog = new ResourceDialog(this, resourceDAO, categoryDAO, null, getCurrentCategoryId());
        dialog.setUrlDetails(text);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshCategories();
            refreshResources();
            setStatusActivity("Added link resource.");
        }
    }

    private void openResource(Resource r) {
        if (r == null || r.location == null || r.location.trim().isEmpty()) return;

        try {
            if (r.type == ResourceType.LINK || r.location.startsWith("http://") || r.location.startsWith("https://")) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(r.location));
                    setStatusActivity("Opened in browser: " + r.location);
                    return;
                }
            }

            File file = new File(r.location);
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this,
                        "File not found on disk at:\n" + r.location,
                        "File Not Found",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                setStatusActivity("Opened resource: " + file.getName());
            } else {
                JOptionPane.showMessageDialog(this, "Opening files is not supported on this platform.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open resource: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openContainingFolder(Resource r) {
        if (r == null || r.location == null) return;
        try {
            File file = new File(r.location);
            File parent = file.getParentFile();
            if (parent != null && parent.exists()) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(parent);
                    setStatusActivity("Opened directory: " + parent.getAbsolutePath());
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Containing directory could not be found.", "Directory Not Found", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptNewCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter name for new category:", "New Category", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Category created = categoryDAO.addCategory(name.trim());
            if (created != null) {
                refreshCategories();
                // Select newly created category
                for (int i = 0; i < categoryListModel.size(); i++) {
                    if (categoryListModel.get(i).id == created.id) {
                        categoryList.setSelectedIndex(i);
                        break;
                    }
                }
                setStatusActivity("Created category: " + created.name);
            } else {
                JOptionPane.showMessageDialog(this, "A category with that name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renameSelectedCategory() {
        CategoryItem item = categoryList.getSelectedValue();
        if (item == null || item.isSpecial()) {
            JOptionPane.showMessageDialog(this, "Please select a custom category to rename.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String newName = JOptionPane.showInputDialog(this, "Enter new name for category:", item.name);
        if (newName != null && !newName.trim().isEmpty() && !newName.trim().equalsIgnoreCase(item.name)) {
            boolean ok = categoryDAO.updateCategory(item.id, newName.trim());
            if (ok) {
                refreshCategories();
                refreshResources();
                setStatusActivity("Renamed category to: " + newName.trim());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to rename category. The name might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedCategory() {
        CategoryItem item = categoryList.getSelectedValue();
        if (item == null || item.isSpecial()) {
            JOptionPane.showMessageDialog(this, "Please select a custom category to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Category> allCategories = categoryDAO.getAllCategories();
        if (allCategories.size() <= 1) {
            JOptionPane.showMessageDialog(this, "Cannot delete the only remaining category.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete category '" + item.name + "'?\nResources inside will be moved to another category.",
                "Confirm Delete Category",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = categoryDAO.deleteCategory(item.id);
            if (ok) {
                refreshCategories();
                refreshResources();
                setStatusActivity("Deleted category: " + item.name);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete category.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAboutDialog() {
        String msg = "<html><body style='width: 320px; padding: 5px;'>"
                + "<h2 style='margin-bottom: 2px;'>Digital Resource Manager</h2>"
                + "<p><i>A software tool to store, organize, and manage digital files in one place.</i></p>"
                + "<hr/>"
                + "<p><b>Purpose:</b></p>"
                + "<ul>"
                + "<li>Simplify access to digital resources</li>"
                + "<li>Keep files structured and easy to find</li>"
                + "<li>Maintain resource data efficiently</li>"
                + "</ul>"
                + "<p><b>Supported Types:</b></p>"
                + "<ul>"
                + "<li><b>PDF</b> – documents and reading materials</li>"
                + "<li><b>Image</b> – photos and visual content</li>"
                + "<li><b>Video</b> – multimedia files</li>"
                + "<li><b>Link</b> – web-based resources</li>"
                + "</ul>"
                + "<p><b>Drag & Drop:</b> Simply drop files or links anywhere in the application to import them instantly!</p>"
                + "</body></html>";

        JOptionPane.showMessageDialog(this, msg, "About Digital Resource Manager", JOptionPane.INFORMATION_MESSAGE);
    }

    public void saveScreenshot() {
        try {
            File dir = new File("screenshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File file = new File(dir, "screenshot_" + timestamp + ".png");

            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                    Math.max(1, getWidth()), Math.max(1, getHeight()), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintAll(g2);
            g2.dispose();

            javax.imageio.ImageIO.write(img, "png", file);
            setStatusActivity("Screenshot saved: " + file.getName());
            JOptionPane.showMessageDialog(this,
                    "Screenshot saved successfully to:\n" + file.getAbsolutePath(),
                    "Screenshot Captured",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to capture screenshot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out, " + currentUser.username + "?",
                "Log Out Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
            });
        }
    }

    private void setStatusActivity(String msg) {
        statusActivityLabel.setText(msg);
    }
}
