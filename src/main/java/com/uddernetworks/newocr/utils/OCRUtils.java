package com.uddernetworks.newocr.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Some various utility methods used by the OCR that may assist others using the library.
 */
public class OCRUtils {

    /**
     * An ImageIO.read() replacement, which in tests can be up to 15x faster. This has shown to significantly improve
     * the OCR's performance both in training and actual usage.
     *
     * @param input The file to read
     * @return The BufferedImage of the file
     */
    public static BufferedImage readImage(File input) {
        BufferedImage bufferedImage = null;
        try {
            ImageIcon imageIcon = new ImageIcon(input.toURI().toURL());
            bufferedImage = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics graphics = bufferedImage.getGraphics();
            graphics.drawImage(imageIcon.getImage(), 0, 0, null);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return bufferedImage;
    }

    /**
     * Removes all common spaces between all newlines, useful if the OCR say adds an extra 2 spaces before all lines of
     * text, this will remove the 2 spaces.
     *
     * @param string The input string
     * @return The input string trimmed properly
     */
    public static String removeLeadingSpaces(String string) {
        var split = string.split("\n");
        var commonSpaces = Arrays.stream(split).mapToInt(OCRUtils::countLeadingSpaces).min().orElse(0);
        if (commonSpaces == 0) return string;
        return Arrays.stream(split).map(line -> line.substring(commonSpaces)).collect(Collectors.joining("\n"));
    }

    private static int countLeadingSpaces(String input) {
        return input.length() - input.stripLeading().length();
    }

    /*
     * Advanced/Convenient Comparisons
     */

    /**
     * Gets the difference between two doubles.
     *
     * @param one The first number
     * @param two The second number
     * @return The difference
     */
    public static double diff(double one, double two) {
        return Math.abs(one - two);
    }

    /**
     * Gets the difference between two ints
     *
     * @param one The first number
     * @param two The second number
     * @return The difference
     */
    public static int diff(int one, int two) {
        return Math.abs(one - two);
    }

    /**
     * Gets the difference of two arrays' values.
     *
     * @param input1 The first array
     * @param input2 The second array
     * @return An array with the same length as the inputs containing the difference of both arrays' respective values
     */
    public static OptionalDouble getDifferencesFrom(double[] input1, double[] input2) {
        if (input1 == null || input2 == null || input1.length != input2.length) return OptionalDouble.empty();
        var res = 0D;

        for (int i = 0; i < input1.length; i++) {
            res += Math.pow(input1[i] - input2[i], 2);
        }

        return OptionalDouble.of(res);
    }

    /**
     * Gets if a given number is within two bounds.
     *
     * @param lowerBound The lower bound to check
     * @param upperBound The upper bound to check
     * @param value      The value to check
     * @return If the two values are within the given bounds
     */
    public static boolean isWithin(double lowerBound, double upperBound, double value) {
        return lowerBound <= value && value <= upperBound;
    }

    /*
     * Image-related methods
     */

    /**
     * Creates a grid of booleans from a {@link BufferedImage} with the same dimensions as the image.
     *
     * @param bufferedImage The input {@link BufferedImage}
     * @return The created grid
     */
    public static boolean[][] createGrid(BufferedImage bufferedImage) {
        return new boolean[bufferedImage.getHeight()][bufferedImage.getWidth()];
    }

    /**
     * Populates a boolean 2D array with the same dimensions as the input image where each pixel is represented by a
     * boolean value, black being `true`, white being `false`.
     *
     * @param input  The input image
     * @param values The mutable empty grid
     */
    public static void toGrid(BufferedImage input, boolean[][] values) {
        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                values[arrY][arrX++] = new Color(input.getRGB(x, y)).equals(Color.BLACK);
            }

            arrX = 0;
            arrY++;
        }
    }

    /**
     * Gets if the row has any `true` (Black) values in it
     *
     * @param values The grid of image values
     * @param y      The Y coordinate of the row to check
     * @return If the row has anything in it
     */
    public static boolean isRowPopulated(boolean[][] values, int y) {
        for (int x = 0; x < values[y].length; x++) {
            if (values[y][x]) return true;
        }

        return false;
    }

    /**
     * Binarizes the input image, making all pixels wither black or white with an alpha of 255
     *
     * @param input The input image to be filtered
     * @return The filtered image
     */
    public static Optional<BufferedImage> filter(BufferedImage input) {
        var result = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                result.setRGB(x, y, isBlack(input, x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }

        return Optional.of(result);
    }

    /**
     * Gets if a pixel should be considered black.
     *
     * @param image The input image
     * @param x     The X coordinate to check
     * @param y     The Y coordinate to check
     * @return If the pixel should be considered black
     */
    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3D < 255 * 0.75;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

    /*
     * For debugging
     */

    /**
     * Creates an image from a grid of booleans, `true` being black and `false` being white.
     *
     * @param values The values to convert into an image
     * @param path   The path of the file
     */
    public static void makeImage(boolean[][] values, String path) {
        try {
            BufferedImage image = new BufferedImage(values[0].length, values.length, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setRGB(x, y, (values[y][x] ? Color.BLACK : Color.WHITE).getRGB());
                }
            }

            ImageIO.write(image, "png", new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Colors in a horizontal line of an image.
     *
     * @param image The image to color on
     * @param color The color to use
     * @param y     The Y value of the horizontal line
     * @param x     The X start position of the line
     * @param width The width of the line to draw
     */
    public static void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    /**
     * Colors in a vertical line of an image.
     *
     * @param image  The image to color on
     * @param color  The color to use
     * @param y      The Y start position of the line
     * @param x      The X value of the vertical line
     * @param height The height of the line to draw
     */
    public static void colorColumn(BufferedImage image, Color color, int x, int y, int height) {
        for (int y2 = 0; y2 < height; y2++) {
            image.setRGB(x, y + y2, color.getRGB());
        }
    }

    /**
     * Prints a grid of booleans to console using full width characters so it will appear proportional and not skewed
     * with spaces and the filling character.
     *
     * @param values The values to print out
     */
    public static void printOut(boolean[][] values) {
        for (boolean[] row : values) {
            for (boolean bool : row) {
                System.out.print(bool ? "＃" : "　");
            }

            System.out.println();
        }
    }
}
