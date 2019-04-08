package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class HorizontalLineSimilarityRule extends BasicSimilarityRule {

    public HorizontalLineSimilarityRule() {
        super(MINUS, EQUALS_BOTTOM, EQUALS_TOP, UNDERSCORE);
    }
}
