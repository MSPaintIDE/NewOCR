package com.uddernetworks.newocr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An object to store line data for {@link ImageLetter}s on a scanned image.
 */
public class ScannedImage {

    private Map<Integer, List<ImageLetter>> grid = new LinkedHashMap<>();

    /**
     * Gets the string of a scanned image
     * @return The string of a scanned image
     */
    public String getPrettyString() {
        StringBuilder stringBuilder = new StringBuilder();

        grid.values().forEach(line -> {
            line.stream()
                    .map(ImageLetter::getLetter)
                    .forEach(stringBuilder::append);
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
    }

    /**
     * gets the amount of lines in the image.
     * @return The amount of lines in the image
     */
    public int getLineCount() {
        return this.grid.size();
    }

    /**
     * Returns the raw, mutable grid of {@link ImageLetter}s internally used with the key of the mpa being the exact Y
     * position of the line.
     * @return The raw, mutable grid of values
     */
    public Map<Integer, List<ImageLetter>> getGrid() {
        return this.grid;
    }

    /**
     * Adds a line containing {@link ImageLetter}s
     * @param y The exact Y position of the line
     * @param databaseCharacterList A list of {@link ImageLetter}s as the line
     */
    public void addLine(int y, List<ImageLetter> databaseCharacterList) {
        grid.put(y, databaseCharacterList);
    }

    /**
     * Gets the line at the given index value.
     * @param y The index of the line
     * @return The line at the given index value
     */
    public List<ImageLetter> getLine(int y) {
        return grid.get(new ArrayList<>(grid.keySet()).get(y));
    }

    /**
     * Gets both the line Y and values at the given index value.
     * @param y The index of the line
     * @return The line at the given Y index value
     */
    public Map.Entry<Integer, List<ImageLetter>> getLineEntry(int y) {
        return new ArrayList<>(grid.entrySet()).get(y);
    }

}
