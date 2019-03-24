package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class PercentDotSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        return first.getLetter() == '%' && first.getModifier() <= 1;
    }
}
