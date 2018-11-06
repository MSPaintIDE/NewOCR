package com.uddernetworks.newocr;

import java.util.LinkedList;
import java.util.List;

/**
 * An object to store line data for {@link ImageLetter}s on a scanned image.
 */
public class ScannedImage {

    private List<List<ImageLetter>> grid = new LinkedList<>();

    /**
     * Gets the string of a scanned image
     * @return The string of a scanned image
     */
    public String getPrettyString() {
        StringBuilder stringBuilder = new StringBuilder();

        grid.forEach(line -> {
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
     * Returns the raw, mutable grid of {@link ImageLetter}s internally used.
     * @return The raw, mutable grid of values
     */
    public List<List<ImageLetter>> getGrid() {
        return this.grid;
    }

    /**
     * Adds a line containing {@link ImageLetter}s
     * @param databaseCharacterList A list of {@link ImageLetter}s as the line
     */
    public void addLine(List<ImageLetter> databaseCharacterList) {
        grid.add(databaseCharacterList);
    }

    /**
     * Gets the line at the given Y index value.
     * @param y The index of the line
     * @return The line at the given Y index value
     */
    public List<ImageLetter> getLine(int y) {
        return grid.get(y);
    }

}
