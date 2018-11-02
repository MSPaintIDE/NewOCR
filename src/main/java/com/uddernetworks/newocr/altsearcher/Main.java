package com.uddernetworks.newocr.altsearcher;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException { // alphabet48
        new Main().run(args);
    }

    private void run(String[] args) throws IOException {
        DatabaseManager databaseManager = new DatabaseManager(args[0], args[1], args[2]);

        Scanner scanner = new Scanner(System.in);

        OCRHandle ocrHandle = new OCRHandle(databaseManager);

        System.out.println("Do you want to train? yes/no");

        String inputLine = scanner.nextLine();
        if (inputLine.equalsIgnoreCase("yes") || inputLine.equalsIgnoreCase("y")) {
            System.out.println("Generating features...");
            long start = System.currentTimeMillis();
            ocrHandle.trainImage(new File("E:\\NewOCR\\training.png"));
            System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");
            System.exit(0);
        }

        ScannedImage scannedImage = ocrHandle.scanImage(new File("E:\\NewOCR\\HWTest.png"));
        System.out.println("Got:\n" + scannedImage.getPrettyString());
    }

}
