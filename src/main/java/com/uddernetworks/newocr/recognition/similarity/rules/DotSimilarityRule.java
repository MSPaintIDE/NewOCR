package com.uddernetworks.newocr.recognition.similarity.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;

public class DotSimilarityRule implements SimilarRule {

    @Override
    public boolean matchesLetter(ImageLetter first) {
        return false;
    }

    @Override
    public Optional<Object2DoubleMap.Entry<ImageLetter>> process(List<Object2DoubleMap.Entry<ImageLetter>> data) {
        // TODO: Implement
        return Optional.empty();
    }
}
