package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * The resulting object from an image being scanned.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public interface ScannedImage {
    /**
     * Gets the string of a scanned image
     *
     * @return The string of a scanned image
     */
    String getPrettyString();

    /**
     * Removes all common leading spaces, just like how {@link OCRUtils#removeLeadingSpaces(String)} removes empty
     * spaces, but with actual {@link ImageLetter} objects.
     *
     * @return The current {@link ScannedImage}
     */
    ScannedImage stripLeadingSpaces();

    /**
     * Gets the letter at the given index of the actual {@link ScannedImage#getPrettyString()} position, meaning
     * newlines are not returned.
     *
     * @param index The character index
     * @return The ImageLetter at the given position, if found
     */
    Optional<ImageLetter> letterAt(int index);

    /**
     * Gets the line at the stored index. This is NOT by the y value.
     *
     * @param index The index of the row
     * @return The row
     */
    Optional<List<ImageLetter>> getGridLineAtIndex(int index) throws IndexOutOfBoundsException;

    /**
     * gets the amount of lines in the image.
     *
     * @return The amount of lines in the image
     */
    int getLineCount();

    /**
     * Returns the raw, mutable grid of {@link ImageLetter}s internally used with the key of the mpa being the exact Y
     * position of the line.
     *
     * @return The raw, mutable grid of values
     */
    Int2ObjectMap<List<ImageLetter>> getGrid();

    /**
     * Adds a line containing {@link ImageLetter}s.
     *
     * @param y                     The exact Y position of the line
     * @param databaseCharacterList A list of {@link ImageLetter}s as the line
     */
    void addLine(int y, List<ImageLetter> databaseCharacterList);

    /**
     * Gets the line at the given index value.
     *
     * @param y The index of the line
     * @return The line at the given index value
     */
    List<ImageLetter> getLine(int y);

    /**
     * Gets both the line Y and values at the given index value.
     *
     * @param y The index of the line
     * @return The line at the given Y index value
     */
    Int2ObjectMap.Entry<List<ImageLetter>> getLineEntry(int y);

    /**
     * Gets the original image scanned by the OCR.
     *
     * @return The original image, which may be null if pulled from caches
     */
    BufferedImage getOriginalImage();

    /**
     * Gets the original {@link File} scanned by the OCR.
     *
     * @return The original File, which may be null if pulled from caches
     */
    File getOriginalFile();
}
