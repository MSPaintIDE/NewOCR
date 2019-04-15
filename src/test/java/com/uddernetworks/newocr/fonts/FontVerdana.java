package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.ScannedImage;
import org.junit.BeforeClass;
import org.junit.Test;

public class FontVerdana extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeClass
    public static void setUp() throws Exception {
        trainImage = generate("Verdana", "fonts/Verdana");
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
