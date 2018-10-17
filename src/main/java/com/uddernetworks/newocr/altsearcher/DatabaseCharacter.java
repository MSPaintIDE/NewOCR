package com.uddernetworks.newocr.altsearcher;

public class DatabaseCharacter {
    private char letter;
    private double[] data = new double[17];
    private double avgWidth;
    private double avgHeight;
    private int minFontSize;
    private int maxFontSize;
    private double minCenter; // Pixels from the top
    private double maxCenter; // Pixels from the top
    private boolean hasDot;
    private LetterMeta letterMeta;

    private int x;
    private int y;
    private double ratio;

    public DatabaseCharacter(char letter) {
        this.letter = letter;
    }

    public char getLetter() {
        return letter;
    }

    public double[] getData() {
        return data;
    }

    public void addDataPoint(int index, double data) {
        this.data[index] = data;
    }

    public void setData(double avgWidth, double avgHeight, int minFontSize, int maxFontSize) {
        this.avgWidth = avgWidth;
        this.avgHeight = avgHeight;
        this.minFontSize = minFontSize;
        this.maxFontSize = maxFontSize;
    }

    public double getAvgWidth() {
        return avgWidth;
    }

    public void setAvgWidth(double avgWidth) {
        this.avgWidth = avgWidth;
    }

    public double getAvgHeight() {
        return avgHeight;
    }

    public void setAvgHeight(double avgHeight) {
        this.avgHeight = avgHeight;
    }

    public int getMinFontSize() {
        return minFontSize;
    }

    public void setMinFontSize(int minFontSize) {
        this.minFontSize = minFontSize;
    }

    public int getMaxFontSize() {
        return maxFontSize;
    }

    public void setMaxFontSize(int maxFontSize) {
        this.maxFontSize = maxFontSize;
    }

//    public double getCenter() {
//        return center;
//    }
//
//    public void setCenter(double center) {
//        this.center = center;
//    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public DatabaseCharacter copy() {
        DatabaseCharacter copy = new DatabaseCharacter(this.letter);
        copy.data = this.data;
        copy.avgWidth = this.avgWidth;
        copy.avgHeight = this.avgHeight;
        copy.minFontSize = this.minFontSize;
        copy.maxFontSize = this.maxFontSize;
        copy.minCenter = this.minCenter;
        copy.maxCenter = this.maxCenter;
        copy.hasDot = this.hasDot;
        return copy;
    }

    @Override
    public String toString() {
        return String.valueOf(this.letter);
    }

    public void setMinCenter(double minCenter) {
        this.minCenter = minCenter;
    }

    public void setMaxCenter(double maxCenter) {
        this.maxCenter = maxCenter;
    }

    public double getMinCenter() {
        return minCenter;
    }

    public double getMaxCenter() {
        return maxCenter;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public double getRatio() {
        return ratio;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DatabaseCharacter && ((DatabaseCharacter) obj).letter == this.letter;
    }

    public void setHasDot(boolean hasDot) {
//        if (!hasDot) System.out.println("Setting has!!!!!");
        this.hasDot = hasDot;
    }

    public boolean hasDot() {
        return this.hasDot;
    }

    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }
}
