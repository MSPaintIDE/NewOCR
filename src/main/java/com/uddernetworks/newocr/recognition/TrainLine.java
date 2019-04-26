package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;

import java.util.List;

/**
 * Stores characters at a top and bottom Y position after an image has been scanned.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class TrainLine implements CharacterLine {

    private List<SearchCharacter> letters;
    private int topY;
    private int bottomY;

    /**
     * Creates a new {@link TrainLine} with a list of characters populating it.
     *
     * @param letters The list of {@link SearchCharacter}s to initially populate the line
     * @param topY    The top Y coordinate of the image the line starts at
     * @param bottomY The bottom Y coordinate of the image the line ends at
     */
    public TrainLine(List<SearchCharacter> letters, int topY, int bottomY) {
        this.letters = letters;
        this.topY = topY;
        this.bottomY = bottomY;
    }

    @Override
    public List<SearchCharacter> getLetters() {
        return this.letters;
    }

    @Override
    public int topY() {
        return this.topY;
    }

    @Override
    public int bottomY() {
        return this.bottomY;
    }
}
