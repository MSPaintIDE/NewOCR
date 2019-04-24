package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class HorizontalLineSimilarityRule extends BasicSimilarityRule {

    public HorizontalLineSimilarityRule() {
        super("horizontal-line", MINUS, EQUALS_BOTTOM, EQUALS_TOP, UNDERSCORE);
    }
}
