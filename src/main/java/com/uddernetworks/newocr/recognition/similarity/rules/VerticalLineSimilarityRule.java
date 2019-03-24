package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class VerticalLineSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        var letter = first.getLetter();
        var mod = first.getModifier();
        return (letter == '\'')
                || (letter == '"')
                || (letter == 'i' && mod == 1);
    }
}
