package com.uddernetworks.newocr.database;

import com.uddernetworks.newocr.character.DatabaseCharacter;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The manager that provides methods to connect and interact with the database.
 */
public interface DatabaseManager {

    /**
     * Sets the name of the database. This has no effect on the actual function of the database.
     *
     * @param name The name of the database
     */
    void setName(String name);

    /**
     * Gets the name of the database, which is by default it is either the file name or URL (Whichever one is given).
     *
     * @return The name of the database
     */
    String getName();

    /**
     * Gets the {@link DataSource} used by the DatabaseManager
     *
     * @return The {@link DataSource} used by the DatabaseManager
     */
    DataSource getDataSource();

    /**
     * Inserts into the `letters` table with a modifier of 0.
     *
     * @param letter        The character to insert
     * @param averageWidth  The average width of the character
     * @param averageHeight The average height of the character
     * @param minCenter     The minimum relative center from the top found in the training ste for the font size
     * @param maxCenter     The maximum relative center from the top found in the training ste for the font size
     * @param isLetter      If the character is a letter (true) or if it is a space (false)
     */
    void createLetterEntry(char letter, double averageWidth, double averageHeight, double minCenter, double maxCenter, boolean isLetter);

    /**
     * Inserts into the `letters` table.
     *
     * @param letter        The character to insert
     * @param modifier      The modifier of the data, for multi-part characters such as "
     * @param averageWidth  The average width of the character
     * @param averageHeight The average height of the character
     * @param minCenter     The minimum relative center from the top found in the training ste for the font size
     * @param maxCenter     The maximum relative center from the top found in the training ste for the font size
     * @param isLetter      If the character is a letter (true) or if it is a space (false)
     */
    void createLetterEntry(char letter, int modifier, double averageWidth, double averageHeight, double minCenter, double maxCenter, boolean isLetter);

    /**
     * Clears all data revolving around a character from both the `letters` and `sectionData` table.
     *
     * @param letter The character to clear
     */
    void clearLetterSegments(char letter);

    /**
     * Adds segments (Percentage data points) to the database for a certain character, with a modifier of 0.
     *
     * @param letter   The character to add segments to
     * @param segments An array with a length of 17 all &lt;= 1 as percentage data points
     */
    void addLetterSegments(char letter, double[] segments);

    /**
     * Adds segments (Percentage data points) to the database for a certain character.
     *
     * @param letter   The character to add segments to
     * @param modifier The modifier of the letter
     * @param segments An array with a length of 17 all &lt;= 1 as percentage data points
     */
    void addLetterSegments(char letter, int modifier, double[] segments);

    /**
     * Gets all the {@link DatabaseCharacter}s in the database
     *
     * @return A Future of all the {@link DatabaseCharacter}s
     */
    Future<List<DatabaseCharacter>> getAllCharacterSegments();

    /**
     * Adds a piece of data in the database (Never overrides existing data) to be averaged and fetched later.
     *
     * @param name   The name of the data
     * @param values The value to be added
     */
    void addAveragedData(String name, double[] values);

    /**
     * Adds a piece of data in the database (Never overrides existing data) to be averaged and fetched later.
     *
     * @param name   The name of the data
     * @param values The value to be added
     */
    void addAveragedData(String name, DoubleList values);

    /**
     * Gets the average value of the given data name, added from {@link DatabaseManager#addAveragedData(String, double[])}.
     * If no data is found, it will return -1.
     *
     * @param name The name of the data to fetch and average
     * @return The averaged data, being -1 if no data is found
     */
    Future<Double> getAveragedData(String name);

    /**
     * Adds a custom between-character space amount for after a character, as some fonts have different padding after
     * certain character. This calculated with of padding after a character is subtracted from the amount needed for a
     * space.
     *
     * @param letter The letter before the space width
     * @param ratio  The width/height ratio of the space
     */
    void addCustomSpace(char letter, double ratio);

    /**
     * Gets the custom between-character space associated with the character, to appear after the character.
     *
     * @param letter The letter this space associates with
     * @return The custom between-character space width/height ratio, or 0 if no custom space is found
     */
    Future<Double> getCustomSpace(char letter);

    /**
     * Sets the boolean property to the database.
     *
     * @param name  The name of the property
     * @param value The property value
     */
    void setProperty(String name, boolean value);

    /**
     * Gets the boolean property by the given name.
     *
     * @param name The name of the property to get
     * @return The property value
     */
    Future<Optional<Boolean>> getProperty(String name);

    /**
     * Sets the database value as trained or not. This only changes a boolean in the database.
     *
     * @param trained If the database is trained or not
     */
    void setTrained(boolean trained);

    /**
     * Gets if the database has been trained already.
     *
     * @return If the database has been trained
     */
    Future<Optional<Boolean>> isTrained();

    /**
     * Gets if the database has been trained already. This differs from {@link DatabaseManager#isTrained()} because
     * this is done fully synchronous and defaults to false if not found.
     *
     * @return If the database has been trained
     */
    boolean isTrainedSync();

    /**
     * Clears all data in the database, primarily used for before training.
     */
    void clearData();

    /**
     * Shuts down all executor threads when the program is ready to be terminated.
     */
    void shutdown();

    /**
     * Shuts down all executor threads when the program is ready to be terminated after the specified time.
     *
     * @param unit The Unit of time
     * @param duration How long until it should shut down
     */
    void shutdown(TimeUnit unit, long duration);

    /**
     * Gets if the database manager is running off of the internal HSQLDB database or the external MySQL database.
     *
     * @return If the database manager is using the internal HSQLDB database
     */
    boolean usesInternal();
}
