package com.uddernetworks.newocr.altsearcher;

import java.util.LinkedList;
import java.util.List;

public class ScannedImage {

    private List<List<ImageLetter>> grid = new LinkedList<>();

    public ScannedImage() {}

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

    public void addLine(List<ImageLetter> databaseCharacterList) {
        grid.add(databaseCharacterList);
    }

}
