package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.train.OCROptions;
import org.junit.BeforeClass;
import org.junit.Test;

public class FontComicSansMS extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        trainImage = generate("Comic Sans MS", new OCROptions().setSpecialSpaces('`', '\''));
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
