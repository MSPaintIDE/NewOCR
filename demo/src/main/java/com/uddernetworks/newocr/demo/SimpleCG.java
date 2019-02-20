package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.OCRHandle;
import com.uddernetworks.newocr.database.OCRDatabaseManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SimpleCG {

    private BufferedImage original;
    private int width;
    private File output = new File("E:\\TestOCR\\output.png");
    private static final int BLACK = Color.BLACK.getRGB();
    private static final int WHITE = Color.WHITE.getRGB();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        new SimpleCG().main();
    }

    private void main() throws IOException, InterruptedException, ExecutionException {
        var databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        var scanner = new Scanner(System.in);
        var ocrHandle = new OCRHandle(databaseManager);

        System.setProperty("newocr.debug", "true");

        System.out.println("Do you want to train? (y)es/no");

        var inputLine = scanner.nextLine();

        if ("yes".equalsIgnoreCase(inputLine) || "y".equalsIgnoreCase(inputLine)) {
            System.out.println("Generating features...");
            var start = System.currentTimeMillis();
            ocrHandle.trainImage(new File("training.png"));
            System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");
            // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay
            // before killing the threads.
            TimeUnit.SECONDS.sleep(1L);
            databaseManager.shutdown();
            return;
        }

//         Warm up and load classes for everything, which can add over 1400ms to the first scan
//        ocrHandle.scanImage(new File("src\\main\\resources\\warmup.png"));

        System.out.println("Real scanning:");

        var scannedImage = ocrHandle.scanImage(new File("tresttt.png"));

        System.out.println("Got:\n" + scannedImage.getPrettyString());

        databaseManager.shutdown();
    }

}
