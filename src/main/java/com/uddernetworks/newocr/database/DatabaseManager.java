package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.LetterMeta;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Future;

public interface DatabaseManager {

    /**
     * Gets the {@link DataSource} used by the DatabaseManager
     *
     * @return The {@link DataSource} used by the DatabaseManager
     */
    DataSource getDataSource();

    /**
     * Inserts into the `letters` table.
     *
     * @param letter The character to insert
     * @param averageWidth The average width of the character
     * @param averageHeight The average height of the character
     * @param minCenter The minimum relative center from the top found in the training ste for the font size
     * @param maxCenter The maximum relative center from the top found in the training ste for the font size
     * @param hasDot If the character has a dot in it
     * @param letterMeta The {@link LetterMeta} of the character
     * @param isLetter If the charcater is a letter (true) or if it is a space (false)
     * @return A Future
     */
    void createLetterEntry(char letter, double averageWidth, double averageHeight, double minCenter, double maxCenter, boolean hasDot, LetterMeta letterMeta, boolean isLetter);

    /**
     * Clears all data revolving around a character from both the `letters` and `sectionData` table.
     *
     * @param letter The character to clear
     */
    void clearLetterSegments(char letter);

    /**
     * Adds segments (Percentage data points) to the database for a certain character.
     *
     * @param letter The character to add segments to
     * @param segments An array with a length of 17 all <= 1 as percentage data points
     */
    void addLetterSegments(char letter, double[] segments);

    /**
     * Gets all the {@link DatabaseCharacter}s in the database
     *
     * @return A Future of all the {@link DatabaseCharacter}s
     */
    Future<List<DatabaseCharacter>> getAllCharacterSegments();

    /**
     * Adds a piece of data in the database (Never overrides existing data) to be averaged and fetched later.
     *
     * @param name The name of the data
     * @param value The value to be added
     */
    void addAveragedData(String name, double[] value);

    /**
     * Gets the average value of the given data name, added from {@link DatabaseManager#addAveragedData(String, double[])}.
     * If no data is found, it will return -1.
     *
     * @param name The name of the data to fetch and average
     * @return The averaged data, being -1 if no data is found
     */
    Future<Double> getAveragedData(String name);

    /**
     * Clears all data in the database, primarily used for before training.
     */
    void clearData();

    /**
     * Shuts down all executor threads when the program is ready to be terminated.
     */
    void shutdown();

    /**
     * Gets if the database manager is running off of the internal HSQLDB database or the external MySQL database.
     *
     * @return If the database manager is using the internal HSQLDB database
     */
    boolean usesInternal();
}
