package com.uddernetworks.newocr.altsearcher;

public class FontBounds {
    private int minFont;
    private int maxFont;

    public FontBounds(int minFont, int maxFont) {
        this.minFont = minFont;
        this.maxFont = maxFont;
    }

    public int getMinFont() {
        return minFont;
    }

    public int getMaxFont() {
        return maxFont;
    }

    public boolean isInbetween(int font) {
        return minFont <= font && font <= maxFont;
    }
}