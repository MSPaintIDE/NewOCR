package com.uddernetworks.newocr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ParsingImage {

    private BufferedImage image;
    private short[][] originalData;
    private List<ParsingLine> lines = new ArrayList<>();

    public ParsingImage(BufferedImage image, short[][] values) {
        this.image = image;
        this.originalData = values;
    }

    public void parseLines() {
        int height = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            if (Main.isRowPopulated(this.originalData, y)) {
                height++;
            } else if (height > 0) {
                int heightUntil = 0;
                int finalSpace = -1;

                // Seeing if the gap under the character is <= the height of the above piece. This is mainly for seeing
                // if the dot on an 'i' is <= is above the rest of the character the same amount as its height (Making it a proper 'i' in Verdana
                for (int i = 0; i < height; i++) {
                    if (y + i >= this.originalData.length) {
                        finalSpace = 0;
                        break;
                    }

                    if (Main.isRowPopulated(this.originalData, y + i)) {
                        if (finalSpace == -1) finalSpace = heightUntil;
                    } else {
                        heightUntil++;
                    }
                }

                if (finalSpace > 0) {
                    System.out.println("Vertical separation was " + finalSpace + " yet height was " + height);
                    if (height == finalSpace) {
                        y += finalSpace;
                        height += finalSpace;
                    } else {
                        lines.add(new ParsingLine(this, y - height, height));
                        height = 0;
                    }
                } else {
                    lines.add(new ParsingLine(this, y - height, height));
                    height = 0;
                }
            } else {
                if (height == 0) continue;
                lines.add(new ParsingLine(this, y - height, height));
                height = 0;
            }
        }
    }

    public void graphLines() {
        for (ParsingLine line : lines) {
            Main.colorRow(this.image, Color.RED, line.getY());
            Main.colorRow(this.image, Color.RED, line.getY() + line.getHeight());
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public short[][] getOriginalData() {
        return originalData;
    }

    public List<ParsingLine> getLines() {
        return lines;
    }
}
