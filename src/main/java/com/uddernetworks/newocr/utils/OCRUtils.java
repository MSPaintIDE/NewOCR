package com.uddernetworks.newocr.utils;

import com.uddernetworks.newocr.character.SearchCharacter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Some various utility methods used by the OCR that may assist others using the library.
 */
public class OCRUtils {

    public static final AbstractMap.SimpleEntry<Integer, Integer> ZERO_PLACEHOLDER = new AbstractMap.SimpleEntry<>(0, 0);

    /*
     * Advanced/Convenient Comparisons
     */

    /**
     * Gets the difference between two doubles.
     * @param one The first number
     * @param two The second number
     * @return The difference
     */
    public static double getDiff(double one, double two) {
        return Math.max(one, two) - Math.min(one, two);
    }

    /**
     * Gets the difference between two ints
     * @param one The first number
     * @param two The second number
     * @return The difference
     */
    public static int getDiff(int one, int two) {
        return Math.max(one, two) - Math.min(one, two);
    }

    /**
     * Gets if two ints are within a given double.
     * @param one Bound 1
     * @param two Bound 2
     * @param within The number
     * @return If one and two are within `within`
     */
    public static boolean isWithin(int one, int two, double within) {
        double diff = Math.max((double) one, (double) two) - Math.min((double) one, (double) two);
        return diff <= within;
    }

    /**
     * Gets if the difference of the two given ints are between both of the two doubles given.
     * @param one The first number
     * @param two The second number
     * @param lowerBound The lower bound to check
     * @param upperBound The upper bound to check
     * @return If the difference of the two given ints are between both of the two doubles given
     */
    public static boolean isWithin(int one, int two, double lowerBound, double upperBound) {
        double diff = Math.max((double) one, (double) two) - Math.min((double) one, (double) two);
        return diff <= upperBound && lowerBound <= diff;
    }

    /**
     * Gets the percentage difference of two different 2D boolean arrays.
     * @param input1 The first 2D array
     * @param input2 The second 2D array
     * @return The percentage difference <= 1
     */
    public static double getDifferencesFrom2D(boolean[][] input1, boolean[][] input2) {
        if (input1.length != input2.length) return 1D;
        double result = 0;
        for (int x = 0; x < input1.length; x++) {
            for (int y = 0; y < input1[0].length; y++) {
                if (input1[x][y] != input2[x][y]) result++;
            }
        }

        return result / ((double) input1.length * (double) input1[0].length);
    }

    /**
     * Gets the difference of two arrays' values.
     * @param input1 The first array
     * @param input2 The second array
     * @return An array with the same length as the inputs containing the difference of both arrays' respective values
     */
    public static double[] getDifferencesFrom(double[] input1, double[] input2) {
        if (input1.length != input2.length) return null;
        double[] ret = new double[input1.length];

        for (int i = 0; i < input1.length; i++) {
            double one = input1[i];
            double two = input2[i];

            ret[i] = Math.max(one, two) - Math.min(one, two);
        }

        return ret;
    }

    /**
     * Gets if a given number is within two bounds. The same as {@link #isWithin(double, double, double)} but with ints.
     * @param lowerBound The lower bound to check
     * @param upperBound The upper bound to check
     * @param value The value to check
     * @return If the two values are within the given bounds
     */
    public static boolean isWithin(int lowerBound, int upperBound, int value) {
        return lowerBound <= value && value <= upperBound;
    }

    /**
     * Gets if a given number is within two bounds. The same as {@link #isWithin(int, int, double)} but with doubles.
     * @param lowerBound The lower bound to check
     * @param upperBound The upper bound to check
     * @param value The value to check
     * @return If the two values are within the given bounds
     */
    public static boolean isWithin(double lowerBound, double upperBound, double value) {
        return lowerBound <= value && value <= upperBound;
    }

    /**
     * Gets if the difference or two doubles is less than or equal to another given double.
     * @param num1 The first number
     * @param num2 The second number
     * @param amount The inclusive amount the difference can be
     * @return If the difference is less than or equal to the `amount`
     */
    public static boolean checkDifference(double num1, double num2, double amount) {
        return Math.max(num1, num2) - Math.min(num1, num2) <= amount;
    }

    /**
     * Sorts a Map by its values
     * @param map The Map to sort
     * @return The sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /*
     * Image-related methods
     */

    /**
     * Creates a grid of booleans from a {@link BufferedImage} with the same dimensions as the image.
     * @param bufferedImage The input {@link BufferedImage}
     * @return The created grid
     */
    public static boolean[][] createGrid(BufferedImage bufferedImage) {
        boolean[][] values = new boolean[bufferedImage.getHeight()][];
        for (int i = 0; i < values.length; i++) {
            boolean[] row = new boolean[bufferedImage.getWidth()];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = false;

            values[i] = row;
        }

        return values;
    }

    /**
     * Populates a boolean 2D array with the same dimensions as the input image where each pixel is represented by a
     * boolean value, black being `true`, white being `false`.
     * @param input The input image
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
     * @param values The grid of image values
     * @param y The Y coordinate of the row to check
     * @return If the row has anything in it
     */
    public static boolean isRowPopulated(boolean[][] values, int y) {
        for (int x = 0; x < values[y].length; x++) {
            if (values[y][x]) return true;
        }

        return false;
    }

    /**
     * Gets all the characters between the two Y values (The line bounds) form the {@link SearchCharacter} list.
     * @param topY The top Y value of the line
     * @param bottomY The bottom Y value of the line
     * @param searchCharacters The {@link SearchCharacter} list to check from
     * @return The {@link SearchCharacter} objects between the given Y values
     */
    public static List<SearchCharacter> findCharactersAtLine(int topY, int bottomY, List<SearchCharacter> searchCharacters) {
        return searchCharacters
                .stream()
                .sorted()
                .filter(searchCharacter -> OCRUtils.isWithin(topY, bottomY, searchCharacter.getY()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Sets all pixels from input to temp. When running in the program if the System property `newocr.rewrite` is set to
     * true, it will write the image to stop any weird image decoding issues
     * @param temp The empty image with the same size as the input that will be written to
     * @param input The input that will be read from
     */
    public static void rewriteImage(BufferedImage temp, BufferedImage input) {
        for (int y = 0; y < temp.getHeight(); y++) {
            for (int x = 0; x < temp.getWidth(); x++) {
                temp.setRGB(x, y, input.getRGB(x, y));
            }
        }
    }

    /**
     * Gets if a {@link SearchCharacter} is fully black for things like . or the sections of =
     * @param searchCharacter The input {@link SearchCharacter} to check
     * @return If the input is all black
     */
    public static boolean isAllBlack(SearchCharacter searchCharacter) {
        // TODO: Replace with a difference check with threshold and/or a circular check for other fonts
        for (boolean[] row : searchCharacter.getValues()) {
            for (boolean bool : row) {
                if (!bool) return false;
            }
        }

        return true;
    }

    /**
     * Binarizes the input image, making all pixels wither black or white with an alpha of 255
     * @param bufferedImage The input image to be mutated
     */
    public static void filter(BufferedImage bufferedImage) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                Color writeColor = isBlack(bufferedImage, x, y) ? new Color(0, 0, 0, 255) : new Color(255, 255, 255, 255);
                bufferedImage.setRGB(x, y, writeColor.getRGB());
            }
        }
    }

    /**
     * Gets if a pixel should be considered black.
     * @param image The input image
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return If the pixel should be considered black
     */
    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 255 * 0.75;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

    /*
     * Getting array sections
     */

    /**
     * Splits a grid of values in half horizontally
     * @param values The grid to split
     * @return A stream of 2 halves, top and bottom
     */
    public static Stream<boolean[][]> getHorizontalHalf(boolean[][] values) {
        int topHeight = values.length / 2;
        int bottomHeight = values.length - topHeight;

        boolean[][] topHalf = new boolean[topHeight][];
        boolean[][] bottomHalf = new boolean[bottomHeight][];

        for (int y = 0; y < values.length; y++) {
            if (y < topHeight) {
                topHalf[y] = values[y];
            } else {
                bottomHalf[y - topHeight] = values[y];
            }
        }

        return Stream.of(topHalf, bottomHalf).sequential();
    }

    /**
     * Splits a grid of values in thirds horizontally
     * @param values The grid to split
     * @return A stream of 3 thirds: top, middle, and bottom
     */
    public static Stream<boolean[][]> getHorizontalThird(boolean[][] values) {
        int topHeight = values.length / 3;
        int middleHeight = values.length - topHeight * 2;
        int bottomHeight = topHeight;

        boolean[][] topThird = new boolean[topHeight][];
        boolean[][] middleThird = new boolean[middleHeight][];
        boolean[][] bottomThird = new boolean[bottomHeight][];

        for (int y = 0; y < values.length; y++) {
            if (y < topHeight) {
                topThird[y] = values[y];
            } else if (y < topHeight + middleHeight) {
                middleThird[y - topHeight] = values[y];
            } else {
                bottomThird[y - topHeight - middleHeight] = values[y];
            }
        }

        return Stream.of(topThird, middleThird, bottomThird).sequential();
    }

    /**
     * Splits a grid of values in half vertically
     * @param values The grid to split
     * @return A stream of 2 halves, left and right
     */
    public static Stream<boolean[][]> getVerticalHalf(boolean[][] values) {
        if (values.length == 0) return Stream.of(null, null);
        int leftHeight = values[0].length / 2;
        int rightHeight = values[0].length - leftHeight;

        boolean[][] leftHalf = new boolean[values.length][];
        boolean[][] rightHalf = new boolean[values.length][];

        for (int i = 0; i < values.length; i++) {
            leftHalf[i] = new boolean[leftHeight];
            rightHalf[i] = new boolean[rightHeight];
        }

        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[0].length; x++) {
                if (x < leftHeight) {
                    leftHalf[y][x] = values[y][x];
                } else {
                    rightHalf[y][x - leftHeight] = values[y][x];
                }
            }
        }

        return Stream.of(leftHalf, rightHalf).sequential();
    }

    /**
     * Splits a grid of values in thirds vertically
     * @param values The grid to split
     * @return A stream of 3 thirds: left, middle, and right
     */
    public static Stream<AbstractMap.SimpleEntry<Integer, Integer>> getVerticalThird(boolean[][] values) {
        if (values.length == 0) return Stream.of(ZERO_PLACEHOLDER, ZERO_PLACEHOLDER, ZERO_PLACEHOLDER);
        int leftHeight = values[0].length / 3;
        int middleHeight = values[0].length - leftHeight * 2;

        int leftSize = 0, leftTrue = 0;
        int middleSize = 0, middleTrue = 0;
        int rightSize = 0, rightTrue = 0;

        for (boolean[] line : values) {
            for (int x = 0; x < values[0].length; x++) {
                if (x < leftHeight) {
                    if (line[x]) leftTrue++;
                    leftSize++;
                } else if (x < middleHeight + leftHeight) {
                    if (line[x]) middleTrue++;
                    middleSize++;
                } else {
                    if (line[x]) rightTrue++;
                    rightSize++;
                }
            }
        }

        return Stream.of(new AbstractMap.SimpleEntry<>(leftTrue, leftSize),
                new AbstractMap.SimpleEntry<>(middleTrue, middleSize),
                new AbstractMap.SimpleEntry<>(rightTrue, rightSize)).sequential();
    }

    /**
     * Splits a grid of values in half diagonally. The diagonal line will be going from the top left to bototm right if
     * `increasing` is `true`, and top left to bottom right if it is `false`.
     * @param values The grid to split into halves diagonally
     * @param increasing The line's slope will be positive when `true`, and negative when `false`.
     * @return A List of 2 halves
     */
    public static List<Map.Entry<Integer, Integer>> getDiagonal(boolean[][] values, boolean increasing) {
        double slope = (double) values.length / (double) values[0].length;

        List<Integer> yPositions = new ArrayList<>();

        for (int x = 0; x < values[0].length; x++) {
            double y = slope * x;
            if (increasing) y = values.length - y;
            yPositions.add((int) y);
        }

        int topSize = 0;
        int topTrue = 0;
        int bottomSize = 0;
        int bottomTrue = 0;

        for (int x = 0; x < values[0].length; x++) {
            int yPos = yPositions.get(x);
            for (int y = 0; y < values.length; y++) {
                if (y < yPos) {
                    if (values[y][x]) bottomTrue++;
                    bottomSize++;
                } else {
                    if (values[y][x]) topTrue++;
                    topSize++;
                }
            }
        }

        List<Map.Entry<Integer, Integer>> ret = new LinkedList<>();
        ret.add(new AbstractMap.SimpleEntry<>(topTrue, topSize));
        ret.add(new AbstractMap.SimpleEntry<>(bottomTrue, bottomSize));
        return ret;
    }

    /*
     * For debugging
     */

    /**
     * Creates an image from a grid of booleans, `true` being black and `false` being white.
     * @param values The values to convert into an image
     * @param path The path of the file
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
     * @param image The image to color on
     * @param color The color to use
     * @param y The Y value of the horizontal line
     * @param x The X start position of the line
     * @param width The width of the line to draw
     */
    public static void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    /**
     * Colors in a vertical line of an image.
     * @param image The image to color on
     * @param color The color to use
     * @param y The Y start position of the line
     * @param x The X value of the vertical line
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
     * @param values The values to print out
     */
    public static void printOut(boolean[][] values) {
        for (boolean[] row : values) {
            for (boolean bool : row) {
                System.out.print(bool ? "＃" : "　");
            }

            System.out.println("");
        }
    }
}
