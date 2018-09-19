package com.uddernetworks.newocr;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ParsingLine {

    private ParsingImage parsingImage;
    private int y;
    private int height;
    private List<OCRCharacter> characters = new ArrayList<>();
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
        int width = 0;
        for (int x = 0; x < this.lineData[0].length; x++) {
            if (Main.isColumnPopulated(this.lineData, x)) {
                width++;
            } else if (width > 0) {
                characters.add(new OCRCharacter(this.parsingImage, x - width, width));
                width = 0;
            }
        }
    }

    public void graph() {
        for (OCRCharacter character : characters) {
            Main.colorColumn(this.parsingImage.getImage(), Color.BLUE, character.getX(), this.y, this.height);
            Main.colorColumn(this.parsingImage.getImage(), Color.BLUE, character.getX() + character.getWidth(), this.y, this.height);
        }
    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return height;
    }
}
