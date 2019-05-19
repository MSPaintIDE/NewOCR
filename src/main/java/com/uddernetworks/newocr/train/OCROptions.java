package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * General options used by the OCR scanning and training.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class OCROptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private double maxPercentDiffToMerge = 0.5;
    private double sizeRatioWeight = 4;
    private Map<Letter, Double> specificRatioWeights = new HashMap<>();
    private ImageReadMethod imageReadMethod = ImageReadMethod.IMAGE_ICON;

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
     * Adds a custom width/height ratio for the given {@link Letter}.
     *
     * @param letter The {@link Letter} to set the weight of
     * @param weight The weight to set
     */
    public void addRatioWeight(Letter letter, double weight) {
        this.specificRatioWeights.put(letter, weight);
    }

    /**
     * Adds a custom width/height ratio for the each {@link Letter} i nthe given list.
     *
     * @param letters The {@link Letter} list to set the weight of
     * @param weight The weight to set
     */
    public void addRatioWeights(List<Letter> letters, double weight) {
        letters.forEach(letter -> this.specificRatioWeights.put(letter, weight));
    }

    /**
     * Adds a custom width/height ratio for all {@link Letter}s matching the given {@link SimilarRule}.
     *
     * @param similarRule The {@link SimilarRule} to match to letters
     * @param weight The weight to set
     */
    public void addRatioWeightFromRule(SimilarRule similarRule, double weight) {
        Arrays.stream(Letter.values()).filter(similarRule::matchesLetter).forEach(letter -> this.specificRatioWeights.put(letter, weight));
    }

    /**
     * Adds a custom width/height ratio for all {@link Letter}s matching all the given {@link SimilarRule}s.
     *
     * @param similarRules The {@link SimilarRule} list to match to letters
     * @param weight The weight to set
     */
    public void addRatioWeightsFromRules(List<SimilarRule> similarRules, double weight) {
        Arrays.stream(Letter.values())
                .filter(letter ->
                        similarRules.stream()
                                .anyMatch(rule -> rule.matchesLetter(letter)))
                .forEach(letter -> this.specificRatioWeights.put(letter, weight));
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
     * Gets the amount the width/height radio should be multiplied across all a character's potential matches, to
     * increase its effects compared to the actual section similarity.
     *
     * @param letter The letter to get the width/height ratio of
     * @return The weight of the width/height ratio
     */
    public double getSizeRatioWeight(Letter letter) {
        return sizeRatioWeight * this.specificRatioWeights.getOrDefault(letter, 1D);
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

    /**
     * Gets the method that will be used to read the image for both training and scanning.
     *
     * @return The {@link ImageReadMethod} used in training and scanning.
     */
    public ImageReadMethod getImageReadMethod() {
        return imageReadMethod;
    }

    /**
     * Sets the {@link ImageReadMethod} used during training and scanning.
     *
     * @param imageReadMethod The {@link ImageReadMethod} to set
     */
    public void setImageReadMethod(ImageReadMethod imageReadMethod) {
        this.imageReadMethod = imageReadMethod;
    }
}
