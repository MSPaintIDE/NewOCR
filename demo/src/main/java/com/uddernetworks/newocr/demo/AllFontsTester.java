package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;
import com.uddernetworks.newocr.train.TrainOptions;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.DELETE;
import static org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation.EQUAL;

public class AllFontsTester {

    public static void main(String[] args) throws IOException {

        var databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        var ocrScan = new OCRScan(databaseManager);
        var ocrTrain = new OCRTrain(databaseManager);

        var testAll = new File("test_all.png");

        Font[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts(); // Get the fontNames
        double total = fontNames.length;
        for (int i = 0; i < fontNames.length; i++) {
            var fontName = fontNames[i].getFontName();

            try {
                new ComputerTrainGenerator().generateTrainingImage(testAll, new TrainGeneratorOptions()
                        .setFontFamily(fontName)
                        .setMaxFontSize(90)
                        .setMinFontSize(30));

                ocrTrain.trainImage(testAll, new TrainOptions().setSpecialSpaces('`'));

                var scannedString = ocrScan.scanImage(testAll).getPrettyString();
                var diffMatchPath = new DiffMatchPatch();
                var lines = scannedString.split("\n");
                var differences = 0;
                for (String line : lines) {
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

                addAccuracy(fontName, accuracy);
            } catch (Exception e) {
                e.printStackTrace();
                addAccuracy(fontName, -1);
            }

            System.err.println(i + "/" + ((int) total) + " done (" + (total / (double) i) + "%)");
        }
    }

    private static void addAccuracy(String fontName, double accuracy) throws IOException {
        File file = new File("append.txt");
        FileWriter fr = new FileWriter(file, true);
        if (accuracy == -1) {
            fr.write(fontName + ",error\n");
        } else {
            fr.write(fontName + "," + accuracy + "%\n");
        }
        fr.close();
    }

}
