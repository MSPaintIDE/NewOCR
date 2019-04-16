package com.uddernetworks.newocr.character;

/**
 * Used for storage of data to go into our from the database.
 */
public class DatabaseCharacter extends Character {

    private double[] data = new double[17];
    private double avgWidth;
    private double avgHeight;
    private double minCenter; // Pixels from the top
    private double maxCenter; // Pixels from the top

    /**
     * Creates a DatabaseCharacter from the given character with a modifier of 0.
     *
     * @param letter The letter of the object
     */
    public DatabaseCharacter(char letter) {
        super(letter);
    }

    /**
     * Creates a DatabaseCharacter from the given character.
     *
     * @param letter The letter of the object
     * @param modifier The modifier of the character
     */
    public DatabaseCharacter(char letter, int modifier) {
        super(letter, modifier);
    }

    /**
     * Gets the 16 sectioned percentages for the stored character.
     *
     * @return An array of percentages &lt;= 1 with a length of 17
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DatabaseCharacter) {
            var characterObj = ((DatabaseCharacter) obj);
            return characterObj.letter == this.letter && characterObj.modifier == modifier;
        }

        return false;
    }

}
