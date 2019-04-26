package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.utils.ConversionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Creates a simple image to train on from a given font.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class ComputerTrainGenerator implements TrainGenerator {

    @Override
    public void generateTrainingImage(File file) {
        generateTrainingImage(file, new TrainGeneratorOptions());
    }

    @Override
    public void generateTrainingImage(File file, TrainGeneratorOptions options) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        RenderingHints rht = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rht);

        Font font = new Font(options.getFontFamily(), Font.PLAIN, options.getMaxFontSize());
        graphics.setFont(font);

        int newHeight = 11;

        int size2 = options.getMaxFontSize();
        for (int i = 0; i < options.getMaxFontSize() - options.getMinFontSize(); i++) {
            newHeight += size2 + 11;
            size2--;
        }

        image = new BufferedImage(graphics.getFontMetrics().stringWidth(OCRScan.RAW_STRING) + 50, newHeight, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        graphics = image.createGraphics();

        RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHints(rh);

        int size = options.getMaxFontSize();
        int offset = options.getMaxFontSize();
        for (int i = 0; i < options.getMaxFontSize() - options.getMinFontSize(); i++) {
            drawLine(graphics, options.getFontFamily(), offset, size);
            offset += ConversionUtils.pointToPixel(size) + 15;
            size--;
        }

        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawLine(Graphics2D drawTo, String fontName, int yOffset, int size) {
        Font font = new Font(fontName, Font.PLAIN, size);
        drawTo.setFont(font);
        drawTo.setPaint(Color.BLACK);

        drawTo.drawString(OCRScan.RAW_STRING, 10, yOffset);
    }

}
