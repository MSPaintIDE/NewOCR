package com.uddernetworks.newocr;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

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
     * @param x The X location of the current black pixel
     * @param y The Y location of the current black pixel
     * @param coordinates The mutable list of coordinates that will have each new coordinate added to it
     */
    public void scanFrom(int x, int y, List<Map.Entry<Integer, Integer>> coordinates) {
        if (hasValue(x, y)) {
            coordinates.add(new AbstractMap.SimpleEntry<>(x, y));

            scanFrom(x, y + 1, coordinates);
            scanFrom(x, y - 1, coordinates);
            scanFrom(x + 1, y, coordinates);
            scanFrom(x - 1, y, coordinates);
            scanFrom(x + 1, y + 1, coordinates);
            scanFrom(x + 1, y - 1, coordinates);
            scanFrom(x - 1, y + 1, coordinates);
            scanFrom(x - 1, y - 1, coordinates);
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
}
