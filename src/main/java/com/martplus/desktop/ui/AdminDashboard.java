package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.*;
import com.martplus.desktop.model.Order;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.ActivityService;
import com.martplus.desktop.service.OrderService;
import com.martplus.desktop.service.ProductService;
import com.martplus.desktop.service.UserService;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Concrete Admin Dashboard extending BaseDashboard.
 * Faithfully matches frontend/admin/dashboard.html with platform-wide KPI metric cards,
 * quick administrative shortcuts, and audit governance.
 */
public class AdminDashboard extends BaseDashboard {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ActivityService activityService;

    private JPanel overviewPanel;
    private UserManagementPanel userManagementPanel;
    private ProductManagementPanel productManagementPanel;
    private OrderPanel orderPanel;
    private ActivityLogPanel activityLogPanel;

    private JLabel totalUsersVal;
    private JLabel totalProductsVal;
    private JLabel totalOrdersVal;
    private JLabel platformVolumeVal;

    public AdminDashboard(MainWindow parentWindow, User currentUser) {
        super(parentWindow, currentUser);

        var userDao = new JdbcUserDao();
        var productDao = new JdbcProductDao();
        var categoryDao = new JdbcCategoryDao();
        var cartDao = new JdbcCartDao();
        var orderDao = new JdbcOrderDao();
        var inventoryDao = new JdbcInventoryDao();
        var addressDao = new JdbcAddressDao();
        var activityLogDao = new JdbcActivityLogDao();

        this.activityService = new ActivityService(activityLogDao);
        this.userService = new UserService(userDao, activityService);
        this.productService = new ProductService(productDao, categoryDao, activityService);
        this.orderService = new OrderService(orderDao, cartDao, productDao, addressDao, inventoryDao, userDao, activityService);

        initView();
    }

    @Override
    public void initView() {
        overviewPanel = createOverviewPanel();
        userManagementPanel = new UserManagementPanel(userService, currentUser);
        productManagementPanel = new ProductManagementPanel(productService, currentUser, true);
        orderPanel = new OrderPanel(orderService, currentUser, false, true);
        activityLogPanel = new ActivityLogPanel(activityService);

        mainContentContainer.add(overviewPanel, "OVERVIEW");
        mainContentContainer.add(userManagementPanel, "USERS");
        mainContentContainer.add(productManagementPanel, "CATALOG");
        mainContentContainer.add(orderPanel, "ORDERS");
        mainContentContainer.add(activityLogPanel, "LOGS");

        selectNavCard("OVERVIEW");
        loadAdminMetrics();
    }

    @Override
    protected void buildNavigation(JPanel navPanel) {
        addNavButton(navPanel, "OVERVIEW", "📊  Platform Overview");
        addNavButton(navPanel, "USERS", "👥  User Management");
        addNavButton(navPanel, "CATALOG", "📦  Catalog Oversight");
        addNavButton(navPanel, "ORDERS", "📑  Order Management");
        addNavButton(navPanel, "LOGS", "🛡️  Activity Logs");
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UITheme.BG_LIGHT);

        // Header Section
        JPanel headerCard = UITheme.createCard();
        headerCard.setLayout(new BorderLayout());

        JPanel textSide = new JPanel();
        textSide.setOpaque(false);
        textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("MARTplus Platform Administration & Governance");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel sub = new JLabel("Real-time PostgreSQL JDBC operational metrics, user governance, and security audit trail.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        textSide.add(title);
        textSide.add(Box.createRigidArea(new Dimension(0, 4)));
        textSide.add(sub);

        headerCard.add(textSide, BorderLayout.CENTER);
        panel.add(headerCard, BorderLayout.NORTH);

        // Center: 4 Metric Cards Grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 16));
        metricsGrid.setOpaque(false);

        totalUsersVal = new JLabel("0");
        totalProductsVal = new JLabel("0");
        totalOrdersVal = new JLabel("0");
        platformVolumeVal = new JLabel("₹0.00");

        metricsGrid.add(createModernMetricCard("Registered Users", totalUsersVal, "👥", UITheme.PRIMARY));
        metricsGrid.add(createModernMetricCard("Total Listed Items", totalProductsVal, "📦", UITheme.INFO_BLUE));
        metricsGrid.add(createModernMetricCard("Platform Orders", totalOrdersVal, "📑", UITheme.WARNING_AMBER));
        metricsGrid.add(createModernMetricCard("Total GMV Volume", platformVolumeVal, "💳", UITheme.SUCCESS_GREEN));

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.add(metricsGrid, BorderLayout.NORTH);

        // Administrative Shortcuts Card
        JPanel quickActions = UITheme.createCard();
        quickActions.setLayout(new BorderLayout());

        JLabel actionTitle = new JLabel("Administrative Control Shortcuts");
        actionTitle.setFont(UITheme.FONT_SUBTITLE);
        actionTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        btnRow.setOpaque(false);

        JButton gotoUsers = UITheme.createPrimaryButton("+ Create System User");
        gotoUsers.addActionListener(e -> {
            selectNavCard("USERS");
            userManagementPanel.triggerAddUser();
        });
        btnRow.add(gotoUsers);

        JButton gotoCatalog = UITheme.createSecondaryButton("Inspect Catalog Listings");
        gotoCatalog.addActionListener(e -> selectNavCard("CATALOG"));
        btnRow.add(gotoCatalog);

        JButton gotoOrders = UITheme.createSecondaryButton("Platform Order Queue");
        gotoOrders.addActionListener(e -> selectNavCard("ORDERS"));
        btnRow.add(gotoOrders);

        JButton gotoLogs = UITheme.createSecondaryButton("Security Audit Trail");
        gotoLogs.addActionListener(e -> selectNavCard("LOGS"));
        btnRow.add(gotoLogs);

        quickActions.add(actionTitle, BorderLayout.NORTH);
        quickActions.add(btnRow, BorderLayout.CENTER);

        centerWrapper.add(quickActions, BorderLayout.CENTER);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createModernMetricCard(String title, JLabel valueLabel, String iconEmoji, Color accentColor) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_SMALL_BOLD);
        titleLbl.setForeground(UITheme.TEXT_SECONDARY);
        top.add(titleLbl, BorderLayout.WEST);

        JLabel iconPill = new JLabel(" " + iconEmoji + " ");
        iconPill.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconPill.setOpaque(true);
        iconPill.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
        iconPill.setBorder(new EmptyBorder(3, 6, 3, 6));
        top.add(iconPill, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    public void loadAdminMetrics() {
        AsyncOperation.run(
                () -> {
                    List<User> users = userService.getAllUsers(currentUser);
                    List<Product> products = productService.getAllProductsForAdmin(currentUser);
                    List<Order> orders = orderService.getAllOrdersForAdmin(currentUser, null);

                    BigDecimal gmv = BigDecimal.ZERO;
                    for (Order o : orders) {
                        if (o.getTotalAmount() != null) {
                            gmv = gmv.add(o.getTotalAmount());
                        }
                    }

                    return new Object[]{users.size(), products.size(), orders.size(), gmv};
                },
                results -> {
                    totalUsersVal.setText(String.valueOf(results[0]));
                    totalProductsVal.setText(String.valueOf(results[1]));
                    totalOrdersVal.setText(String.valueOf(results[2]));
                    platformVolumeVal.setText("₹" + String.format("%.2f", (BigDecimal) results[3]));
                },
                ex -> System.err.println("Failed to load admin metrics: " + ex.getMessage())
        );
    }

    @Override
    public void refreshData() {
        updateStatus("Refreshing platform administrative metrics...");
        loadAdminMetrics();
        if (userManagementPanel != null) userManagementPanel.loadUsers();
        if (productManagementPanel != null) productManagementPanel.loadProducts();
        if (orderPanel != null) orderPanel.loadOrders();
        if (activityLogPanel != null) activityLogPanel.loadLogs();
        updateStatus("Admin metrics synchronized");
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
