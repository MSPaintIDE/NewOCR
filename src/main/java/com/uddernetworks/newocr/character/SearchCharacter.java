package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.LetterMeta;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An object meant to store characters directly scanned from an image and that is being searched for/mutated.
 */
public class SearchCharacter implements Comparable<SearchCharacter> {

    private char knownChar = '?';
    private boolean[][] values;
    private List<IntPair> coordinates;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean hasDot;
    private LetterMeta letterMeta = LetterMeta.NONE;
    private List<IntPair> segments = new LinkedList<>();
    private double[] segmentPercentages = new double[8 + 9]; // Percentage <= 1 // FIrst 8 are the normal ones, last 9 are for the grid created

    /**
     * Creates a SearchCharacter from a list of coordinates used by the character.
     * @param coordinates Coordinates used by the character
     */
    public SearchCharacter(List<IntPair> coordinates) {
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
     * Gets if the character is probably a dot.
     * @return If the character is probably a dot
     */
    public boolean isProbablyDot() {
        int diff = Math.max(width, height) - Math.min(width, height);
        return diff <= 3;
    }

    /**
     * Gets if the character is probably a circle of a percent.
     * @return If the character is probably a circle of a percent
     */
    public boolean isProbablyCircleOfPercent() {
        double ratio = (double) width + 1 / (double) height + 1;
        return ratio <= 0.9 && ratio >= 0.7;
    }

    /**
     * Gets if the character is probably an apostrophe.
     * @return If the character is probably an apostrophe
     */
    public boolean isProbablyApostraphe() {
        double ratio = (double) width / (double) height;
        return (ratio <= 0.375 && ratio >= 0.166) || (width == 1 && (height == 4 || height == 5));
    }

    /**
     * Gets if the character is probably a colon.
     * @return If the character is probably a colon
     */
    public boolean isProbablyColon() {
        double ratio = (Math.min(this.width, this.height) + 1D) / (Math.max(this.width, this.height) + 1D);
        return (ratio <= 1D && ratio >= 0.7D)
                || (height * 4 < width)
                || ((width == 3 && height == 3)
                    || (width == 2 && height == 3)
                    || (width == 2 && height == 2)
                    || (width == 1 || height == 2));
    }

    /**
     * Adds coordinates to the character.
     * @param dotCoordinates The coordinates to add
     */
    public void addDot(List<IntPair> dotCoordinates) {
        boolean[][] values = new boolean[this.height + 1][];

        for (int i = 0; i < values.length; i++) {
            values[i] = new boolean[width + 1];
        }

        int yOffset = this.height - this.values.length + 1;

        for (int y = 0; y < this.values.length; y++) {
            System.arraycopy(this.values[y], 0, values[y + yOffset], 0, this.values[0].length);
        }

        dotCoordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);

        this.values = values;
        this.hasDot = true;
    }

    /**
     * Adds a set of coordinates to the character to the current character (Assuming the current character is a percent).
     * @param dotCoordinates The coordinates to add from the circle of the percent
     * @param left If the percentage circle is on the left or right
     */
    public void addPercentageCircle(List<IntPair> dotCoordinates, boolean left) {
        boolean[][] values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        int yOffset = this.height - this.values.length + 1;

        int offset = left ? Math.abs(width + 1 - this.values[0].length) : 0;

        for (int y = 0; y < this.values.length; y++) {
            System.arraycopy(this.values[y], 0, values[y + yOffset], offset, this.values[0].length);
        }

        dotCoordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);

        this.values = values;
        this.hasDot = true;
    }

    /**
     * Gets the raw grid of boolean values from the image of this character.
     * @return The raw grid of boolean values from the image of this character
     */
    public boolean[][] getValues() {
        return values;
    }

    /**
     * Gets the coordinates of the character.
     * @return The coordinates
     */
    public List<IntPair> getCoordinates() {
        return coordinates;
    }

    /**
     * Gets the X position of the character.
     * @return The X position of ths character
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y position of the character.
     * @return The Y position of ths character
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the width of the character.
     * @return The width of ths character
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the character.
     * @return The height of ths character
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the X position of the character.
     * @param x the X position to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Sets the Y position of the character.
     * @param y the Y position to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Sets the width of the character.
     * @param width The width of the character
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the character.
     * @param height The height of the character
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets if the given coordinate is within the bounds of this character.
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
     * Gets if the given Y position is within the Y bounds of the current character.
     * @param y The Y position to check
     * @return If the given Y position is within the Y bounds of the current character
     */
    public boolean isInYBounds(int y) {
        return y <= this.y + this.height
                && y >= this.y;
    }

    /**
     * Gets if the given Y position is within the X bounds of the current character.
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
     * @param entry The data segment in the format of [total black, size of segment]
     */
    public void addSegment(IntPair entry) {
        this.segments.add(entry);
    }

    /**
     * Gets the raw segments added via {@link #addSegment(IntPair)} where the Entry format is
     * [total black, size of segment].
     * @return The raw segments
     */
    public List<IntPair> getSegments() {
        return segments;
    }

    /**
     * Gets the raw segment percentages all <= 1. This will return an empty array until {@link #applySections()} and
     * {@link #analyzeSlices()} have been invoked.
     * @return The raw array of segment percentages with a length of 17
     */
    public double[] getSegmentPercentages() {
        return this.segmentPercentages;
    }

    /**
     * Gets the known character of this object. If it has not been fount yet, it will return `?`.
     * @return The known character
     */
    public char getKnownChar() {
        return knownChar;
    }

    /**
     * Sets the known character.
     * @param knownChar The know character
     */
    public void setKnownChar(char knownChar) {
        this.knownChar = knownChar;
    }

    /**
     * Gets If this character has a dot.
     * @return If this character has a dot
     */
    public boolean hasDot() {
        return this.hasDot;
    }

    /**
     * Sets if this character has a dot in it.
     * @param hasDot If this character has a dot
     */
    public void setHasDot(boolean hasDot) {
        this.hasDot = hasDot;
    }

    @Override
    public int compareTo(SearchCharacter searchCharacter) {
        return x - searchCharacter.x;
    }

    @Override
    public String toString() {
        return String.valueOf(knownChar);
    }

    /**
     * Gets the raw 2D array of values of the character.
     * @return The raw 2D array of values of the character
     */
    public boolean[][] getData() {
        return values;
    }

    /**
     * Gets the {@link LetterMeta} of the current character.
     * @return The {@link LetterMeta} of the current character
     */
    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    /**
     * Sets the {@link LetterMeta} for the current character.
     * @param letterMeta The {@link LetterMeta} for the current character
     */
    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }

}
