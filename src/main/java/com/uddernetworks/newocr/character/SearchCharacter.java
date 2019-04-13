package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An object meant to store characters directly scanned from an image and that is being searched for/mutated.
 */
public class SearchCharacter implements Comparable<SearchCharacter> {

    private char knownChar = '?';
    private int modifier = 0;
    private boolean[][] values;
    private List<IntPair> coordinates;
    private int x;
    private int y;
    private int width;
    private int height;
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

    public void merge(SearchCharacter searchCharacter) {
        this.coordinates.addAll(searchCharacter.coordinates);
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

        coordinates.forEach(pair -> values[pair.getValue() - this.y][pair.getKey() - this.x] = true);
    }

    /**
     * Gets the raw grid of boolean values from the image of this character.
     *
     * @return The raw grid of boolean values from the image of this character
     */
    public boolean[][] getValues() {
        return values;
    }

    /**
     * Gets the coordinates of the character.
     *
     * @return The coordinates
     */
    public List<IntPair> getCoordinates() {
        return coordinates;
    }

    /**
     * Gets the X position of the character.
     *
     * @return The X position of ths character
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y position of the character.
     *
     * @return The Y position of ths character
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the width of the character.
     *
     * @return The width of ths character
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the character.
     *
     * @return The height of ths character
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the X position of the character.
     *
     * @param x the X position to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the Y position of the character.
     *
     * @param y the Y position to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Sets the width of the character.
     *
     * @param width The width of the character
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the character.
     *
     * @param height The height of the character
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets if the given coordinate is within the bounds of this character.
     *
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return If the coordinate is within this character
     */
    public boolean isInBounds(int x, int y) {
        return x <= this.x + this.width
                && x >= this.x
                && y <= this.y + this.height
                && y >= this.y;
    }

    /**
     * Gets if another {@link SearchCharacter} is overlapping the current {@link SearchCharacter} at all.
     *
     * @param searchCharacter The {@link SearchCharacter} to check for overlapping
     * @return If the given {@link SearchCharacter} is overlapping the current {@link SearchCharacter}
     */
    public boolean isOverlaping(SearchCharacter searchCharacter) {
        if (isInBounds(searchCharacter.getX(), searchCharacter.getY())) return true;
        if (isInBounds(searchCharacter.getX(), searchCharacter.getY() + searchCharacter.getHeight())) return true;
        if (isInBounds(searchCharacter.getX() + searchCharacter.getWidth(), searchCharacter.getY())) return true;
        if (isInBounds(searchCharacter.getX() + searchCharacter.getWidth(), searchCharacter.getY() + searchCharacter.getHeight())) return true;
        return false;
    }

    /**
     * Gets if another {@link SearchCharacter}'s black pixels overlap the current {@link SearchCharacter} at all.
     *
     * @param searchCharacter The {@link SearchCharacter} to check for overlapping
     * @return If the given {@link SearchCharacter} is overlapping the current {@link SearchCharacter}
     */
    public boolean isOverlappingPixels(SearchCharacter searchCharacter) {
        return searchCharacter.coordinates.parallelStream().anyMatch(coordinate -> this.coordinates.contains(coordinate));
//        return !Collections.disjoint(searchCharacter.coordinates, this.coordinates);
    }

    /**
     * Gets if another {@link SearchCharacter} is overlapping the current {@link SearchCharacter} at all in the X axis.
     *
     * @param searchCharacter The {@link SearchCharacter} to check for overlapping
     * @return If the given {@link SearchCharacter} is overlapping the current {@link SearchCharacter}
     */
    public boolean isOverlappingX(SearchCharacter searchCharacter) {
        // Thanks https://nedbatchelder.com/blog/201310/range_overlap_in_two_compares.html :)
        return getX() + getWidth() >= searchCharacter.getX() && searchCharacter.getX() + searchCharacter.getWidth() >= getX();
    }

    /**
     * Gets if another {@link SearchCharacter} is overlapping the current {@link SearchCharacter} at all in the X axis.
     *
     * @param searchCharacter The {@link SearchCharacter} to check for overlapping
     * @return If the given {@link SearchCharacter} is overlapping the current {@link SearchCharacter}
     */
    public boolean isOverlappingY(SearchCharacter searchCharacter) {
        // Thanks https://nedbatchelder.com/blog/201310/range_overlap_in_two_compares.html :)
        return getY() + getHeight() >= searchCharacter.getY() && searchCharacter.getY() + searchCharacter.getHeight() >= getY();
    }

    /**
     * Gets if the given Y position is within the Y bounds of the current character.
     *
     * @param y The Y position to check
     * @return If the given Y position is within the Y bounds of the current character
     */
    public boolean isInYBounds(int y) {
        return y <= this.y + this.height
                && y >= this.y;
    }

    /**
     * Gets if the given Y position is within the X bounds of the current character.
     *
     * @param x The Y position to check
     * @return If the given Y position is within the X bounds of the current character
     */
    public boolean isInXBounds(int x) {
        return x <= this.x + this.width
                && x >= this.x;
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
     * Gets the known character of this object. If it has not been fount yet, it will return `?`.
     *
     * @return The known character
     */
    public char getKnownChar() {
        return knownChar;
    }

    /**
     * Sets the known character.
     *
     * @param knownChar The know character
     */
    public void setKnownChar(char knownChar) {
        this.knownChar = knownChar;
    }

    /**
     * Sets the modifier for the current character.
     *
     * @param modifier The modifier of the character
     */
    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    /**
     * Gets the modifier for the current character.
     *
     * @return The modifier for the current character
     */
    public int getModifier() {
        return modifier;
    }

    @Override
    public int compareTo(SearchCharacter searchCharacter) {
        return x - searchCharacter.x;
    }

    @Override
    public String toString() {
        return String.valueOf(knownChar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.knownChar, this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SearchCharacter)) return false;
        var character = (SearchCharacter) obj;
        return character.knownChar == this.knownChar
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height;
    }

    /**
     * Gets the raw 2D array of values of the character.
     *
     * @return The raw 2D array of values of the character
     */
    public boolean[][] getData() {
        return values;
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
    public SearchCharacter setCenterOffset(double centerOffset) {
        this.centerOffset = centerOffset;
        return this;
    }
}
