package com.uddernetworks.newocr.train;

public class TrainGeneratorOptions {
    private int maxFontSize = 90;
    private int minFontSize = 30;
    private String fontFamily = "Comic Sans MS";

    public int getMaxFontSize() {
        return maxFontSize;
    }

    public TrainGeneratorOptions setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
        return this;
    }

    public int getMinFontSize() {
        return minFontSize;
    }

    public TrainGeneratorOptions setMinFontSize(int minFontSize) {
        this.minFontSize = minFontSize;
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public TrainGeneratorOptions setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }
}
