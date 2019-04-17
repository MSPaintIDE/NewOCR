package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.FontTestNameGenerator;
import com.uddernetworks.newocr.ScannedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(FontTestNameGenerator.class)
public class FontMSGothic extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeAll
    public static void setUp() throws Exception {
        trainImage = generate("MS Gothic", "fonts/MSGothic");
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
