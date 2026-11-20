package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.CartService;
import com.martplus.desktop.service.WishlistService;
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
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modern, E-Commerce Product Details Dialog.
 * Features a structured two-column layout inspired by the official MARTplus website:
 * - Left column: Aspect-ratio preserved high-resolution product showcase container.
 * - Right column: Hierarchical product metadata, live stock status, seller badge,
 *   and non-editable clean wrapped description.
 * - Fixed footer: Close action, interactive quantity stepper (- / +), live subtotal calculation,
 *   and responsive Add to Cart trigger.
 * Reference: frontend/product-details.html
 */
public class ProductDetailsDialog extends JDialog {

    private final Product product;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final User currentUser;
    private final Runnable onActionCallback;

    private int selectedQuantity = 1;
    private JLabel qtyValueLabel;
    private JLabel subtotalLabel;
    private JButton minusBtn;
    private JButton plusBtn;
    private JButton addToCartBtn;

    public ProductDetailsDialog(Window owner, Product product, CartService cartService,
                                WishlistService wishlistService, User currentUser,
                                Runnable onActionCallback) {
        super(owner, product != null ? product.getName() + " — MARTplus" : "Product Details", ModalityType.APPLICATION_MODAL);
        this.product = product;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.currentUser = currentUser;
        this.onActionCallback = onActionCallback;

        int availableStock = (product != null && product.getStock() != null) ? Math.max(0, product.getStock()) : 0;
        this.selectedQuantity = availableStock > 0 ? 1 : 0;

        initUI();
    }

    private void initUI() {
        setSize(820, 540);
        setMinimumSize(new Dimension(780, 500));
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Color.WHITE);

        if (product == null) {
            add(new JLabel("Product details unavailable.", SwingConstants.CENTER), BorderLayout.CENTER);
            return;
        }

        // Center Content: Two-Column Showcase Grid
        add(createMainContentPanel(), BorderLayout.CENTER);

        // Fixed Footer: Quantity controls, subtotal, and cart action
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createMainContentPanel() {
        JPanel container = new JPanel(new BorderLayout(24, 0));
        container.setBackground(Color.WHITE);
        container.setBorder(new EmptyBorder(22, 24, 16, 24));

        // LEFT COLUMN — Product Image Showcase
        container.add(createImageColumn(), BorderLayout.WEST);

        // RIGHT COLUMN — Product Information & Description
        container.add(createInfoColumn(), BorderLayout.CENTER);

        return container;
    }

    /**
     * Left Column: Clean product image container with aspect-ratio scaling
     * and status overlay badges.
     */
    private JPanel createImageColumn() {
        JPanel imageCard = new JPanel(new BorderLayout());
        imageCard.setPreferredSize(new Dimension(340, 420));
        imageCard.setBackground(new Color(0xF8, 0xF9, 0xFC));
        imageCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Top Overlay: Stock Badge (Left) & Optional Wishlist Toggle (Right)
        JPanel topOverlay = new JPanel(new BorderLayout());
        topOverlay.setOpaque(false);

        boolean inStock = product.isAvailable() && product.getStock() > 0;
        JLabel stockBadge = inStock
                ? UITheme.createBadge("In Stock", new Color(0xDC, 0xFC, 0xE7), UITheme.SUCCESS_GREEN)
                : UITheme.createBadge("Out of Stock", new Color(0xFE, 0xE2, 0xE2), UITheme.DANGER_RED);
        topOverlay.add(stockBadge, BorderLayout.WEST);

        if (wishlistService != null && currentUser != null && currentUser.getId() != null) {
            JButton heartBtn = createWishlistHeartButton();
            topOverlay.add(heartBtn, BorderLayout.EAST);
        }

        imageCard.add(topOverlay, BorderLayout.NORTH);

        // Center: High-Resolution Scaled Product Image
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setIcon(ImageUtil.createPlaceholder(316, 300, "📦"));

        // Asynchronous image loading with aspect-ratio scaling
        ImageUtil.loadProductImageAsync(product.getImageUrl(), 316, 300, imageLabel::setIcon);
        imageCard.add(imageLabel, BorderLayout.CENTER);

        // Bottom: Subtle authenticity guarantee note
        JLabel guaranteeLabel = new JLabel("✓ 100% Verified Quality • Fast Delivery", SwingConstants.CENTER);
        guaranteeLabel.setFont(UITheme.FONT_TINY);
        guaranteeLabel.setForeground(UITheme.TEXT_MUTED);
        guaranteeLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        imageCard.add(guaranteeLabel, BorderLayout.SOUTH);

        return imageCard;
    }

    /**
     * Right Column: Structured Product Information adhering strictly to required order:
     * 1. Product name
     * 2. Category
     * 3. Price
     * 4. Availability
     * 5. Seller name
     * 6. Product description
     */
    private JPanel createInfoColumn() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        // 1. Category Badge
        String categoryName = (product.getCategory() != null && !product.getCategory().isBlank())
                ? product.getCategory().toUpperCase()
                : "GENERAL";
        JLabel categoryBadge = UITheme.createBadge(categoryName, UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
        categoryBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(categoryBadge);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. Product Name — Large, bold heading
        JLabel titleLabel = new JLabel("<html><body style='width: 380px; font-weight: bold; line-height: 1.25; color: #111827;'>"
                + escapeHtml(product.getName()) + "</body></html>");
        titleLabel.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(titleLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 3. Price — Prominent purple price
        JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        priceRow.setBackground(Color.WHITE);
        priceRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel("₹" + String.format("%.2f", product.getPrice()));
        priceLabel.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 26));
        priceLabel.setForeground(UITheme.PRIMARY);
        priceRow.add(priceLabel);

        JLabel taxNote = new JLabel("(Inclusive of all taxes)");
        taxNote.setFont(UITheme.FONT_SMALL);
        taxNote.setForeground(UITheme.TEXT_SECONDARY);
        priceRow.add(taxNote);

        infoPanel.add(priceRow);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // 4. Availability — Green or red stock indicator
        JPanel stockRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stockRow.setBackground(Color.WHITE);
        stockRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        boolean inStock = product.isAvailable() && product.getStock() > 0;
        JLabel stockDot = new JLabel("●");
        stockDot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stockDot.setForeground(inStock ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
        stockRow.add(stockDot);

        JLabel stockText = new JLabel(inStock
                ? "In Stock (" + product.getStock() + " units available)"
                : "Currently Out of Stock");
        stockText.setFont(UITheme.FONT_BODY_BOLD);
        stockText.setForeground(inStock ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED);
        stockRow.add(stockText);

        infoPanel.add(stockRow);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 5. Seller Name — Subtle secondary text
        String seller = product.getSellerName() != null ? product.getSellerName() : "Seller #" + product.getSellerId();
        JLabel sellerLabel = new JLabel("🏪 Sold by: " + seller);
        sellerLabel.setFont(UITheme.FONT_BODY);
        sellerLabel.setForeground(UITheme.TEXT_SECONDARY);
        sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(sellerLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // 6. Product Description — Normal, readable, non-editable wrapped text block
        JLabel descHeader = new JLabel("Product Description");
        descHeader.setFont(UITheme.FONT_HEADER);
        descHeader.setForeground(UITheme.TEXT_PRIMARY);
        descHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(descHeader);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JTextPane descPane = new JTextPane();
        descPane.setEditable(false);
        descPane.setFocusable(false);
        descPane.setText(ProductPanel.getSanitizedDescription(product));
        descPane.setFont(UITheme.FONT_BODY);
        descPane.setForeground(new Color(0x37, 0x41, 0x51));
        descPane.setBackground(new Color(0xF9, 0xFA, 0xFB));
        descPane.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane descScroll = new JScrollPane(descPane);
        descScroll.setBorder(new LineBorder(UITheme.BORDER_SUBTLE, 1, true));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setPreferredSize(new Dimension(390, 120));
        descScroll.setMaximumSize(new Dimension(420, 140));
        descScroll.getVerticalScrollBar().setUnitIncrement(12);

        infoPanel.add(descScroll);
        infoPanel.add(Box.createVerticalGlue());

        return infoPanel;
    }

    /**
     * Fixed Footer containing:
     * - Close button (Left)
     * - Subtotal display, interactive quantity stepper (- / +), and Add to Cart action (Right)
     */
    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_LIGHT),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Left Action: Close
        JButton closeBtn = UITheme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn, BorderLayout.WEST);

        // Right Actions: Subtotal + Quantity Stepper + Add to Cart
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightActions.setBackground(Color.WHITE);

        // Live Subtotal Label
        subtotalLabel = new JLabel();
        subtotalLabel.setFont(UITheme.FONT_BODY_BOLD);
        subtotalLabel.setForeground(UITheme.TEXT_PRIMARY);
        updateSubtotalLabel();
        rightActions.add(subtotalLabel);

        // Quantity Stepper: [-] [ 1 ] [+]
        JPanel stepper = createQuantityStepper();
        rightActions.add(stepper);

        // Add to Cart Primary Button
        boolean inStock = product.isAvailable() && product.getStock() > 0;
        addToCartBtn = UITheme.createPrimaryButton("🛒 Add to Cart");
        addToCartBtn.setEnabled(inStock);
        if (!inStock) {
            addToCartBtn.setText("Out of Stock");
        }
        addToCartBtn.addActionListener(e -> handleAddToCart());
        rightActions.add(addToCartBtn);

        footer.add(rightActions, BorderLayout.EAST);
        return footer;
    }

    private JPanel createQuantityStepper() {
        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        stepper.setBackground(Color.WHITE);
        stepper.setBorder(new LineBorder(UITheme.BORDER_LIGHT, 1, true));

        int maxStock = (product.getStock() != null) ? product.getStock() : 0;

        minusBtn = createStepperButton("−");
        minusBtn.setEnabled(selectedQuantity > 1);
        minusBtn.addActionListener(e -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                updateQuantityDisplay(maxStock);
            }
        });

        qtyValueLabel = new JLabel(String.valueOf(selectedQuantity), SwingConstants.CENTER);
        qtyValueLabel.setPreferredSize(new Dimension(38, 36));
        qtyValueLabel.setFont(UITheme.FONT_BODY_BOLD);
        qtyValueLabel.setForeground(UITheme.TEXT_PRIMARY);

        plusBtn = createStepperButton("+");
        plusBtn.setEnabled(selectedQuantity < maxStock);
        plusBtn.addActionListener(e -> {
            if (selectedQuantity < maxStock) {
                selectedQuantity++;
                updateQuantityDisplay(maxStock);
            }
        });

        stepper.add(minusBtn);
        stepper.add(qtyValueLabel);
        stepper.add(plusBtn);

        return stepper;
    }

    private JButton createStepperButton(String symbol) {
        JButton btn = new JButton(symbol);
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(new Color(0x37, 0x41, 0x51));
        btn.setBackground(new Color(0xF9, 0xFA, 0xFB));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(new Color(0xEE, 0xED, 0xFF));
                    btn.setForeground(UITheme.PRIMARY);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(new Color(0xF9, 0xFA, 0xFB));
                    btn.setForeground(new Color(0x37, 0x41, 0x51));
                }
            }
        });

        return btn;
    }

    private void updateQuantityDisplay(int maxStock) {
        qtyValueLabel.setText(String.valueOf(selectedQuantity));
        minusBtn.setEnabled(selectedQuantity > 1);
        plusBtn.setEnabled(selectedQuantity < maxStock);
        updateSubtotalLabel();
    }

    private void updateSubtotalLabel() {
        if (subtotalLabel == null) return;
        BigDecimal price = (product != null && product.getPrice() != null) ? product.getPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(Math.max(0, selectedQuantity)));
        subtotalLabel.setText("Subtotal: ₹" + String.format("%.2f", subtotal));
    }

    private void handleAddToCart() {
        if (selectedQuantity <= 0 || !product.isAvailable()) {
            DialogUtil.showWarning(this, "This product is currently out of stock.");
            return;
        }

        if (currentUser == null || currentUser.getId() == null) {
            DialogUtil.showWarning(this, "Please sign in with a Buyer account to add items to your cart.");
            return;
        }

        addToCartBtn.setEnabled(false);
        addToCartBtn.setText("Adding...");

        AsyncOperation.run(
                () -> {
                    cartService.addToCart(currentUser.getId(), product.getId(), selectedQuantity);
                    return null;
                },
                res -> {
                    DialogUtil.showSuccess(this, "Added " + selectedQuantity + " item(s) of '"
                            + product.getName() + "' to your cart!");
                    if (onActionCallback != null) {
                        onActionCallback.run();
                    }
                    dispose();
                },
                ex -> {
                    addToCartBtn.setEnabled(true);
                    addToCartBtn.setText("🛒 Add to Cart");
                    DialogUtil.showError(this, "Failed to add to cart: " + ex.getMessage());
                }
        );
    }

    private JButton createWishlistHeartButton() {
        AtomicBoolean isSaved = new AtomicBoolean(false);
        JButton heartBtn = new JButton("♡");
        heartBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 17));
        heartBtn.setForeground(UITheme.TEXT_SECONDARY);
        heartBtn.setBackground(new Color(255, 255, 255, 230));
        heartBtn.setPreferredSize(new Dimension(34, 34));
        heartBtn.setFocusPainted(false);
        heartBtn.setBorder(new LineBorder(UITheme.BORDER_LIGHT, 1, true));
        heartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heartBtn.setToolTipText("Save to wishlist");

        // Asynchronously check initial status
        AsyncOperation.<Boolean>run(
                () -> wishlistService.isProductInWishlist(currentUser.getId(), product.getId()),
                saved -> {
                    boolean val = Boolean.TRUE.equals(saved);
                    isSaved.set(val);
                    heartBtn.setText(val ? "♥" : "♡");
                    heartBtn.setForeground(val ? UITheme.DANGER_RED : UITheme.TEXT_SECONDARY);
                    heartBtn.setToolTipText(val ? "Remove from wishlist" : "Save to wishlist");
                },
                ignored -> {}
        );

        heartBtn.addActionListener(e -> {
            heartBtn.setEnabled(false);
            boolean currentlySaved = isSaved.get();
            if (currentlySaved) {
                AsyncOperation.run(
                        () -> {
                            wishlistService.removeByProduct(currentUser.getId(), product.getId());
                            return null;
                        },
                        res -> {
                            isSaved.set(false);
                            heartBtn.setText("♡");
                            heartBtn.setForeground(UITheme.TEXT_SECONDARY);
                            heartBtn.setToolTipText("Save to wishlist");
                            heartBtn.setEnabled(true);
                            DialogUtil.showSuccess(this, "Removed from wishlist.");
                            if (onActionCallback != null) onActionCallback.run();
                        },
                        ex -> {
                            heartBtn.setEnabled(true);
                            DialogUtil.showError(this, ex.getMessage());
                        }
                );
            } else {
                AsyncOperation.run(
                        () -> {
                            wishlistService.addToWishlist(currentUser.getId(), product.getId());
                            return null;
                        },
                        res -> {
                            isSaved.set(true);
                            heartBtn.setText("♥");
                            heartBtn.setForeground(UITheme.DANGER_RED);
                            heartBtn.setToolTipText("Remove from wishlist");
                            heartBtn.setEnabled(true);
                            DialogUtil.showSuccess(this, "Added to wishlist!");
                            if (onActionCallback != null) onActionCallback.run();
                        },
                        ex -> {
                            heartBtn.setEnabled(true);
                            DialogUtil.showError(this, ex.getMessage());
                        }
                );
            }
        });

        return heartBtn;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
