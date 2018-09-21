package com.uddernetworks.newocr.altsearcher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\input.png"));
        boolean[][] values = createGrid(input);
        List<SearchCharacter> searchCharcaters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                temp.setRGB(x, y, input.getRGB(x, y));
            }
        }

        input = temp;

         // Pre-filter

        filter(input);

         // End pre-filters

        ImageIO.write(temp, "png", new File("E:\\NewOCR\\tempout1.png"));

        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                values[arrY][arrX++] = new Color(input.getRGB(x, y)).equals(Color.BLACK);
            }

            arrX = 0;
            arrY++;
        }

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <=-- y;) {
//        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (coordinates.size() != 0) {
                    SearchCharacter searchCharacter = new SearchCharacter(coordinates);

                    if (searchCharacter.isProbablyDot()) {
                        // TODO: Do stuff
                    }

                    searchCharcaters.add(searchCharacter);
                    searchCharacter.drawTo(input);

                    coordinates = new ArrayList<>();
                }
            }
        }

        System.out.println(searchCharcaters.size() + " characters found");

        ImageIO.write(temp, "png", new File("E:\\NewOCR\\tempout.png"));
    }



    public static void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    public static void colorColumn(BufferedImage image, Color color, int x, int y, int height) {
        for (int y2 = 0; y2 < height; y2++) {
            image.setRGB(x, y + y2, color.getRGB());
        }
    }

    public static boolean[][] createGrid(BufferedImage bufferedImage) {
        boolean[][] values = new boolean[bufferedImage.getHeight()][];
        for (int i = 0; i < values.length; i++) {
            boolean[] row = new boolean[bufferedImage.getWidth()];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = false;

            values[i] = row;
        }

        return values;
    }

    public static void printOut(boolean[][] values) {
        for (boolean[] row : values) {
            for (boolean bool : row) {
                System.out.print(bool ? "1" : "0");
            }

            System.out.println("");
        }
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    public static void filter(BufferedImage bufferedImage) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                bufferedImage.setRGB(x, y, isBlack(bufferedImage, x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
    }

    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 128;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
