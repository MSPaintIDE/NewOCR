package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TrainDemo {

    private static Logger LOGGER = LoggerFactory.getLogger(TrainDemo.class);

    public static void main(String[] args) throws IOException {
        var databaseManager = new OCRDatabaseManager(new File("database\\ocr_db_traindemo"));
        var similarityManager = new DefaultSimilarityManager();
        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);

        var fontConfiguration = new HOCONFontConfiguration("fonts/ComicSans", new ConfigReflectionCacher(), similarityManager, mergenceManager);
        var ocrTrain = new OCRTrain(databaseManager, fontConfiguration.fetchOptions(), similarityManager);

        var trainImage = new File("demo\\src\\main\\resources\\training.png");

        new ComputerTrainGenerator().generateTrainingImage(trainImage, new TrainGeneratorOptions().setFontFamily("Comic Sans MS"));

        LOGGER.info("Starting training...");

        var start = System.currentTimeMillis();
        ocrTrain.trainImage(trainImage);

        LOGGER.info("Finished training in " + (System.currentTimeMillis() - start) + "ms");

        // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay
        // before killing the threads.
        databaseManager.shutdown(TimeUnit.SECONDS, 1L);
    }

}
