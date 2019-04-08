package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class VerticalLineSimilarityRule extends BasicSimilarityRule {

    public VerticalLineSimilarityRule() {
        super(APOSTROPHE, QUOTE_LEFT, QUOTE_RIGHT, PIPE, l, i, EXCLAMATION);
    }
}
