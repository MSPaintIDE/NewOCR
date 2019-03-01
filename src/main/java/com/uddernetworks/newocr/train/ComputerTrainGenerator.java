package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.utils.ConversionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ComputerTrainGenerator implements TrainGenerator {

    @Override
    public void generateTrainingImage(File file) {
        generateTrainingImage(file,new TrainGeneratorOptions());
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

        image = new BufferedImage(graphics.getFontMetrics().stringWidth(OCRTrain.TRAIN_STRING) + 50, newHeight, BufferedImage.TYPE_INT_ARGB);
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
            drawLine(graphics, OCRTrain.TRAIN_STRING, options.getFontFamily(), offset, size);
            offset += ConversionUtils.pointToPixel(size) + 15;
            size--;
        }

        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawLine(Graphics2D drawTo, String line, String fontName, int yOffset, int size) {
        Font font = new Font(fontName, Font.PLAIN, size);
        drawTo.setFont(font);
        drawTo.setPaint(Color.BLACK);

        drawTo.drawString(line, 10, yOffset);
    }

}
