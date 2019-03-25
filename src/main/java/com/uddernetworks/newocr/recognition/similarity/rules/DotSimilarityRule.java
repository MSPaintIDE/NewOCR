package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class DotSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        var letter = first.getLetter();
        var mod = first.getModifier();
        return (letter == '.')
                || (letter == ':')
                || (letter == '!' && mod == 1)
                || (letter == ';' && mod == 0)
                || (letter == 'i' && mod == 0)
                || (letter == 'j' && mod == 0)
                || (letter == '?' && mod == 0);
    }
}
