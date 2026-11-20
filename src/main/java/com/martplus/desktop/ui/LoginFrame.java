package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.JdbcUserDao;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.AuthenticationService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.ImageUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern Split-Card Login Screen for MARTplus Desktop.
 * Visually matches frontend/login.html with a left purple gradient brand showcase
 * and a right clean authentication form connecting directly to PostgreSQL.
 */
public class LoginFrame extends JPanel {

    private final MainWindow mainWindow;
    private final AuthenticationService authService;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;
    private JButton createAccountButton;
    private JButton exitButton;
    private JLabel statusLabel;

    public LoginFrame(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.authService = new AuthenticationService(new JdbcUserDao());

        setLayout(new GridBagLayout());
        setBackground(UITheme.BG_LIGHT);

        buildSplitCardUI();
    }

    private void buildSplitCardUI() {
        // Outer Card Container (Matches website auth-wrapper max-width & border-radius)
        JPanel authWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(UITheme.BORDER_LIGHT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        authWrapper.setOpaque(false);
        authWrapper.setPreferredSize(new Dimension(860, 580));
        authWrapper.setMaximumSize(new Dimension(860, 580));

        // 1. LEFT BRAND PANEL (Purple gradient showcase)
        JPanel brandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UITheme.PRIMARY,
                        getWidth(), getHeight(), UITheme.PRIMARY_DARK
                );
                g2.setPaint(gp);
                // Fill left rounded rectangle
                g2.fillRoundRect(0, 0, getWidth() + 20, getHeight(), 24, 24);
                g2.dispose();
            }
        };
        brandPanel.setOpaque(false);
        brandPanel.setPreferredSize(new Dimension(380, 580));
        brandPanel.setLayout(new BorderLayout());
        brandPanel.setBorder(new EmptyBorder(44, 40, 44, 40));

        // Left Brand Header
        JPanel brandTop = new JPanel();
        brandTop.setOpaque(false);
        brandTop.setLayout(new BoxLayout(brandTop, BoxLayout.Y_AXIS));

        // Brand Logo Row
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoIcon = new JLabel(ImageUtil.getLogoIcon(32, 32));
        logoRow.add(logoIcon);

        JLabel brandName = new JLabel("MARTplus");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 26));
        brandName.setForeground(Color.WHITE);
        logoRow.add(brandName);

        brandTop.add(logoRow);
        brandTop.add(Box.createRigidArea(new Dimension(0, 24)));

        JLabel heroTitle = new JLabel("<html>Welcome to<br>MARTplus Platform</html>");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(heroTitle);

        brandTop.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel heroDesc = new JLabel("<html>Enterprise desktop edition providing direct JDBC PostgreSQL synchronization, multi-vendor marketplace governance, and ACID transactions.</html>");
        heroDesc.setFont(UITheme.FONT_BODY);
        heroDesc.setForeground(new Color(0xEE, 0xED, 0xFF));
        heroDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(heroDesc);

        brandPanel.add(brandTop, BorderLayout.NORTH);

        // Feature Highlights
        JPanel featuresPanel = new JPanel();
        featuresPanel.setOpaque(false);
        featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
        featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] features = {
                "✓ Direct JDBC Connection Pool",
                "✓ 12-Step Atomic Checkout Isolation",
                "✓ Multi-Vendor Buyer, Seller & Admin",
                "✓ Real-time Inventory Concurrency Control"
        };

        for (String feat : features) {
            JLabel featLbl = new JLabel(feat);
            featLbl.setFont(UITheme.FONT_BODY_BOLD);
            featLbl.setForeground(Color.WHITE);
            featLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            featuresPanel.add(featLbl);
            featuresPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        brandPanel.add(featuresPanel, BorderLayout.CENTER);

        // Bottom Platform Tag
        JLabel platformTag = new JLabel("Java 21 • Swing • FlatLaf • PostgreSQL");
        platformTag.setFont(UITheme.FONT_SMALL);
        platformTag.setForeground(new Color(0xD1, 0xD5, 0xDB));
        brandPanel.add(platformTag, BorderLayout.SOUTH);

        authWrapper.add(brandPanel, BorderLayout.WEST);

        // 2. RIGHT AUTH FORM PANEL
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(40, 44, 40, 44));

        JLabel signInTitle = new JLabel("Welcome Back");
        signInTitle.setFont(UITheme.FONT_TITLE);
        signInTitle.setForeground(UITheme.TEXT_PRIMARY);
        signInTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(signInTitle);

        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel signInSub = new JLabel("Please sign in to access your MARTplus workspace");
        signInSub.setFont(UITheme.FONT_BODY);
        signInSub.setForeground(UITheme.TEXT_SECONDARY);
        signInSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(signInSub);

        formPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // Email Address
        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(UITheme.FONT_BODY_BOLD);
        emailLabel.setForeground(UITheme.TEXT_PRIMARY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(emailLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        emailField = new JTextField();
        emailField.setFont(UITheme.FONT_BODY);
        emailField.setMaximumSize(new Dimension(380, 40));
        emailField.setPreferredSize(new Dimension(380, 40));
        emailField.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(emailField);

        formPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Password
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_BODY_BOLD);
        passLabel.setForeground(UITheme.TEXT_PRIMARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        passwordField = new JPasswordField();
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setMaximumSize(new Dimension(380, 40));
        passwordField.setPreferredSize(new Dimension(380, 40));
        passwordField.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passwordField);

        formPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        // Role Selector
        JLabel roleLabel = new JLabel("Account Role");
        roleLabel.setFont(UITheme.FONT_BODY_BOLD);
        roleLabel.setForeground(UITheme.TEXT_PRIMARY);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(roleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        roleComboBox = new JComboBox<>(new String[]{"BUYER", "SELLER", "ADMIN"});
        roleComboBox.setFont(UITheme.FONT_BODY);
        roleComboBox.setMaximumSize(new Dimension(380, 38));
        roleComboBox.setPreferredSize(new Dimension(380, 38));
        roleComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(roleComboBox);

        formPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        // Sign In Button
        loginButton = UITheme.createPrimaryButton("Sign In to MARTplus");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setMaximumSize(new Dimension(380, 42));
        loginButton.setPreferredSize(new Dimension(380, 42));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());
        formPanel.add(loginButton);

        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Create New Account Button (Distinct, prominent action below Sign In)
        createAccountButton = new JButton("Create New Account");
        createAccountButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createAccountButton.setForeground(UITheme.PRIMARY);
        createAccountButton.setBackground(UITheme.PRIMARY_LIGHT);
        createAccountButton.setFocusPainted(false);
        createAccountButton.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xD8, 0xD4, 0xFE), 1, true),
                new EmptyBorder(9, 14, 9, 14)
        ));
        createAccountButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createAccountButton.setMaximumSize(new Dimension(380, 42));
        createAccountButton.setPreferredSize(new Dimension(380, 42));
        createAccountButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        createAccountButton.setOpaque(true);
        createAccountButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (createAccountButton.isEnabled()) {
                    createAccountButton.setBackground(new Color(0xE0, 0xDE, 0xFF));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (createAccountButton.isEnabled()) {
                    createAccountButton.setBackground(UITheme.PRIMARY_LIGHT);
                }
            }
        });
        createAccountButton.addActionListener(e -> mainWindow.showRegister());
        formPanel.add(createAccountButton);

        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Exit Application Button
        exitButton = UITheme.createSecondaryButton("Exit Application");
        exitButton.setMaximumSize(new Dimension(380, 36));
        exitButton.setPreferredSize(new Dimension(380, 36));
        exitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitButton.addActionListener(e -> System.exit(0));
        formPanel.add(exitButton);

        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Status Label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(statusLabel);

        // Enter key listener on password field
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        });

        authWrapper.add(formPanel, BorderLayout.CENTER);

        add(authWrapper);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleComboBox.getSelectedItem();

        if (email.isEmpty()) {
            DialogUtil.showWarning(this, "Please enter your email address.");
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            DialogUtil.showWarning(this, "Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        setControlsEnabled(false);
        statusLabel.setText("Connecting directly to PostgreSQL via JDBC...");
        statusLabel.setForeground(UITheme.PRIMARY);

        AsyncOperation.run(
                () -> authService.login(email, password, role),
                authenticatedUser -> {
                    setControlsEnabled(true);
                    statusLabel.setText("Authenticated! Loading " + role + " workspace...");
                    statusLabel.setForeground(UITheme.SUCCESS_GREEN);
                    mainWindow.showDashboard(authenticatedUser);
                },
                throwable -> {
                    setControlsEnabled(true);
                    statusLabel.setText("Authentication failed.");
                    statusLabel.setForeground(UITheme.DANGER_RED);
                    DialogUtil.showError(this, throwable.getMessage());
                    passwordField.setText("");
                    passwordField.requestFocus();
                }
        );
    }

    private void setControlsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        createAccountButton.setEnabled(enabled);
        exitButton.setEnabled(enabled);
        emailField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        roleComboBox.setEnabled(enabled);
    }

    public void resetFields() {
        passwordField.setText("");
        statusLabel.setText(" ");
        setControlsEnabled(true);
    }

    /**
     * Pre-fills the login screen after successful registration and prompts for password.
     */
    public void prefill(String email, String role) {
        resetFields();
        if (email != null && !email.isBlank()) {
            emailField.setText(email.trim());
        }
        if (role != null && !role.isBlank()) {
            roleComboBox.setSelectedItem(role.trim().toUpperCase());
        }
        passwordField.setText("");
        passwordField.requestFocus();
        statusLabel.setText("Account created! Please enter your password to sign in.");
        statusLabel.setForeground(UITheme.SUCCESS_GREEN);
    }
}
