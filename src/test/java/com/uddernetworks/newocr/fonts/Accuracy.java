package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.DELETE;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.EQUAL;
import static org.junit.Assert.assertTrue;

public class Accuracy {

    private static Logger LOGGER = LoggerFactory.getLogger(Accuracy.class);
    private static final double MINIMUM_SUCCESS_RATE = 95; // Requires at least a 95% success rate

    public static ScannedImage generate(String fontFamily) throws IOException, ExecutionException, InterruptedException {
        return generate(fontFamily, new OCROptions(), new DefaultSimilarityManager().loadDefaults());
    }

    public static ScannedImage generate(String fontFamily, OCROptions options) throws IOException, ExecutionException, InterruptedException {
        return generate(fontFamily, options, new DefaultSimilarityManager().loadDefaults());
    }

    public static ScannedImage generate(String fontFamily, OCROptions options, SimilarityManager similarityManager) throws IOException, ExecutionException, InterruptedException {
        LOGGER.info("Setting up database...");

        var strippedName = fontFamily.replaceAll("[^a-zA-Z\\d\\s:]", "_");
        var readingImage = new File("src\\test\\resources\\training_" + strippedName + ".png");
        var databaseManager = new OCRDatabaseManager(new File("src\\test\\resources\\database\\ocr_db_" + strippedName));

        var ocrHandle = new OCRScan(databaseManager, options, similarityManager);
        var ocrTrain = new OCRTrain(databaseManager, options, similarityManager);

        LOGGER.info("Generating image for {}", fontFamily);
        new ComputerTrainGenerator().generateTrainingImage(readingImage, new TrainGeneratorOptions()
                .setFontFamily(fontFamily));

        LOGGER.info("Starting training for {}...", fontFamily);

        var start = System.currentTimeMillis();
        ocrTrain.trainImage(readingImage);

        LOGGER.info("Finished training in {}ms", System.currentTimeMillis() - start);

        LOGGER.info("Scanning training image...");

        return ocrHandle.scanImage(readingImage);
    }

    public void accuracyTest(ScannedImage trainImage) {
        var scannedString = trainImage.getPrettyString();
        System.out.println(scannedString);
        var diffMatchPath = new DiffMatchPatch();
        var lines = scannedString.split("\n");
        var differences = 0;
        for (String line : lines) {
            var difference = diffMatchPath.diffMain(line, OCRScan.RAW_STRING);
            final int[] insert = {0};
            final int[] delete = {0};
            difference.stream().filter(diff -> diff.operation != EQUAL)
                    .forEach(diff -> {
                        if (diff.operation == DELETE) {
                            delete[0] += diff.text.length();
                        } else {
                            insert[0] += diff.text.length();
                        }
                    });
            differences += Math.max(insert[0], delete[0]);
        }

        var totalChars = lines.length * OCRScan.RAW_STRING.length();
        var accuracy = (Math.round((1 - (double) differences / (double) totalChars) * 100_00D) / 100D);
        LOGGER.info("{} errors out of {} at a {}% success rate", differences, totalChars, accuracy);

        assertTrue(accuracy >= MINIMUM_SUCCESS_RATE); // We're looking for at *least* a 95% success rate
    }

}
