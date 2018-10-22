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
//        System.out.println("font = [" + font + "]");
        return minFont <= font && font <= maxFont;
    }

    @Override
    public String toString() {
        return "FontBounds[" + this.minFont + " - " + this.maxFont + "]";
    }
}