package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;

public interface SimilarRule {

    /**
     * Gets if the current rule matches with the given {@link ImageLetter} and is allowed to be processed.
     *
     * @param first The first {@link ImageLetter} of the data
     * @return If the given {@link ImageLetter} can be processed by the current rule
     */
    boolean matchesLetter(ImageLetter first);

    /**
     * Gets the name of the rule.
     *
     * @return The name of the rule
     */
    String getName();

    /**
     * When given a list of the potential results of a character (Irrelevant what character it is), this will find the
     * character lowest in the list that does not match the first character's letter and modifier to the current
     * rule.
     *
     * @param data The possible combination data
     * @return If found, the second character
     */
    default Optional<Object2DoubleMap.Entry<ImageLetter>> process(List<Object2DoubleMap.Entry<ImageLetter>> data) {
        for (var entry : data) {
            if (matchesLetter(entry.getKey())) continue;
            return Optional.of(entry);
        }

        return Optional.empty();
    }
}
