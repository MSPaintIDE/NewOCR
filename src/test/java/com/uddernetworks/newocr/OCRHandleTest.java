package com.uddernetworks.newocr;

import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.recognition.Scan;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.DELETE;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.EQUAL;
import static org.junit.Assert.assertTrue;

public class OCRHandleTest {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRHandleTest.class);
    private static final double MINIMUM_SUCCESS_RATE = 95; // Requires at least a 95% success rate

    private static DatabaseManager databaseManager;
    private static Scan ocrHandle;
    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        LOGGER.info("Setting up database...");
        databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        ocrHandle = new OCRScan(databaseManager);

        LOGGER.info("Scanning training image...");
        trainImage = ocrHandle.scanImage(new File("src\\test\\resources\\training.png"));
    }

    @Test
    public void accuracyTest() {
        var scannedString = trainImage.getPrettyString();
        System.out.println(scannedString);
        var diffMatchPath = new DiffMatchPatch();
        var lines = scannedString.split("\n");
        var differences = 0;
        for (String line : lines) {
//            if (line.contains("         ")) continue;
//            System.out.println(line);
            var difference = diffMatchPath.diffMain(line, OCRTrain.TRAIN_STRING);
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

        var totalChars = lines.length * OCRTrain.TRAIN_STRING.length();
        var accuracy = (Math.round((1 - (double) differences / (double) totalChars) * 100_00D) / 100D);
        LOGGER.info(differences + " errors out of " + totalChars + " at a " + accuracy + "% success rate");

        assertTrue(accuracy >= MINIMUM_SUCCESS_RATE); // We're looking for at *least* a 95% success rate
    }

}
