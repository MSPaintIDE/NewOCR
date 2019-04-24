package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;

import java.util.List;

/**
 * An object to store {@link SearchCharacter} data for a scanned line.
 */
public interface CharacterLine {

    /**
     * Gets the letters in the line ordered by X value.
     *
     * @return The letters in the line
     */
    List<SearchCharacter> getLetters();

    /**
     * Gets the top Y coordinate of the line.
     *
     * @return The top Y coordinate of the line
     */
    int topY();

    /**
     * Gets the top Y coordinate of the line.
     *
     * @return The top Y coordinate of the line
     */
    int bottomY();
}
