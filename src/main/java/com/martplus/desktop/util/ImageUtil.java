package com.martplus.desktop.util;

import com.martplus.desktop.concurrency.BackgroundTaskManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * High-performance, asynchronous image loading and caching utility.
 * Guarantees that image network/file I/O never blocks the Swing Event Dispatch Thread (EDT).
 * Provides aspect-ratio-preserving scaling and graceful visual placeholders for missing,
 * null, or invalid image URLs.
 */
public final class ImageUtil {

    private static final ConcurrentHashMap<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static ImageIcon appLogoIcon = null;

    private ImageUtil() {
    }

    /**
     * Retrieves the MARTplus brand logo image icon from resources.
     */
    public static synchronized ImageIcon getAppLogo(int width, int height) {
        if (appLogoIcon == null) {
            try {
                InputStream is = ImageUtil.class.getResourceAsStream("/icons/mplus-logo.png");
                if (is != null) {
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) {
                        appLogoIcon = scalePreservingAspectRatio(img, width, height);
                        return appLogoIcon;
                    }
                }
            } catch (Exception ignored) {
            }
            // Fallback generated logo
            appLogoIcon = createFallbackLogo(width, height);
        }
        return appLogoIcon;
    }

    public static Image getLogoImage() {
        ImageIcon icon = getAppLogo(64, 64);
        return icon != null ? icon.getImage() : null;
    }

    public static ImageIcon getLogoIcon(int width, int height) {
        return getAppLogo(width, height);
    }

    public static ImageIcon createFallbackLogo(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UITheme.PRIMARY);
        g2.fillRoundRect(0, 0, width, height, Math.max(6, width / 4), Math.max(6, height / 4));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (height * 0.55)));
        FontMetrics fm = g2.getFontMetrics();
        String text = "M+";
        int tx = (width - fm.stringWidth(text)) / 2;
        int ty = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Scales an image while strictly preserving its original aspect ratio.
     * The scaled image is centered inside a transparent canvas of exactly maxWidth x maxHeight,
     * ensuring no stretching or UI layout jumping occurs.
     */
    public static ImageIcon scalePreservingAspectRatio(BufferedImage original, int maxWidth, int maxHeight) {
        if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
            return createPlaceholder(maxWidth, maxHeight, "📦");
        }

        int origW = original.getWidth();
        int origH = original.getHeight();

        double scale = Math.min((double) maxWidth / origW, (double) maxHeight / origH);
        int targetW = Math.max(1, (int) Math.round(origW * scale));
        int targetH = Math.max(1, (int) Math.round(origH * scale));

        BufferedImage canvas = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (maxWidth - targetW) / 2;
        int y = (maxHeight - targetH) / 2;

        Image scaledImg = original.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
        g2.drawImage(scaledImg, x, y, null);
        g2.dispose();

        return new ImageIcon(canvas);
    }

    /**
     * Loads a product image asynchronously and dispatches the resulting ImageIcon to the EDT.
     * Supports HTTP/HTTPS URLs, local files, and classpath resources.
     * If URL is null, invalid, or fails, a beautiful placeholder icon is dispatched instead.
     */
    public static void loadProductImageAsync(String imageUrl, int width, int height, Consumer<ImageIcon> callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.equalsIgnoreCase("null")) {
            SwingUtilities.invokeLater(() -> callback.accept(createPlaceholder(width, height, "📦")));
            return;
        }

        String trimmed = imageUrl.trim();
        String cacheKey = trimmed + "_" + width + "x" + height;
        ImageIcon cached = CACHE.get(cacheKey);
        if (cached != null) {
            SwingUtilities.invokeLater(() -> callback.accept(cached));
            return;
        }

        // Execute background fetch on worker thread pool
        BackgroundTaskManager.submit(() -> {
            ImageIcon icon;
            try {
                BufferedImage original = null;

                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    URL url = URI.create(trimmed).toURL();
                    original = ImageIO.read(url);
                } else if (trimmed.startsWith("file:/")) {
                    URL url = URI.create(trimmed).toURL();
                    original = ImageIO.read(url);
                } else if (new File(trimmed).exists()) {
                    original = ImageIO.read(new File(trimmed));
                } else {
                    // Try classpath lookup
                    String resourcePath = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
                    InputStream is = ImageUtil.class.getResourceAsStream(resourcePath);
                    if (is != null) {
                        original = ImageIO.read(is);
                    }
                }

                if (original != null) {
                    icon = scalePreservingAspectRatio(original, width, height);
                    CACHE.put(cacheKey, icon);
                } else {
                    icon = createPlaceholder(width, height, "📦");
                }
            } catch (Exception e) {
                icon = createPlaceholder(width, height, "🛍️");
            }

            final ImageIcon finalIcon = icon;
            SwingUtilities.invokeLater(() -> callback.accept(finalIcon));
        });
    }

    /**
     * Generates a modern product image placeholder matching the website design.
     * Displays a clean soft container, rounded inner card, central emoji/icon,
     * and subtle catalog branding on larger sizes.
     */
    public static ImageIcon createPlaceholder(int width, int height, String iconChar) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Soft background #F8F9FC
        g2.setColor(new Color(0xF8, 0xF9, 0xFC));
        g2.fillRect(0, 0, width, height);

        // Inner white rounded card
        int cardPad = Math.max(8, (int) (Math.min(width, height) * 0.08));
        int cardW = width - (cardPad * 2);
        int cardH = height - (cardPad * 2);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(cardPad, cardPad, cardW, cardH, 16, 16);

        // Subtle border #E5E7EB
        g2.setColor(new Color(0xE5, 0xE7, 0xEB));
        g2.drawRoundRect(cardPad, cardPad, cardW, cardH, 16, 16);

        // Central icon
        g2.setColor(new Color(0x9C, 0xA3, 0xAF));
        int fontSize = Math.max(22, (int) (Math.min(width, height) * 0.28));
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (width - fm.stringWidth(iconChar)) / 2;
        int ty = (height - fm.getHeight()) / 2 + fm.getAscent();
        if (height >= 180) {
            ty -= 12; // Shift up slightly to fit subtitle
        }
        g2.drawString(iconChar, tx, ty);

        // Subtle subtitle on larger views (Product Details modal)
        if (height >= 180) {
            g2.setColor(new Color(0x9C, 0xA3, 0xAF));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics subFm = g2.getFontMetrics();
            String subText = "MARTplus Catalog Item";
            int subX = (width - subFm.stringWidth(subText)) / 2;
            int subY = ty + 24;
            g2.drawString(subText, subX, subY);
        }

        g2.dispose();
        return new ImageIcon(img);
    }
}
