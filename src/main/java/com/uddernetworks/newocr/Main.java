package com.uddernetworks.newocr;

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

    public static void main(String[] args) throws IOException { // alphabet48
//        new Main().run(args);
        new Main().getSections(args);
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

    private void getSections(String[] args) throws IOException {
        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\E.png"));
        boolean[][] values = OCRUtils.createGrid(input);

        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

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

        // 1 - top left left
        // 2 - top left right
        // 3 - top right right
        // 4 - top right left
        // 5 - bottom left right
        // 6 - bottom left left
        // 7 -
        // 8 -

        System.out.println(searchCharacter.getSegments());
        System.out.println(searchCharacter.getSegments().stream().map(entry -> (double) entry.getKey() / (double) entry.getValue()).collect(Collectors.toList()));
    }

}
