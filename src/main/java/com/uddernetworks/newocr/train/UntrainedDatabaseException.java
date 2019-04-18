package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.database.DatabaseManager;

public class UntrainedDatabaseException extends RuntimeException {

    public UntrainedDatabaseException(DatabaseManager databaseManager) {
        super("The given database " + databaseManager.getName() + " has not been trained yet.");
    }
}
