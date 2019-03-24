package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Matches characters together with the same modifier of 0
 */
public abstract class BasicSimilarityRule implements SimilarRule {

    private Set<Character> characters;

    public BasicSimilarityRule(char... characters) {
        this.characters = IntStream.range(0, characters.length).mapToObj(x -> characters[x]).collect(Collectors.toSet());
    }

    @Override
    public boolean matchesLetter(ImageLetter first) {
        return this.characters.contains(first.getLetter());
    }
}
