package com.martplus.desktop;

import com.martplus.desktop.model.Product;
import com.martplus.desktop.ui.ProductPanel;
import com.martplus.desktop.util.ImageUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Details UI & Image Aspect-Ratio Scaling Tests")
class ProductDetailsUITest {

    @Test
    @DisplayName("Image scaling strictly preserves aspect ratio without distortion")
    void testScalePreservingAspectRatio() {
        // Create a 400x200 wide rectangle image (2:1 aspect ratio)
        BufferedImage wideImg = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        
        // Target container is 300x300 square
        ImageIcon scaledIcon = ImageUtil.scalePreservingAspectRatio(wideImg, 300, 300);
        assertNotNull(scaledIcon);
        assertEquals(300, scaledIcon.getIconWidth());
        assertEquals(300, scaledIcon.getIconHeight());
    }

    @Test
    @DisplayName("Placeholder generation handles compact and modal dimensions gracefully")
    void testPlaceholderGeneration() {
        ImageIcon cardPlaceholder = ImageUtil.createPlaceholder(234, 150, "📦");
        assertNotNull(cardPlaceholder);
        assertEquals(234, cardPlaceholder.getIconWidth());
        assertEquals(150, cardPlaceholder.getIconHeight());

        ImageIcon modalPlaceholder = ImageUtil.createPlaceholder(340, 320, "🛍️");
        assertNotNull(modalPlaceholder);
        assertEquals(340, modalPlaceholder.getIconWidth());
        assertEquals(320, modalPlaceholder.getIconHeight());
    }

    @Test
    @DisplayName("Product description sanitizer eliminates back-to-back duplicate text")
    void testDescriptionSanitization() {
        Product p = new Product();
        p.setDescription("Premium wireless noise cancelling headphones. Premium wireless noise cancelling headphones.");

        String sanitized = ProductPanel.getSanitizedDescription(p);
        assertEquals("Premium wireless noise cancelling headphones.", sanitized);
    }

    @Test
    @DisplayName("Product description sanitizer preserves clean descriptions")
    void testCleanDescriptionPreserved() {
        Product p = new Product();
        p.setDescription("Ergonomic mechanical keyboard with custom RGB backlighting.");

        String sanitized = ProductPanel.getSanitizedDescription(p);
        assertEquals("Ergonomic mechanical keyboard with custom RGB backlighting.", sanitized);
    }
}

