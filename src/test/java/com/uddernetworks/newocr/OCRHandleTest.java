package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;

public class OCRHandleTest {

    private final double ACCURACY = -0.9; // What the accuracy threshold of all tests should (Max of 1)

    private DatabaseManager databaseManager;
    private OCRHandle ocrHandle;
    private ScannedImage trainImage;

    @Before
    public void setUp() throws Exception {
        System.out.println("Setting up database...");
        this.databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        this.ocrHandle = new OCRHandle(this.databaseManager);

        System.out.println("Scanning training image...");
        this.trainImage = this.ocrHandle.scanImage(new File("src\\test\\resources\\size\\training.png"));
    }

    @Test
    public void characterSizeRecognizer() throws ExecutionException, InterruptedException {
        int characterDepth = 20;

        List<Double> def = new ArrayList<>();
        List<Double> gen = new ArrayList<>();
        for (int i = 0; i < this.trainImage.getLineCount() * characterDepth; i++) {
            def.add((double) i);
            gen.add(0D);
        }

        for (int i = 0; i < this.trainImage.getLineCount(); i++) {
            for (int i1 = 0; i1 < characterDepth; i1++) {
                ImageLetter firstOfLine = this.trainImage.getLine(i).get(i1);

                if (firstOfLine.getLetter() == ' ') { // Ignore spaces (Not found in the database)
                    def.set(i * characterDepth + i1, -2D); // The -2 values will be ignored later
                    gen.set(i * characterDepth + i1, -2D);
                    continue;
                }

                Future<Integer> fontSize = this.ocrHandle.getFontSize(firstOfLine);
                gen.set(i * characterDepth + i1, (double) fontSize.get());
            }
        }

        double[] defArray = def.stream().filter(val -> val != -2).mapToDouble(Double::doubleValue).toArray();
        double[] genArray = gen.stream().filter(val -> val != -2).mapToDouble(Double::doubleValue).toArray();

        double coeff = Math.abs(new PearsonsCorrelation().correlation(defArray, genArray));

        System.out.println("Accuracy is " + coeff);

        assertTrue(coeff >= ACCURACY);
    }
}
