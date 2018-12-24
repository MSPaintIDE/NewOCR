package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.FontBounds;
import com.uddernetworks.newocr.LetterMeta;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Future;

public interface DatabaseManager {

    /**
     * Gets the {@link DataSource} used by the DatabaseManager
     * @return The {@link DataSource} used by the DatabaseManager
     */
    DataSource getDataSource();

    /**
     * Inserts into the `letters` table.
     * @param letter The character to insert
     * @param averageWidth The average width of the character
     * @param averageHeight The average height of the character
     * @param minFontSize The minimum font size populated by this character
     * @param maxFontSize The maximum font size populate by this character
     * @param minCenter The minimum relative center from the top found in the training ste for the font size
     * @param maxCenter The maximum relative center from the top found in the training ste for the font size
     * @param hasDot If the character has a dot in it
     * @param letterMeta The {@link LetterMeta} of the character
     * @param isLetter If the charcater is a letter (true) or if it is a space (false)
     * @return A Future
     */
    Future createLetterEntry(char letter, double averageWidth, double averageHeight, int minFontSize, int maxFontSize, double minCenter, double maxCenter, boolean hasDot, LetterMeta letterMeta, boolean isLetter);

    /**
     * Clears all data revolving around a character from both the `letters` and `sectionData` table.
     * @param letter The charcater to cleare
     * @param minFontSize The minimum font size to clear
     * @param maxFontSize The maximum font size to clear
     * @return A Future
     */
    Future clearLetterSegments(char letter, int minFontSize, int maxFontSize);

    /**
     * Adds segments (Percentage data points) to the database for a certain character.
     * @param letter The character to add segments to
     * @param minFontSize The minimum font size for the character
     * @param maxFontSize The maximum font size for the character
     * @param segments An array with a length of 17 all <= 1 as percentage data points
     * @return A Future
     */
    Future addLetterSegments(char letter, int minFontSize, int maxFontSize, double[] segments);

    /**
     * Gets all the {@link DatabaseCharacter}s between the given {@link FontBounds}.
     * @param fontBounds The {@link FontBounds} get the characters between
     * @return A Future of all the {@link DatabaseCharacter}s
     */
    Future<List<DatabaseCharacter>> getAllCharacterSegments(FontBounds fontBounds);
}
