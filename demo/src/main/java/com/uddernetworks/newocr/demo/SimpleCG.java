package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class SimpleCG {

    private static Logger LOGGER = LoggerFactory.getLogger(SimpleCG.class);

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        new SimpleCG().mainInstance(args);
    }

    private void mainInstance(String[] args) throws IOException, InterruptedException, ExecutionException {
        boolean mono = args.length >= 1 && args[0].equalsIgnoreCase("mono");

        var databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db_" + (mono ? "mono" : "cms")));
        var scanner = new Scanner(System.in);
        var options = new OCROptions();
        if (mono) {
            System.out.println("Mono!");
            options.setSpecialSpaces('`', '\'')
                    .addRequireSizeCheck(PERIOD, EXCLAMATION_DOT, j_DOT, i_DOT); // Added by this
        } else {
            options.setSpecialSpaces('`');
        }

        var ocrScan = new OCRScan(databaseManager, options);
        var ocrTrain = new OCRTrain(databaseManager, options);

        LOGGER.info("Do you want to train? (y)es/no");

        var inputLine = args.length > 0 && args[0].equalsIgnoreCase("train") ? "yes" : scanner.nextLine();

        if ("yes".equalsIgnoreCase(inputLine) || "y".equalsIgnoreCase(inputLine)) {

            if (args.length >= 2) {
                String fontName = args[1];
                LOGGER.info("Generating image for " + fontName);
                new ComputerTrainGenerator().generateTrainingImage(new File("training.png"), new TrainGeneratorOptions()
                        .setFontFamily(fontName));
            }

            LOGGER.info("Starting training...");

            var start = System.currentTimeMillis();
            ocrTrain.trainImage(new File("training_" + (mono ? "mono" : "cms") + ".png"));

            LOGGER.info("Finished training in " + (System.currentTimeMillis() - start) + "ms");

            // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay
            // before killing the threads.
            TimeUnit.SECONDS.sleep(1L);
            databaseManager.shutdown();
            return;
        }

//         Warm up and load classes for everything, which can add over 1400ms to the first scan
        // TODO: Fully implement warming up
//        ocrScan.scanImage(new File("src\\main\\resources\\warmup.png"));

//        var scannedImage = ocrScan.scanImage(new File("test_" + (mono ? "mono" : "cms") + ".png"));
        var scannedImage = ocrScan.scanImage(new File("training_" + (mono ? "mono" : "cms") + ".png"));

        LOGGER.info("Got:\n" + scannedImage.getPrettyString());

        databaseManager.shutdown();
    }

}
