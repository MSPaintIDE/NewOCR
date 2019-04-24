package com.uddernetworks.newocr.train;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * General options used by the OCR scanning and training.
 */
public class OCROptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private double maxPercentDiffToMerge = 0.5;
    private double sizeRatioWeight = 4;

    /**
     * Gets the characters requiring custom trained spaces.
     *
     * @return The characters requiring custom spacing
     */
    public Set<Character> getSpecialSpaces() {
        return this.specialSpaces;
    }

    /**
     * Sets the characters requiring custom trained spaces.
     *
     * @param specialSpaces The characters requiring separate training for their trailing spaces.
     * @return The current {@link OCROptions} object
     */
    public OCROptions setSpecialSpaces(Set<Character> specialSpaces) {
        this.specialSpaces = new HashSet<>(specialSpaces);
        return this;
    }

    /**
     * Sets the characters requiring custom trained spaces.
     *
     * @param specialSpaces The characters requiring separate training for their trailing spaces.
     * @return The current {@link OCROptions} object
     */
    public OCROptions setSpecialSpaces(char... specialSpaces) {
        this.specialSpaces = IntStream.range(0, specialSpaces.length)
                .mapToObj(x -> specialSpaces[x])
                .collect(Collectors.toSet());
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
     * is primarily for when underscores are below a line, and will need to be X% smaller than the line to merge.
     * <p>
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
