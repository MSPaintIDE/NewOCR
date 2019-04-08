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
     * @return The current {@link SimilarityManager}
     */
    SimilarityManager addSimilarity(SimilarRule rule);

    /**
     * Removes a {@link SimilarRule} from the internal list if found.
     *
     * @param ruleClass The {@link SimilarRule} to add if found
     */
    SimilarityManager removeSimilarity(Class<? extends SimilarRule> ruleClass);

    /**
     * Finds any matching {@link SimilarRule}s for the first {@link ImageLetter}, and then checks if the second
     * {@link ImageLetter} also matches any of them.
     *
     * @param first The first {@link ImageLetter}
     * @param second The second {@link ImageLetter}
     * @return If the two {@link ImageLetter}s are similar
     */
    boolean isSimilar(ImageLetter first, ImageLetter second);

    /**
     * Gets the instance of {@link SimilarRule} with the given class.
     *
     * @param similarityRuleClass The class to get the instance of
     * @return A {@link SimilarRule} with the given class
     */
    Optional<SimilarRule> getRule(Class<? extends SimilarRule> similarityRuleClass);

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
