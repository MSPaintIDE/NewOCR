package com.uddernetworks.newocr.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class SegmentationUtils {

    public static final IntPair ZERO_PLACEHOLDER = new IntPair(0, 0);

    /**
     * Splits a grid of values in half horizontally
     *
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
     *
     * @param values The grid to split
     * @return A stream of 3 thirds: top, middle, and bottom
     */
    public static Stream<boolean[][]> getHorizontalThird(boolean[][] values) {
        int topHeight = values.length / 3;
        int middleHeight = values.length - topHeight * 2;

        boolean[][] topThird = new boolean[topHeight][];
        boolean[][] middleThird = new boolean[middleHeight][];
        boolean[][] bottomThird = new boolean[topHeight][];

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
     *
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
     *
     * @param values The grid to split
     * @return A stream of 3 thirds: left, middle, and right
     */
    public static Stream<IntPair> getVerticalThird(boolean[][] values) {
        if (values.length == 0) return Stream.of(ZERO_PLACEHOLDER, ZERO_PLACEHOLDER, ZERO_PLACEHOLDER);
        int leftHeight = values[0].length / 3;
        int middleHeight = values[0].length - leftHeight * 2;

        int leftSize = 0, leftTrue = 0;
        int middleSize = 0, middleTrue = 0;
        int rightSize = 0, rightTrue = 0;

        for (boolean[] line : values) {
            for (int x = 0; x < values[0].length; x++) {
                if (x < leftHeight) {
                    if (line[x]) {
                        leftTrue++;
                    }

                    leftSize++;
                } else if (x < middleHeight + leftHeight) {
                    if (line[x]) {
                        middleTrue++;
                    }

                    middleSize++;
                } else {
                    if (line[x]) {
                        rightTrue++;
                    }

                    rightSize++;
                }
            }
        }

        return Stream.of(new IntPair(leftTrue, leftSize), new IntPair(middleTrue, middleSize), new IntPair(rightTrue, rightSize));
    }

    /**
     * Splits a grid of values in half diagonally. The diagonal line will be going from the top left to bottom right if
     * `increasing` is `true`, and top left to bottom right if it is `false`.
     *
     * @param values     The grid to split into halves diagonally
     * @param increasing The line's slope will be positive when `true`, and negative when `false`.
     * @return A List of 2 halves
     */
    public static List<IntPair> getDiagonal(boolean[][] values, boolean increasing) {
        int topSize = 0;
        int topTrue = 0;
        int bottomSize = 0;
        int bottomTrue = 0;

        if (values != null) {
            double slope = (double) values.length / (double) values[0].length;

            IntList yPositions = new IntArrayList();

            for (int x = 0; x < values[0].length; x++) {
                double y = slope * x;

                if (increasing) {
                    y = values.length - y;
                }

                yPositions.add((int) y);
            }

            for (int x = 0; x < values[0].length; x++) {
                int yPos = yPositions.getInt(x);

                for (int y = 0; y < values.length; y++) {
                    if (y < yPos) {
                        if (values[y][x]) {
                            bottomTrue++;
                        }

                        bottomSize++;
                    } else {
                        if (values[y][x]) {
                            topTrue++;
                        }

                        topSize++;
                    }
                }
            }
        }

        List<IntPair> ret = new LinkedList<>();

        ret.add(new IntPair(topTrue, topSize));
        ret.add(new IntPair(bottomTrue, bottomSize));

        return ret;
    }

}
