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

    // Parse Character Left and Right Bounds
    public void parseCharacterLRB() {
        int width = 0;
        for (int x = 0; x < this.lineData[0].length; x++) {
            if (Main.isColumnPopulated(this.lineData, x)) {
                width++;
            } else if (width > 0) {
                int usingX = x - width;

                OCRCharacter ocrCharacter;
                characters.add(ocrCharacter = new OCRCharacter(this.parsingImage, usingX, width));
                ocrCharacter.setY(this.y);

                short[][] newData = new short[height][];
                for (int i = 0; i < height; i++) newData[i] = new short[width];

                for (int y = 0; y < height; y++) {
                    System.arraycopy(lineData[y], usingX, newData[y], 0, width);
                }

                ocrCharacter.setValues(newData);

                width = 0;
            }
        }

        Main.resetLastColumn();
    }

    // Parse Character Top and Bottom Bounds
    public void parseCharacterTBB() {
        for (OCRCharacter character : this.characters) {
            short[][] data = character.getValues();

            int excessTop = 0;
            int excessBottom = 0;

            for (int y = 0; y < this.lineData.length; y++) {
                if (!Main.isRowPopulated(data, y)) {
                    excessTop++;
                } else {
                    break;
                }
            }

            for (int y = this.lineData.length; 0 <=-- y; ) {
                if (!Main.isRowPopulated(data, y)) {
                    excessBottom++;
                } else {
                    break;
                }
            }

            int newHeight = this.lineData.length - excessTop - excessBottom;

            short[][] newData = new short[newHeight][];
            for (int i = 0; i < newHeight; i++) newData[i] = new short[character.getWidth()];

            for (int y = 0; y < newHeight; y++) {
                System.arraycopy(data[y + excessTop], 0, newData[y], 0, character.getWidth());
            }

            character.setHeight(newHeight);
            character.setY(character.getY() + excessTop);
        }
    }

    // Graph Left and Right bounds
    public void graphLR() {
        for (OCRCharacter character : characters) {
            Main.colorColumn(this.parsingImage.getImage(), Color.BLUE, character.getX(), this.y, this.height);
            Main.colorColumn(this.parsingImage.getImage(), Color.BLUE, character.getX() + character.getWidth(), this.y, this.height);
        }
    }

    // Graph Top and Bottom per-character bounds
    public void graphTB() {
        for (OCRCharacter character : characters) {
            Main.colorRow(this.parsingImage.getImage(), Color.GREEN, character.getY(), character.getX(), character.getWidth());
            Main.colorRow(this.parsingImage.getImage(), Color.GREEN, character.getY() + character.getHeight(), character.getX(), character.getWidth());
        }
    }

    // Graph exact bounding box around character
    public void graphCharacterBoundingBox() {
        for (OCRCharacter character : characters) {
            Main.colorRow(this.parsingImage.getImage(), Color.MAGENTA, character.getY(), character.getX(), character.getWidth());
            Main.colorRow(this.parsingImage.getImage(), Color.MAGENTA, character.getY() + character.getHeight(), character.getX(), character.getWidth() + 1);

            Main.colorColumn(this.parsingImage.getImage(), Color.MAGENTA, character.getX(), character.getY(), character.getHeight());
            Main.colorColumn(this.parsingImage.getImage(), Color.MAGENTA, character.getX() + character.getWidth(), character.getY(), character.getHeight());
        }
    }

    public int getY() {
        return y;
    }

    public int getHeight() {
        return height;
    }
}
