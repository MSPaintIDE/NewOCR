package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An object meant to store characters directly scanned from an image and that is being searched for/mutated.
 */
public class SearchCharacter extends CoordinateCharacter {

    private List<IntPair> segments = new LinkedList<>();
    private double[] segmentPercentages = new double[8 + 9]; // Percentage <= 1 // First 8 are the normal ones, last 9 are for the grid created
    private Map<String, Double> trainingMeta = new HashMap<>();
    private double centerOffset = 0;

    /**
     * Creates a SearchCharacter from a list of coordinates used by the character.
     *
     * @param coordinates Coordinates used by the character
     */
    public SearchCharacter(List<IntPair> coordinates) {
        this(coordinates, 0, 0);
    }

    /**
     * Creates a SearchCharacter from a list of coordinates used by the character.
     *
     * @param coordinates Coordinates used by the character
     * @param xOffset The X offset of the coordinates
     * @param yOffset The Y offset of the coordinates
     */
    public SearchCharacter(List<IntPair> coordinates, int xOffset, int yOffset) {
        this.coordinates = coordinates;
        int maxX = Integer.MIN_VALUE, minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;

        for (var pair : coordinates) {
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

        this.x = minX + xOffset;
        this.y = minY + yOffset;

        this.width = maxX - minX + 1;
        this.height = maxY - minY + 1;

        values = new boolean[this.height][];

        for (int i = 0; i < values.length; i++) {
            values[i] = new boolean[width];
        }

        coordinates.forEach(pair -> values[pair.getValue() - this.y + yOffset][pair.getKey() - this.x + xOffset] = true);
    }

    /**
     * Creates sections and invokes {@link #addSegment(IntPair)} for each one. This is vital for the use of this object.
     */
    public void applySections() {
        AtomicInteger index = new AtomicInteger();
        OCRUtils.getHorizontalHalf(this.values)
                .flatMap(OCRUtils::getVerticalHalf)
                .forEach(section -> OCRUtils.getDiagonal(section, index.get() == 1 || index.getAndIncrement() == 2).forEach(this::addSegment));

        OCRUtils.getHorizontalThird(this.values).forEach(values ->
                OCRUtils.getVerticalThird(values).forEach(this::addSegment));
    }

    /**
     * Performs calculations for the sections added by {@link #addSegment(IntPair)}, getting their <= 1 percentages
     * accessible from {@link #getSegmentPercentages()}. This must be invoked after {@link #applySections()}.
     */
    public void analyzeSlices() {
        AtomicInteger temp = new AtomicInteger();
        this.segmentPercentages = new double[segments.size()];
        this.segments.forEach((entry) -> {
            double amountTrue = entry.getKey();
            double total = entry.getValue();

            double val = total == 0 ? 1 : amountTrue / total;

            this.segmentPercentages[temp.getAndIncrement()] = val;
        });
    }

    /**
     * Adds a data segment to be calculated in the future. The segments may be fetched via {@link #getSegments()}.
     *
     * @param entry The data segment in the format of [total black, size of segment]
     */
    public void addSegment(IntPair entry) {
        this.segments.add(entry);
    }

    /**
     * Gets the raw segments added via {@link #addSegment(IntPair)} where the Entry format is
     * [total black, size of segment].
     *
     * @return The raw segments
     */
    public List<IntPair> getSegments() {
        return segments;
    }

    /**
     * Gets the raw segment percentages all <= 1. This will return an empty array until {@link #applySections()} and
     * {@link #analyzeSlices()} have been invoked.
     *
     * @return The raw array of segment percentages with a length of 17
     */
    public double[] getSegmentPercentages() {
        return this.segmentPercentages;
    }

    /**
     * Gets the training meta with the given name. This contains data such as separation of the dots of an i, data on
     * the holes of a %, etc.
     *
     * @param name The name of the training data
     * @return The value of the training data
     */
    public OptionalDouble getTrainingMeta(String name) {
        return this.trainingMeta.containsKey(name) ? OptionalDouble.of(this.trainingMeta.get(name)) : OptionalDouble.empty();
    }

    /**
     * Sets the training data with a given name.
     *
     * @param name The name of the data
     * @param data The data to set
     */
    public void setTrainingMeta(String name, double data) {
        this.trainingMeta.put(name, data);
    }

    /**
     * Gets the amount away a character is from the center of the line. This isn't useful for detecting single
     * characters.
     *
     * @return The offset of the character
     */
    public double getCenterOffset() {
        return centerOffset;
    }

    /**
     * Sets the amount away a character is from the center of the line. This isn't useful for detecting single
     * characters.
     *
     * @param centerOffset The offset of the character to set
     */
    public void setCenterOffset(double centerOffset) {
        this.centerOffset = centerOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.letter, this.x, this.y, this.width, this.height, this.segments, this.segmentPercentages, this.trainingMeta, this.centerOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SearchCharacter)) return false;
        var character = (SearchCharacter) obj;
        return character.letter == this.letter
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height
                && character.segments == this.segments
                && character.segmentPercentages == this.segmentPercentages
                && character.trainingMeta == this.trainingMeta
                && character.centerOffset == this.centerOffset;
    }
}
