package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.LetterMeta;
import com.uddernetworks.newocr.character.SearchCharacter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * This is an object meant for storing the data for characters in the training stage.
 */
public class TrainedCharacterData {

    private char value;
    private int modifier = 0;
    private boolean hasDot;
    private double widthAverage;
    private double heightAverage;
    private double[] segmentPercentages;
    private double minCenter = -1;
    private double maxCenter = -1;
    private double sizeRatio = -1; //        Width / Height
    private boolean empty = true;
    private LetterMeta letterMeta = LetterMeta.NONE;
    private List<double[]> recalculatingList = new ArrayList<>();
    private DoubleList recalculatingWidths = new DoubleArrayList();
    private DoubleList recalculatingHeights = new DoubleArrayList();

    /**
     * Creates a {@link TrainedCharacterData} from a character value with a modifier of 0.
     *
     * @param value The known character value
     */
    public TrainedCharacterData(char value) {
        this.value = value;
    }

    /**
     * Creates a {@link TrainedCharacterData} from a character value with a given modifier value.
     *
     * @param value The known character value
     * @param modifier The modifier for the character
     */
    public TrainedCharacterData(char value, int modifier) {
        this.value = value;
        this.modifier = modifier;
    }

    /**
     * Gets the assigned character value.
     *
     * @return The assigned character value
     */
    public char getValue() {
        return value;
    }

    /**
     * Gets the modifier for the character.
     *
     * @return The character's modifier
     */
    public int getModifier() {
        return modifier;
    }

    /**
     * Sets the modifier for the character.
     *
     * @param modifier The modifier to set
     */
    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    /**
     * Gets the calculated array of the percentages for each section. The array is always 16 elements with everything
     * being <= 1.
     *
     * @return An array of percentages
     */
    public double[] getSegmentPercentages() {
        return segmentPercentages;
    }

    /**
     * Gets if the trained character has a dot.
     *
     * @return If the trained character has a dot
     */
    public boolean hasDot() {
        return hasDot;
    }

    /**
     * Sets if the trained character has a dot.
     *
     * @param hasDot If the trained character has a dot
     */
    public void setHasDot(boolean hasDot) {
        this.hasDot |= hasDot;
    }

    /**
     * Gets the width/height size ratio.
     *
     * @return The width/height size ratio
     */
    public double getSizeRatio() {
        return sizeRatio;
    }

    /**
     * Adds the given width and height variables to the internal list to be put into calculations upon invoking
     * {@link #finishRecalculations()}.
     *
     * @param width The width of the character
     * @param height The height of the character
     */
    public void recalculateTo(double width, double height) {
        this.empty = false;

        recalculatingWidths.add(width);
        recalculatingHeights.add(height);
    }

    /**
     * Does the same thing as {@link #recalculateTo(double, double)} but with a {@link SearchCharacter}, and it
     * includes its percentages as well.
     *
     * @param searchCharacter The {@link SearchCharacter} to be recalculated to
     */
    public void recalculateTo(SearchCharacter searchCharacter) {
        this.empty = false;
        double[] segmentPercentages = searchCharacter.getSegmentPercentages();

        recalculatingList.add(segmentPercentages);
        if (searchCharacter.getWidth() != 0 && searchCharacter.getHeight() != 0) {
            recalculatingWidths.add((double) searchCharacter.getWidth());
            recalculatingHeights.add((double) searchCharacter.getHeight());
        }
    }

    /**
     * Calculates everything based on the data inserted by {@link #recalculateTo(double, double)} and
     * {@link #recalculateTo(SearchCharacter)} by averaging the width and heights provided, and averaging the
     * percentages retrieved from {@link SearchCharacter}s.
     */
    public void finishRecalculations() {
        OptionalDouble widthAverageOptional = recalculatingWidths.stream().mapToDouble(t -> t).average();
        this.widthAverage = widthAverageOptional.orElse(0D);

        OptionalDouble heightAverageOptional = recalculatingHeights.stream().mapToDouble(t -> t).average();
        this.heightAverage = heightAverageOptional.orElse(0D);

        this.sizeRatio = this.heightAverage != 0 ? this.widthAverage / this.heightAverage : 0;

        if (value == ' ') {
            return;
        }

        this.segmentPercentages = new double[8 + 9];

        for (int i = 0; i < 8 + 9; i++) {
            int finalI = i;
            this.segmentPercentages[i] = recalculatingList.stream().mapToDouble(t -> t[finalI]).average().orElse(0D);
        }
    }

    /**
     * If the given value is less than the minimum center, it's the new minimum center, and if it's bigger than the
     * maximum center, it will be the new maximum center. (Retrievable via {@link #getMaxCenter() and {
     * @link #getMinCenter()}}).
     *
     * @param center The value to add as center
     */
    public void recalculateCenter(double center) {
        if (minCenter == -1 && maxCenter == -1) {
            minCenter = center;
            maxCenter = center;
        } else {
            if (center > maxCenter) {
                maxCenter = center;
            }

            if (center < minCenter) {
                minCenter = center;
            }
        }
    }

    /**
     * Gets the average width for everything trained with this object.
     *
     * @return The average width. Will return 0 if {@link #finishRecalculations()} has not been invoked.
     */
    public double getWidthAverage() {
        return widthAverage;
    }

    /**
     * Gets the average height for everything trained with this object.
     *
     * @return The average height. Will return 0 if {@link #finishRecalculations()} has not been invoked.
     */
    public double getHeightAverage() {
        return heightAverage;
    }

    /**
     * Gets the minimum center value of all the training data used.
     *
     * @return The minimum center value of all the training data used
     */
    public double getMinCenter() {
        return minCenter;
    }

    /**
     * Gets the maximum center value of all the training data used.
     *
     * @return The maximum center value of all the training data used
     */
    public double getMaxCenter() {
        return maxCenter;
    }

    /**
     * Gets if anything has been recalcuated/prepared to be recalculated to the character, e.g. by using
     * {@link #recalculateTo(double, double)} or {@link #recalculateTo(SearchCharacter)}.
     *
     * @return If anything has been recalculated
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Gets the letter meta of the character.
     *
     * @return The letter meta of the character
     */
    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    /**
     * Sets the letter meta of the character.
     *
     * @param letterMeta The letter meta of the character
     */
    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
