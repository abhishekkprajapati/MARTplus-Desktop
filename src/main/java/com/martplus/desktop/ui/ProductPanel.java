package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.model.Product;
import com.martplus.desktop.model.User;
import com.martplus.desktop.model.WishlistItem;
import com.martplus.desktop.service.CartService;
import com.martplus.desktop.service.ProductService;
import com.martplus.desktop.service.WishlistService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.ImageUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modern E-Commerce Product Browsing Panel featuring an asynchronous,
 * responsive Product Card Grid matching the MARTplus website.
 * Reference: frontend/products.html & frontend/css/style.css
 */
public class ProductPanel extends JPanel {

    private final ProductService productService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final User currentUser;

    private final Set<Long> wishlistedProductIds = ConcurrentHashMap.newKeySet();
    private Runnable onWishlistChangedCallback;

    private JTextField searchField;
    private JComboBox<String> categoryCombo;
    private JLabel resultsCountLabel;
    private JPanel gridContainer;
    private JScrollPane scrollPane;
    private List<Product> currentProductList = new ArrayList<>();

    public ProductPanel(ProductService productService, CartService cartService,
                        WishlistService wishlistService, User currentUser) {
        this.productService = productService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.currentUser = currentUser;

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BG_LIGHT);

        add(createTopFilterPanel(), BorderLayout.NORTH);
        add(createGridScrollPane(), BorderLayout.CENTER);

        loadCategories();
        loadProducts();
    }

    private JPanel createTopFilterPanel() {
        JPanel filterCard = UITheme.createCard();
        filterCard.setLayout(new BorderLayout(16, 0));

        JPanel inputsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        inputsPanel.setBackground(Color.WHITE);

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        inputsPanel.add(searchIcon);

        searchField = new JTextField(18);
        searchField.setFont(UITheme.FONT_BODY);
        searchField.putClientProperty("JTextField.placeholderText", "Search products by name...");
        searchField.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchField.addActionListener(e -> loadProducts());
        inputsPanel.add(searchField);

        inputsPanel.add(new JLabel("Category:"));
        categoryCombo = new JComboBox<>(new String[]{"All"});
        categoryCombo.setFont(UITheme.FONT_BODY);
        categoryCombo.setBackground(Color.WHITE);
        categoryCombo.setPreferredSize(new Dimension(160, 36));
        categoryCombo.addActionListener(e -> loadProducts());
        inputsPanel.add(categoryCombo);

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> loadProducts());
        inputsPanel.add(searchBtn);

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            if (categoryCombo.getItemCount() > 0) {
                categoryCombo.setSelectedIndex(0);
            }
            loadProducts();
        });
        inputsPanel.add(resetBtn);

        filterCard.add(inputsPanel, BorderLayout.WEST);

        resultsCountLabel = new JLabel("Loading products...");
        resultsCountLabel.setFont(UITheme.FONT_SMALL_BOLD);
        resultsCountLabel.setForeground(UITheme.TEXT_SECONDARY);
        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rightInfo.setBackground(Color.WHITE);
        rightInfo.add(resultsCountLabel);
        filterCard.add(rightInfo, BorderLayout.EAST);

        return filterCard;
    }

    private JScrollPane createGridScrollPane() {
        gridContainer = new JPanel();
        gridContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        gridContainer.setBackground(UITheme.BG_LIGHT);

        scrollPane = new JScrollPane(gridContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Adjust layout dynamically on window resize
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutGrid();
            }
        });

        return scrollPane;
    }

    public void loadCategories() {
        AsyncOperation.run(
                productService::getUniqueCategoryNames,
                categories -> {
                    categoryCombo.removeAllItems();
                    for (String cat : categories) {
                        categoryCombo.addItem(cat);
                    }
                },
                ex -> System.err.println("Failed to load categories: " + ex.getMessage())
        );
    }

    public void setOnWishlistChangedCallback(Runnable onWishlistChangedCallback) {
        this.onWishlistChangedCallback = onWishlistChangedCallback;
    }

    public void loadProducts() {
        String keyword = searchField.getText().trim();
        String selectedCategory = (String) categoryCombo.getSelectedItem();
        resultsCountLabel.setText("Searching catalog...");

        AsyncOperation.run(
                () -> {
                    List<Product> products = productService.searchProducts(keyword, selectedCategory);
                    Set<Long> savedIds = new HashSet<>();
                    if (currentUser != null && currentUser.getId() != null) {
                        try {
                            List<WishlistItem> wishlist = wishlistService.getWishlistItems(currentUser.getId());
                            for (WishlistItem w : wishlist) {
                                savedIds.add(w.getProductId());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    return new Object[]{products, savedIds};
                },
                results -> {
                    List<Product> products = (List<Product>) ((Object[]) results)[0];
                    Set<Long> savedIds = (Set<Long>) ((Object[]) results)[1];
                    wishlistedProductIds.clear();
                    wishlistedProductIds.addAll(savedIds);
                    currentProductList = products;
                    renderProductGrid(products);
                },
                ex -> {
                    resultsCountLabel.setText("Error loading catalog");
                    DialogUtil.showError(this, "Failed to load products: " + ex.getMessage());
                }
        );
    }

    public void refreshWishlistStatus() {
        if (currentUser == null || currentUser.getId() == null) return;
        AsyncOperation.run(
                () -> wishlistService.getWishlistItems(currentUser.getId()),
                list -> {
                    Set<Long> updated = new HashSet<>();
                    for (WishlistItem w : list) {
                        updated.add(w.getProductId());
                    }
                    if (!wishlistedProductIds.equals(updated)) {
                        wishlistedProductIds.clear();
                        wishlistedProductIds.addAll(updated);
                        renderProductGrid(currentProductList);
                    }
                },
                ex -> System.err.println("Failed to refresh wishlist status: " + ex.getMessage())
        );
    }

    private void renderProductGrid(List<Product> products) {
        gridContainer.removeAll();

        if (products.isEmpty()) {
            resultsCountLabel.setText("0 products found");
            JButton resetAction = UITheme.createPrimaryButton("Reset Filters");
            resetAction.addActionListener(e -> {
                searchField.setText("");
                if (categoryCombo.getItemCount() > 0) categoryCombo.setSelectedIndex(0);
                loadProducts();
            });

            JPanel emptyState = UITheme.createEmptyState(
                    "🔍",
                    "No Products Found",
                    "We couldn't find any products matching your search criteria.",
                    resetAction
            );
            gridContainer.setLayout(new BorderLayout());
            gridContainer.add(emptyState, BorderLayout.CENTER);
        } else {
            resultsCountLabel.setText("Showing " + products.size() + " product" + (products.size() == 1 ? "" : "s"));
            gridContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));

            for (Product product : products) {
                gridContainer.add(createProductCard(product));
            }
        }

        relayoutGrid();
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private void relayoutGrid() {
        int width = scrollPane.getViewport().getWidth();
        if (width > 0 && !currentProductList.isEmpty()) {
            int cardWidthWithGap = 260 + 20;
            int cols = Math.max(1, (width - 10) / cardWidthWithGap);
            int rows = (int) Math.ceil((double) currentProductList.size() / cols);
            int prefHeight = Math.max(500, rows * 405 + 40);
            gridContainer.setPreferredSize(new Dimension(width - 20, prefHeight));
        }
    }

    /**
     * Builds an individual modern Product Card matching the website's .product-card CSS.
     */
    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(260, 385));
        card.setBackground(Color.WHITE);

        LineBorder defaultBorder = new LineBorder(UITheme.BORDER_LIGHT, 1, true);
        LineBorder hoverBorder = new LineBorder(UITheme.PRIMARY, 1, true);
        card.setBorder(new CompoundBorder(defaultBorder, new EmptyBorder(12, 12, 12, 12)));

        // Hover Effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(hoverBorder, new EmptyBorder(12, 12, 12, 12)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(defaultBorder, new EmptyBorder(12, 12, 12, 12)));
            }
        });

        // 1. Top Image Container (Overlay with availability badge & Wishlist heart)
        JPanel imageContainer = new JPanel(new BorderLayout());
        imageContainer.setBackground(new Color(0xF8, 0xF9, 0xFC));
        imageContainer.setPreferredSize(new Dimension(236, 160));
        imageContainer.setBorder(new LineBorder(UITheme.BORDER_SUBTLE, 1, true));

        // Asynchronous image label
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setIcon(ImageUtil.createPlaceholder(234, 158, "📦"));

        // Asynchronously load the product's image with aspect-ratio preservation without blocking EDT
        ImageUtil.loadProductImageAsync(product.getImageUrl(), 234, 158, imageLabel::setIcon);

        // Header overlay over image: Availability Badge (Left) and Wishlist Heart (Right)
        JPanel overlayHeader = new JPanel(new BorderLayout());
        overlayHeader.setOpaque(false);
        overlayHeader.setBorder(new EmptyBorder(8, 8, 0, 8));

        boolean inStock = product.isAvailable() && product.getStock() > 0;
        JLabel badge = inStock
                ? UITheme.createBadge("In Stock", new Color(0xDC, 0xFC, 0xE7), UITheme.SUCCESS_GREEN)
                : UITheme.createBadge("Out of Stock", new Color(0xFE, 0xE2, 0xE2), UITheme.DANGER_RED);
        overlayHeader.add(badge, BorderLayout.WEST);

        // Heart Wishlist Button
        boolean isSaved = wishlistedProductIds.contains(product.getId());
        JButton heartBtn = new JButton(isSaved ? "♥" : "♡");
        heartBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 16));
        heartBtn.setForeground(isSaved ? UITheme.DANGER_RED : UITheme.TEXT_SECONDARY);
        heartBtn.setBackground(new Color(255, 255, 255, 225));
        heartBtn.setPreferredSize(new Dimension(32, 32));
        heartBtn.setFocusPainted(false);
        heartBtn.setBorder(new LineBorder(UITheme.BORDER_LIGHT, 1, true));
        heartBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heartBtn.setOpaque(true);
        heartBtn.setToolTipText(isSaved ? "Remove from wishlist" : "Save to wishlist");

        heartBtn.addActionListener(e -> {
            heartBtn.setEnabled(false);
            boolean currentlySaved = wishlistedProductIds.contains(product.getId());
            if (currentlySaved) {
                AsyncOperation.run(
                        () -> {
                            wishlistService.removeByProduct(currentUser.getId(), product.getId());
                            return null;
                        },
                        res -> {
                            wishlistedProductIds.remove(product.getId());
                            heartBtn.setText("♡");
                            heartBtn.setForeground(UITheme.TEXT_SECONDARY);
                            heartBtn.setToolTipText("Save to wishlist");
                            heartBtn.setEnabled(true);
                            DialogUtil.showSuccess(this, "Removed '" + product.getName() + "' from your wishlist.");
                            if (onWishlistChangedCallback != null) {
                                onWishlistChangedCallback.run();
                            }
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
                            wishlistedProductIds.add(product.getId());
                            heartBtn.setText("♥");
                            heartBtn.setForeground(UITheme.DANGER_RED);
                            heartBtn.setToolTipText("Remove from wishlist");
                            heartBtn.setEnabled(true);
                            DialogUtil.showSuccess(this, "Added '" + product.getName() + "' to your wishlist!");
                            if (onWishlistChangedCallback != null) {
                                onWishlistChangedCallback.run();
                            }
                        },
                        ex -> {
                            heartBtn.setEnabled(true);
                            DialogUtil.showError(this, ex.getMessage());
                        }
                );
            }
        });

        overlayHeader.add(heartBtn, BorderLayout.EAST);

        imageContainer.add(overlayHeader, BorderLayout.NORTH);
        imageContainer.add(imageLabel, BorderLayout.CENTER);
        card.add(imageContainer, BorderLayout.NORTH);

        // 2. Middle Details Panel
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(new EmptyBorder(10, 0, 8, 0));

        // Category
        String catName = (product.getCategory() != null ? product.getCategory() : "GENERAL").toUpperCase();
        JLabel catLabel = new JLabel(catName);
        catLabel.setFont(UITheme.FONT_TINY);
        catLabel.setForeground(UITheme.TEXT_MUTED);
        catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(catLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 3)));

        // Title
        JLabel titleLabel = new JLabel("<html><body style='width: 220px; line-height: 1.2; text-overflow: ellipsis;'>"
                + escapeHtml(product.getName()) + "</body></html>");
        titleLabel.setFont(UITheme.FONT_BODY_BOLD);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleLabel.setPreferredSize(new Dimension(236, 36));
        titleLabel.setMaximumSize(new Dimension(236, 36));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(titleLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        // Seller
        String sellerName = product.getSellerName() != null ? product.getSellerName() : "Seller #" + product.getSellerId();
        JLabel sellerLabel = new JLabel("Sold by: " + sellerName);
        sellerLabel.setFont(UITheme.FONT_TINY);
        sellerLabel.setForeground(UITheme.TEXT_SECONDARY);
        sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(sellerLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // Price and Stock row
        JPanel priceRow = new JPanel(new BorderLayout());
        priceRow.setBackground(Color.WHITE);
        priceRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel("₹" + String.format("%.2f", product.getPrice()));
        priceLabel.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 17));
        priceLabel.setForeground(UITheme.TEXT_PRIMARY);
        priceRow.add(priceLabel, BorderLayout.WEST);

        JLabel stockLabel = new JLabel(product.getStock() + " left");
        stockLabel.setFont(UITheme.FONT_TINY);
        stockLabel.setForeground(product.getStock() <= 5 ? UITheme.WARNING_AMBER : UITheme.SUCCESS_GREEN);
        priceRow.add(stockLabel, BorderLayout.EAST);

        detailsPanel.add(priceRow);
        card.add(detailsPanel, BorderLayout.CENTER);

        // 3. Bottom Actions: Add to Cart & Details
        JPanel actionsPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        actionsPanel.setBackground(Color.WHITE);

        JButton detailsBtn = UITheme.createSecondaryButton("Details");
        detailsBtn.setFont(UITheme.FONT_SMALL_BOLD);
        detailsBtn.addActionListener(e -> showProductDetails(product));
        actionsPanel.add(detailsBtn);

        JButton cartBtn = UITheme.createPrimaryButton("🛒 Cart");
        cartBtn.setFont(UITheme.FONT_SMALL_BOLD);
        cartBtn.setEnabled(inStock);
        if (!inStock) {
            cartBtn.setText("Out of Stock");
        }
        cartBtn.addActionListener(e -> handleAddToCart(product));
        actionsPanel.add(cartBtn);

        card.add(actionsPanel, BorderLayout.SOUTH);
        return card;
    }

    private void handleAddToCart(Product product) {
        if (!product.isAvailable() || product.getStock() <= 0) {
            DialogUtil.showWarning(this, "This product is currently out of stock.");
            return;
        }

        if (currentUser == null || currentUser.getId() == null) {
            DialogUtil.showWarning(this, "Please sign in with a Buyer account to add items to your cart.");
            return;
        }

        AsyncOperation.run(
                () -> {
                    cartService.addToCart(currentUser.getId(), product.getId(), 1);
                    return null;
                },
                res -> DialogUtil.showSuccess(this, "Added 1 item of '" + product.getName() + "' to your cart!"),
                ex -> DialogUtil.showError(this, ex.getMessage())
        );
    }

    private void showProductDetails(Product p) {
        ProductDetailsDialog dialog = new ProductDetailsDialog(
                SwingUtilities.getWindowAncestor(this),
                p,
                cartService,
                wishlistService,
                currentUser,
                this::loadProducts
        );
        dialog.setVisible(true);
    }

    public static String getSanitizedDescription(Product p) {
        if (p == null || p.getDescription() == null || p.getDescription().isBlank()) {
            return "No detailed description provided for this product.";
        }
        String trimmed = p.getDescription().trim();
        int len = trimmed.length();
        // Check for exact back-to-back duplicate text (e.g. repeated description in database)
        if (len >= 4 && len % 2 == 0) {
            String half1 = trimmed.substring(0, len / 2).trim();
            String half2 = trimmed.substring(len / 2).trim();
            if (half1.equalsIgnoreCase(half2)) {
                return half1;
            }
        }
        // Check with small whitespace offset around midpoint
        int mid = len / 2;
        for (int offset = -2; offset <= 2; offset++) {
            int split = mid + offset;
            if (split > 2 && split < len - 2) {
                String s1 = trimmed.substring(0, split).trim();
                String s2 = trimmed.substring(split).trim();
                if (!s1.isEmpty() && s1.equalsIgnoreCase(s2)) {
                    return s1;
                }
            }
        }
        return trimmed;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
