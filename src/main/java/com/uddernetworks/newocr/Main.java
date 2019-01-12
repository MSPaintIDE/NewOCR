package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.utils.OCRUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException { // alphabet48
        new Main().run(args);
//        new Main().getSections(args);
    }

    private void run(String[] args) throws IOException, InterruptedException {
        DatabaseManager databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));

        Scanner scanner = new Scanner(System.in);

        OCRHandle ocrHandle = new OCRHandle(databaseManager);

        System.setProperty("newocr.debug", "true");

        System.out.println("Do you want to train? yes/no");

        String inputLine = scanner.nextLine();
        if (inputLine.equalsIgnoreCase("yes") || inputLine.equalsIgnoreCase("y")) {
            System.out.println("Generating features...");
            long start = System.currentTimeMillis();
            ocrHandle.trainImage(new File("training.png"));
            System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");

            Thread.sleep(1000); // HSQLDB freaks out and kills the database file after writing if it doesn't have some kind of delay before killing the threads
            databaseManager.shutdown();
            return;
        }

        ScannedImage scannedImage = ocrHandle.scanImage(new File("HWTest.png"));
        System.out.println("Got:\n" + scannedImage.getPrettyString());

        databaseManager.shutdown();
    }

    private void getSections(String[] args) throws IOException {
        BufferedImage input = OCRUtils.readImage(new File("E.png"));
        boolean[][] values = OCRUtils.createGrid(input);

        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values);

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

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
