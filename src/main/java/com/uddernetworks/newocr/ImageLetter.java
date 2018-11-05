package com.uddernetworks.newocr;

import java.util.List;
import java.util.Map;

/**
 * An object to contain data from characters directly scanned from an image.
 */
public class ImageLetter {

    private DatabaseCharacter databaseCharacter;
    private int x;
    private int y;
    private int width;
    private int height;
    private double ratio;
    private List<Map.Entry<Integer, Integer>> segments;

    /**
     * Creates an ImageLetter from collected data.
     * @param databaseCharacter The {@link DatabaseCharacter} that is decided to be related to this character
     * @param x The X coordinate of this character
     * @param y The Y coordinate of this character
     * @param width The width of this character
     * @param height The height of this character
     * @param ratio The width/height ratio of this character
     * @param segments The data segments of this character (In form of [Black, Total])
     */
    public ImageLetter(DatabaseCharacter databaseCharacter, int x, int y, int width, int height, double ratio, List<Map.Entry<Integer, Integer>> segments) {
        this.databaseCharacter = databaseCharacter;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.ratio = ratio;
        this.segments = segments;
    }

    /**
     * Gets the {@link DatabaseCharacter} found.
     * @return The {@link DatabaseCharacter} found
     */
    public DatabaseCharacter getDatabaseCharacter() {
        return databaseCharacter;
    }

    /**
     * Gets the X coordinate of this character.
     * @return The X coordinate of this character
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the X coordinate of this character.
     * @param x The X coordinate of this character
     */
    public void setX(int x) {
        this.x = x;
    }


    /**
     * Gets the Y coordinate of this character.
     * @return The Y coordinate of this character
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the Y coordinate of this character.
     * @param y The Y coordinate of this character
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Gets the width of this character.
     * @return The width of this character
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of this character
     * @param width The width of this character
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of this character.
     * @return The height of this character
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of this character.
     * @param height The height of this character
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the width/height ratio of this character.
     * @return The width/height ratio of this character
     */
    public double getRatio() {
        return ratio;
    }

    /**
     * Sets the width/height ratio of this character.
     * @param ratio The width/height ratio of this character
     */
    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    /**
     * Gets the data segments of this character in form of [Black, Total]
     * @return The data segments of this character
     */
    public List<Map.Entry<Integer, Integer>> getSegments() {
        return segments;
    }

    /**
     * Gets the character value found for this character.
     * @return The character value found for this character
     */
    public char getLetter() {
        return this.databaseCharacter.getLetter();
    }
}
