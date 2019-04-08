package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.rules.VerticalLineSimilarityRule;
import com.uddernetworks.newocr.train.OCROptions;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class FontComicSansMS extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        trainImage = generate("Comic Sans MS", new OCROptions().setSpecialSpaces('`', '\''), new DefaultSimilarityManager()
                .loadDefaults()
                .removeSimilarity(VerticalLineSimilarityRule.class)
                .addSimilarity(new VerticalLineSimilarityRule()
                        .addLetter(PERIOD)
                        .addLetter(COLON_TOP)
                        .addLetter(COLON_BOTTOM)
                        .addLetter(EXCLAMATION_DOT)
                        .addLetter(SEMICOLON_TOP)
                        .addLetter(i_DOT)
                        .addLetter(j_DOT)
                        .addLetter(QUESTION_MARK_BOTTOM)
                ));
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
