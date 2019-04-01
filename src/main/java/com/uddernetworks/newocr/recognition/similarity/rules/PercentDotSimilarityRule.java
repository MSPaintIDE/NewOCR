package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class PercentDotSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        switch (Letter.getLetter(first)) {
            case PERCENT_LDOT:
            case PERCENT_RDOT:
                return true;
            default:
                return false;
        }
    }
}
