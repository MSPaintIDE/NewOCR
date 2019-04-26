package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.PERCENT_LDOT;
import static com.uddernetworks.newocr.recognition.similarity.Letter.PERCENT_RDOT;

/**
 * Similarity rule for percent circles/dots.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class PercentDotSimilarityRule extends BasicSimilarityRule {

    public PercentDotSimilarityRule() {
        super("percent-dot", PERCENT_LDOT, PERCENT_RDOT);
    }
}
