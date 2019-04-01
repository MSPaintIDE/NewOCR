package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

public class VerticalLineSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        switch (Letter.getLetter(first)) {
            case APOSTROPHE:
            case QUOTE_LEFT:
            case QUOTE_RIGHT:
            case PIPE:
            case l:
            case i:
            case EXCLAMATION:
                return true;
            default:
                return false;
        }
    }
}
