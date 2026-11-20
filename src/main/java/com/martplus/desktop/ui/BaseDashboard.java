package com.martplus.desktop.ui;

import com.martplus.desktop.config.AppConfig;
import com.martplus.desktop.model.User;
import com.martplus.desktop.util.DateUtil;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.ImageUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Base Class for role-specific Dashboards.
 * Visually matches the MARTplus website's clean white rounded sidebar and header.
 * Reference: frontend/buyer/dashboard.html & frontend/css/dashboard.css
 */
public abstract class BaseDashboard extends JPanel implements DashboardView {

    protected final User currentUser;
    protected final MainWindow parentWindow;

    // Navigation and Panels
    protected final JPanel sidebarPanel;
    protected final JPanel mainContentContainer;
    protected final CardLayout contentCardLayout;
    protected final JLabel statusLabel = new JLabel("PostgreSQL Connected | Ready");
    protected final Map<String, JButton> navButtons = new HashMap<>();

    public BaseDashboard(MainWindow parentWindow, User currentUser) {
        this.parentWindow = parentWindow;
        this.currentUser = currentUser != null ? currentUser : AppConfig.getCurrentUser();

        setLayout(new BorderLayout());
        setBackground(UITheme.BG_LIGHT);

        // Top Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Center Wrapper: holds floating rounded sidebar + main content
        JPanel bodyWrapper = new JPanel(new BorderLayout(24, 0));
        bodyWrapper.setBackground(UITheme.BG_LIGHT);
        bodyWrapper.setBorder(new EmptyBorder(20, 24, 20, 24));

        // White Rounded Sidebar (West)
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(Color.WHITE);
        sidebarPanel.setPreferredSize(new Dimension(264, 680));
        sidebarPanel.setMinimumSize(new Dimension(264, 680));
        sidebarPanel.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(16, 12, 16, 12)
        ));

        // Central Content Area with CardLayout
        contentCardLayout = new CardLayout();
        mainContentContainer = new JPanel(contentCardLayout);
        mainContentContainer.setBackground(UITheme.BG_LIGHT);

        bodyWrapper.add(sidebarPanel, BorderLayout.WEST);
        bodyWrapper.add(mainContentContainer, BorderLayout.CENTER);
        add(bodyWrapper, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel bottomStatusBar = createStatusBar();
        add(bottomStatusBar, BorderLayout.SOUTH);

        // Initialize sidebar header, navigation, and footer
        buildSidebarHeader();
        buildNavigation(sidebarPanel);
        buildSidebarFooter();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_LIGHT));
        header.setPreferredSize(new Dimension(0, 70));

        // Left Header: Brand Logo & Title
        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 15));
        leftHeader.setBackground(Color.WHITE);

        JLabel logoIcon = new JLabel(ImageUtil.getAppLogo(36, 36));
        leftHeader.add(logoIcon);

        JLabel brandText = new JLabel("MARTplus");
        brandText.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 22));
        brandText.setForeground(UITheme.TEXT_PRIMARY);
        leftHeader.add(brandText);

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(UITheme.BORDER_LIGHT);
        leftHeader.add(sep);

        JLabel welcomeMsg = new JLabel("Welcome, " + (currentUser != null ? currentUser.getFirstName() : "User"));
        welcomeMsg.setFont(UITheme.FONT_BODY_BOLD);
        welcomeMsg.setForeground(UITheme.TEXT_SECONDARY);
        leftHeader.add(welcomeMsg);

        // Right Header: Role badge, Avatar, Refresh Button
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 15));
        rightHeader.setBackground(Color.WHITE);

        // Role Badge
        String roleText = (currentUser != null ? currentUser.getRole() : getRoleName()).toUpperCase();
        JLabel roleBadge = UITheme.createBadge(roleText, UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        rightHeader.add(roleBadge);

        // User Avatar Circle
        String fullName = currentUser != null ? currentUser.getFullName() : "User";
        JLabel avatar = UITheme.createAvatar(fullName, 36);
        rightHeader.add(avatar);

        // Refresh Button
        JButton refreshBtn = UITheme.createSecondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        rightHeader.add(refreshBtn);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatusBar() {
        JPanel bottomStatusBar = new JPanel(new BorderLayout());
        bottomStatusBar.setBackground(Color.WHITE);
        bottomStatusBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(6, 24, 6, 24)
        ));

        statusLabel.setText("PostgreSQL Connected | Ready");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.TEXT_SECONDARY);
        bottomStatusBar.add(statusLabel, BorderLayout.WEST);

        JLabel dbBadge = new JLabel("● Direct JDBC Active");
        dbBadge.setFont(UITheme.FONT_SMALL_BOLD);
        dbBadge.setForeground(UITheme.SUCCESS_GREEN);
        bottomStatusBar.add(dbBadge, BorderLayout.EAST);

        return bottomStatusBar;
    }

    private void buildSidebarHeader() {
        JLabel sectionTitle = new JLabel("MAIN NAVIGATION");
        sectionTitle.setFont(UITheme.FONT_TINY);
        sectionTitle.setForeground(UITheme.TEXT_MUTED);
        sectionTitle.setBorder(new EmptyBorder(4, 10, 10, 10));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(sectionTitle);
    }

    private void buildSidebarFooter() {
        sidebarPanel.add(Box.createVerticalGlue());

        // Thin divider
        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setForeground(UITheme.BORDER_SUBTLE);
        sidebarPanel.add(divider);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // User Profile Pill
        JPanel userPill = new JPanel(new BorderLayout(8, 0));
        userPill.setBackground(UITheme.BG_LIGHT);
        userPill.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        userPill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        String fullName = currentUser != null ? currentUser.getFullName() : "User";
        JLabel avatarSmall = UITheme.createAvatar(fullName, 32);
        userPill.add(avatarSmall, BorderLayout.WEST);

        JPanel infoText = new JPanel();
        infoText.setLayout(new BoxLayout(infoText, BoxLayout.Y_AXIS));
        infoText.setBackground(UITheme.BG_LIGHT);

        JLabel nameLabel = new JLabel(fullName);
        nameLabel.setFont(UITheme.FONT_SMALL_BOLD);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel emailLabel = new JLabel(currentUser != null ? currentUser.getEmail() : "user@martplus.com");
        emailLabel.setFont(UITheme.FONT_TINY);
        emailLabel.setForeground(UITheme.TEXT_MUTED);

        infoText.add(nameLabel);
        infoText.add(emailLabel);
        userPill.add(infoText, BorderLayout.CENTER);

        sidebarPanel.add(userPill);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Logout Button matching website .logout-btn
        JButton logoutBtn = new JButton("🚪 Log Out");
        logoutBtn.setFont(UITheme.FONT_BODY_BOLD);
        logoutBtn.setForeground(new Color(0xDC, 0x26, 0x26));
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFE, 0xCA, 0xCA), 1, true),
                new EmptyBorder(9, 12, 9, 12)
        ));
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        logoutBtn.setPreferredSize(new Dimension(240, 40));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setOpaque(true);

        logoutBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                logoutBtn.setBackground(new Color(0xFE, 0xF2, 0xF2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                logoutBtn.setBackground(Color.WHITE);
            }
        });

        logoutBtn.addActionListener(e -> handleLogout());
        sidebarPanel.add(logoutBtn);
    }

    /**
     * Adds a sidebar navigation link matching the website's .dashboard-nav a styles.
     */
    protected JButton addNavButton(JPanel panel, String cardName, String title) {
        JButton btn = new JButton(title);
        btn.setFont(UITheme.FONT_BODY_BOLD);
        btn.setForeground(new Color(0x4B, 0x55, 0x63));
        btn.setBackground(Color.WHITE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(
                new EmptyBorder(1, 0, 1, 0),
                new EmptyBorder(9, 12, 9, 12)
        ));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(240, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(UITheme.PRIMARY_LIGHT)) {
                    btn.setBackground(new Color(0xF5, 0xF3, 0xFF));
                    btn.setForeground(UITheme.PRIMARY);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(UITheme.PRIMARY_LIGHT)) {
                    btn.setBackground(Color.WHITE);
                    btn.setForeground(new Color(0x4B, 0x55, 0x63));
                }
            }
        });

        btn.addActionListener(e -> selectNavCard(cardName));

        panel.add(btn);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        navButtons.put(cardName, btn);
        return btn;
    }

    public void selectNavCard(String cardName) {
        contentCardLayout.show(mainContentContainer, cardName);

        // Highlight selected nav button with website active state (.active: background #EDE9FE, color #635BFF)
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton b = entry.getValue();
            if (entry.getKey().equals(cardName)) {
                b.setBackground(UITheme.PRIMARY_LIGHT);
                b.setForeground(UITheme.PRIMARY);
                b.setFont(UITheme.FONT_BODY_BOLD);
                b.setBorder(new CompoundBorder(
                        new LineBorder(new Color(0xD8, 0xD4, 0xFE), 1, true),
                        new EmptyBorder(8, 11, 8, 11)
                ));
            } else {
                b.setBackground(Color.WHITE);
                b.setForeground(new Color(0x4B, 0x55, 0x63));
                b.setFont(UITheme.FONT_BODY_BOLD);
                b.setBorder(new CompoundBorder(
                        new EmptyBorder(1, 0, 1, 0),
                        new EmptyBorder(9, 12, 9, 12)
                ));
            }
        }
    }

    protected void handleLogout() {
        if (DialogUtil.confirm(this, "Are you sure you want to log out of MARTplus?", "Confirm Logout")) {
            onDeactivated();
            AppConfig.clearSession();
            parentWindow.showLogin();
        }
    }

    protected void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message + " | Updated: " + DateUtil.formatDateTime(LocalDateTime.now()));
        }
    }

    /**
     * Abstract method implemented by sub-classes to build role-specific navigation menus.
     */
    protected abstract void buildNavigation(JPanel navPanel);

    @Override
    public JComponent getViewComponent() {
        return this;
    }

    @Override
    public void onActivated() {
        refreshData();
    }

    @Override
    public void onDeactivated() {
    }
}
