package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.LetterMeta;

/**
 * Used for storage of data to go into our from the database.
 */
public class DatabaseCharacter {

    private char letter;
    private int modifier;
    private double[] data = new double[17];
    private double avgWidth;
    private double avgHeight;
    private double minCenter; // Pixels from the top
    private double maxCenter; // Pixels from the top
    private boolean hasDot;
    private LetterMeta letterMeta;

    /**
     * Creates a DatabaseCharacter from the given character with a modifier of 0.
     *
     * @param letter The letter of the object
     */
    public DatabaseCharacter(char letter) {
        this(letter, 0);
    }

    /**
     * Creates a DatabaseCharacter from the given character.
     *
     * @param letter The letter of the object
     */
    public DatabaseCharacter(char letter, int modifier) {
        this.letter = letter;
        this.modifier = modifier;
    }

    /**
     * Gets the letter for the current DatabaseCharacter.
     *
     * @return The current letter
     */
    public char getLetter() {
        return letter;
    }

    /**
     * Gets the modifier of the letter.
     *
     * @return The modifier
     */
    public int getModifier() {
        return modifier;
    }

    /**
     * Gets the 16 sectioned percentages for the stored character.
     *
     * @return An array of percentages <= 1 with a length of 17
     */
    public double[] getData() {
        return data;
    }

    /**
     * Sets a percentage value to the data.
     *
     * @param index The index of the data to set
     * @param data The percentage of filled in pixels found
     */
    public void addDataPoint(int index, double data) {
        this.data[index] = data;
    }

    /**
     * Sets multiple used data points.
     *
     * @param avgWidth The average width across all used characters in the font sizes
     * @param avgHeight The average height across all used characters in the font sizes
     * @param minCenter The minimum relative center value in the training ste for this character and font size
     * @param maxCenter The maximum relative center value in the training ste for this character and font size
     */
    public void setData(double avgWidth, double avgHeight, double minCenter, double maxCenter) {
        this.avgWidth = avgWidth;
        this.avgHeight = avgHeight;
        this.minCenter = minCenter;
        this.maxCenter = maxCenter;
    }

    /**
     * Gets the average width of the character.
     *
     * @return The average width of the character
     */
    public double getAvgWidth() {
        return avgWidth;
    }

    /**
     * Gets the average height of the character.
     *
     * @return The average height of the character
     */
    public double getAvgHeight() {
        return avgHeight;
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
     * Gets the maximum relative center value from the top of the character found in the training set for this font size.
     *
     * @return The maximum relative center value from the top of the character found in the training set for this font size
     */
    public double getMaxCenter() {
        return maxCenter;
    }

    /**
     * Sets if the current character has a dot at all in it.
     *
     * @param hasDot If the current character has a dot at all in it
     */
    public void setHasDot(boolean hasDot) {
        this.hasDot = hasDot;
    }

    /**
     * Gets if the current character has a dot at all in it.
     *
     * @return If the current character has a dot at all in it
     */
    public boolean hasDot() {
        return this.hasDot;
    }

    /**
     * Gets the {@link LetterMeta} of the current character.
     *
     * @return The {@link LetterMeta} of the current character
     */
    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    /**
     * Sets the {@link LetterMeta} of the current character.
     */
    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }

    @Override
    public String toString() {
        return String.valueOf(this.letter);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DatabaseCharacter) {
            var characterObj = ((DatabaseCharacter) obj);
            return characterObj.letter == this.letter && characterObj.modifier == modifier;
        }

        return false;
    }

}
