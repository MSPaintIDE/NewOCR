package com.uddernetworks.newocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Stream;

public class OCRUtils {

    /*
     * Advanced/Convenient Comparisons
     */

    public static int getDiff(int one, int two) {
        return Math.max(one, two) - Math.min(one, two);
    }

    public static boolean isWithin(int one, int two, double within) {
        double diff = Math.max((double) one, (double) two) - Math.min((double) one, (double) two);
        return diff <= within;
    }

    public static boolean isWithin(int one, int two, double lowerBound, double upperBound) {
        double diff = Math.max((double) one, (double) two) - Math.min((double) one, (double) two);
        return diff <= upperBound && lowerBound <= diff;
    }

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

    public static boolean isWithin(int lowerBound, int upperBound, int value) {
        return lowerBound <= value && value <= upperBound;
    }

    public static boolean isWithin(double lowerBound, double upperBound, double value) {
        return lowerBound <= value && value <= upperBound;
    }

    // Is difference equal to or under
    public static boolean checkDifference(double num1, double num2, double amount) {
        return Math.max(num1, num2) - Math.min(num1, num2) <= amount;
    }

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

    public static boolean[][] createGrid(BufferedImage bufferedImage) {
        boolean[][] values = new boolean[bufferedImage.getHeight()][];
        for (int i = 0; i < values.length; i++) {
            boolean[] row = new boolean[bufferedImage.getWidth()];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = false;

            values[i] = row;
        }

        return values;
    }

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

    /*
     * Getting array sections
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

    public static Stream<AbstractMap.SimpleEntry<Integer, Integer>> getVerticalThird(boolean[][] values) {
        if (values.length == 0) return Stream.of(null, null, null);
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
}
