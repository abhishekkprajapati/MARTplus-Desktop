package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.JdbcUserDao;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.AuthenticationService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.ImageUtil;
import com.martplus.desktop.util.UITheme;
import com.martplus.desktop.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modern Split-Card Registration Screen for MARTplus Desktop.
 * Visually matches frontend/register.html with a left purple gradient brand showcase
 * and a right clean registration form with client and direct JDBC database validation.
 */
public class RegisterFrame extends JPanel {

    private final MainWindow mainWindow;
    private final AuthenticationService authService;

    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JCheckBox showPasswordCheck;
    private JComboBox<String> roleComboBox;
    private JButton registerButton;
    private JButton backToLoginButton;
    private JLabel statusLabel;

    public RegisterFrame(MainWindow mainWindow) {
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
        authWrapper.setPreferredSize(new Dimension(880, 640));
        authWrapper.setMaximumSize(new Dimension(880, 640));

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
                g2.fillRoundRect(0, 0, getWidth() + 20, getHeight(), 24, 24);
                g2.dispose();
            }
        };
        brandPanel.setOpaque(false);
        brandPanel.setPreferredSize(new Dimension(380, 640));
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

        JLabel heroTitle = new JLabel("<html>Join the<br>MARTplus Platform</html>");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTop.add(heroTitle);

        brandTop.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel heroDesc = new JLabel("<html>Create your account to start shopping across thousands of top multi-vendor products or launch your own seller store.</html>");
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
                "✓ Instant Buyer & Seller Registration",
                "✓ Direct JDBC PostgreSQL Integration",
                "✓ Industry-Standard BCrypt Password Security",
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

        // 2. RIGHT REGISTRATION FORM PANEL
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(32, 40, 32, 40));

        JLabel titleLbl = new JLabel("Create New Account");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(titleLbl);

        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        JLabel subLbl = new JLabel("Sign up as a Buyer or Seller to access MARTplus");
        subLbl.setFont(UITheme.FONT_BODY);
        subLbl.setForeground(UITheme.TEXT_SECONDARY);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(subLbl);

        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Name Row: First Name & Last Name (2 columns)
        JPanel nameRow = new JPanel(new GridLayout(1, 2, 12, 0));
        nameRow.setOpaque(false);
        nameRow.setMaximumSize(new Dimension(420, 58));
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // First Name
        JPanel firstCol = new JPanel();
        firstCol.setOpaque(false);
        firstCol.setLayout(new BoxLayout(firstCol, BoxLayout.Y_AXIS));
        JLabel firstLbl = new JLabel("First Name");
        firstLbl.setFont(UITheme.FONT_BODY_BOLD);
        firstLbl.setForeground(UITheme.TEXT_PRIMARY);
        firstLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        firstNameField = createStyledTextField();
        firstCol.add(firstLbl);
        firstCol.add(Box.createRigidArea(new Dimension(0, 4)));
        firstCol.add(firstNameField);
        nameRow.add(firstCol);

        // Last Name
        JPanel lastCol = new JPanel();
        lastCol.setOpaque(false);
        lastCol.setLayout(new BoxLayout(lastCol, BoxLayout.Y_AXIS));
        JLabel lastLbl = new JLabel("Last Name");
        lastLbl.setFont(UITheme.FONT_BODY_BOLD);
        lastLbl.setForeground(UITheme.TEXT_PRIMARY);
        lastLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastNameField = createStyledTextField();
        lastCol.add(lastLbl);
        lastCol.add(Box.createRigidArea(new Dimension(0, 4)));
        lastCol.add(lastNameField);
        nameRow.add(lastCol);

        formPanel.add(nameRow);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Email Address
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UITheme.FONT_BODY_BOLD);
        emailLbl.setForeground(UITheme.TEXT_PRIMARY);
        emailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(emailLbl);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        emailField = createStyledTextField();
        formPanel.add(emailField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Phone Number (10 digits)
        JLabel phoneLbl = new JLabel("Phone Number (10 digits)");
        phoneLbl.setFont(UITheme.FONT_BODY_BOLD);
        phoneLbl.setForeground(UITheme.TEXT_PRIMARY);
        phoneLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(phoneLbl);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        phoneField = createStyledTextField();
        formPanel.add(phoneField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Password & Confirm Password Row (2 columns)
        JPanel passRow = new JPanel(new GridLayout(1, 2, 12, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(420, 58));
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JPanel passCol = new JPanel();
        passCol.setOpaque(false);
        passCol.setLayout(new BoxLayout(passCol, BoxLayout.Y_AXIS));
        JLabel passLabel = new JLabel("Password (min 8 chars)");
        passLabel.setFont(UITheme.FONT_BODY_BOLD);
        passLabel.setForeground(UITheme.TEXT_PRIMARY);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField = createStyledPasswordField();
        passCol.add(passLabel);
        passCol.add(Box.createRigidArea(new Dimension(0, 4)));
        passCol.add(passwordField);
        passRow.add(passCol);

        // Confirm Password
        JPanel confirmCol = new JPanel();
        confirmCol.setOpaque(false);
        confirmCol.setLayout(new BoxLayout(confirmCol, BoxLayout.Y_AXIS));
        JLabel confirmLabel = new JLabel("Confirm Password");
        confirmLabel.setFont(UITheme.FONT_BODY_BOLD);
        confirmLabel.setForeground(UITheme.TEXT_PRIMARY);
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmPasswordField = createStyledPasswordField();
        confirmCol.add(confirmLabel);
        confirmCol.add(Box.createRigidArea(new Dimension(0, 4)));
        confirmCol.add(confirmPasswordField);
        passRow.add(confirmCol);

        formPanel.add(passRow);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        // Password visibility toggle
        showPasswordCheck = new JCheckBox("Show Passwords");
        showPasswordCheck.setFont(UITheme.FONT_SMALL);
        showPasswordCheck.setForeground(UITheme.TEXT_SECONDARY);
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setFocusPainted(false);
        showPasswordCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPasswordCheck.addActionListener(e -> togglePasswordVisibility());
        formPanel.add(showPasswordCheck);

        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Account Role (BUYER or SELLER only - ADMIN omitted)
        JLabel roleLbl = new JLabel("Account Type");
        roleLbl.setFont(UITheme.FONT_BODY_BOLD);
        roleLbl.setForeground(UITheme.TEXT_PRIMARY);
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(roleLbl);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        roleComboBox = new JComboBox<>(new String[]{"BUYER", "SELLER"});
        roleComboBox.setFont(UITheme.FONT_BODY);
        roleComboBox.setMaximumSize(new Dimension(420, 36));
        roleComboBox.setPreferredSize(new Dimension(420, 36));
        roleComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(roleComboBox);

        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // Create Account Button
        registerButton = UITheme.createPrimaryButton("Create Account");
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setMaximumSize(new Dimension(420, 40));
        registerButton.setPreferredSize(new Dimension(420, 40));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> performRegistration());
        formPanel.add(registerButton);

        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Back to Sign In Button
        backToLoginButton = UITheme.createSecondaryButton("Back to Sign In");
        backToLoginButton.setMaximumSize(new Dimension(420, 34));
        backToLoginButton.setPreferredSize(new Dimension(420, 34));
        backToLoginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        backToLoginButton.addActionListener(e -> mainWindow.showLogin());
        formPanel.add(backToLoginButton);

        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Status Feedback Label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(statusLabel);

        // Enter key listener on confirm password field
        confirmPasswordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performRegistration();
                }
            }
        });

        authWrapper.add(formPanel, BorderLayout.CENTER);
        add(authWrapper);
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(UITheme.FONT_BODY);
        tf.setMaximumSize(new Dimension(420, 36));
        tf.setPreferredSize(new Dimension(420, 36));
        tf.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField();
        pf.setFont(UITheme.FONT_BODY);
        pf.setMaximumSize(new Dimension(420, 36));
        pf.setPreferredSize(new Dimension(420, 36));
        pf.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return pf;
    }

    private void togglePasswordVisibility() {
        boolean show = showPasswordCheck.isSelected();
        char echoChar = show ? (char) 0 : '•';
        passwordField.setEchoChar(echoChar);
        confirmPasswordField.setEchoChar(echoChar);
    }

    private void performRegistration() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = (String) roleComboBox.getSelectedItem();

        // Client-side quick checks
        if (firstName.isEmpty()) {
            DialogUtil.showWarning(this, "First name is required and cannot be empty.");
            firstNameField.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            DialogUtil.showWarning(this, "Last name is required and cannot be empty.");
            lastNameField.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            DialogUtil.showWarning(this, "Email address is required and cannot be empty.");
            emailField.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            DialogUtil.showWarning(this, "Phone number is required and cannot be empty.");
            phoneField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            DialogUtil.showWarning(this, "Password is required and cannot be empty.");
            passwordField.requestFocus();
            return;
        }
        if (password.length() < 8) {
            DialogUtil.showWarning(this, "Password must be at least 8 characters long.");
            passwordField.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            DialogUtil.showWarning(this, "Password and Confirm Password do not match.");
            confirmPasswordField.requestFocus();
            return;
        }

        setControlsEnabled(false);
        statusLabel.setText("Connecting directly to PostgreSQL via JDBC...");
        statusLabel.setForeground(UITheme.PRIMARY);

        AsyncOperation.run(
                () -> authService.register(firstName, lastName, email, phone, password, confirmPassword, role),
                registeredUser -> {
                    setControlsEnabled(true);
                    statusLabel.setText("Account registered successfully!");
                    statusLabel.setForeground(UITheme.SUCCESS_GREEN);
                    DialogUtil.showSuccess(this, "Account created successfully for " + registeredUser.getEmail() +
                            " (" + registeredUser.getRole() + ")!\nPlease sign in using your new credentials.");
                    mainWindow.showLoginWithPrefill(registeredUser.getEmail(), registeredUser.getRole());
                },
                throwable -> {
                    setControlsEnabled(true);
                    statusLabel.setText("Registration failed.");
                    statusLabel.setForeground(UITheme.DANGER_RED);
                    DialogUtil.showError(this, throwable.getMessage());
                }
        );
    }

    private void setControlsEnabled(boolean enabled) {
        registerButton.setEnabled(enabled);
        backToLoginButton.setEnabled(enabled);
        firstNameField.setEnabled(enabled);
        lastNameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmPasswordField.setEnabled(enabled);
        roleComboBox.setEnabled(enabled);
        showPasswordCheck.setEnabled(enabled);
    }

    public void resetFields() {
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        roleComboBox.setSelectedIndex(0);
        showPasswordCheck.setSelected(false);
        passwordField.setEchoChar('•');
        confirmPasswordField.setEchoChar('•');
        statusLabel.setText(" ");
        setControlsEnabled(true);
    }
}

