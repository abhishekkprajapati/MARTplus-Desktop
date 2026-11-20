package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.AddressDao;
import com.martplus.desktop.model.Address;
import com.martplus.desktop.model.CartItem;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.CartService;
import com.martplus.desktop.service.OrderService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.ImageUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Shopping Cart Panel displaying item cards with quantity steppers,
 * dynamic image loading, and an Order Summary checkout sidebar.
 * Reference: frontend/cart.html & frontend/checkout.html
 */
public class CartPanel extends JPanel {

    private final CartService cartService;
    private final OrderService orderService;
    private final AddressDao addressDao;
    private final User currentUser;
    private final Runnable onOrderPlacedCallback;
    private Consumer<String> navigationCallback = null;

    private JPanel itemsContainer;
    private JScrollPane scrollPane;
    private JPanel summaryPanel;
    private JLabel summaryItemsCountLabel;
    private JLabel summarySubtotalLabel;
    private JLabel summaryTotalLabel;
    private JButton checkoutBtn;
    private JButton clearCartBtn;

    private List<CartItem> cartItems = new ArrayList<>();

    public CartPanel(CartService cartService, OrderService orderService,
                     AddressDao addressDao, User currentUser, Runnable onOrderPlacedCallback) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.addressDao = addressDao;
        this.currentUser = currentUser;
        this.onOrderPlacedCallback = onOrderPlacedCallback;

        setLayout(new BorderLayout(24, 0));
        setBackground(UITheme.BG_LIGHT);

        buildUI();
        loadCart();
    }

    public void setNavigationCallback(Consumer<String> callback) {
        this.navigationCallback = callback;
    }

    private void buildUI() {
        // Left / Center: Items ScrollPane
        itemsContainer = new JPanel();
        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        itemsContainer.setBackground(UITheme.BG_LIGHT);

        scrollPane = new JScrollPane(itemsContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // Right Side: Order Summary Card
        summaryPanel = createOrderSummaryCard();
        add(summaryPanel, BorderLayout.EAST);
    }

    private JPanel createOrderSummaryCard() {
        JPanel summaryCard = new JPanel();
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBackground(Color.WHITE);
        summaryCard.setPreferredSize(new Dimension(320, 0));
        summaryCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(24, 20, 24, 20)
        ));

        JLabel title = new JLabel("Order Summary");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryCard.add(title);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 16)));

        // Items Subtotal Row
        JPanel subtotalRow = new JPanel(new BorderLayout());
        subtotalRow.setBackground(Color.WHITE);
        subtotalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryItemsCountLabel = new JLabel("Subtotal (0 items):");
        summaryItemsCountLabel.setFont(UITheme.FONT_BODY);
        summaryItemsCountLabel.setForeground(UITheme.TEXT_SECONDARY);
        summarySubtotalLabel = new JLabel("₹0.00");
        summarySubtotalLabel.setFont(UITheme.FONT_BODY_BOLD);
        subtotalRow.add(summaryItemsCountLabel, BorderLayout.WEST);
        subtotalRow.add(summarySubtotalLabel, BorderLayout.EAST);
        summaryCard.add(subtotalRow);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 10)));

        // Delivery Estimate Row
        JPanel deliveryRow = new JPanel(new BorderLayout());
        deliveryRow.setBackground(Color.WHITE);
        deliveryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel delLabel = new JLabel("Standard Delivery:");
        delLabel.setFont(UITheme.FONT_BODY);
        delLabel.setForeground(UITheme.TEXT_SECONDARY);
        JLabel delFree = new JLabel("FREE");
        delFree.setFont(UITheme.FONT_BODY_BOLD);
        delFree.setForeground(UITheme.SUCCESS_GREEN);
        deliveryRow.add(delLabel, BorderLayout.WEST);
        deliveryRow.add(delFree, BorderLayout.EAST);
        summaryCard.add(deliveryRow);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 16)));

        JSeparator div = new JSeparator(JSeparator.HORIZONTAL);
        div.setForeground(UITheme.BORDER_LIGHT);
        div.setMaximumSize(new Dimension(280, 1));
        div.setAlignmentX(Component.LEFT_ALIGNMENT);
        summaryCard.add(div);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 16)));

        // Total Row
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setBackground(Color.WHITE);
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalText = new JLabel("Total Amount:");
        totalText.setFont(UITheme.FONT_HEADER);
        totalText.setForeground(UITheme.TEXT_PRIMARY);
        summaryTotalLabel = new JLabel("₹0.00");
        summaryTotalLabel.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 20));
        summaryTotalLabel.setForeground(UITheme.PRIMARY);
        totalRow.add(totalText, BorderLayout.WEST);
        totalRow.add(summaryTotalLabel, BorderLayout.EAST);
        summaryCard.add(totalRow);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 24)));

        // Checkout Button
        checkoutBtn = UITheme.createPrimaryButton("Proceed to Checkout ➔");
        checkoutBtn.setMaximumSize(new Dimension(280, 42));
        checkoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkoutBtn.addActionListener(e -> handleCheckout());
        summaryCard.add(checkoutBtn);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 10)));

        // Clear Cart Button
        clearCartBtn = UITheme.createSecondaryButton("Clear Shopping Cart");
        clearCartBtn.setMaximumSize(new Dimension(280, 38));
        clearCartBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        clearCartBtn.addActionListener(e -> handleClearCart());
        summaryCard.add(clearCartBtn);

        summaryCard.add(Box.createVerticalGlue());
        return summaryCard;
    }

    public void loadCart() {
        AsyncOperation.run(
                () -> cartService.getCartItems(currentUser.getId()),
                items -> {
                    cartItems = items;
                    renderCartItems(items);
                },
                ex -> DialogUtil.showError(this, "Failed to load cart: " + ex.getMessage())
        );
    }

    private void renderCartItems(List<CartItem> items) {
        itemsContainer.removeAll();

        if (items.isEmpty()) {
            JButton browseBtn = UITheme.createPrimaryButton("Browse Products");
            browseBtn.addActionListener(e -> {
                if (navigationCallback != null) {
                    navigationCallback.accept("PRODUCTS");
                }
            });

            JPanel emptyState = UITheme.createEmptyState(
                    "🛒",
                    "Your Shopping Cart is Empty",
                    "Explore the MARTplus catalog and discover top deals from verified sellers.",
                    browseBtn
            );
            itemsContainer.setLayout(new BorderLayout());
            itemsContainer.add(emptyState, BorderLayout.CENTER);

            summaryItemsCountLabel.setText("Subtotal (0 items):");
            summarySubtotalLabel.setText("₹0.00");
            summaryTotalLabel.setText("₹0.00");
            checkoutBtn.setEnabled(false);
            clearCartBtn.setEnabled(false);
        } else {
            itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));

            BigDecimal total = BigDecimal.ZERO;
            int totalQty = 0;

            for (CartItem item : items) {
                total = total.add(item.getSubtotal());
                totalQty += item.getQuantity();
                itemsContainer.add(createCartItemRow(item));
                itemsContainer.add(Box.createRigidArea(new Dimension(0, 12)));
            }

            summaryItemsCountLabel.setText("Subtotal (" + totalQty + " item" + (totalQty == 1 ? "" : "s") + "):");
            String formattedTotal = "₹" + String.format("%.2f", total);
            summarySubtotalLabel.setText(formattedTotal);
            summaryTotalLabel.setText(formattedTotal);
            checkoutBtn.setEnabled(true);
            clearCartBtn.setEnabled(true);
        }

        itemsContainer.revalidate();
        itemsContainer.repaint();
    }

    /**
     * Builds a modern cart item row with product thumbnail, quantity controls, and subtotal.
     */
    private JPanel createCartItemRow(CartItem item) {
        Product p = item.getProduct();
        String prodName = p != null ? p.getName() : "Product #" + item.getProductId();
        String category = p != null && p.getCategory() != null ? p.getCategory() : "GENERAL";
        BigDecimal unitPrice = p != null && p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
        String imageUrl = p != null ? p.getImageUrl() : null;

        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(1600, 100));

        // Left Image Thumbnail
        JLabel imgLabel = new JLabel();
        imgLabel.setPreferredSize(new Dimension(72, 72));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setIcon(ImageUtil.createPlaceholder(72, 72, "📦"));
        ImageUtil.loadProductImageAsync(imageUrl, 72, 72, imgLabel::setIcon);
        card.add(imgLabel, BorderLayout.WEST);

        // Center Information
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JLabel catLbl = new JLabel(category.toUpperCase());
        catLbl.setFont(UITheme.FONT_TINY);
        catLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel titleLbl = new JLabel(prodName);
        titleLbl.setFont(UITheme.FONT_BODY_BOLD);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel priceLbl = new JLabel("Unit Price: ₹" + String.format("%.2f", unitPrice));
        priceLbl.setFont(UITheme.FONT_SMALL);
        priceLbl.setForeground(UITheme.TEXT_SECONDARY);

        infoPanel.add(catLbl);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        infoPanel.add(titleLbl);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        infoPanel.add(priceLbl);
        card.add(infoPanel, BorderLayout.CENTER);

        // Right Panel: Quantity Stepper, Subtotal & Remove
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 16));
        rightControls.setBackground(Color.WHITE);

        // Stepper: [-] [Qty] [+]
        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        stepper.setBackground(UITheme.BG_LIGHT);
        stepper.setBorder(new LineBorder(UITheme.BORDER_LIGHT, 1, true));

        JButton decBtn = new JButton("-");
        decBtn.setFont(UITheme.FONT_BODY_BOLD);
        decBtn.setPreferredSize(new Dimension(28, 28));
        decBtn.setFocusPainted(false);
        decBtn.setBackground(Color.WHITE);
        decBtn.setBorder(BorderFactory.createEmptyBorder());
        decBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        decBtn.addActionListener(e -> {
            if (item.getQuantity() > 1) {
                updateItemQuantity(item.getId(), item.getQuantity() - 1);
            } else {
                handleRemoveItem(item);
            }
        });

        JLabel qtyLabel = new JLabel(String.valueOf(item.getQuantity()), SwingConstants.CENTER);
        qtyLabel.setFont(UITheme.FONT_BODY_BOLD);
        qtyLabel.setPreferredSize(new Dimension(30, 28));

        JButton incBtn = new JButton("+");
        incBtn.setFont(UITheme.FONT_BODY_BOLD);
        incBtn.setPreferredSize(new Dimension(28, 28));
        incBtn.setFocusPainted(false);
        incBtn.setBackground(Color.WHITE);
        incBtn.setBorder(BorderFactory.createEmptyBorder());
        incBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        incBtn.addActionListener(e -> updateItemQuantity(item.getId(), item.getQuantity() + 1));

        stepper.add(decBtn);
        stepper.add(qtyLabel);
        stepper.add(incBtn);
        rightControls.add(stepper);

        // Subtotal Label
        JLabel subtotalVal = new JLabel("₹" + String.format("%.2f", item.getSubtotal()));
        subtotalVal.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 15));
        subtotalVal.setForeground(UITheme.TEXT_PRIMARY);
        subtotalVal.setPreferredSize(new Dimension(100, 28));
        subtotalVal.setHorizontalAlignment(SwingConstants.RIGHT);
        rightControls.add(subtotalVal);

        // Remove Button
        JButton removeBtn = new JButton("🗑️");
        removeBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        removeBtn.setForeground(UITheme.DANGER_RED);
        removeBtn.setBackground(Color.WHITE);
        removeBtn.setFocusPainted(false);
        removeBtn.setBorder(new EmptyBorder(4, 6, 4, 6));
        removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeBtn.setToolTipText("Remove from Cart");
        removeBtn.addActionListener(e -> handleRemoveItem(item));
        rightControls.add(removeBtn);

        card.add(rightControls, BorderLayout.EAST);
        return card;
    }

    private void updateItemQuantity(Long cartItemId, int newQty) {
        AsyncOperation.run(
                () -> {
                    cartService.updateQuantity(cartItemId, newQty);
                    return null;
                },
                res -> loadCart(),
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }

    private void handleRemoveItem(CartItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : "item";
        if (DialogUtil.confirm(this, "Remove '" + name + "' from your cart?", "Confirm Removal")) {
            AsyncOperation.run(
                    () -> {
                        cartService.removeFromCart(item.getId());
                        return null;
                    },
                    res -> loadCart(),
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        }
    }

    private void handleClearCart() {
        if (DialogUtil.confirm(this, "Are you sure you want to clear your entire cart?", "Clear Cart")) {
            AsyncOperation.run(
                    () -> {
                        cartService.clearCart(currentUser.getId());
                        return null;
                    },
                    res -> loadCart(),
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        }
    }

    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            DialogUtil.showWarning(this, "Your shopping cart is empty.");
            return;
        }

        List<Address> addresses = addressDao.findByBuyerId(currentUser.getId());
        if (addresses.isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "You do not have any saved delivery addresses. Would you like to add one now?",
                    "Delivery Address Needed", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                promptAddNewAddress();
            }
            return;
        }

        showCheckoutDialog(addresses);
    }

    private void showCheckoutDialog(List<Address> addresses) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Confirm Order & Checkout", true);
        dialog.setLayout(new BorderLayout(16, 16));
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 10));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        JLabel addrHeader = new JLabel("1. Select Delivery Address:");
        addrHeader.setFont(UITheme.FONT_BODY_BOLD);
        panel.add(addrHeader);

        JComboBox<Address> addressCombo = new JComboBox<>(addresses.toArray(new Address[0]));
        addressCombo.setFont(UITheme.FONT_BODY);
        addressCombo.setBackground(Color.WHITE);
        panel.add(addressCombo);

        JButton addAddressBtn = UITheme.createSecondaryButton("+ Add New Address");
        addAddressBtn.addActionListener(e -> {
            promptAddNewAddress();
            dialog.dispose();
            handleCheckout();
        });
        panel.add(addAddressBtn);

        JLabel payHeader = new JLabel("2. Select Payment Method:");
        payHeader.setFont(UITheme.FONT_BODY_BOLD);
        panel.add(payHeader);

        JComboBox<String> paymentCombo = new JComboBox<>(new String[]{"COD (Cash on Delivery)", "UPI", "CREDIT_CARD", "DEBIT_CARD", "NET_BANKING"});
        paymentCombo.setFont(UITheme.FONT_BODY);
        paymentCombo.setBackground(Color.WHITE);
        panel.add(paymentCombo);

        BigDecimal total = cartService.calculateCartTotal(cartItems);
        JLabel summaryLabel = new JLabel("Total Payable: ₹" + String.format("%.2f", total));
        summaryLabel.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 18));
        summaryLabel.setForeground(UITheme.PRIMARY);
        panel.add(summaryLabel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT));

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton placeOrderBtn = UITheme.createPrimaryButton("Place Order Now");
        placeOrderBtn.addActionListener(e -> {
            Address selectedAddr = (Address) addressCombo.getSelectedItem();
            String rawPayMethod = (String) paymentCombo.getSelectedItem();
            String paymentMethod = rawPayMethod != null && rawPayMethod.startsWith("COD") ? "COD" : rawPayMethod;

            if (selectedAddr == null) {
                DialogUtil.showWarning(dialog, "Please select an address.");
                return;
            }

            placeOrderBtn.setEnabled(false);
            placeOrderBtn.setText("Processing Transaction...");

            AsyncOperation.run(
                    () -> orderService.processCheckout(currentUser, selectedAddr.getId(), paymentMethod),
                    order -> {
                        dialog.dispose();
                        DialogUtil.showSuccess(this,
                                "Order #" + order.getId() + " placed successfully!\n" +
                                "Total: ₹" + String.format("%.2f", order.getTotalAmount()) + "\n" +
                                "Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")\n" +
                                "Your cart has been cleared and a tracking milestone created.");
                        loadCart();
                        if (onOrderPlacedCallback != null) {
                            onOrderPlacedCallback.run();
                        }
                    },
                    ex -> {
                        placeOrderBtn.setEnabled(true);
                        placeOrderBtn.setText("Place Order Now");
                        DialogUtil.showError(dialog, "Checkout Failed: " + ex.getMessage());
                    }
            );
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(placeOrderBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void promptAddNewAddress() {
        JTextField nameField = new JTextField(currentUser.getFullName());
        JTextField phoneField = new JTextField(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        JTextField lineField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField pinField = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Full Name:")); form.add(nameField);
        form.add(new JLabel("Phone (10 digits):")); form.add(phoneField);
        form.add(new JLabel("Street Address:")); form.add(lineField);
        form.add(new JLabel("City:")); form.add(cityField);
        form.add(new JLabel("State:")); form.add(stateField);
        form.add(new JLabel("PIN Code:")); form.add(pinField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Delivery Address",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                Address address = new Address(
                        null,
                        currentUser.getId(),
                        nameField.getText().trim(),
                        phoneField.getText().trim(),
                        lineField.getText().trim(),
                        cityField.getText().trim(),
                        stateField.getText().trim(),
                        pinField.getText().trim(),
                        true,
                        null,
                        null
                );
                addressDao.save(address);
                DialogUtil.showSuccess(this, "Address saved successfully!");
            } catch (Exception ex) {
                DialogUtil.showError(this, "Failed to save address: " + ex.getMessage());
            }
        }
    }
}
