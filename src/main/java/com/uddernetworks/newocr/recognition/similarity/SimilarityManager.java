package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;

public interface SimilarityManager {

    /**
     * Adds a {@link SimilarRule} to the internal list.
     *
     * @param rule The {@link SimilarRule} to add
     */
    void addSimilarity(SimilarRule rule);

    /**
     * When given a list of the potential results of a character (Irrelevant what character it is), this will find the
     * character lowest in the list that does not match the first character's letter and modifier to any of the added
     * {@link SimilarRule}s.
     *
     * @param data The possible combination data
     * @return If found, the second character
     */
    Optional<Object2DoubleMap.Entry<ImageLetter>> getSecondHighest(List<Object2DoubleMap.Entry<ImageLetter>> data);
}
