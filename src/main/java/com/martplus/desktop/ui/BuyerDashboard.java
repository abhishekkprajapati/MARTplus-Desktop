package com.martplus.desktop.ui;

import com.martplus.desktop.concurrency.AsyncOperation;
import com.martplus.desktop.dao.AddressDao;
import com.martplus.desktop.dao.JdbcAddressDao;
import com.martplus.desktop.dao.JdbcCartDao;
import com.martplus.desktop.dao.JdbcCategoryDao;
import com.martplus.desktop.dao.JdbcInventoryDao;
import com.martplus.desktop.dao.JdbcOrderDao;
import com.martplus.desktop.dao.JdbcProductDao;
import com.martplus.desktop.dao.JdbcUserDao;
import com.martplus.desktop.dao.JdbcWishlistDao;
import com.martplus.desktop.model.Address;
import com.martplus.desktop.model.User;
import com.martplus.desktop.service.CartService;
import com.martplus.desktop.service.OrderService;
import com.martplus.desktop.service.ProductService;
import com.martplus.desktop.service.UserService;
import com.martplus.desktop.service.WishlistService;
import com.martplus.desktop.util.DialogUtil;
import com.martplus.desktop.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * Concrete Buyer Dashboard extending BaseDashboard.
 * Faithfully styled to match the official MARTplus website Buyer interface:
 * frontend/buyer/dashboard.html, frontend/buyer/addresses.html, and frontend/buyer/profile.html.
 */
public class BuyerDashboard extends BaseDashboard {

    private ProductPanel productPanel;
    private CartPanel cartPanel;
    private WishlistPanel wishlistPanel;
    private OrderPanel orderPanel;
    private JPanel addressPanel;
    private JPanel profilePanel;

    private final ProductService productService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final OrderService orderService;
    private final UserService userService;
    private final AddressDao addressDao;

    // Address card container for dynamic rendering
    private JPanel addressCardsContainer;

    public BuyerDashboard(MainWindow parentWindow, User currentUser) {
        super(parentWindow, currentUser);

        var productDao = new JdbcProductDao();
        var categoryDao = new JdbcCategoryDao();
        var cartDao = new JdbcCartDao();
        var wishlistDao = new JdbcWishlistDao();
        var orderDao = new JdbcOrderDao();
        var userDao = new JdbcUserDao();
        var inventoryDao = new JdbcInventoryDao();
        this.addressDao = new JdbcAddressDao();

        this.productService = new ProductService(productDao, categoryDao, null);
        this.cartService = new CartService(cartDao, productDao);
        this.wishlistService = new WishlistService(wishlistDao);
        this.orderService = new OrderService(orderDao, cartDao, productDao, addressDao, inventoryDao, userDao, null);
        this.userService = new UserService(userDao, null);

        initView();
    }

    @Override
    public void initView() {
        productPanel = new ProductPanel(productService, cartService, wishlistService, currentUser);
        productPanel.setOnWishlistChangedCallback(() -> {
            if (wishlistPanel != null) {
                wishlistPanel.loadWishlist();
            }
        });
        cartPanel = new CartPanel(cartService, orderService, addressDao, currentUser, () -> {
            if (orderPanel != null) {
                orderPanel.loadOrders();
            }
        });
        // Hook navigation callback so "Browse Products" in empty cart switches to products card
        cartPanel.setNavigationCallback(this::selectNavCard);

        wishlistPanel = new WishlistPanel(wishlistService, cartService, currentUser);
        // Hook navigation callback so "Browse Products" in empty wishlist switches to products card
        wishlistPanel.setNavigationCallback(this::selectNavCard);

        orderPanel = new OrderPanel(orderService, currentUser, false, false);
        addressPanel = createAddressManagementPanel();
        profilePanel = createProfilePanel();

        mainContentContainer.add(productPanel, "PRODUCTS");
        mainContentContainer.add(cartPanel, "CART");
        mainContentContainer.add(wishlistPanel, "WISHLIST");
        mainContentContainer.add(orderPanel, "ORDERS");
        mainContentContainer.add(addressPanel, "ADDRESSES");
        mainContentContainer.add(profilePanel, "PROFILE");

        selectNavCard("PRODUCTS");
    }

    @Override
    protected void buildNavigation(JPanel navPanel) {
        addNavButton(navPanel, "PRODUCTS", "🛍️  Browse Products");
        addNavButton(navPanel, "CART", "🛒  Shopping Cart");
        addNavButton(navPanel, "WISHLIST", "♡  My Wishlist");
        addNavButton(navPanel, "ORDERS", "📦  My Orders");
        addNavButton(navPanel, "ADDRESSES", "📍  Saved Addresses");
        addNavButton(navPanel, "PROFILE", "👤  My Profile");
    }

    @Override
    public void selectNavCard(String cardName) {
        super.selectNavCard(cardName);
        if ("WISHLIST".equals(cardName) && wishlistPanel != null) {
            wishlistPanel.loadWishlist();
        } else if ("CART".equals(cardName) && cartPanel != null) {
            cartPanel.loadCart();
        } else if ("ORDERS".equals(cardName) && orderPanel != null) {
            orderPanel.loadOrders();
        } else if ("ADDRESSES".equals(cardName)) {
            loadAddressesAsync();
        } else if ("PRODUCTS".equals(cardName) && productPanel != null) {
            productPanel.refreshWishlistStatus();
        }
    }

    @Override
    public void refreshData() {
        updateStatus("Refreshing buyer data from PostgreSQL...");
        if (productPanel != null) productPanel.loadProducts();
        if (cartPanel != null) cartPanel.loadCart();
        if (wishlistPanel != null) wishlistPanel.loadWishlist();
        if (orderPanel != null) orderPanel.loadOrders();
        loadAddressesAsync();
        updateStatus("Buyer data synchronized");
    }

    @Override
    public String getRoleName() {
        return "BUYER";
    }

    // =========================================================================
    // SAVED ADDRESSES PANEL (Matching frontend/buyer/addresses.html)
    // =========================================================================

    private JPanel createAddressManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(UITheme.BG_LIGHT);

        // Header Section with Title, Subtitle, and "+ Add New Address" Button
        JPanel headerCard = UITheme.createCard();
        headerCard.setLayout(new BorderLayout());

        JPanel textSide = new JPanel();
        textSide.setOpaque(false);
        textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel("Your Delivery Addresses");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subLbl = new JLabel("Manage your delivery destinations for instant one-click checkout.");
        subLbl.setFont(UITheme.FONT_BODY);
        subLbl.setForeground(UITheme.TEXT_SECONDARY);

        textSide.add(titleLbl);
        textSide.add(Box.createRigidArea(new Dimension(0, 4)));
        textSide.add(subLbl);

        JButton addBtn = UITheme.createPrimaryButton("+ Add New Address");
        addBtn.addActionListener(e -> showAddAddressDialog());

        headerCard.add(textSide, BorderLayout.WEST);
        headerCard.add(addBtn, BorderLayout.EAST);
        panel.add(headerCard, BorderLayout.NORTH);

        // Dynamic Address Cards Container (Wrapped in JScrollPane)
        addressCardsContainer = new JPanel();
        addressCardsContainer.setOpaque(false);
        addressCardsContainer.setLayout(new BoxLayout(addressCardsContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(addressCardsContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UITheme.BG_LIGHT);
        panel.add(scroll, BorderLayout.CENTER);

        loadAddressesAsync();

        return panel;
    }

    private void loadAddressesAsync() {
        if (addressCardsContainer == null) return;

        AsyncOperation.run(
                () -> addressDao.findByBuyerId(currentUser.getId()),
                this::renderAddressCards,
                ex -> System.err.println("Failed to load addresses: " + ex.getMessage())
        );
    }

    private void renderAddressCards(List<Address> addresses) {
        addressCardsContainer.removeAll();

        if (addresses == null || addresses.isEmpty()) {
            JPanel empty = UITheme.createEmptyState(
                    "📍",
                    "No Saved Addresses",
                    "You haven't saved any delivery addresses yet. Add one to make checkout quick and easy.",
                    "+ Add Delivery Address",
                    this::showAddAddressDialog
            );
            addressCardsContainer.add(empty);
        } else {
            // Render a 2-column or grid-like layout of address cards
            JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
            grid.setOpaque(false);

            for (Address a : addresses) {
                grid.add(createAddressCard(a));
            }

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(grid, BorderLayout.NORTH);
            addressCardsContainer.add(wrapper);
        }

        addressCardsContainer.revalidate();
        addressCardsContainer.repaint();
    }

    private JPanel createAddressCard(Address a) {
        JPanel card = UITheme.createCard();
        card.setLayout(new BorderLayout(0, 12));

        // Top Row: Recipient Name + Phone + Optional DEFAULT pill
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel leftMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftMeta.setOpaque(false);

        JLabel iconLbl = new JLabel("📍");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        leftMeta.add(iconLbl);

        JLabel nameLbl = new JLabel(a.getFullName());
        nameLbl.setFont(UITheme.FONT_SUBTITLE);
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);
        leftMeta.add(nameLbl);

        topRow.add(leftMeta, BorderLayout.WEST);

        if (a.isDefaultAddress()) {
            JLabel defBadge = UITheme.createPillBadge("DEFAULT", UITheme.PRIMARY_LIGHT, UITheme.PRIMARY);
            topRow.add(defBadge, BorderLayout.EAST);
        }

        card.add(topRow, BorderLayout.NORTH);

        // Center: Formatted Street, City, State, PIN, Phone
        JPanel detailsPanel = new JPanel();
        detailsPanel.setOpaque(false);
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        JLabel line1 = new JLabel(a.getAddressLine());
        line1.setFont(UITheme.FONT_BODY);
        line1.setForeground(UITheme.TEXT_SECONDARY);

        JLabel line2 = new JLabel(a.getCity() + ", " + a.getState() + " — " + a.getPincode());
        line2.setFont(UITheme.FONT_BODY);
        line2.setForeground(UITheme.TEXT_SECONDARY);

        JLabel phoneLbl = new JLabel("📞 " + a.getPhone());
        phoneLbl.setFont(UITheme.FONT_SMALL_BOLD);
        phoneLbl.setForeground(UITheme.TEXT_PRIMARY);

        detailsPanel.add(line1);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        detailsPanel.add(line2);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        detailsPanel.add(phoneLbl);

        card.add(detailsPanel, BorderLayout.CENTER);

        // Bottom Actions: Delete Button + Optional "Set as Default" button
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setOpaque(false);

        if (!a.isDefaultAddress()) {
            JButton makeDefaultBtn = UITheme.createSecondaryButton("Set Default");
            makeDefaultBtn.setFont(UITheme.FONT_SMALL);
            makeDefaultBtn.addActionListener(e -> {
                AsyncOperation.run(
                        () -> {
                            addressDao.setDefaultAddress(a.getId(), currentUser.getId());
                            return null;
                        },
                        res -> loadAddressesAsync(),
                        ex -> DialogUtil.showError(this, ex.getMessage())
                );
            });
            actionRow.add(makeDefaultBtn);
        }

        JButton deleteBtn = UITheme.createDangerButton("Delete");
        deleteBtn.setFont(UITheme.FONT_SMALL);
        deleteBtn.addActionListener(e -> {
            if (DialogUtil.confirm(this, "Are you sure you want to remove this delivery address?", "Delete Address")) {
                AsyncOperation.run(
                        () -> {
                            addressDao.deleteById(a.getId());
                            return null;
                        },
                        res -> loadAddressesAsync(),
                        ex -> DialogUtil.showError(this, ex.getMessage())
                );
            }
        });
        actionRow.add(deleteBtn);

        card.add(actionRow, BorderLayout.SOUTH);
        return card;
    }

    private void showAddAddressDialog() {
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField lineField = new JTextField();
        JTextField cityField = new JTextField();
        JTextField stateField = new JTextField();
        JTextField pinField = new JTextField();
        JCheckBox defaultCheck = new JCheckBox("Set as default delivery address");
        defaultCheck.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.add(new JLabel("Full Name:")); form.add(nameField);
        form.add(new JLabel("Phone (10 digits):")); form.add(phoneField);
        form.add(new JLabel("Address Line:")); form.add(lineField);
        form.add(new JLabel("City:")); form.add(cityField);
        form.add(new JLabel("State:")); form.add(stateField);
        form.add(new JLabel("PIN Code:")); form.add(pinField);
        form.add(new JLabel("Default:")); form.add(defaultCheck);

        int opt = JOptionPane.showConfirmDialog(this, form, "Add New Delivery Address",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (opt == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String line = lineField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String pin = pinField.getText().trim();

            if (name.isEmpty() || phone.isEmpty() || line.isEmpty() || city.isEmpty() || state.isEmpty() || pin.isEmpty()) {
                DialogUtil.showWarning(this, "Please fill in all address fields.");
                return;
            }

            Address newAddr = new Address(null, currentUser.getId(), name, phone, line, city, state, pin, defaultCheck.isSelected(), null, null);
            AsyncOperation.run(
                    () -> addressDao.save(newAddr),
                    saved -> {
                        DialogUtil.showSuccess(this, "Delivery address saved successfully!");
                        loadAddressesAsync();
                    },
                    ex -> DialogUtil.showError(this, "Failed to save address: " + ex.getMessage())
            );
        }
    }

    // =========================================================================
    // MY PROFILE PANEL (Matching frontend/buyer/profile.html)
    // =========================================================================

    private JPanel createProfilePanel() {
        JPanel container = new JPanel(new BorderLayout(0, 20));
        container.setBackground(UITheme.BG_LIGHT);

        // 1. Profile Overview Header Card with Large Avatar Circle
        JPanel overviewCard = UITheme.createCard();
        overviewCard.setLayout(new BorderLayout(20, 0));

        // 82px Avatar Circle
        JLabel avatarCircle = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.PRIMARY_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(UITheme.PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String initial = currentUser.getFirstName() != null && !currentUser.getFirstName().isEmpty()
                        ? currentUser.getFirstName().substring(0, 1).toUpperCase() : "U";
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatarCircle.setPreferredSize(new Dimension(82, 82));
        overviewCard.add(avatarCircle, BorderLayout.WEST);

        // Info Block: Full Name, Email, Role Pill
        JPanel infoBlock = new JPanel();
        infoBlock.setOpaque(false);
        infoBlock.setLayout(new BoxLayout(infoBlock, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel(currentUser.getFullName());
        nameLbl.setFont(UITheme.FONT_TITLE);
        nameLbl.setForeground(UITheme.TEXT_PRIMARY);

        JLabel emailLbl = new JLabel(currentUser.getEmail());
        emailLbl.setFont(UITheme.FONT_BODY);
        emailLbl.setForeground(UITheme.TEXT_SECONDARY);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(UITheme.createPillBadge("ACCOUNT ROLE: " + currentUser.getRole(), UITheme.PRIMARY_LIGHT, UITheme.PRIMARY));
        badgeRow.add(UITheme.createPillBadge("STATUS: " + currentUser.getStatus(),
                "ACTIVE".equalsIgnoreCase(currentUser.getStatus()) ? new Color(0xDC, 0xFC, 0xE7) : new Color(0xFE, 0xE2, 0xE2),
                "ACTIVE".equalsIgnoreCase(currentUser.getStatus()) ? UITheme.SUCCESS_GREEN : UITheme.DANGER_RED));

        infoBlock.add(nameLbl);
        infoBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        infoBlock.add(emailLbl);
        infoBlock.add(Box.createRigidArea(new Dimension(0, 8)));
        infoBlock.add(badgeRow);

        overviewCard.add(infoBlock, BorderLayout.CENTER);
        container.add(overviewCard, BorderLayout.NORTH);

        // 2. Personal Information Form Card
        JPanel formCard = UITheme.createCard();
        formCard.setLayout(new BorderLayout(0, 16));

        JLabel formTitle = new JLabel("Personal Information & Settings");
        formTitle.setFont(UITheme.FONT_SUBTITLE);
        formTitle.setForeground(UITheme.TEXT_PRIMARY);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel fieldsGrid = new JPanel(new GridLayout(0, 2, 20, 16));
        fieldsGrid.setBackground(Color.WHITE);

        JTextField firstField = new JTextField(currentUser.getFirstName());
        firstField.setFont(UITheme.FONT_BODY);

        JTextField lastField = new JTextField(currentUser.getLastName());
        lastField.setFont(UITheme.FONT_BODY);

        JTextField emailField = new JTextField(currentUser.getEmail());
        emailField.setFont(UITheme.FONT_BODY);
        emailField.setEditable(false);
        emailField.setBackground(new Color(0xF3, 0xF4, 0xF6));

        JTextField phoneField = new JTextField(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        phoneField.setFont(UITheme.FONT_BODY);

        fieldsGrid.add(createFormField("First Name", firstField));
        fieldsGrid.add(createFormField("Last Name", lastField));
        fieldsGrid.add(createFormField("Email Address (Immutable Primary ID)", emailField));
        fieldsGrid.add(createFormField("Phone Number", phoneField));

        formCard.add(fieldsGrid, BorderLayout.CENTER);

        // Save Button Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnRow.setBackground(Color.WHITE);

        JButton saveBtn = UITheme.createPrimaryButton("Save Profile Changes");
        saveBtn.addActionListener(e -> {
            String newFirst = firstField.getText().trim();
            String newLast = lastField.getText().trim();
            String newPhone = phoneField.getText().trim();

            AsyncOperation.run(
                    () -> {
                        userService.updateProfile(currentUser, newFirst, newLast, newPhone);
                        return null;
                    },
                    res -> {
                        currentUser.setFirstName(newFirst);
                        currentUser.setLastName(newLast);
                        currentUser.setPhone(newPhone);
                        nameLbl.setText(currentUser.getFullName());
                        DialogUtil.showSuccess(this, "Profile information saved successfully!");
                    },
                    ex -> DialogUtil.showError(this, ex.getMessage())
            );
        });

        btnRow.add(saveBtn);
        formCard.add(btnRow, BorderLayout.SOUTH);

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setOpaque(false);
        formWrapper.add(formCard, BorderLayout.NORTH);

        container.add(formWrapper, BorderLayout.CENTER);
        return container;
    }

    private JPanel createFormField(String labelText, JComponent field) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_BODY_BOLD);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JTextField tf) {
            tf.setPreferredSize(new Dimension(300, 44));
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            tf.setMinimumSize(new Dimension(100, 44));
            tf.setFont(UITheme.FONT_BODY);
            tf.setBorder(new CompoundBorder(
                    new LineBorder(UITheme.BORDER_LIGHT, 1, true),
                    new EmptyBorder(10, 14, 10, 14)
            ));
        }

        p.add(lbl);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(field);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        return p;
    }
}
