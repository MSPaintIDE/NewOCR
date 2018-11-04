package com.uddernetworks.newocr;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class SearchImage {

    private boolean[][] values;
    private int width;
    private int height;

    public SearchImage(boolean[][] values, int width, int height) {
        this.values = values;
        this.width = width;
        this.height = height;
    }

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

    public boolean hasValue(int x, int y) {
        if (x < 0 || y < 0 || y >= values.length || x >= values[y].length) return false;
        boolean value = values[y][x];
        values[y][x] = false;
        return value;
    }

    public boolean getValue(int x, int y) {
        return values[y][x];
    }

    public boolean[][] getValues() {
        return values;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
