package ui;

import database.CategoryDAO;
import database.ResourceDAO;
import model.Category;
import model.Resource;
import model.ResourceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Dialog for adding or editing a Resource.
 */
public class ResourceDialog extends JDialog {

    private final ResourceDAO resourceDAO;
    private final CategoryDAO categoryDAO;
    private final Resource existingResource;
    private boolean saved = false;

    private JTextField titleField;
    private JComboBox<ResourceType> typeCombo;
    private JComboBox<Category> categoryCombo;
    private JTextField locationField;
    private JTextArea descriptionArea;
    private JCheckBox favoriteCheckBox;
    private JButton browseButton;

    public ResourceDialog(Frame parent, ResourceDAO resourceDAO, CategoryDAO categoryDAO, Resource resourceToEdit, Integer defaultCategoryId) {
        super(parent, resourceToEdit == null ? "Add Resource" : "Edit Resource", true);
        this.resourceDAO = resourceDAO;
        this.categoryDAO = categoryDAO;
        this.existingResource = resourceToEdit;

        initComponents(defaultCategoryId);
        if (resourceToEdit != null) {
            populateFields(resourceToEdit);
        }

        pack();
        setMinimumSize(new Dimension(480, 420));
        setLocationRelativeTo(parent);
    }

    private void initComponents(Integer defaultCategoryId) {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        titleField = new JTextField(25);
        formPanel.add(titleField, gbc);

        // Resource Type
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Type:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        typeCombo = new JComboBox<>(ResourceType.values());
        formPanel.add(typeCombo, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Category:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        categoryCombo = new JComboBox<>();
        refreshCategories(defaultCategoryId);
        formPanel.add(categoryCombo, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JButton newCatButton = new JButton("+ New");
        newCatButton.setToolTipText("Create a new category");
        newCatButton.addActionListener(e -> promptNewCategory());
        formPanel.add(newCatButton, gbc);

        // Location / URL
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Location / URL:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        locationField = new JTextField(20);
        formPanel.add(locationField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.weightx = 0;
        browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> chooseFile());
        formPanel.add(browseButton, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(4, 25);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        // Favorite
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        favoriteCheckBox = new JCheckBox("Mark as Favorite (★)");
        formPanel.add(favoriteCheckBox, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Button bar at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveButton = new JButton(existingResource == null ? "Add" : "Save Changes");
        saveButton.addActionListener(e -> onSave());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(saveButton);
    }

    private void refreshCategories(Integer selectedCategoryId) {
        categoryCombo.removeAllItems();
        List<Category> categories = categoryDAO.getAllCategories();
        Category toSelect = null;
        for (Category c : categories) {
            categoryCombo.addItem(c);
            if (selectedCategoryId != null && c.id == selectedCategoryId) {
                toSelect = c;
            }
        }
        if (toSelect != null) {
            categoryCombo.setSelectedItem(toSelect);
        }
    }

    private void promptNewCategory() {
        String name = JOptionPane.showInputDialog(this, "Enter new category name:", "New Category", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Category created = categoryDAO.addCategory(name.trim());
            if (created != null) {
                refreshCategories(created.id);
            } else {
                JOptionPane.showMessageDialog(this, "A category with this name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Resource File");
        if (locationField.getText() != null && !locationField.getText().trim().isEmpty()) {
            File currentFile = new File(locationField.getText().trim());
            if (currentFile.exists()) {
                chooser.setSelectedFile(currentFile);
            }
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            setFileDetails(selectedFile);
        }
    }

    public void setFileDetails(File file) {
        if (file == null) return;
        locationField.setText(file.getAbsolutePath());

        // Suggest title if empty
        if (titleField.getText().trim().isEmpty()) {
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                titleField.setText(name.substring(0, dotIndex));
            } else {
                titleField.setText(name);
            }
        }

        // Auto-detect type
        ResourceType detected = ResourceType.fromFileName(file.getName());
        typeCombo.setSelectedItem(detected);
    }

    public void setUrlDetails(String url) {
        if (url == null) return;
        locationField.setText(url);
        typeCombo.setSelectedItem(ResourceType.LINK);
        if (titleField.getText().trim().isEmpty()) {
            try {
                String clean = url.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
                int slash = clean.indexOf('/');
                if (slash > 0) {
                    titleField.setText(clean.substring(0, slash));
                } else {
                    titleField.setText(clean);
                }
            } catch (Exception ignored) {
                titleField.setText("Web Resource");
            }
        }
    }

    private void populateFields(Resource r) {
        titleField.setText(r.title);
        typeCombo.setSelectedItem(r.type);
        locationField.setText(r.location);
        descriptionArea.setText(r.description);
        favoriteCheckBox.setSelected(r.favorite);

        for (int i = 0; i < categoryCombo.getItemCount(); i++) {
            Category c = categoryCombo.getItemAt(i);
            if (c.id == r.categoryId) {
                categoryCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a resource title.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }

        String location = locationField.getText().trim();
        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please specify a file path or URL for the resource location.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            locationField.requestFocus();
            return;
        }

        ResourceType type = (ResourceType) typeCombo.getSelectedItem();
        Category cat = (Category) categoryCombo.getSelectedItem();
        int catId = (cat != null) ? cat.id : 1;
        String desc = descriptionArea.getText().trim();
        boolean fav = favoriteCheckBox.isSelected();

        if (existingResource == null) {
            Resource newResource = new Resource(title, desc, type, location, catId, fav);
            boolean ok = resourceDAO.addResource(newResource);
            if (ok) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save resource to database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            existingResource.title = title;
            existingResource.type = type;
            existingResource.location = location;
            existingResource.categoryId = catId;
            existingResource.description = desc;
            existingResource.favorite = fav;

            boolean ok = resourceDAO.updateResource(existingResource);
            if (ok) {
                saved = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update resource in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
