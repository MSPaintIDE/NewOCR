package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

import java.util.EnumSet;
import java.util.Set;

/**
 * A simple {@link SimilarRule} that is used by {@link com.uddernetworks.newocr.configuration.HOCONFontConfiguration}
 * and by other default {@link SimilarRule}s.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class BasicSimilarityRule implements SimilarRule {

    private Set<Letter> characters;
    private String name;

    public BasicSimilarityRule(String name, Set<Letter> characters) {
        this.name = name;
        this.characters = EnumSet.copyOf(characters);
    }

    public BasicSimilarityRule(String name, Letter... characters) {
        this.name = name;
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
    public String getName() {
        return this.name;
    }

    @Override
    public boolean matchesLetter(ImageLetter first) {
        return this.characters.contains(Letter.getLetter(first));
    }
}
