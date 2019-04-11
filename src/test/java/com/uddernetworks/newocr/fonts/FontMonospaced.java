package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import org.junit.BeforeClass;
import org.junit.Test;

public class FontMonospaced extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        trainImage = generate("Monospaced.plain", "fonts/Monospaced.plain");
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
