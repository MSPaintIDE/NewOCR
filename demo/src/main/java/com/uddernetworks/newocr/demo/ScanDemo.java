package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ScanDemo {

    private static Logger LOGGER = LoggerFactory.getLogger(ScanDemo.class);

    public static void main(String[] args) throws IOException {
        var databaseManager = new OCRDatabaseManager(new File("database\\ocr_db_traindemo"));
        var similarityManager = new DefaultSimilarityManager();
        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);

        if (!databaseManager.isTrainedSync()) {
            LOGGER.error("The database has not been trained yet! Please run com.uddernetworks.newocr.demo.TrainDemo to train it and try again.");
            databaseManager.shutdown(TimeUnit.SECONDS, 1L);
            return;
        }

        var fontConfiguration = new HOCONFontConfiguration("fonts/ComicSans", new ConfigReflectionCacher(), similarityManager, mergenceManager);
        var ocrScan = new OCRScan(databaseManager, fontConfiguration.fetchOptions(), similarityManager);

        LOGGER.info("Starting training...");

        var start = System.currentTimeMillis();

        ScannedImage scannedImage = ocrScan.scanImage(new File("demo\\src\\main\\resources\\scan.png"));

        LOGGER.info("Got:\n" + scannedImage.getPrettyString());

        LOGGER.info("Finished scanning in " + (System.currentTimeMillis() - start) + "ms");

        // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay
        // before killing the threads.
        databaseManager.shutdown(TimeUnit.SECONDS, 1L);
    }

}
