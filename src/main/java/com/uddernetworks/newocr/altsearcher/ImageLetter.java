package com.uddernetworks.newocr.altsearcher;

import java.util.List;
import java.util.Map;

public class ImageLetter {

    private DatabaseCharacter databaseCharacter;
    private int x;
    private int y;
    private int width;
    private int height;
    private double ratio;
    private List<Map.Entry<Integer, Integer>> segments;

    public ImageLetter(DatabaseCharacter databaseCharacter) {
        this.databaseCharacter = databaseCharacter;
    }

    public ImageLetter(DatabaseCharacter databaseCharacter, int x, int y, int width, int height, double ratio, List<Map.Entry<Integer, Integer>> segments) {
        this.databaseCharacter = databaseCharacter;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.ratio = ratio;
        this.segments = segments;
    }

    public DatabaseCharacter getDatabaseCharacter() {
        return databaseCharacter;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public List<Map.Entry<Integer, Integer>> getSegments() {
        return segments;
    }

    public char getLetter() {
        return this.databaseCharacter.getLetter();
    }
}
