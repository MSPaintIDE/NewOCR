package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.OCRActions;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class IndTest {

    public static void main(String[] args) throws IOException, InterruptedException {
        var input = OCRUtils.readImage(new File("E:\\NewOCR\\ind2\\8.png"));
        var values = OCRUtils.createGrid(input);

        input = OCRUtils.filter(input).orElseThrow();

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);

        var databaseManager = new OCRDatabaseManager(new File("database" + File.separator + "ocr_db"));
        var actions = new OCRActions(databaseManager, new DefaultSimilarityManager().loadDefaults());

        var characters = new ArrayList<SearchCharacter>();
        actions.getLetters(searchImage, characters);

        var testing = characters.get(0);

        System.out.println("\n=========================\n");
        OCRUtils.printOut(testing.getValues());
        System.out.println("\n=========================");

        actions.getCharacterFor(testing).ifPresentOrElse(imageLetter -> {
            System.out.println("Found " + imageLetter.getLetter());
        }, () -> {
            System.out.println("Couldn't find a matching character!");
        });

        TimeUnit.SECONDS.sleep(1L);
        databaseManager.shutdown();
    }

}
