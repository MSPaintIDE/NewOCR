package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.train.TrainOptions;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SimpleCG {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        new SimpleCG().mainInstance(args);
    }

    private void mainInstance(String[] args) throws IOException, InterruptedException, ExecutionException {
        var databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        var scanner = new Scanner(System.in);
        var ocrScan = new OCRScan(databaseManager);
        var ocrTrain = new OCRTrain(databaseManager);

        System.setProperty("newocr.debug", "true");

        System.out.println("Do you want to train? (y)es/no");

        var inputLine = args.length > 0 && args[0].equalsIgnoreCase("train") ? "yes" : scanner.nextLine();

        if ("yes".equalsIgnoreCase(inputLine) || "y".equalsIgnoreCase(inputLine)) {
            System.out.println("Generating features...");

            var start = System.currentTimeMillis();
            ocrTrain.trainImage(new File("training.png"), new TrainOptions() {{
                setSpecialSpaces('`');
            }});

            System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");
            // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay
            // before killing the threads.
            TimeUnit.SECONDS.sleep(1L);
            databaseManager.shutdown();
            return;
        }

//         Warm up and load classes for everything, which can add over 1400ms to the first scan
        // TODO: Fully implement warming up
        ocrScan.scanImage(new File("src\\main\\resources\\warmup.png"));

        var scannedImage = ocrScan.scanImage(new File("training.png"));

        System.out.println("Got:\n" + scannedImage.getPrettyString());

        databaseManager.shutdown();
    }

}
