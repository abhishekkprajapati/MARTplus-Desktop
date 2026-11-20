package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.model.WishlistItem;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modern Wishlist Panel displaying saved product cards with thumbnail images,
 * stock status badges, move-to-cart action, and a dedicated empty state.
 * Reference: frontend/wishlist.html
 */
public class WishlistPanel extends JPanel {

    private final WishlistService wishlistService;
    private final CartService cartService;
    private final User currentUser;
    private Consumer<String> navigationCallback = null;

    private JPanel gridContainer;
    private JScrollPane scrollPane;
    private JLabel countLabel;
    private List<WishlistItem> items = new ArrayList<>();

    public WishlistPanel(WishlistService wishlistService, CartService cartService, User currentUser) {
        this.wishlistService = wishlistService;
        this.cartService = cartService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopHeader(), BorderLayout.NORTH);
        add(createScrollPane(), BorderLayout.CENTER);

        loadWishlist();
    }

    public void setNavigationCallback(Consumer<String> callback) {
        this.navigationCallback = callback;
    }

    private JPanel createTopHeader() {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(Color.WHITE);
        JLabel title = new JLabel("My Saved Wishlist");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        left.add(title);

        countLabel = new JLabel("0 items");
        countLabel.setFont(UITheme.FONT_BODY);
        countLabel.setForeground(UITheme.TEXT_SECONDARY);
        left.add(countLabel);
        card.add(left, BorderLayout.WEST);

        JButton refreshBtn = UITheme.createSecondaryButton("⟳ Refresh");
        refreshBtn.addActionListener(e -> loadWishlist());
        card.add(refreshBtn, BorderLayout.EAST);

        return card;
    }

    private JScrollPane createScrollPane() {
        gridContainer = new JPanel();
        gridContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        gridContainer.setBackground(UITheme.BG_LIGHT);

        scrollPane = new JScrollPane(gridContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }

    public void loadWishlist() {
        AsyncOperation.run(
                () -> wishlistService.getWishlistItems(currentUser.getId()),
                list -> {
                    items = list;
                    renderWishlist(list);
                },
                ex -> DialogUtil.showError(this, "Failed to load wishlist: " + ex.getMessage())
        );
    }

    private void renderWishlist(List<WishlistItem> list) {
        gridContainer.removeAll();

        if (list.isEmpty()) {
            countLabel.setText("0 items");
            JButton browseBtn = UITheme.createPrimaryButton("Browse Products");
            browseBtn.addActionListener(e -> {
                if (navigationCallback != null) {
                    navigationCallback.accept("PRODUCTS");
                }
            });

            JPanel emptyState = UITheme.createEmptyState(
                    "♡",
                    "Your Wishlist is Empty",
                    "Save items you want to keep track of by clicking the heart button on any product.",
                    browseBtn
            );
            gridContainer.setLayout(new BorderLayout());
            gridContainer.add(emptyState, BorderLayout.CENTER);
        } else {
            countLabel.setText(list.size() + " item" + (list.size() == 1 ? "" : "s"));
            gridContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));

            for (WishlistItem item : list) {
                gridContainer.add(createWishlistCard(item));
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JPanel createWishlistCard(WishlistItem item) {
        Product p = item.getProduct();
        String name = p != null ? p.getName() : "Product #" + item.getProductId();
        String cat = p != null && p.getCategory() != null ? p.getCategory().toUpperCase() : "GENERAL";
        BigDecimal price = p != null && p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
        int stock = p != null && p.getStock() != null ? p.getStock() : 0;
        boolean inStock = p != null && p.isAvailable();
        String imgUrl = p != null ? p.getImageUrl() : null;

        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(250, 330));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Thumbnail
        JPanel imgBox = new JPanel(new BorderLayout());
        imgBox.setBackground(new Color(0xF8, 0xF9, 0xFC));
        imgBox.setPreferredSize(new Dimension(226, 130));
        imgBox.setBorder(new LineBorder(UITheme.BORDER_SUBTLE, 1, true));

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setIcon(ImageUtil.createPlaceholder(224, 128, "♡"));
        ImageUtil.loadProductImageAsync(imgUrl, 224, 128, imgLabel::setIcon);
        imgBox.add(imgLabel, BorderLayout.CENTER);
        card.add(imgBox, BorderLayout.NORTH);

        // Details
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(10, 0, 8, 0));

        JLabel catLbl = new JLabel(cat);
        catLbl.setFont(UITheme.FONT_TINY);
        catLbl.setForeground(UITheme.TEXT_MUTED);

        JLabel titleLbl = new JLabel("<html><body style='width: 210px;'>" + escapeHtml(name) + "</body></html>");
        titleLbl.setFont(UITheme.FONT_BODY_BOLD);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JPanel priceStock = new JPanel(new BorderLayout());
        priceStock.setBackground(Color.WHITE);
        JLabel priceLbl = new JLabel("₹" + String.format("%.2f", price));
        priceLbl.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 16));
        priceLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel stockBadge = inStock
                ? UITheme.createBadge(stock + " in stock", new Color(0xDC, 0xFC, 0xE7), UITheme.SUCCESS_GREEN)
                : UITheme.createBadge("Out of stock", new Color(0xFE, 0xE2, 0xE2), UITheme.DANGER_RED);

        priceStock.add(priceLbl, BorderLayout.WEST);
        priceStock.add(stockBadge, BorderLayout.EAST);

        details.add(catLbl);
        details.add(Box.createRigidArea(new Dimension(0, 3)));
        details.add(titleLbl);
        details.add(Box.createRigidArea(new Dimension(0, 6)));
        details.add(priceStock);
        card.add(details, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setBackground(Color.WHITE);

        JButton removeBtn = UITheme.createDangerButton("Remove");
        removeBtn.setFont(UITheme.FONT_SMALL_BOLD);
        removeBtn.addActionListener(e -> handleRemove(item));
        actions.add(removeBtn);

        JButton moveBtn = UITheme.createPrimaryButton("🛒 Cart");
        moveBtn.setFont(UITheme.FONT_SMALL_BOLD);
        moveBtn.setEnabled(inStock);
        moveBtn.addActionListener(e -> handleMoveToCart(item));
        actions.add(moveBtn);

        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void handleMoveToCart(WishlistItem item) {
        Product p = item.getProduct();
        if (p == null || !p.isAvailable()) {
            DialogUtil.showWarning(this, "This product is currently out of stock.");
            return;
        }

        AsyncOperation.run(
                () -> {
                    cartService.addToCart(currentUser.getId(), item.getProductId(), 1);
                    wishlistService.removeFromWishlist(item.getId());
                    return null;
                },
                res -> {
                    DialogUtil.showSuccess(this, "Moved '" + p.getName() + "' to your cart!");
                    loadWishlist();
                },
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }

    private void handleRemove(WishlistItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : "product";
        if (DialogUtil.confirm(this, "Remove '" + name + "' from your wishlist?", "Remove Wishlist Item")) {
            AsyncOperation.run(
                    () -> {
                        wishlistService.removeFromWishlist(item.getId());
                        return null;
                    },
                    res -> loadWishlist(),
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
