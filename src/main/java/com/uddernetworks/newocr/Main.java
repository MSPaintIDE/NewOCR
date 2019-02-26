package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException { // alphabet48
        new Main().sections();
//        new Main().run(args);
        //new Main().getSections(args);
    }

    private void sections() {
        var realPeriod = new double[]{0.8333333333333334, 1.0, 1.0, 0.8333333333333334, 1.0, 0.8571428571428571, 0.8, 1.0, 0.75, 1.0, 0.75, 1.0, 1.0, 1.0, 0.75, 1.0, 0.75};
        var databasePeriod = new double[] {0.7642246642246642, 0.8632478632478632, 0.7777777777777778, 0.8427350427350427, 0.8646316646316646, 0.79993894993895, 0.5666666666666667, 1, 0.47435897435897434, 1, 0.44943019943019946, 0.9726495726495727, 1, 0.9602564102564103, 0.48005698005698005, 1, 0.44943019943019946};

        var realA = new double[] {0.4519230769230769, 0.5, 0.4777777777777778, 0.532608695652174, 0.4895833333333333, 0.5656565656565656, 0.6041666666666666, 0.46464646464646464, 0.375, 0.6222222222222222, 0.5694444444444444, 0.7272727272727273, 0.0, 0.625, 0.5833333333333334, 0.5555555555555556, 0.6944444444444444};
        var databaseA = new double[] {0.4879910157503004, 0.5141307532100219, 0.5099396320252035, 0.5617981610335039, 0.5247199687519521, 0.5697274518994554, 0.6605900306224911, 0.4887968254598567, 0.43351970323124167, 0.633487275999977, 0.6064323141246218, 0.6870607347794376, 0, 0.6234997112825167, 0.6168965293965295, 0.5919030832544029, 0.7346883707460631};

        OCRUtils.getDifferencesFrom(realPeriod, databasePeriod).ifPresent(charDifference -> {
            var value = Arrays.stream(charDifference).average().orElse(-1);
            System.out.println("Arr diff:\n\t" + Arrays.toString(charDifference));
            System.out.println("Difference from real period and database: " + value);
        });

        OCRUtils.getDifferencesFrom(realA, databaseA).ifPresent(charDifference -> {
            var value = Arrays.stream(charDifference).average().orElse(-1);
            System.out.println("\n\nArr diff:\n\t" + Arrays.toString(charDifference));
            System.out.println("Difference from real A and database: " + value);
        });

        OCRUtils.getDifferencesFrom(realA, realPeriod).ifPresent(charDifference -> {
            var value = Arrays.stream(charDifference).average().orElse(-1);
            System.out.println("\n\nDifference real A and real period: " + value);
        });

        OCRUtils.getDifferencesFrom(databaseA, databasePeriod).ifPresent(charDifference -> {
            var value = Arrays.stream(charDifference).average().orElse(-1);
            System.out.println("\n\nDifference DATABASE A and real period: " + value);
        });
    }

    private void run(String[] args) throws IOException, InterruptedException, ExecutionException {
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

        var scannedImage = ocrHandle.scanImage(new File("HWTest.png"));

        System.out.println("Got:\n" + scannedImage.getPrettyString());

        databaseManager.shutdown();
    }

    private void getSections(String[] args) throws IOException {
        BufferedImage input = OCRUtils.readImage(new File("E.png"));
        boolean[][] values = OCRUtils.createGrid(input);

        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values);

        List<IntPair> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                if (searchImage.getValue(x, y)) {
                    searchImage.scanFrom(x, y, coordinates);
                    break;
                }
            }
        }

        SearchCharacter searchCharacter = new SearchCharacter(coordinates);
        searchCharacter.applySections();

        System.out.println(searchCharacter.getSegments());
        System.out.println(searchCharacter.getSegments().stream().map(entry -> (double) entry.getKey() / (double) entry.getValue()).collect(Collectors.toList()));
    }

}
