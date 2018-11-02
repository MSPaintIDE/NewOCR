package com.uddernetworks.newocr.altsearcher;

import java.util.LinkedList;
import java.util.List;

public class ScannedImage {

    private List<List<DatabaseCharacter>> grid = new LinkedList<>();

    public ScannedImage() {

    }

    public String getPrettyString() {
        StringBuilder stringBuilder = new StringBuilder();

        grid.forEach(line -> {
            line.stream()
                    .map(DatabaseCharacter::getLetter)
                    .forEach(stringBuilder::append);
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
    }

    public void addLine(List<DatabaseCharacter> databaseCharacterList) {
        grid.add(databaseCharacterList);
    }

}
