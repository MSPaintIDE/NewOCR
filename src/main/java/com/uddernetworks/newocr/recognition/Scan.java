package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;

import java.io.File;
import java.util.List;

public interface Scan {

    /**
     * Scans the input image and returns a {@link ScannedImage} containing all the characters and their info.
     *
     * @param file The input image to be scanned
     * @return A {@link ScannedImage} containing all scanned character data
     */
    ScannedImage scanImage(File file);

    /**
     * Gets and inserts all the spaces of the current line based on the font size given (The first character of the line
     * by default). This method adds the spaces to the end of the line currently, so a resort is needed.
     *
     * @param line     The line to add spaces to
     * @param fontSize The font size to base the space widths off of
     * @return A copy of the input {@link ImageLetter} List, but with spaces appended to the end
     */
    List<ImageLetter> getSpacesFor(List<ImageLetter> line, int fontSize);

    MergenceManager getMergenceManager();

    /**
     * Gets the full space character count for the blank gap divided by the space width. This is calculated by getting
     * the amount of times the space can fit in evenly (x % 1) and if the remaining value is within 0.2 of 1, it is
     * considered a space.
     *
     * @param input The amount of spaces that fit in the gap (gap / spaceWidth)
     * @return The amount of spaces that is found as a whole number
     */
    int spaceRound(double input);
}
