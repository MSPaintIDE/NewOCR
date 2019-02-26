package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.utils.ConversionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TrainGenerator {

    private static String trainString = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjiklmnopqrstuvwxyz{|}~W W";
    public static final int UPPER_FONT_BOUND = 90;
    public static final int LOWER_FONT_BOUND = 30;

    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(1500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        RenderingHints rht = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rht);

        Font font = new Font("Monospaced.plain", Font.PLAIN, 92);
        graphics.setFont(font);

        int newHeight = 11;

        int size2 = UPPER_FONT_BOUND;
        for (int i = 0; i < UPPER_FONT_BOUND - LOWER_FONT_BOUND; i++) {
            newHeight += size2 + 11;
            size2--;
        }

        image = new BufferedImage(graphics.getFontMetrics().stringWidth(trainString) + 50, newHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        graphics = image.createGraphics();

        RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rh);

        int size = UPPER_FONT_BOUND;
        int offset = UPPER_FONT_BOUND;
        for (int i = 0; i < UPPER_FONT_BOUND - LOWER_FONT_BOUND; i++) {
            drawLine(graphics, trainString, offset, size);
            offset += ConversionUtils.pointToPixel(size) + 15;
            size--;
        }

        try {
            ImageIO.write(image, "png", new File("training_mono.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawLine(Graphics2D drawTo, String line, int yOffset, int size) {
        Font font = new Font("Monospaced.plain", Font.PLAIN, size);
        drawTo.setFont(font);
        drawTo.setPaint(Color.BLACK);

        drawTo.drawString(line, 10, yOffset);
    }

}
