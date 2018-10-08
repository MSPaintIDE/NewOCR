package com.uddernetworks.newocr.altsearcher;

public class DatabaseCharacter {
    private char letter;
    private double[] data = new double[17];
    private double avgWidth;
    private double avgHeight;
    private int minFontSize;
    private int maxFontSize;
    private double center; // Pixels from the top

    private int x;
    private int y;
    private int centerExact;

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

    public double getCenter() {
        return center;
    }

    public void setCenter(double center) {
        this.center = center;
    }

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

    public int getCenterExact() {
        return centerExact;
    }

    public void setCenterExact(int centerExact) {
        this.centerExact = centerExact;
    }

    public DatabaseCharacter copy() {
        DatabaseCharacter copy = new DatabaseCharacter(this.letter);
        copy.data = this.data;
        copy.avgWidth = this.avgWidth;
        copy.avgHeight = this.avgHeight;
        copy.minFontSize = this.minFontSize;
        copy.maxFontSize = this.maxFontSize;
        copy.center = this.center;
        return copy;
    }

    @Override
    public String toString() {
        return String.valueOf(this.letter);
    }
}
