package com.uddernetworks.newocr.train;

/**
 * Defines options for the actual generation of the image to train on.
 */
public class TrainGeneratorOptions {
    private int maxFontSize = 90;
    private int minFontSize = 30;
    private String fontFamily = "";

    /**
     * Gets the maximum font size to generate up to in points.
     *
     * @return The maximum font size
     */
    public int getMaxFontSize() {
        return maxFontSize;
    }

    /**
     * Sets the maximum font size to generate up to in points.
     *
     * @param maxFontSize The maximum font size
     * @return The current {@link TrainGeneratorOptions}
     */
    public TrainGeneratorOptions setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
        return this;
    }

    /**
     * Gets the minimum font size to generate down to in points.
     *
     * @return The minimum font size
     */
    public int getMinFontSize() {
        return minFontSize;
    }

    /**
     * Sets the minimum font size to generate up to in points.
     *
     * @param minFontSize The minimum font size
     * @return The current {@link TrainGeneratorOptions}
     */
    public TrainGeneratorOptions setMinFontSize(int minFontSize) {
        this.minFontSize = minFontSize;
        return this;
    }

    /**
     * Gets the system font family used during training image generation.
     *
     * @return The font family
     */
    public String getFontFamily() {
        return fontFamily;
    }

    /**
     * Sets the system font family used during training image generation.
     *
     * @param fontFamily The font family to set
     * @return The current {@link TrainGeneratorOptions}
     */
    public TrainGeneratorOptions setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }
}
