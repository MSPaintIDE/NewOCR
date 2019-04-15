package com.uddernetworks.newocr.train;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OCROptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private int maxCorrectionIterations = 10;
    private double maxPercentDiffToMerge = 0.5;
    private double maxPercentDistanceToMerge = 0.25;
    private double sizeRatioWeight = 4;

    public Set<Character> getSpecialSpaces() {
        return this.specialSpaces;
    }

    public OCROptions setSpecialSpaces(Set<Character> specialSpaces) {
        this.specialSpaces = new HashSet<>(specialSpaces);
        return this;
    }

    public OCROptions setSpecialSpaces(char... specialSpaces) {
        this.specialSpaces = IntStream.range(0, specialSpaces.length)
                .mapToObj(x -> specialSpaces[x])
                .collect(Collectors.toSet());
        return this;
    }

    /**
     * Gets the value set by {@link OCROptions#setMaxCorrectionIterations(int)}
     *
     * @return The value set by {@link OCROptions#setMaxCorrectionIterations(int)}
     */
    public int getMaxCorrectionIterations() {
        return maxCorrectionIterations;
    }

    /**
     * Sets the maximum amount of times the system will try and go through and correct errors.
     *
     * The value by default is 10
     *
     * @param maxCorrectionIterations The amount of iterations to set
     * @return The current {@link OCROptions} object
     */
    public OCROptions setMaxCorrectionIterations(int maxCorrectionIterations) {
        this.maxCorrectionIterations = maxCorrectionIterations;
        return this;
    }

    /**
     * Gets the value set by {@link OCROptions#setMaxPercentDiffToMerge(double)}
     *
     * @return The value set by {@link OCROptions#setMaxPercentDiffToMerge(double)}
     */
    public double getMaxPercentDiffToMerge() {
        return maxPercentDiffToMerge;
    }

    /**
     * Sets the maximum percentage difference a line must be in order to merge in the very first phase of training. This
     * is primarily for when underscores are below a line, and will need to be X% smaller than the line to merge. They
     * will also need to be at least {@link OCROptions#setMaxPercentDistanceToMerge(double)} percent away to merge.
     *
     * This value is by default 0.5
     *
     * @param maxPercentDiffToMerge The percentage to set
     * @return The current {@link OCROptions} object
     */
    public OCROptions setMaxPercentDiffToMerge(double maxPercentDiffToMerge) {
        this.maxPercentDiffToMerge = maxPercentDiffToMerge;
        return this;
    }

    /**
     * Gets the value set by {@link OCROptions#setMaxPercentDistanceToMerge(double)}
     *
     * @return The value set by {@link OCROptions#setMaxPercentDistanceToMerge(double)}
     */
    public double getMaxPercentDistanceToMerge() {
        return maxPercentDistanceToMerge;
    }

    /**
     * Sets the maximum percentage of the top line a lower line much be away compared to its height in order to merge.
     * E.g. This value is set as 50%, and a top line is 100px high. Another line must be at least 50px from the line.
     *
     * This value is by default 0.25
     *
     * @param maxPercentDistanceToMerge The percentage to set
     * @return The current {@link OCROptions} object
     */
    public OCROptions setMaxPercentDistanceToMerge(double maxPercentDistanceToMerge) {
        this.maxPercentDistanceToMerge = maxPercentDistanceToMerge;
        return this;
    }

    /**
     * Gets the amount the width/height radio should be multiplied across all a character's potential matches, to
     * increase its effects compared to the actual section similarity.
     *
     * @return The weight of the width/height ratio
     */
    public double getSizeRatioWeight() {
        return sizeRatioWeight;
    }

    /**
     * Sets the amount the width/height radio should be multiplied across all a character's potential matches, to
     * increase its effects compared to the actual section similarity.
     *
     * @param sizeRatioWeight The weight of the width/height ratio
     * @return The current {@link OCROptions} object
     */
    public OCROptions setSizeRatioWeight(double sizeRatioWeight) {
        this.sizeRatioWeight = sizeRatioWeight;
        return this;
    }
}
