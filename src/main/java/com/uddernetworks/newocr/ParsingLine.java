package com.uddernetworks.newocr;

import java.util.List;

public class ParsingLine {

    private ParsingImage parsingImage;
    private int y;
    private int height;
    private List<OCRCharacter> characters;
    private short[][] lineData;

    public ParsingLine(ParsingImage parsingImage, int y, int height) {
        this.parsingImage = parsingImage;
        this.y = y;
        this.height = height;

        short[][] original = parsingImage.getOriginalData();
        lineData = new short[height][];
        for (int i = 0; i < height; i++) lineData[i] = new short[original[0].length];

        for (int i = 0; i < height; i++) {
            System.arraycopy(original[y + i], 0, lineData[i], 0, original[0].length);
        }
    }

    // Parse Character Left-Right Bounds
    public void parseCharacterLRB() {

    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return height;
    }
}
