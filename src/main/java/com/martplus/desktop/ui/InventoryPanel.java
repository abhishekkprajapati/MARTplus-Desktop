package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.InventoryService;
import com.martplus.desktop.service.ProductService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for Seller stock management, low-stock alerts, and synchronized updates.
 * Demonstrates:
 * 1. Synchronized inventory updates via ReentrantLock concurrency control.
 * 2. Real-time stock status monitoring matching frontend/seller/inventory.html.
 */
public class InventoryPanel extends JPanel {

    private final InventoryService inventoryService;
    private final ProductService productService;
    private final User currentUser;

    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private JCheckBox lowStockOnlyCheck;
    private JLabel lowStockAlertBanner;
    private List<Product> products = new ArrayList<>();

    public InventoryPanel(InventoryService inventoryService, ProductService productService, User currentUser) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopFilterPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createBottomActionPanel(), BorderLayout.SOUTH);

        loadInventory();
    }

    private JPanel createTopFilterPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);

        // Low stock warning banner with soft amber styling
        lowStockAlertBanner = new JLabel("  ⚠️ Monitoring stock levels. Products with 5 or fewer units require immediate replenishment.");
        lowStockAlertBanner.setFont(UITheme.FONT_BODY_BOLD);
        lowStockAlertBanner.setForeground(new Color(0xB4, 0x53, 0x09));
        lowStockAlertBanner.setBackground(new Color(0xFE, 0xF3, 0xC7));
        lowStockAlertBanner.setOpaque(true);
        lowStockAlertBanner.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFC, 0xD3, 0x4D), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        container.add(lowStockAlertBanner, BorderLayout.NORTH);

        JPanel filterCard = UITheme.createCard();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 0));

        lowStockOnlyCheck = new JCheckBox("Filter: Low Stock Products Only (<= 5 units)");
        lowStockOnlyCheck.setFont(UITheme.FONT_BODY_BOLD);
        lowStockOnlyCheck.setForeground(UITheme.TEXT_PRIMARY);
        lowStockOnlyCheck.setBackground(Color.WHITE);
        lowStockOnlyCheck.addActionListener(e -> loadInventory());
        filterCard.add(lowStockOnlyCheck);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Stock");
        refreshBtn.addActionListener(e -> loadInventory());
        filterCard.add(refreshBtn);

        container.add(filterCard, BorderLayout.CENTER);
        return container;
    }

    private JPanel createTablePanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        String[] cols = {"Product ID", "Product Name", "Category", "Price (₹)", "Current Stock", "Stock Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        inventoryTable = new JTable(tableModel);
        UITheme.styleTable(inventoryTable);
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        inventoryTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        inventoryTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        inventoryTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        inventoryTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        inventoryTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        inventoryTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        // Custom Cell Renderer for Stock Status Column
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                lbl.setFont(UITheme.FONT_BODY_BOLD);
                if (status.contains("IN STOCK")) {
                    lbl.setForeground(UITheme.SUCCESS_GREEN);
                } else if (status.contains("LOW STOCK")) {
                    lbl.setForeground(UITheme.WARNING_AMBER);
                } else {
                    lbl.setForeground(UITheme.DANGER_RED);
                }
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(inventoryTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBottomActionPanel() {
        JPanel card = UITheme.createCard();
        card.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 0));

        JButton quickAdd5 = UITheme.createSecondaryButton("+5 Quick Stock");
        quickAdd5.addActionListener(e -> handleQuickAdjust(5));
        card.add(quickAdd5);

        JButton quickAdd20 = UITheme.createSecondaryButton("+20 Quick Stock");
        quickAdd20.addActionListener(e -> handleQuickAdjust(20));
        card.add(quickAdd20);

        JButton updateStockBtn = UITheme.createPrimaryButton("Set Exact Stock Quantity");
        updateStockBtn.addActionListener(e -> handleSetStock());
        card.add(updateStockBtn);

        return card;
    }

    public void loadInventory() {
        boolean lowOnly = lowStockOnlyCheck.isSelected();

        AsyncOperation.run(
                () -> lowOnly
                        ? inventoryService.getLowStockAlerts(currentUser.getId(), 5)
                        : productService.getProductsBySeller(currentUser.getId()),
                list -> {
                    products = list;
                    tableModel.setRowCount(0);
                    int lowStockCount = 0;

                    for (Product p : list) {
                        int stock = p.getStock() != null ? p.getStock() : 0;
                        String statusText;
                        if (stock == 0) {
                            statusText = "OUT OF STOCK";
                            lowStockCount++;
                        } else if (stock <= 5) {
                            statusText = "LOW STOCK (" + stock + " left)";
                            lowStockCount++;
                        } else {
                            statusText = "IN STOCK";
                        }

                        tableModel.addRow(new Object[]{
                                p.getId(),
                                p.getName(),
                                p.getCategory(),
                                String.format("%.2f", p.getPrice()),
                                stock,
                                statusText
                        });
                    }

                    if (lowStockCount > 0) {
                        lowStockAlertBanner.setText("  ⚠️ Attention: " + lowStockCount + " product(s) have low or zero stock. Replenish immediately.");
                    } else {
                        lowStockAlertBanner.setText("  ✓ All monitored products currently satisfy safety stock thresholds (> 5 units).");
                    }
                },
                ex -> DialogUtil.showError(this, "Failed to load inventory: " + ex.getMessage())
        );
    }

    private void handleQuickAdjust(int amount) {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            DialogUtil.showWarning(this, "Please select a product from the table first.");
            return;
        }

        Product p = products.get(row);
        AsyncOperation.run(
                () -> {
                    inventoryService.adjustStock(p.getId(), amount);
                    return null;
                },
                res -> {
                    DialogUtil.showSuccess(this, "Added " + amount + " units to " + p.getName());
                    loadInventory();
                },
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }

    private void handleSetStock() {
        int row = inventoryTable.getSelectedRow();
        if (row < 0) {
            DialogUtil.showWarning(this, "Please select a product from the table first.");
            return;
        }

        Product p = products.get(row);
        String input = JOptionPane.showInputDialog(this,
                "Enter new exact stock quantity for '" + p.getName() + "':",
                p.getStock() != null ? p.getStock() : 0);

        if (input != null && !input.trim().isEmpty()) {
            try {
                int newQty = Integer.parseInt(input.trim());
                if (newQty < 0) {
                    DialogUtil.showWarning(this, "Stock quantity cannot be negative.");
                    return;
                }

                AsyncOperation.run(
                        () -> {
                            inventoryService.updateStock(p.getId(), newQty);
                            return null;
                        },
                        res -> {
                            DialogUtil.showSuccess(this, "Stock for '" + p.getName() + "' updated to " + newQty);
                            loadInventory();
                        },
                        ex -> DialogUtil.showError(this, ex.getMessage())
                );
            } catch (NumberFormatException ex) {
                DialogUtil.showWarning(this, "Invalid number format: " + input);
            }
        }
    }
}
