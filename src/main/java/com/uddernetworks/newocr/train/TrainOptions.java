package com.uddernetworks.newocr.train;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainOptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private int maxCorrectionIterations = 10;
    private double maxPercentDiffToMerge = 0.5;
    private double maxPercentDistanceToMerge = 0.25;

    public Set<Character> getSpecialSpaces() {
        return specialSpaces;
    }

    public TrainOptions setSpecialSpaces(char... specialSpaces) {
        this.specialSpaces = IntStream.range(0, specialSpaces.length)
                .mapToObj(x -> specialSpaces[x])
                .collect(Collectors.toSet());
        return this;
    }

    /**
     * Gets the value set by {@link TrainOptions#setMaxCorrectionIterations(int)}
     *
     * @return The value set by {@link TrainOptions#setMaxCorrectionIterations(int)}
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
     * @return The current {@link TrainOptions} object
     */
    public TrainOptions setMaxCorrectionIterations(int maxCorrectionIterations) {
        this.maxCorrectionIterations = maxCorrectionIterations;
        return this;
    }

    /**
     * Gets the value set by {@link TrainOptions#setMaxPercentDiffToMerge(double)}
     *
     * @return The value set by {@link TrainOptions#setMaxPercentDiffToMerge(double)}
     */
    public double getMaxPercentDiffToMerge() {
        return maxPercentDiffToMerge;
    }

    /**
     * Sets the maximum percentage difference a line must be in order to merge in the very first phase of training. This
     * is primarily for when underscores are below a line, and will need to be X% smaller than the line to merge. They
     * will also need to be at least {@link TrainOptions#setMaxPercentDistanceToMerge(double)} percent away to merge.
     *
     * This value is by default 0.5
     *
     * @param maxPercentDiffToMerge The percentage to set
     * @return The current {@link TrainOptions} object
     */
    public TrainOptions setMaxPercentDiffToMerge(double maxPercentDiffToMerge) {
        this.maxPercentDiffToMerge = maxPercentDiffToMerge;
        return this;
    }

    /**
     * Gets the value set by {@link TrainOptions#setMaxPercentDistanceToMerge(double)}
     *
     * @return The value set by {@link TrainOptions#setMaxPercentDistanceToMerge(double)}
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
     * @return The current {@link TrainOptions} object
     */
    public TrainOptions setMaxPercentDistanceToMerge(double maxPercentDistanceToMerge) {
        this.maxPercentDistanceToMerge = maxPercentDistanceToMerge;
        return this;
    }
}
