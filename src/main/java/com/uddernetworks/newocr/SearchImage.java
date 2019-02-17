package com.uddernetworks.newocr;

import com.uddernetworks.newocr.utils.IntPair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a way to easily get touching coordinates of black pixels.
 * This uses a boolean array, because they will always be either black
 * or white, and it's *much* faster than reading an image's color.
 */
public class SearchImage {

    private boolean[][] values;

    /**
     * Creates a {@link SearchImage} from a boolean grid.
     * @param values The boolean grid of the image
     */
    public SearchImage(boolean[][] values) {
        this.values = values;
    }

    /**
     * Adds the surrounding black pixels from the given coordinate to the coordinates parameter.
     * When a new value sis count, it is set to false to stop duplicates without checking each entry
     * of the map.
     * @param originalX The X location of the current black pixel
     * @param originalY The Y location of the current black pixel
     * @param coordinates The mutable list of coordinates that will have each new coordinate added to it
     */
    public void scanFrom(int originalX, int originalY, List<IntPair> coordinates) {
        if (!hasValue(originalX, originalY)) {
            return;
        }

        var nextProcessing = new ArrayList<IntPair>();
        var processingBuffer = new ArrayList<IntPair>();

        nextProcessing.add(new IntPair(originalX, originalY));

        while (true) {
            for (var pair : nextProcessing) {
                coordinates.add(pair);

                int x = pair.getKey();
                int y = pair.getValue();

                if (hasValue(x, y + 1)) {
                    processingBuffer.add(new IntPair(x, y + 1));
                }

                if (hasValue(x, y - 1)) {
                    processingBuffer.add(new IntPair(x, y - 1));
                }

                if (hasValue(x + 1, y)) {
                    processingBuffer.add(new IntPair(x + 1, y));
                }

                if (hasValue(x - 1, y)) {
                    processingBuffer.add(new IntPair(x - 1, y));
                }

                if (hasValue(x + 1, y + 1)) {
                    processingBuffer.add(new IntPair(x + 1, y + 1));
                }

                if (hasValue(x + 1, y - 1)) {
                    processingBuffer.add(new IntPair(x + 1, y - 1));
                }

                if (hasValue(x - 1, y + 1)) {
                    processingBuffer.add(new IntPair(x - 1, y + 1));
                }

                if (hasValue(x - 1, y - 1)) {
                    processingBuffer.add(new IntPair(x - 1, y - 1));
                }
            }

            if (processingBuffer.isEmpty()) {
                return;
            }

            nextProcessing.clear();
            var temp = nextProcessing;
            nextProcessing = processingBuffer;
            processingBuffer = temp;
        }
    }

    /**
     * Gets the value of the given coordinates. If it's true, it will set the value to false and return true.
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return The value of the coordinates
     */
    public boolean hasValue(int x, int y) {
        if (x < 0 || y < 0 || y >= values.length || x >= values[y].length) return false;
        boolean value = values[y][x];
        values[y][x] = false;
        return value;
    }

    /**
     * Gets the coordinate of the given coordinates. Similar to {@link SearchImage#hasValue(int, int)}, but
     * it performs no mutation to the values grid.
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return The value of the coordinates
     */
    public boolean getValue(int x, int y) {
        return values[y][x];
    }

    /**
     * Gets the raw values grid.
     * @return The raw values grid
     */
    public boolean[][] getValues() {
        return values;
    }

    /**
     * Gets the width computed from the internal value 2D array.
     *
     * @return The width of the image
     */
    public int getWidth() {
        return this.values[0].length;
    }

    /**
     * Gets the height computed from the internal value 2D array.
     *
     * @return The height of the image
     */
    public int getHeight() {
        return this.values.length;
    }

    /**
     * Gets a {@link SearchImage} from the bounds of the current image.
     *
     * @param x The X position to start at
     * @param y The Y position to start at
     * @param width The width of the sub image
     * @param height The height of the sub image
     * @return The inner image from the coordinates given
     */
    public SearchImage getSubimage(int x, int y, int width, int height) {
        var sub = new boolean[height][];

        for (int i = 0; i < height; i++) {
            sub[i] = Arrays.copyOfRange(this.values[i + y], x, x + width);
        }

        return new SearchImage(sub);
    }

    @Override
    public String toString() {
        var ret = new StringBuilder();
        for (var row : this.values) {
            for (var val : row) {
                ret.append(val ? '\uff03' : '\uff0e');
            }
            ret.append('\n');
        }

        return ret.toString();
    }

    public BufferedImage toImage() {
        var image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        var black = Color.BLACK.getRGB();
        var white = Color.WHITE.getRGB();

        for (int y = 0; y < this.values.length; y++) {
            for (int x = 0; x < this.values[0].length; x++) {
                image.setRGB(x, y, this.values[y][x] ? black : white);
            }
        }

        return image;
    }
}
