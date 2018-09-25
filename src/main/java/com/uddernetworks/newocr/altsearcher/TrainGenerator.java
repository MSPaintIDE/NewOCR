package com.uddernetworks.newocr.altsearcher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TrainGenerator {

    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(1500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        String message = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjijklmnopqrstuvwxyz{|}~";

        RenderingHints rht = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rht);

        Font fontt = new Font("Verdana", Font.PLAIN, 92);
        graphics.setFont(fontt);

//        List<String> linesList = Arrays.asList(message.split("\n"));

        int newHeight = 100;

        int size2 = 92;
        for (int i = 0; i < 92 - 12; i++) {
            newHeight += size2 + 1;
        }

        System.out.println("newHeight = " + newHeight);
        image = new BufferedImage(graphics.getFontMetrics().stringWidth(message) + 50, newHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        graphics = image.createGraphics();

        RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rh);

        int size = 92;
        int offset = 92;
        for (int i = 0; i < 92 - 12; i++) {
//        for (int i = 0; i < linesList.size(); i++) {
//            graphics.drawString(linesList.get(i), 0, 100);
            drawLine(graphics, message, offset, size);
            offset += size + 10;
            size--;
        }

        try {
            ImageIO.write(image, "png", new File("E:\\NewOCR\\training.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawLine(Graphics2D drawTo, String line, int yOffset, int size) {
        Font font = new Font("Verdana", Font.PLAIN, size);
        drawTo.setFont(font);
        drawTo.setPaint(Color.BLACK);

        drawTo.drawString(line, 10, yOffset);
    }

}