package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.FontConfiguration;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.recognition.ScannedImage;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
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

import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.DELETE;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.EQUAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Accuracy {

    private static Logger LOGGER = LoggerFactory.getLogger(Accuracy.class);
    private static final double MINIMUM_SUCCESS_RATE = 98; // Requires at least a 98% success rate
    private static final boolean TRIM_SPACES = true; // If spaces before the input should be trimmed, to take into account input image padding

    public static ScannedImage generate(String fontFamily, String configFileName) throws IOException {
        var strippedName = fontFamily.replaceAll("[^a-zA-Z\\d\\s:]", "_");
        var databaseManager = new OCRDatabaseManager(new File("src\\test\\resources\\database\\ocr_db_" + strippedName));
        var similarityManager = new DefaultSimilarityManager();

        var fontConfiguration = new HOCONFontConfiguration(configFileName, new ConfigReflectionCacher());
        var options = fontConfiguration.fetchOptions();
        fontConfiguration.fetchAndApplySimilarities(similarityManager);

        return generate(fontFamily, options, similarityManager, databaseManager, fontConfiguration);
    }

    public static ScannedImage generate(String fontFamily, OCROptions options, SimilarityManager similarityManager, DatabaseManager databaseManager, FontConfiguration fontConfiguration) {
        LOGGER.info("Setting up database...");

        var readingImage = new File("src\\test\\resources\\training_" + fontFamily.replaceAll("[^a-zA-Z\\d\\s:]", "_") + ".png");

        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);
        var ocrTrain = new OCRTrain(databaseManager, options);

        LOGGER.info("Generating image for {}", fontFamily);
        new ComputerTrainGenerator().generateTrainingImage(readingImage, new TrainGeneratorOptions()
                .setFontFamily(fontFamily));

        LOGGER.info("Starting training for {}...", fontFamily);

        var start = System.currentTimeMillis();
        ocrTrain.trainImage(readingImage);

        LOGGER.info("Finished training in {}ms", System.currentTimeMillis() - start);

        // It needs some kind of delay, I need to inspect in the future on what causes this. Check the repo for any
        // issues on the matter before reporting.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {}

        fontConfiguration.fetchAndApplyMergeRules(mergenceManager);
        var ocrScan = new OCRScan(databaseManager, options, similarityManager, mergenceManager);

        LOGGER.info("Scanning training image...");

        return ocrScan.scanImage(readingImage);
    }

    public void accuracyTest(ScannedImage trainImage) {
        var scannedString = trainImage.getPrettyString();
        var diffMatchPath = new DiffMatchPatch();
        var lines = scannedString.split("\n");
        var differences = 0;
        for (String line : lines) {
            line = TRIM_SPACES ? line.trim() : line;
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

            System.out.println(line);
        }

        var totalChars = lines.length * OCRScan.RAW_STRING.length();
        var accuracy = (Math.round((1 - (double) differences / (double) totalChars) * 100_00D) / 100D);
        LOGGER.info("{} errors out of {} at a {}% success rate", differences, totalChars, accuracy);

        assertTrue(accuracy >= MINIMUM_SUCCESS_RATE); // We're looking for at *least* a 95% success rate
    }

}
