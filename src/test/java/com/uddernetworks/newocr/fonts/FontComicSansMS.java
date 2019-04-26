package com.uddernetworks.newocr.fonts;

import com.uddernetworks.newocr.FontTestNameGenerator;
import com.uddernetworks.newocr.recognition.ScannedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(FontTestNameGenerator.class)
public class FontComicSansMS extends Accuracy {

    private static ScannedImage trainImage;

    @BeforeAll
    public static void setUp() throws Exception {
        trainImage = generate("Comic Sans MS", "fonts/ComicSans");
    }

    @Test
    public void accuracyTest() {
        super.accuracyTest(trainImage);
    }

}
