package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.Order;
import com.martplus.desktop.model.OrderItem;
import com.martplus.desktop.model.OrderTracking;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.OrderService;
import com.martplus.desktop.util.DateUtil;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Order Management Panel featuring structured order cards,
 * status badges, item breakdown inspection, and graphical tracking timelines.
 * Reference: frontend/buyer/dashboard.html & frontend/seller/orders.html
 */
public class OrderPanel extends JPanel {

    private final OrderService orderService;
    private final User currentUser;
    private final boolean isSellerView;
    private final boolean isAdminView;

    private JPanel ordersContainer;
    private JScrollPane scrollPane;
    private JComboBox<String> statusFilterCombo;
    private JLabel countLabel;
    private List<Order> currentOrders = new ArrayList<>();

    public OrderPanel(OrderService orderService, User currentUser, boolean isSellerView, boolean isAdminView) {
        this.orderService = orderService;
        this.currentUser = currentUser;
        this.isSellerView = isSellerView;
        this.isAdminView = isAdminView;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopFilterBar(), BorderLayout.NORTH);
        add(createOrdersScrollPane(), BorderLayout.CENTER);

        loadOrders();
    }

    private JPanel createTopFilterBar() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setBackground(Color.WHITE);

        String titleText = isAdminView ? "Platform Customer Orders"
                : isSellerView ? "Fulfillment Orders (Your Items)" : "Your Order History";
        JLabel title = new JLabel(titleText);
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        left.add(title);

        left.add(new JLabel("Status:"));
        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "PLACED", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"});
        statusFilterCombo.setFont(UITheme.FONT_BODY);
        statusFilterCombo.setBackground(Color.WHITE);
        statusFilterCombo.addActionListener(e -> loadOrders());
        left.add(statusFilterCombo);

        JButton refreshBtn = UITheme.createSecondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadOrders());
        left.add(refreshBtn);

        card.add(left, BorderLayout.WEST);

        countLabel = new JLabel("Loading orders...");
        countLabel.setFont(UITheme.FONT_SMALL_BOLD);
        countLabel.setForeground(UITheme.TEXT_SECONDARY);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setBackground(Color.WHITE);
        right.add(countLabel);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    private JScrollPane createOrdersScrollPane() {
        ordersContainer = new JPanel();
        ordersContainer.setLayout(new BoxLayout(ordersContainer, BoxLayout.Y_AXIS));
        ordersContainer.setBackground(UITheme.BG_LIGHT);

        scrollPane = new JScrollPane(ordersContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    public void loadOrders() {
        String filter = (String) statusFilterCombo.getSelectedItem();
        countLabel.setText("Fetching orders...");

        AsyncOperation.run(
                () -> {
                    if (isAdminView) {
                        return orderService.getAllOrdersForAdmin(currentUser, filter);
                    } else if (isSellerView) {
                        return orderService.getOrdersForSeller(currentUser.getId());
                    } else {
                        return orderService.getOrdersForBuyer(currentUser.getId());
                    }
                },
                list -> {
                    currentOrders = list;
                    renderOrders(list, filter);
                },
                ex -> {
                    countLabel.setText("Error loading orders");
                    DialogUtil.showError(this, "Failed to load orders: " + ex.getMessage());
                }
        );
    }

    private void renderOrders(List<Order> list, String filter) {
        ordersContainer.removeAll();

        List<Order> displayList = new ArrayList<>();
        for (Order o : list) {
            if (filter != null && !filter.equalsIgnoreCase("ALL") &&
                    !o.getStatus().equalsIgnoreCase(filter)) {
                continue;
            }
            displayList.add(o);
        }

        if (displayList.isEmpty()) {
            countLabel.setText("0 orders");
            JPanel empty = UITheme.createEmptyState(
                    "📦",
                    "No Orders Found",
                    "No order records currently match the selected status filter.",
                    null
            );
            ordersContainer.setLayout(new BorderLayout());
            ordersContainer.add(empty, BorderLayout.CENTER);
        } else {
            countLabel.setText("Total: " + displayList.size() + " order" + (displayList.size() == 1 ? "" : "s"));
            ordersContainer.setLayout(new BoxLayout(ordersContainer, BoxLayout.Y_AXIS));

            for (Order o : displayList) {
                ordersContainer.add(createOrderCard(o));
                ordersContainer.add(Box.createRigidArea(new Dimension(0, 14)));
            }
        }

        ordersContainer.revalidate();
        ordersContainer.repaint();
    }

    private JPanel createOrderCard(Order o) {
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));
        card.setMaximumSize(new Dimension(1600, 125));

        // Top Header of the card: Order #, Date, Status Badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(Color.WHITE);

        JPanel leftMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftMeta.setBackground(Color.WHITE);

        JLabel orderIdLbl = new JLabel("Order #" + o.getId());
        orderIdLbl.setFont(UITheme.FONT_SUBTITLE);
        orderIdLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel dateLbl = new JLabel("Placed on " + DateUtil.formatDateTime(o.getCreatedAt()));
        dateLbl.setFont(UITheme.FONT_SMALL);
        dateLbl.setForeground(UITheme.TEXT_MUTED);

        leftMeta.add(orderIdLbl);
        leftMeta.add(dateLbl);
        topRow.add(leftMeta, BorderLayout.WEST);

        // Status Badge
        JLabel statusBadge = createOrderStatusBadge(o.getStatus());
        topRow.add(statusBadge, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Center Info: Total, Payment Method, Buyer
        JPanel centerRow = new JPanel(new GridLayout(1, 3, 16, 0));
        centerRow.setBackground(Color.WHITE);

        JPanel col1 = new JPanel();
        col1.setLayout(new BoxLayout(col1, BoxLayout.Y_AXIS));
        col1.setBackground(Color.WHITE);
        JLabel c1Title = new JLabel("Total Amount");
        c1Title.setFont(UITheme.FONT_TINY);
        c1Title.setForeground(UITheme.TEXT_MUTED);
        JLabel c1Val = new JLabel("₹" + String.format("%.2f", o.getTotalAmount()));
        c1Val.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 16));
        c1Val.setForeground(UITheme.PRIMARY);
        col1.add(c1Title);
        col1.add(Box.createRigidArea(new Dimension(0, 2)));
        col1.add(c1Val);

        JPanel col2 = new JPanel();
        col2.setLayout(new BoxLayout(col2, BoxLayout.Y_AXIS));
        col2.setBackground(Color.WHITE);
        JLabel c2Title = new JLabel("Payment");
        c2Title.setFont(UITheme.FONT_TINY);
        c2Title.setForeground(UITheme.TEXT_MUTED);
        JLabel c2Val = new JLabel(o.getPaymentMethod() + " (" + o.getPaymentStatus() + ")");
        c2Val.setFont(UITheme.FONT_BODY_BOLD);
        c2Val.setForeground(UITheme.TEXT_PRIMARY);
        col2.add(c2Title);
        col2.add(Box.createRigidArea(new Dimension(0, 2)));
        col2.add(c2Val);

        JPanel col3 = new JPanel();
        col3.setLayout(new BoxLayout(col3, BoxLayout.Y_AXIS));
        col3.setBackground(Color.WHITE);
        JLabel c3Title = new JLabel("Customer / Destination");
        c3Title.setFont(UITheme.FONT_TINY);
        c3Title.setForeground(UITheme.TEXT_MUTED);
        String custText = o.getBuyerName() != null ? o.getBuyerName() : "Buyer #" + o.getBuyerId();
        JLabel c3Val = new JLabel(custText);
        c3Val.setFont(UITheme.FONT_BODY);
        c3Val.setForeground(UITheme.TEXT_SECONDARY);
        col3.add(c3Title);
        col3.add(Box.createRigidArea(new Dimension(0, 2)));
        col3.add(c3Val);

        centerRow.add(col1);
        centerRow.add(col2);
        centerRow.add(col3);
        card.add(centerRow, BorderLayout.CENTER);

        // Bottom Actions: View Items, Track Order, and Update Status
        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomActions.setBackground(Color.WHITE);

        JButton viewItemsBtn = UITheme.createSecondaryButton("Inspect Items");
        viewItemsBtn.setFont(UITheme.FONT_SMALL_BOLD);
        viewItemsBtn.addActionListener(e -> handleViewItems(o));
        bottomActions.add(viewItemsBtn);

        JButton trackBtn = UITheme.createSecondaryButton("🚚 Track Milestone");
        trackBtn.setFont(UITheme.FONT_SMALL_BOLD);
        trackBtn.addActionListener(e -> handleTrackOrder(o));
        bottomActions.add(trackBtn);

        if (isAdminView || isSellerView) {
            JButton updateBtn = UITheme.createPrimaryButton("Update Status");
            updateBtn.setFont(UITheme.FONT_SMALL_BOLD);
            updateBtn.addActionListener(e -> handleUpdateStatus(o));
            bottomActions.add(updateBtn);
        }

        card.add(bottomActions, BorderLayout.SOUTH);
        return card;
    }

    private JLabel createOrderStatusBadge(String status) {
        String s = status != null ? status.toUpperCase() : "PLACED";
        return switch (s) {
            case "DELIVERED" -> UITheme.createBadge("Delivered", new Color(0xDC, 0xFC, 0xE7), UITheme.SUCCESS_GREEN);
            case "SHIPPED" -> UITheme.createBadge("Shipped", new Color(0xED, 0xE9, 0xFE), UITheme.PRIMARY);
            case "CONFIRMED" -> UITheme.createBadge("Confirmed", new Color(0xFE, 0xF3, 0xC7), UITheme.WARNING_AMBER);
            case "CANCELLED" -> UITheme.createBadge("Cancelled", new Color(0xFE, 0xE2, 0xE2), UITheme.DANGER_RED);
            default -> UITheme.createBadge(s, new Color(0xEE, 0xED, 0xFF), UITheme.PRIMARY_DARK);
        };
    }

    private void handleViewItems(Order order) {
        AsyncOperation.run(
                () -> isSellerView
                        ? orderService.getOrderItemsForSeller(order.getId(), currentUser.getId())
                        : orderService.getOrderItems(order.getId()),
                items -> showItemsDialog(order, items),
                ex -> DialogUtil.showError(this, "Failed to load order items: " + ex.getMessage())
        );
    }

    private void showItemsDialog(Order order, List<OrderItem> items) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Order #" + order.getId() + " — Item Breakdown", true);
        dialog.setLayout(new BorderLayout(16, 16));
        dialog.setSize(620, 380);
        dialog.setLocationRelativeTo(this);

        String[] cols = {"Item ID", "Product Name", "Category", "Quantity", "Unit Price", "Subtotal"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (OrderItem oi : items) {
            model.addRow(new Object[]{
                    "#" + oi.getId(),
                    oi.getProductName() != null ? oi.getProductName() : "Product #" + oi.getProductId(),
                    oi.getProductCategory() != null ? oi.getProductCategory() : "-",
                    oi.getQuantity(),
                    "₹" + String.format("%.2f", oi.getPrice()),
                    "₹" + String.format("%.2f", oi.getSubtotal())
            });
        }

        JTable table = new JTable(model);
        UITheme.styleTable(table);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.WHITE);
        center.setBorder(new EmptyBorder(16, 16, 0, 16));
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        closePanel.setBackground(Color.WHITE);
        JButton close = UITheme.createPrimaryButton("Close");
        close.addActionListener(e -> dialog.dispose());
        closePanel.add(close);
        dialog.add(closePanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleTrackOrder(Order order) {
        AsyncOperation.run(
                () -> orderService.getOrderTracking(order.getId()),
                trackingList -> showTrackingDialog(order, trackingList),
                ex -> DialogUtil.showError(this, "Failed to load tracking: " + ex.getMessage())
        );
    }

    private void showTrackingDialog(Order order, List<OrderTracking> list) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Tracking Timeline — Order #" + order.getId(), true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.setSize(520, 380);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        content.setBackground(Color.WHITE);

        if (list.isEmpty()) {
            content.add(new JLabel("No tracking milestones recorded yet for this order."));
        } else {
            for (OrderTracking ot : list) {
                JPanel item = new JPanel(new BorderLayout(12, 4));
                item.setBackground(Color.WHITE);
                item.setBorder(new EmptyBorder(8, 0, 12, 0));

                JLabel status = new JLabel("● " + ot.getStatus());
                status.setFont(UITheme.FONT_BODY_BOLD);
                status.setForeground(UITheme.PRIMARY);

                JLabel desc = new JLabel(ot.getDescription());
                desc.setFont(UITheme.FONT_BODY);
                desc.setForeground(UITheme.TEXT_PRIMARY);

                JLabel time = new JLabel(DateUtil.formatDateTime(ot.getCreatedAt()));
                time.setFont(UITheme.FONT_TINY);
                time.setForeground(UITheme.TEXT_MUTED);

                item.add(status, BorderLayout.NORTH);
                item.add(desc, BorderLayout.CENTER);
                item.add(time, BorderLayout.SOUTH);

                content.add(item);
                content.add(new JSeparator(JSeparator.HORIZONTAL));
            }
        }

        dialog.add(new JScrollPane(content), BorderLayout.CENTER);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        closePanel.setBackground(Color.WHITE);
        JButton close = UITheme.createPrimaryButton("Close");
        close.addActionListener(e -> dialog.dispose());
        closePanel.add(close);
        dialog.add(closePanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void handleUpdateStatus(Order order) {
        String[] statuses = {"CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        String selected = (String) JOptionPane.showInputDialog(this,
                "Select new fulfillment status for Order #" + order.getId() + " (Current: " + order.getStatus() + "):",
                "Update Order Status", JOptionPane.QUESTION_MESSAGE, null, statuses, order.getStatus());

        if (selected == null || selected.isBlank()) return;

        AsyncOperation.run(
                () -> {
                    orderService.updateOrderStatus(currentUser, order.getId(), selected);
                    return null;
                },
                res -> {
                    DialogUtil.showSuccess(this, "Order #" + order.getId() + " status updated to " + selected);
                    loadOrders();
                },
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }
}
