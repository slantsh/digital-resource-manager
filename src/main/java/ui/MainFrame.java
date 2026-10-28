package ui;

import database.CategoryDAO;
import database.ResourceDAO;
import model.Category;
import model.Resource;
import model.ResourceType;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application window for Digital Resource Manager.
 */
public class MainFrame extends JFrame {

    private final CategoryDAO categoryDAO;
    private final ResourceDAO resourceDAO;

    // UI Components
    private DefaultListModel<CategoryItem> categoryListModel;
    private JList<CategoryItem> categoryList;
    private ResourceTableModel tableModel;
    private JTable resourceTable;
    private DropZonePanel dropZonePanel;
    private JTextField searchField;
    private JComboBox<String> typeFilterCombo;
    private JCheckBox favoritesFilterCheck;
    private JLabel statusCountLabel;
    private JLabel statusActivityLabel;

    public MainFrame() {
        super("Digital Resource Manager");
        this.categoryDAO = new CategoryDAO();
        this.resourceDAO = new ResourceDAO();

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

        JMenuItem deleteItem = new JMenuItem("Delete Selected Resource", KeyEvent.VK_D);
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

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            refreshCategories();
            refreshResources();
        });
        toolbar.add(refreshBtn);

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

        splitPane.setLeftComponent(sidebarPanel);

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

        resourceTable = new JTable(tableModel);
        resourceTable.setRowHeight(26);
        resourceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resourceTable.setAutoCreateRowSorter(true);
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

        resourceTable.getColumnModel().getColumn(4).setPreferredWidth(320);
        resourceTable.getColumnModel().getColumn(4).setCellRenderer(new TableRenderers.PaddedTextRenderer());

        // Table interaction: Double click to open
        resourceTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = resourceTable.getSelectedRow();
                    if (viewRow >= 0 && resourceTable.getSelectedColumn() != 0) {
                        int modelRow = resourceTable.convertRowIndexToModel(viewRow);
                        Resource r = tableModel.getResourceAt(modelRow);
                        if (r != null) {
                            openResource(r);
                        }
                    }
                }
            }
        });

        // Table context popup menu
        initTableContextMenu();

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

    private void initTableContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("Open Resource");
        openItem.setFont(openItem.getFont().deriveFont(Font.BOLD));
        openItem.addActionListener(e -> {
            Resource r = getSelectedResource();
            if (r != null) openResource(r);
        });

        JMenuItem openFolderItem = new JMenuItem("Show Containing Folder");
        openFolderItem.addActionListener(e -> {
            Resource r = getSelectedResource();
            if (r != null) openContainingFolder(r);
        });

        JMenuItem toggleFavItem = new JMenuItem("Toggle Favorite");
        toggleFavItem.addActionListener(e -> toggleSelectedFavorite());

        JMenuItem editItem = new JMenuItem("Edit Details...");
        editItem.addActionListener(e -> editSelectedResource());

        JMenuItem copyPathItem = new JMenuItem("Copy Path / URL");
        copyPathItem.addActionListener(e -> {
            Resource r = getSelectedResource();
            if (r != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(r.location), null);
                setStatusActivity("Copied location to clipboard: " + r.location);
            }
        });

        JMenuItem deleteItem = new JMenuItem("Delete Resource");
        deleteItem.addActionListener(e -> deleteSelectedResource());

        popupMenu.add(openItem);
        popupMenu.add(openFolderItem);
        popupMenu.addSeparator();
        popupMenu.add(toggleFavItem);
        popupMenu.add(editItem);
        popupMenu.add(copyPathItem);
        popupMenu.addSeparator();
        popupMenu.add(deleteItem);

        resourceTable.setComponentPopupMenu(popupMenu);
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

        List<Resource> resources = resourceDAO.filterResources(categoryIdFilter, typeFilter, query, favoritesOnly);

        // Fetch category map
        List<Category> allCategories = categoryDAO.getAllCategories();
        Map<Integer, String> catMap = new HashMap<>();
        for (Category c : allCategories) {
            catMap.put(c.id, c.name);
        }

        tableModel.setData(resources, catMap);

        int totalCount = categoryDAO.getTotalResourceCount();
        statusCountLabel.setText("Total: " + totalCount + " resources | Showing: " + resources.size());
    }

    private Resource getSelectedResource() {
        int viewRow = resourceTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = resourceTable.convertRowIndexToModel(viewRow);
            return tableModel.getResourceAt(modelRow);
        }
        return null;
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
        Resource r = getSelectedResource();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a resource to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + r.title + "' from the manager?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = resourceDAO.deleteResource(r.id);
            if (ok) {
                refreshCategories();
                refreshResources();
                setStatusActivity("Deleted: " + r.title);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete resource.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void toggleSelectedFavorite() {
        Resource r = getSelectedResource();
        if (r != null) {
            boolean newFav = !r.favorite;
            r.favorite = newFav;
            resourceDAO.setFavorite(r.id, newFav);
            tableModel.fireTableDataChanged();
            refreshCategories();
            setStatusActivity((newFav ? "Added to" : "Removed from") + " favorites: " + r.title);
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

    private void setStatusActivity(String msg) {
        statusActivityLabel.setText(msg);
    }
}
