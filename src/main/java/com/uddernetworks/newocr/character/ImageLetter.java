package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.utils.IntPair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An object to contain data from characters directly scanned from an image.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class ImageLetter extends CoordinateCharacter {

    private double averageWidth;
    private double averageHeight;
    private double ratio;
    private Object data;
    private double maxCenter;
    private double minCenter;

    private List<Object2DoubleMap.Entry<ImageLetter>> closestMatches = new ArrayList<>();

    /**
     * Creates an ImageLetter from collected data.
     *
     * @param letter        The letter value
     * @param modifier      The modifier of the letter
     * @param x             The X coordinate of this character
     * @param y             The Y coordinate of this character
     * @param width         The width of this character
     * @param height        The height of this character
     * @param averageWidth  The average width of the character
     * @param averageHeight The average height of the character
     * @param ratio         The width/height ratio of this character
     */
    public ImageLetter(char letter, int modifier, int x, int y, int width, int height, double averageWidth, double averageHeight, double ratio) {
        this(letter, modifier, x, y, width, height, averageWidth, averageHeight, ratio, null);
    }

    /**
     * Creates an ImageLetter from collected data.
     *
     * @param letter        The letter value
     * @param modifier      The modifier of the letter
     * @param x             The X coordinate of this character
     * @param y             The Y coordinate of this character
     * @param width         The width of this character
     * @param height        The height of this character
     * @param averageWidth  The average width of the character
     * @param averageHeight The average height of the character
     * @param ratio         The width/height ratio of this character
     * @param coordinates   The data coordinates of this character (In form of [Black, Total])
     */
    public ImageLetter(char letter, int modifier, int x, int y, int width, int height, double averageWidth, double averageHeight, double ratio, List<IntPair> coordinates) {
        this.letter = letter;
        this.modifier = modifier;
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
     * Gets any data set to the {@link ImageLetter} object, useful for storing any needed data about the character to be
     * used in the future.
     *
     * @param clazz The class type of the data, only used for getting the returning type
     * @param <T>   The type of the data
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
     * @param maxCenter The maximum relative center value from the top of the character found in the training set for this font size
     */
    public void setMaxCenter(double maxCenter) {
        this.maxCenter = maxCenter;
    }

    public List<Object2DoubleMap.Entry<ImageLetter>> getClosestMatches() {
        return closestMatches;
    }

    public void setClosestMatches(List<Object2DoubleMap.Entry<ImageLetter>> closestMatches) {
        this.closestMatches = closestMatches;
    }

    public void setNextClosest() {
        copyProperties(this.closestMatches.remove(0).getKey());
    }

    /**
     * Copies the properties from the given {@link ImageLetter} to the current one.
     *
     * @param imageLetter The {@link ImageLetter} to copy data from
     */
    public void copyProperties(ImageLetter imageLetter) {
        this.letter = imageLetter.letter;
        this.modifier = imageLetter.modifier;
        this.x = imageLetter.x;
        this.y = imageLetter.y;
        this.width = imageLetter.width;
        this.height = imageLetter.height;
        this.averageWidth = imageLetter.averageWidth;
        this.averageHeight = imageLetter.averageHeight;
        this.ratio = imageLetter.ratio;
        this.values = imageLetter.values;
        this.coordinates = imageLetter.coordinates;
        this.data = imageLetter.data;
        this.maxCenter = imageLetter.maxCenter;
        this.minCenter = imageLetter.minCenter;
        this.amountOfMerges = imageLetter.amountOfMerges;
    }

    @Override
    public String toString() {
        return getLetter() + "_" + getModifier();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.letter, this.x, this.y, this.width, this.height, this.averageWidth, this.averageHeight, this.ratio, this.data, this.maxCenter, this.minCenter);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImageLetter)) return false;
        var character = (ImageLetter) obj;
        return character.letter == this.letter
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height
                && character.averageWidth == this.averageWidth
                && character.averageHeight == this.averageHeight
                && character.ratio == this.ratio
                && character.data == this.data
                && character.maxCenter == this.maxCenter
                && character.minCenter == this.minCenter;
    }
}
