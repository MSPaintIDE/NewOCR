package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class DotSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        switch (Letter.getLetter(first)) {
            case PERIOD:
            case COLON_TOP:
            case COLON_BOTTOM:
            case EXCLAMATION_DOT:
            case SEMICOLON_BOTTOM:
            case i_DOT:
            case j_DOT:
            case QUESTION_MARK_BOTTOM:
                return true;
            default:
                return false;
        }
    }
}
