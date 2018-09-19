package com.uddernetworks.newocr;

public class OCRCharacter {

    private ParsingImage parsingImage;
    private int x;
    private int y;
    private int height;
    private int width;

    public OCRCharacter(ParsingImage parsingImage, int x, int width) {
        this.parsingImage = parsingImage;
        this.x = x;
        this.width = width;
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

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
