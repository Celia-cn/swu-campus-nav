package com.swu.gps.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 将帖子标题渲染为简单配图（PNG）。
 */
public final class TitleImageGenerator {

    private TitleImageGenerator() {
    }

    public static byte[] renderTitlePng(String title) throws IOException {
        String text = title == null ? "" : title.trim();
        if (text.isEmpty()) {
            text = "分享";
        }
        int width = 720;
        int height = 360;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 250, 248));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(21, 101, 192));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        drawWrappedText(g, text, 40, 80, width - 80, 48);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static void drawWrappedText(Graphics2D g, String text, int x, int y, int maxWidth, int lineHeight) {
        StringBuilder line = new StringBuilder();
        int drawY = y;
        for (int i = 0; i < text.length(); i++) {
            line.append(text.charAt(i));
            if (g.getFontMetrics().stringWidth(line.toString()) > maxWidth) {
                String current = line.substring(0, line.length() - 1);
                g.drawString(current, x, drawY);
                drawY += lineHeight;
                line = new StringBuilder().append(text.charAt(i));
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, drawY);
        }
    }
}
