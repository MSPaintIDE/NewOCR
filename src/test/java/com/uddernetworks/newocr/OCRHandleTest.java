package com.uddernetworks.newocr;

import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import org.junit.Before;

import java.io.File;

public class OCRHandleTest {

    private final double ACCURACY = -0.9; // What the accuracy threshold of all tests should (Max of 1)

    private DatabaseManager databaseManager;
    private OCRHandle ocrHandle;
    private ScannedImage trainImage;

    @Before
    public void setUp() throws Exception {
        System.out.println("Setting up database...");
        this.databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        this.ocrHandle = new OCRHandle(this.databaseManager);

        System.out.println("Scanning training image...");
        this.trainImage = this.ocrHandle.scanImage(new File("src\\test\\resources\\size\\training.png"));
    }

    // TODO: Add tests :(

}
