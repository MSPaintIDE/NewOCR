package com.uddernetworks.newocr.altsearcher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchCharacter {

    private boolean[][] values;
    private int x;
    private int y;
    private int width;
    private int height;

    public SearchCharacter(List<Map.Entry<Integer, Integer>> coordinates) {
        List<Integer> xStream = coordinates.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Integer> yStream = coordinates.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        int maxX = xStream.stream().max(Integer::compareTo).get();
        int minX = xStream.stream().min(Integer::compareTo).get();

        int maxY = yStream.stream().max(Integer::compareTo).get();
        int minY = yStream.stream().min(Integer::compareTo).get();

        this.x = minX;
        this.y = minY;

        this.width = maxX - minX;
        this.height = maxY - minY;

        values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        coordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);
    }

    public boolean isProbablyDot() {
        return width == height
                || width - 1 == height
                || width == height - 1;
    }

    public void addDot(List<Map.Entry<Integer, Integer>> dotCoordinates) {
        boolean[][] values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        int yOffset = this.height - this.values.length + 1;

        for (int y = 0; y < this.values.length; y++) {
            System.arraycopy(this.values[y], 0, values[y + yOffset], 0, this.values[0].length);
        }

        dotCoordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);

        this.values = values;
    }

    public void drawTo(BufferedImage image) {

        // Top
        Main.colorRow(image, Color.MAGENTA, y, x, width);

        // Bottom
        Main.colorRow(image, Color.MAGENTA, y + height, x, width + 1);

        // Left
        Main.colorColumn(image, Color.MAGENTA, x, y, height);

        // Right
        Main.colorColumn(image, Color.MAGENTA, x + width, y, height);
    }

    public boolean[][] getValues() {
        return values;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
