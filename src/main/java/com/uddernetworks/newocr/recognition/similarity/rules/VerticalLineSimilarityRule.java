package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

/**
 * Similarity rule for vertical lines.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class VerticalLineSimilarityRule extends BasicSimilarityRule {

    public VerticalLineSimilarityRule() {
        super("vertical-line", APOSTROPHE, QUOTE_LEFT, QUOTE_RIGHT, PIPE, l, i, EXCLAMATION);
    }
}
