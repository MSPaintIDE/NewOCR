package com.uddernetworks.newocr.recognition.similarity.rules;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

public class DotSimilarityRule extends BasicSimilarityRule {

    public DotSimilarityRule() {
        super("dot", PERIOD, COLON_TOP, COLON_BOTTOM, EXCLAMATION_DOT, SEMICOLON_TOP, i_DOT, j_DOT, QUESTION_MARK_BOTTOM);
    }
}
