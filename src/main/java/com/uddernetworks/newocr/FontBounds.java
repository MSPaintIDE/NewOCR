package com.uddernetworks.newocr;

/**
 * An object storing the data for a set of bounds (Both upper and lower)
 */
public class FontBounds {
    private int minFont;
    private int maxFont;

    /**
     * Creates a FontBounds object from the given bounds
     * @param minFont The upper limit of allowed font size
     * @param maxFont The lower limit of allowed font size
     */
    public FontBounds(int minFont, int maxFont) {
        this.minFont = minFont;
        this.maxFont = maxFont;
    }

    /**
     * Gets the minimum allowed font size
     * @return The minimum allowed font size
     */
    public int getMinFont() {
        return minFont;
    }

    /**
     * Gets the maximum allowed font size
     * @return The maximum allowed font size
     */
    public int getMaxFont() {
        return maxFont;
    }

    /**
     * Gets if the given size is contained or included in the font bounds. This method is simple but is good for a
     * visual comparison.
     * @param font The font size to compare
     * @return If the font is between or in the borders
     */
    public boolean isInbetween(int font) {
        return minFont <= font && font <= maxFont;
    }

    @Override
    public String toString() {
        return "FontBounds[" + this.minFont + " - " + this.maxFont + "]";
    }
}