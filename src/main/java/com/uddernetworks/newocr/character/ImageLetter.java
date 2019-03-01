package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.utils.IntPair;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An object to contain data from characters directly scanned from an image.
 */
public class ImageLetter {

    private char letter;
    private int x;
    private int y;
    private int width;
    private int height;
    private double averageWidth;
    private double averageHeight;
    private double ratio;
    private boolean[][] values;
    private List<IntPair> coordinates;
    private Object data;
    private double maxCenter;
    private double minCenter;

    /**
     * Creates an ImageLetter from collected data.
     * @param x The X coordinate of this character
     * @param y The Y coordinate of this character
     * @param width The width of this character
     * @param height The height of this character
     * @param ratio The width/height ratio of this character
     */
    public ImageLetter(char letter, int x, int y, int width, int height, double averageWidth, double averageHeight, double ratio) {
        this(letter, x, y, width, height, averageWidth, averageHeight, ratio, null);
    }

    /**
     * Creates an ImageLetter from collected data.
     *
     * @param x The X coordinate of this character
     * @param y The Y coordinate of this character
     * @param width The width of this character
     * @param height The height of this character
     * @param ratio The width/height ratio of this character
     * @param coordinates The data coordinates of this character (In form of [Black, Total])
     */
    public ImageLetter(char letter, int x, int y, int width, int height, double averageWidth, double averageHeight, double ratio, List<IntPair> coordinates) {
        this.letter = letter;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.averageWidth = averageWidth;
        this.averageHeight = averageHeight;
        this.ratio = ratio;
        this.coordinates = coordinates;
    }

    /**
     * Merges the given {@link ImageLetter} with the current one, possibly changing width, height, X and Y values, along with
     * combining the current and given {@link ImageLetter}'s coordinates and values (Accessible via {@link ImageLetter#getCoordinates()} and {@link ImageLetter#getValues()} respectively)
     *
     * @param imageLetter The {@link ImageLetter} to merge into the current one
     */
    public void merge(ImageLetter imageLetter) {
        this.coordinates = Stream.of(this.coordinates, imageLetter.coordinates).flatMap(List::stream).collect(Collectors.toList());
        int maxX = Integer.MIN_VALUE, minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;

        for (var pair : this.coordinates) {
            int key = pair.getKey(), value = pair.getValue();

            if (key > maxX) {
                maxX = key;
            }

            if (key < minX) {
                minX = key;
            }

            if (value > maxY) {
                maxY = value;
            }

            if (value < minY) {
                minY = value;
            }
        }

        this.x = minX;
        this.y = minY;

        this.width = maxX - minX;
        this.height = maxY - minY;

        values = new boolean[this.height + 1][];

        for (int i = 0; i < values.length; i++) {
            values[i] = new boolean[width + 1];
        }

        this.coordinates.forEach(pair -> values[pair.getValue() - this.y][pair.getKey() - this.x] = true);
    }

    /**
     * Gets the X coordinate of this character.
     *
     * @return The X coordinate of this character
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the X coordinate of this character.
     *
     * @param x The X coordinate of this character
     */
    public void setX(int x) {
        this.x = x;
    }


    /**
     * Gets the Y coordinate of this character.
     *
     * @return The Y coordinate of this character
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the Y coordinate of this character.
     *
     * @param y The Y coordinate of this character
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Gets the width of this character.
     *
     * @return The width of this character
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of this character
     *
     * @param width The width of this character
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of this character.
     *
     * @return The height of this character
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of this character.
     *
     * @param height The height of this character
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the average width of this character's trained data.
     *
     * @return The average width of the character
     */
    public double getAverageWidth() {
        return averageWidth;
    }

    /**
     * Sets the average width of this character's trained data.
     *
     * @param averageWidth The average width of the character
     */
    public void setAverageWidth(double averageWidth) {
        this.averageWidth = averageWidth;
    }

    /**
     * Gets the average height of this character's trained data.
     *
     * @return The average height of the character
     */
    public double getAverageHeight() {
        return averageHeight;
    }

    /**
     * Sets the average height of this character's trained data.
     *
     * @param averageHeight The average height of the character
     */
    public void setAverageHeight(double averageHeight) {
        this.averageHeight = averageHeight;
    }

    /**
     * Gets the width/height ratio of this character.
     *
     * @return The width/height ratio of this character
     */
    public double getRatio() {
        return ratio;
    }

    /**
     * Sets the width/height ratio of this character.
     *
     * @param ratio The width/height ratio of this character
     */
    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    /**
     * Gets the data coordinates of this character in form of [Black, Total]
     *
     * @return The data coordinates of this character
     */
    public List<IntPair> getCoordinates() {
        return coordinates;
    }

    /**
     * Gets the character value found for this character.
     *
     * @return The character value found for this character
     */
    public char getLetter() {
        return this.letter;
    }

    /**
     * Sets the letter value for this character.
     *
     * @param letter The character value for this character.
     */
    public void setLetter(char letter) {
        this.letter = letter;
    }

    /**
     * Gets any data set to the {@link ImageLetter} object, useful for storing any needed data about the character to be
     * used in the future.
     *
     * @return Data set to the character
     */
    public <T> Optional<T> getData(Class<T> clazz) {
        return clazz.isInstance(data) ? Optional.of(clazz.cast(data)) : Optional.empty();
    }

    /**
     * Gets the raw data Object set to the {@link ImageLetter} object, useful for storing any needed data about the
     * character to be used in the future.
     *
     * @return Data set to the character
     */
    public Optional<Object> getData() {
        return Optional.of(this.data);
    }

    /**
     * Sets any data to the {@link ImageLetter} object, useful for storing any needed data about the character to be
     * used in the future.
     *
     * @param data The data to be set
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Gets the black (true) and white (false) pixels of the scanned character.
     *
     * @return The grid of black or white values
     */
    public boolean[][] getValues() {
        return values;
    }

    /**
     * Sets the black (true) and white (false) pixels of the scanned character.
     *
     * @param values The grid of black or white values. Will return `null` for spaces
     */
    public void setValues(boolean[][] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return String.valueOf(getLetter());
    }

    /**
     * Gets the minimum relative center value from the top of the character found in the training set for this font size.
     *
     * @return The minimum relative center value from the top of the character found in the training set for this font size
     */
    public double getMinCenter() {
        return minCenter;
    }

    /**
     * Sets the minimum relative center value from the top of the character found in the training set for this font size.
     *
     * @param minCenter The minimum relative center value from the top of the character found in the training set for this font size
     */
    public void setMinCenter(double minCenter) {
        this.minCenter = minCenter;
    }

    /**
     * Gets the maximum relative center value from the top of the character found in the training set for this font size.
     *
     * @return The maximum relative center value from the top of the character found in the training set for this font size
     */
    public double getMaxCenter() {
        return maxCenter;
    }

    /**
     * Sets the maximum relative center value from the top of the character found in the training set for this font size.
     *
     * @param maxCenter  The maximum relative center value from the top of the character found in the training set for this font size
     */
    public void setMaxCenter(double maxCenter) {
        this.maxCenter = maxCenter;
    }
}
