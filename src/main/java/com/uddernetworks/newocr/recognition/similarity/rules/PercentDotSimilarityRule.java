package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.PERCENT_LDOT;
import static com.uddernetworks.newocr.recognition.similarity.Letter.PERCENT_RDOT;

public class PercentDotSimilarityRule extends BasicSimilarityRule {

    public PercentDotSimilarityRule() {
        super(PERCENT_LDOT, PERCENT_RDOT);
    }
}
