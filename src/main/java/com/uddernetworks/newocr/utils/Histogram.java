package com.uddernetworks.newocr.utils;

import com.uddernetworks.newocr.SearchImage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class Histogram {

    private SearchImage image;

    public Histogram(SearchImage image) {
        this.image = image;
    }

    public List<IntPair> generateHistogram(boolean horizontal) {
        return horizontal ? generateHorizontalHistogram() : generateVerticalHistogram();
    }

    // Makes a histogram with the 0 point being at the bottom, measuring columns
    public List<IntPair> generateVerticalHistogram() {
        var ret = new LinkedList<IntPair>();
        var width = this.image.getWidth();
        var height = this.image.getHeight();

        for (int x = 0; x < width; x++) {
            var finalX = x;
            ret.add(new IntPair(x, (int) IntStream.range(0, height).filter(y -> this.image.getValue(finalX, y)).count()));
        }

        return ret;
    }

    // Makes a histogram with the 0 point on the left, measuring rows
    public List<IntPair> generateHorizontalHistogram() {
        var ret = new ArrayList<IntPair>();
        var width = this.image.getWidth();
        var height = this.image.getHeight();

        for (int y = 0; y < height; y++) {
            var finalY = y;
            ret.add(new IntPair(y, (int) IntStream.range(0, width).filter(x -> this.image.getValue(x, finalY)).count()));
        }

        return ret;
    }

    /**
     * Gets the conjoined rows/lines
     *
     * @return The lines with the IntPair being <fromY, toY>
     */
    public List<IntPair> getWholeLines() {
        var ret = new ArrayList<IntPair>();
        var width = this.image.getWidth();
        var height = this.image.getHeight();

        var emptyCounter = 0;
        var topBuffer = true;
        for (int y = 0; y < height; y++) {
            int finalY = y;
            var empty = IntStream.range(0, width).noneMatch(x -> this.image.getValue(x, finalY));
            if (empty) {
                if (topBuffer) {
                    topBuffer = false;
                    continue;
                }

                if (emptyCounter > 0) {
                    ret.add(new IntPair(y - emptyCounter, y));
                    emptyCounter = 0;
                }

            } else if (!topBuffer) {
                emptyCounter++;
            }
        }

        return ret;
    }

    /**
     * Gets the conjoined columns
     *
     * @return The lines with the IntPair being <fromX, toX>
     */
    public List<IntPair> getWholeColumns() {
        var ret = new ArrayList<IntPair>();
        var width = this.image.getWidth();
        var height = this.image.getHeight();

        var emptyCounter = 0;
        var leftBuffer = true;
        for (int x = 0; x < width; x++) {
            int finalX = x;
            var empty = IntStream.range(0, height).noneMatch(y -> this.image.getValue(finalX, y));
            if (empty) {
                if (leftBuffer) {
                    leftBuffer = false;
                    continue;
                }

                if (emptyCounter > 0) {
                    ret.add(new IntPair(x - emptyCounter, x));
                    emptyCounter = 0;
                }

            } else if (!leftBuffer) {
                emptyCounter++;
            }
        }

        return ret;
    }

    public IntPair getVerticalPadding() {
        var topPadding = 0;
        var bottomPadding = 0;

        var height = this.image.getHeight();
        var width = this.image.getWidth();
        for (int y = 0; y < height / 2; y++) {
            int finalY = y;
            var empty = IntStream.range(0, width).noneMatch(x -> this.image.getValue(x, finalY));
            if (empty) topPadding++;
            else break;
        }

        for (int y = height - 1; y > height / 2; y--) {
            int finalY = y;
            var empty = IntStream.range(0, width).noneMatch(x -> this.image.getValue(x, finalY));
            if (empty) bottomPadding++;
            else break;
        }

        return new IntPair(topPadding, bottomPadding);
    }

}
