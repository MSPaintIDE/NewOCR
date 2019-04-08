package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

import java.util.EnumSet;
import java.util.Set;

/**
 * Matches characters together with the same modifier of 0
 */
public class BasicSimilarityRule implements SimilarRule {

    private Set<Letter> characters;

    public BasicSimilarityRule(Set<Letter> characters) {
        this.characters = EnumSet.copyOf(characters);
    }

    public BasicSimilarityRule(Letter... characters) {
        this.characters = characters.length > 0 ?
                EnumSet.of(characters[0], characters) :
                EnumSet.noneOf(Letter.class);
    }

    public BasicSimilarityRule addLetter(Letter letter) {
        this.characters.add(letter);
        return this;
    }

    public void removeLetter(Letter letter) {
        this.characters.remove(letter);
    }

    @Override
    public boolean matchesLetter(ImageLetter first) {
        return this.characters.contains(Letter.getLetter(first));
    }
}
