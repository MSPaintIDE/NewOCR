package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.FontTestNameGenerator;
import com.uddernetworks.newocr.recognition.ScannedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(FontTestNameGenerator.class)
public class FontVerdana extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeAll
    public static void setUp() throws Exception {
        trainImage = generate("Verdana", "fonts/Verdana");
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
