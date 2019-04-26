package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

/**
 * Similarity rule for horizontal lines.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class HorizontalLineSimilarityRule extends BasicSimilarityRule {

    public HorizontalLineSimilarityRule() {
        super("horizontal-line", MINUS, EQUALS_BOTTOM, EQUALS_TOP, UNDERSCORE);
    }
}
