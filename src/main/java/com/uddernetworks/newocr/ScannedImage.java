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
     * Adds a line containing {@link ImageLetter}s
     * @param databaseCharacterList A list of {@link ImageLetter}s as the line
     */
    public void addLine(List<ImageLetter> databaseCharacterList) {
        grid.add(databaseCharacterList);
    }

}
