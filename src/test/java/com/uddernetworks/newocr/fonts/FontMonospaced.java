package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.train.OCROptions;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class FontMonospaced extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        trainImage = generate("Monospaced.plain", new OCROptions().setSpecialSpaces('`', '\'', '|', '{', '}')
                .addRequireSizeCheck(PERIOD, EXCLAMATION_DOT, j_DOT, i_DOT));
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
