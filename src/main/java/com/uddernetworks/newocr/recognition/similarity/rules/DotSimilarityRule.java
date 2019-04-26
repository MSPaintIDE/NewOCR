package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

/**
 * Similarity rule for dot-like characters.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class DotSimilarityRule extends BasicSimilarityRule {

    public DotSimilarityRule() {
        super("dot", PERIOD, COLON_TOP, COLON_BOTTOM, EXCLAMATION_DOT, SEMICOLON_TOP, i_DOT, j_DOT, QUESTION_MARK_BOTTOM);
    }
}
