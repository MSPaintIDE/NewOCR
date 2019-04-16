package com.uddernetworks.newocr.character;

import java.util.LinkedList;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * This is an object meant for storing the data for characters in the training stage.
 */
public class TrainedCharacterData extends Character {

    private double widthAverage;
    private double heightAverage;
    private double[] segmentPercentages;
    private double minCenter = -1;
    private double maxCenter = -1;
    private double sizeRatio = -1; //        Width / Height
    private boolean empty = true;
    private LinkedList<double[]> recalculatingList = new LinkedList<>();
    private LinkedList<Double> recalculatingWidths = new LinkedList<>();
    private LinkedList<Double> recalculatingHeights = new LinkedList<>();

    /**
     * Creates a {@link TrainedCharacterData} from a character letter with a modifier of 0.
     *
     * @param letter The known character letter
     */
    public TrainedCharacterData(char letter) {
        super(letter);
    }

    /**
     * Creates a {@link TrainedCharacterData} from a character value with a given modifier value.
     *
     * @param letter The known character value
     * @param modifier The modifier for the character
     */
    public TrainedCharacterData(char letter, int modifier) {
        super(letter, modifier);
    }

    /**
     * Gets the calculated array of the percentages for each section. The array is always 16 elements with everything
     * being &lt;= 1.
     *
     * @return An array of percentages
     */
    public double[] getSegmentPercentages() {
        return segmentPercentages;
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
     * Removed the last x entries added by {@link TrainedCharacterData#recalculateTo(SearchCharacter)}.
     *
     * @param amount The amount of entries to remove
     */
    public void undoLastRecalculations(int amount) {
        boolean removingList = amount < this.recalculatingList.size();
        boolean removingHeights = amount < this.recalculatingHeights.size();
        boolean removingWidths = amount < this.recalculatingWidths.size();

        if (!removingList) this.recalculatingList.clear();
        if (!removingHeights) this.recalculatingHeights.clear();
        if (!removingWidths) this.recalculatingWidths.clear();

        for (int i = 0; i < amount; i++) {
            if (removingList) this.recalculatingList.removeLast();
            if (removingHeights) this.recalculatingHeights.removeLast();
            if (removingWidths) this.recalculatingWidths.removeLast();
        }

        finishRecalculations();
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

        if (this.letter == ' ') {
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
     * maximum center, it will be the new maximum center. (Retrievable via {@link #getMaxCenter() and
     * {@link #getMinCenter()}}).
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

    @Override
    public int hashCode() {
        return Objects.hash(this.letter, this.x, this.y, this.width, this.height, this.widthAverage, this.heightAverage, this.segmentPercentages, this.minCenter, this.maxCenter, this.sizeRatio, this.empty);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TrainedCharacterData)) return false;
        var character = (TrainedCharacterData) obj;
        return character.letter == this.letter
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height
                && character.widthAverage == this.widthAverage
                && character.heightAverage == this.heightAverage
                && character.segmentPercentages == this.segmentPercentages
                && character.minCenter == this.minCenter
                && character.maxCenter == this.maxCenter
                && character.sizeRatio == this.sizeRatio
                && character.empty == this.empty
                && character.recalculatingList == this.recalculatingList
                && character.recalculatingWidths == this.recalculatingWidths
                && character.recalculatingHeights == this.recalculatingHeights;
    }

}
