package ui;

import database.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Login and registration window for Digital Resource Manager.
 * Ensures each user accesses their own isolated resources and collections.
 */
public class LoginFrame extends JFrame {

    private final UserDAO userDAO;

    // Login tab components
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private JButton loginButton;

    // Register tab components
    private JTextField regUserField;
    private JPasswordField regPassField;
    private JPasswordField regConfirmPassField;
    private JButton regButton;

    // Common feedback
    private JLabel statusLabel;
    private JTabbedPane tabbedPane;

    public LoginFrame() {
        super("Sign In - Digital Resource Manager");
        this.userDAO = new UserDAO();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(430, 480);
        setResizable(false);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // 1. Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("📚 Digital Resource Manager");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Personal Asset & Resource Library");
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(15));

        rootPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. Tabs: Login & Register
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Sign In", createLoginPanel());
        tabbedPane.addTab("Create Account", createRegisterPanel());

        tabbedPane.addChangeListener(e -> {
            clearStatus();
            if (tabbedPane.getSelectedIndex() == 0) {
                getRootPane().setDefaultButton(loginButton);
                SwingUtilities.invokeLater(() -> loginUserField.requestFocusInWindow());
            } else {
                getRootPane().setDefaultButton(regButton);
                SwingUtilities.invokeLater(() -> regUserField.requestFocusInWindow());
            }
        });

        rootPanel.add(tabbedPane, BorderLayout.CENTER);

        // 3. Status Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        rootPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        getRootPane().setDefaultButton(loginButton);

        // Focus first field after display
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                loginUserField.requestFocusInWindow();
            }
        });
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(userLbl.getFont().deriveFont(Font.BOLD));
        panel.add(userLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        loginUserField = new JTextField(15);
        loginUserField.addActionListener(e -> loginPassField.requestFocusInWindow());
        panel.add(loginUserField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(passLbl.getFont().deriveFont(Font.BOLD));
        panel.add(passLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        loginPassField = new JPasswordField(15);
        loginPassField.addActionListener(e -> doLogin());
        panel.add(loginPassField, gbc);

        // Login Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 6, 6, 6);
        loginButton = new JButton("Sign In");
        loginButton.setFont(loginButton.getFont().deriveFont(Font.BOLD, 13f));
        loginButton.setPreferredSize(new Dimension(110, 32));
        loginButton.addActionListener(e -> doLogin());
        panel.add(loginButton, gbc);

        // Demo account hint
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 6, 0, 6);
        JLabel hintLabel = new JLabel("💡 Default account: admin / admin");
        hintLabel.setForeground(new Color(110, 110, 110));
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(hintLabel, gbc);

        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.35;
        JLabel userLbl = new JLabel("Username:");
        userLbl.setFont(userLbl.getFont().deriveFont(Font.BOLD));
        panel.add(userLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
        regUserField = new JTextField(15);
        regUserField.addActionListener(e -> regPassField.requestFocusInWindow());
        panel.add(regUserField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.35;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(passLbl.getFont().deriveFont(Font.BOLD));
        panel.add(passLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.65;
        regPassField = new JPasswordField(15);
        regPassField.addActionListener(e -> regConfirmPassField.requestFocusInWindow());
        panel.add(regPassField, gbc);

        // Confirm Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.35;
        JLabel confirmLbl = new JLabel("Confirm:");
        confirmLbl.setFont(confirmLbl.getFont().deriveFont(Font.BOLD));
        panel.add(confirmLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.65;
        regConfirmPassField = new JPasswordField(15);
        regConfirmPassField.addActionListener(e -> doRegister());
        panel.add(regConfirmPassField, gbc);

        // Register Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 5, 5, 5);
        regButton = new JButton("Create New Account");
        regButton.setFont(regButton.getFont().deriveFont(Font.BOLD, 13f));
        regButton.setPreferredSize(new Dimension(160, 32));
        regButton.addActionListener(e -> doRegister());
        panel.add(regButton, gbc);

        // Info Note
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 5, 0, 5);
        JLabel noteLabel = new JLabel("Each user gets an isolated private library");
        noteLabel.setForeground(new Color(110, 110, 110));
        noteLabel.setFont(noteLabel.getFont().deriveFont(11f));
        noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(noteLabel, gbc);

        return panel;
    }

    private void doLogin() {
        String username = loginUserField.getText().trim();
        String password = new String(loginPassField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        User user = userDAO.authenticate(username, password);
        if (user != null) {
            launchMainApp(user);
        } else {
            showError("Invalid username or password.");
            loginPassField.setText("");
            loginPassField.requestFocusInWindow();
        }
    }

    private void doRegister() {
        String username = regUserField.getText().trim();
        String password = new String(regPassField.getPassword());
        String confirm = new String(regConfirmPassField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill out all registration fields.");
            return;
        }

        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            regUserField.requestFocusInWindow();
            return;
        }

        if (password.length() < 3) {
            showError("Password must be at least 3 characters.");
            regPassField.requestFocusInWindow();
            return;
        }

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            regConfirmPassField.setText("");
            regConfirmPassField.requestFocusInWindow();
            return;
        }

        try {
            User newUser = userDAO.register(username, password);
            if (newUser != null) {
                showSuccess("Account created! Logging in...");
                launchMainApp(newUser);
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void launchMainApp(User user) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
        });
    }

    private void showError(String message) {
        statusLabel.setForeground(new Color(190, 30, 30));
        statusLabel.setText("✖ " + message);
    }

    private void showSuccess(String message) {
        statusLabel.setForeground(new Color(30, 140, 40));
        statusLabel.setText("✔ " + message);
    }

    private void clearStatus() {
        statusLabel.setText(" ");
    }
}
