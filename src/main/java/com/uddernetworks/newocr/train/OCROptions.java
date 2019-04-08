package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.recognition.similarity.Letter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class OCROptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private Set<Letter> requireSizeCheck = EnumSet.of(MINUS, PIPE, UNDERSCORE, EQUALS_TOP, EQUALS_BOTTOM);
    private int maxCorrectionIterations = 10;
    private double maxPercentDiffToMerge = 0.5;
    private double maxPercentDistanceToMerge = 0.25;

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

    public Set<Letter> getRequireSizeCheck() {
        return this.requireSizeCheck;
    }

    public OCROptions setRequireSizeCheck(EnumSet<Letter> requireSizeCheck) {
        this.requireSizeCheck = EnumSet.copyOf(requireSizeCheck);
        return this;
    }

    /**
     * Defines an array of {@link Letter}s that require the size to be checked if the closest and second-closest match
     * to a character are in this list. The closest width/height ratio is then selected as the correct character.
     *
     * By default this internal collection is populated by:
     * - {@link Letter#MINUS}
     * - {@link Letter#PIPE}
     * - {@link Letter#UNDERSCORE}
     * - {@link Letter#EQUALS_TOP}
     * - {@link Letter#EQUALS_BOTTOM}
     *
     * @param requireSizeCheck The {@link Letter}s to set
     * @return The current {@link OCROptions} object
     */
    public OCROptions setRequireSizeCheck(Letter... requireSizeCheck) {
        this.requireSizeCheck = Arrays.stream(requireSizeCheck, 0, requireSizeCheck.length)
                .collect(Collectors.toSet());
        return this;
    }

    /**
     * Adds to the preexisting {@link Letter} list requiring size checks. Same thing as
     * {@link OCROptions#setRequireSizeCheck(Letter...)} but with previous letters unmodified.
     *
     * By default this internal collection is populated by:
     *      * - {@link Letter#MINUS}
     *      * - {@link Letter#PIPE}
     *      * - {@link Letter#UNDERSCORE}
     *      * - {@link Letter#EQUALS_TOP}
     *      * - {@link Letter#EQUALS_BOTTOM}
     *
     * @param requireSizeChecks The {@link Letter}s to add
     * @return The current {@link OCROptions} object
     */
    public OCROptions addRequireSizeCheck(Letter... requireSizeChecks) {
        this.requireSizeCheck.addAll(Arrays.asList(requireSizeChecks));
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
}
