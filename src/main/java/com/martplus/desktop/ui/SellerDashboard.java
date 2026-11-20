package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.*;
import com.martplus.desktop.model.Order;
import com.martplus.desktop.model.OrderItem;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.InventoryService;
import com.martplus.desktop.service.OrderService;
import com.martplus.desktop.service.ProductService;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Concrete Seller Dashboard extending BaseDashboard.
 * Faithfully matches frontend/seller/dashboard.html with a welcome gradient banner,
 * 4 metric KPI analytics cards, and quick store governance actions.
 */
public class SellerDashboard extends BaseDashboard {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final OrderService orderService;

    private JPanel overviewPanel;
    private ProductManagementPanel productManagementPanel;
    private InventoryPanel inventoryPanel;
    private OrderPanel orderPanel;

    private JLabel totalProductsVal;
    private JLabel totalStockVal;
    private JLabel totalOrdersVal;
    private JLabel totalRevenueVal;

    public SellerDashboard(MainWindow parentWindow, User currentUser) {
        super(parentWindow, currentUser);

        var productDao = new JdbcProductDao();
        var categoryDao = new JdbcCategoryDao();
        var cartDao = new JdbcCartDao();
        var orderDao = new JdbcOrderDao();
        var inventoryDao = new JdbcInventoryDao();
        var addressDao = new JdbcAddressDao();
        var userDao = new JdbcUserDao();

        this.productService = new ProductService(productDao, categoryDao, null);
        this.inventoryService = new InventoryService(inventoryDao, productDao);
        this.orderService = new OrderService(orderDao, cartDao, productDao, addressDao, inventoryDao, userDao, null);

        initView();
    }

    @Override
    public void initView() {
        overviewPanel = createOverviewPanel();
        productManagementPanel = new ProductManagementPanel(productService, currentUser, false);
        inventoryPanel = new InventoryPanel(inventoryService, productService, currentUser);
        orderPanel = new OrderPanel(orderService, currentUser, true, false);

        mainContentContainer.add(overviewPanel, "OVERVIEW");
        mainContentContainer.add(productManagementPanel, "PRODUCTS");
        mainContentContainer.add(inventoryPanel, "INVENTORY");
        mainContentContainer.add(orderPanel, "ORDERS");

        selectNavCard("OVERVIEW");
        loadOverviewMetrics();
    }

    @Override
    protected void buildNavigation(JPanel navPanel) {
        addNavButton(navPanel, "OVERVIEW", "📊  Seller Overview");
        addNavButton(navPanel, "PRODUCTS", "📦  My Products");
        addNavButton(navPanel, "INVENTORY", "📋  Inventory & Stock");
        addNavButton(navPanel, "ORDERS", "🚚  Orders & Fulfillment");
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UITheme.BG_LIGHT);

        // 1. Welcome Gradient Box (Matches website .welcome-box)
        JPanel welcomeBox = new JPanel(new BorderLayout(20, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x63, 0x5B, 0xFF),
                        getWidth(), getHeight(), new Color(0x7C, 0x3A, 0xED)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        welcomeBox.setOpaque(false);
        welcomeBox.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel welcomeText = new JPanel();
        welcomeText.setOpaque(false);
        welcomeText.setLayout(new BoxLayout(welcomeText, BoxLayout.Y_AXIS));

        JLabel greeting = new JLabel("Welcome back, " + currentUser.getFullName() + "!");
        greeting.setFont(UITheme.FONT_TITLE);
        greeting.setForeground(Color.WHITE);

        JLabel greetingSub = new JLabel("Here is what's happening with your store today. Manage products, stock, and fulfillment.");
        greetingSub.setFont(UITheme.FONT_BODY);
        greetingSub.setForeground(new Color(255, 255, 255, 210));

        welcomeText.add(greeting);
        welcomeText.add(Box.createRigidArea(new Dimension(0, 6)));
        welcomeText.add(greetingSub);

        JButton addProdQuick = new JButton("+ Add New Product");
        addProdQuick.setFont(UITheme.FONT_BODY_BOLD);
        addProdQuick.setForeground(UITheme.PRIMARY);
        addProdQuick.setBackground(Color.WHITE);
        addProdQuick.setFocusPainted(false);
        addProdQuick.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addProdQuick.setBorder(new CompoundBorder(
                new LineBorder(Color.WHITE, 1, true),
                new EmptyBorder(10, 18, 10, 18)
        ));
        addProdQuick.setOpaque(true);
        addProdQuick.addActionListener(e -> {
            selectNavCard("PRODUCTS");
            productManagementPanel.triggerAddProduct();
        });

        welcomeBox.add(welcomeText, BorderLayout.WEST);
        welcomeBox.add(addProdQuick, BorderLayout.EAST);
        panel.add(welcomeBox, BorderLayout.NORTH);

        // 2. Center: 4 Modern Metric KPI Cards Grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 16));
        metricsGrid.setOpaque(false);

        totalProductsVal = new JLabel("0");
        totalStockVal = new JLabel("0");
        totalOrdersVal = new JLabel("0");
        totalRevenueVal = new JLabel("₹0.00");

        metricsGrid.add(createModernMetricCard("Total Listed Items", totalProductsVal, "📦", UITheme.PRIMARY));
        metricsGrid.add(createModernMetricCard("Total Stock Units", totalStockVal, "📊", UITheme.INFO_BLUE));
        metricsGrid.add(createModernMetricCard("Customer Orders", totalOrdersVal, "🚚", UITheme.WARNING_AMBER));
        metricsGrid.add(createModernMetricCard("Estimated Revenue", totalRevenueVal, "💰", UITheme.SUCCESS_GREEN));

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.add(metricsGrid, BorderLayout.NORTH);

        // Quick Store Actions Card
        JPanel quickActions = UITheme.createCard();
        quickActions.setLayout(new BorderLayout());

        JLabel actionTitle = new JLabel("Store Quick Actions & Management Shortcuts");
        actionTitle.setFont(UITheme.FONT_SUBTITLE);
        actionTitle.setForeground(UITheme.TEXT_PRIMARY);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        btnRow.setOpaque(false);

        JButton gotoProducts = UITheme.createPrimaryButton("+ Add New Product");
        gotoProducts.addActionListener(e -> {
            selectNavCard("PRODUCTS");
            productManagementPanel.triggerAddProduct();
        });
        btnRow.add(gotoProducts);

        JButton gotoInventory = UITheme.createSecondaryButton("Check Low Stock");
        gotoInventory.addActionListener(e -> selectNavCard("INVENTORY"));
        btnRow.add(gotoInventory);

        JButton gotoOrders = UITheme.createSecondaryButton("Manage Orders & Shipments");
        gotoOrders.addActionListener(e -> selectNavCard("ORDERS"));
        btnRow.add(gotoOrders);

        quickActions.add(actionTitle, BorderLayout.NORTH);
        quickActions.add(btnRow, BorderLayout.CENTER);

        centerWrapper.add(quickActions, BorderLayout.CENTER);
        panel.add(centerWrapper, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createModernMetricCard(String title, JLabel valueLabel, String iconEmoji, Color accentColor) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, 10));

        // Top Row: Title + Icon Pill
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

        // Value in Large Bold Typography
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    public void loadOverviewMetrics() {
        AsyncOperation.run(
                () -> {
                    List<Product> myProducts = productService.getProductsBySeller(currentUser.getId());
                    List<Order> myOrders = orderService.getOrdersForSeller(currentUser.getId());

                    int totalStock = 0;
                    for (Product p : myProducts) {
                        totalStock += (p.getStock() != null ? p.getStock() : 0);
                    }

                    BigDecimal revenue = BigDecimal.ZERO;
                    for (Order o : myOrders) {
                        List<OrderItem> items = orderService.getOrderItemsForSeller(o.getId(), currentUser.getId());
                        for (OrderItem oi : items) {
                            revenue = revenue.add(oi.getSubtotal());
                        }
                    }

                    return new Object[]{myProducts.size(), totalStock, myOrders.size(), revenue};
                },
                results -> {
                    totalProductsVal.setText(String.valueOf(results[0]));
                    totalStockVal.setText(String.valueOf(results[1]));
                    totalOrdersVal.setText(String.valueOf(results[2]));
                    totalRevenueVal.setText("₹" + String.format("%.2f", (BigDecimal) results[3]));
                },
                ex -> System.err.println("Failed to load seller metrics: " + ex.getMessage())
        );
    }

    @Override
    public void refreshData() {
        updateStatus("Refreshing seller store data...");
        loadOverviewMetrics();
        if (productManagementPanel != null) productManagementPanel.loadProducts();
        if (inventoryPanel != null) inventoryPanel.loadInventory();
        if (orderPanel != null) orderPanel.loadOrders();
        updateStatus("Seller store synchronized");
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}
