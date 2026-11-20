package com.martplus.desktop.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * UI Theme specifications matching the official MARTplus website design system.
 * Reference: frontend/css/style.css & frontend/css/dashboard.css
 */
public final class UITheme {

    // =========================================================
    // COLOR PALETTE (Exact Website Match)
    // =========================================================
    public static final Color PRIMARY = new Color(0x63, 0x5B, 0xFF);          // #635BFF Primary Indigo/Purple
    public static final Color PRIMARY_DARK = new Color(0x51, 0x48, 0xE5);     // #5148E5 Hover Primary
    public static final Color PRIMARY_LIGHT = new Color(0xEE, 0xED, 0xFF);    // #EEEDFF Active Nav / Pill bg
    public static final Color ACCENT = new Color(0xFF, 0x6B, 0x35);           // #FF6B35 Orange Accent
    public static final Color BG_LIGHT = new Color(0xF8, 0xF9, 0xFC);         // #F8F9FC Canvas / Dashboard Background
    public static final Color CARD_BG = Color.WHITE;                          // #FFFFFF White Cards
    public static final Color TEXT_PRIMARY = new Color(0x11, 0x18, 0x27);     // #111827 Dark Headings & Primary Text
    public static final Color TEXT_SECONDARY = new Color(0x6B, 0x72, 0x80);   // #6B7280 Muted Secondary Labels
    public static final Color TEXT_MUTED = new Color(0x9C, 0xA3, 0xAF);       // #9CA3AF Placeholders & Small text
    public static final Color BORDER_LIGHT = new Color(0xE5, 0xE7, 0xEB);     // #E5E7EB Standard Card & Nav Border
    public static final Color BORDER_SUBTLE = new Color(0xF0, 0xF1, 0xF4);    // #F0F1F4 Subtle dividers
    public static final Color SUCCESS_GREEN = new Color(0x16, 0xA3, 0x4A);    // #16A34A Success (In Stock / Active)
    public static final Color DANGER_RED = new Color(0xEF, 0x44, 0x44);       // #EF4444 Danger (Out of Stock / Delete)
    public static final Color WARNING_AMBER = new Color(0xF5, 0x9E, 0x0B);    // #F59E0B Warning (Low Stock / Pending)
    public static final Color INFO_BLUE = new Color(0x3B, 0x82, 0xF6);        // #3B82F6 Info Blue

    // Backward-compatibility aliases
    public static final Color NAVY_PRIMARY = TEXT_PRIMARY;
    public static final Color NAVY_SECONDARY = Color.WHITE; // Redesigned to white sidebar!
    public static final Color NAVY_HOVER = PRIMARY_LIGHT;
    public static final Color INDIGO_ACCENT = PRIMARY;
    public static final Color PURPLE_ACCENT = PRIMARY_DARK;
    public static final Color BLUE_ACCENT = INFO_BLUE;

    // =========================================================
    // TYPOGRAPHY (Inter / System Sans-Serif Hierarchy)
    // =========================================================
    private static final String FONT_FAMILY = selectFontFamily();

    private static String selectFontFamily() {
        String[] preferred = {"Inter", "Segoe UI", "-apple-system", "Helvetica Neue", "Arial", "SansSerif"};
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();
        for (String pref : preferred) {
            for (String avail : available) {
                if (avail.equalsIgnoreCase(pref)) {
                    return avail;
                }
            }
        }
        return "SansSerif";
    }

    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 17);
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_TINY = new Font(FONT_FAMILY, Font.BOLD, 10);

    private UITheme() {
    }

    // =========================================================
    // BORDERS & PADDINGS
    // =========================================================
    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER_LIGHT, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        );
    }

    public static Border createSubtleBorder() {
        return new LineBorder(BORDER_LIGHT, 1, true);
    }

    public static Border createPadding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    // =========================================================
    // BUTTON FACTORIES (Matching Website Styles)
    // =========================================================

    /**
     * Primary Action Button: #635BFF with hover #5148E5 and smooth rounded edges.
     */
    public static JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(PRIMARY, 1, true),
                new EmptyBorder(9, 18, 9, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY_DARK);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(PRIMARY);
                }
            }
        });

        return button;
    }

    /**
     * Secondary / Outline Button: White background with #E5E7EB border and #4B5563 text.
     */
    public static JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(new Color(0x37, 0x41, 0x51));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(BORDER_LIGHT, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(0xF3, 0xF4, 0xF6));
                    button.setForeground(PRIMARY);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(Color.WHITE);
                    button.setForeground(new Color(0x37, 0x41, 0x51));
                }
            }
        });

        return button;
    }

    /**
     * Danger Button: Soft red background with red text or solid red for critical deletes.
     */
    public static JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(new Color(0xDC, 0x26, 0x26));
        button.setBackground(new Color(0xFE, 0xF2, 0xF2));
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xFE, 0xCA, 0xCA), 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(DANGER_RED);
                    button.setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(0xFE, 0xF2, 0xF2));
                    button.setForeground(new Color(0xDC, 0x26, 0x26));
                }
            }
        });

        return button;
    }

    /**
     * Accent Button: #FF6B35 Orange Accent button.
     */
    public static JButton createAccentButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(ACCENT, 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        return button;
    }

    // =========================================================
    // CARD & CONTAINER FACTORIES
    // =========================================================
    public static JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(createCardBorder());
        return card;
    }

    /**
     * Generates a modern status pill/badge component.
     */
    public static JLabel createBadge(String text, Color bgColor, Color textColor) {
        JLabel badge = new JLabel(" " + text.toUpperCase() + " ");
        badge.setFont(FONT_TINY);
        badge.setForeground(textColor);
        badge.setBackground(bgColor);
        badge.setOpaque(true);
        badge.setBorder(new CompoundBorder(
                new LineBorder(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 120), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        return badge;
    }

    public static JLabel createPillBadge(String text, Color bgColor, Color textColor) {
        return createBadge(text, bgColor, textColor);
    }

    /**
     * Circular User Avatar with initials.
     */
    public static JLabel createAvatar(String name, int size) {
        String initials = "U";
        if (name != null && !name.trim().isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
        }

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background: #EDE9FE
        g2.setColor(new Color(0xED, 0xE9, 0xFE));
        g2.fillOval(0, 0, size, size);

        // Initials text: #635BFF
        g2.setColor(PRIMARY);
        g2.setFont(new Font(FONT_FAMILY, Font.BOLD, (int) (size * 0.42)));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (size - fm.stringWidth(initials)) / 2;
        int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(initials, tx, ty);
        g2.dispose();

        return new JLabel(new ImageIcon(img));
    }

    /**
     * Creates an empty state panel with icon, headline, and subtext.
     */
    public static JPanel createEmptyState(String emoji, String title, String subtitle, JButton actionBtn) {
        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.setBorder(new CompoundBorder(
                createCardBorder(),
                new EmptyBorder(40, 30, 40, 30)
        ));

        JLabel iconLabel = new JLabel(emoji);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SUBTITLE);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(FONT_BODY);
        subLabel.setForeground(TEXT_SECONDARY);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(iconLabel);
        emptyPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        emptyPanel.add(titleLabel);
        emptyPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        emptyPanel.add(subLabel);

        if (actionBtn != null) {
            emptyPanel.add(Box.createRigidArea(new Dimension(0, 18)));
            actionBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyPanel.add(actionBtn);
        }

        emptyPanel.add(Box.createVerticalGlue());
        return emptyPanel;
    }

    public static JPanel createEmptyState(String emoji, String title, String subtitle, String actionText, Runnable onAction) {
        JButton btn = null;
        if (actionText != null && onAction != null) {
            btn = createPrimaryButton(actionText);
            btn.addActionListener(e -> onAction.run());
        }
        return createEmptyState(emoji, title, subtitle, btn);
    }

    // =========================================================
    // TABLE STYLING (Matching Modern Web Data Grids)
    // =========================================================
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(38);
        table.setGridColor(BORDER_SUBTLE);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT_PRIMARY);

        // Header Styling
        table.getTableHeader().setFont(FONT_BODY_BOLD);
        table.getTableHeader().setBackground(new Color(0xF9, 0xFA, 0xFB));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));
        table.getTableHeader().setReorderingAllowed(false);
    }
}
