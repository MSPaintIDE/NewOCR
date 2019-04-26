package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.database.DatabaseManager;

/**
 * An exception thrown when a database has not been trained, and it tries to scan an image.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class UntrainedDatabaseException extends RuntimeException {

    public UntrainedDatabaseException(DatabaseManager databaseManager) {
        super("The given database " + databaseManager.getName() + " has not been trained yet.");
    }
}
