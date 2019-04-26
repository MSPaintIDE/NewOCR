package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The manager for {@link SimilarRule}s, usually derived from
 * {@link com.uddernetworks.newocr.configuration.FontConfiguration}s.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
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
     * @return The current {@link SimilarityManager}
     */
    SimilarityManager removeSimilarity(Class<? extends SimilarRule> ruleClass);

    /**
     * Finds any matching {@link SimilarRule}s for the first {@link ImageLetter}, and then checks if the second
     * {@link ImageLetter} also matches any of them.
     *
     * @param first  The first {@link ImageLetter}
     * @param second The second {@link ImageLetter}
     * @return If the two {@link ImageLetter}s are similar
     */
    boolean isSimilar(ImageLetter first, ImageLetter second);

    /**
     * Gets the instance of {@link SimilarRule} with the given name.
     *
     * @param similarityRuleName The name of the {@link SimilarRule} to get, if present
     * @return A {@link SimilarRule} with the given name
     */
    Optional<SimilarRule> getRule(String similarityRuleName);

    /**
     * Gets a rule from the given name, and if found, sends it through the consumer. A message is sent saying the
     * {@link SimilarRule} is not found if one isn't found.
     *
     * @param similarityRuleName The name of the {@link SimilarRule} to get
     * @param ruleConsumer       The consumer to be given the {@link SimilarRule} if found
     */
    void getSafeRule(String similarityRuleName, Consumer<SimilarRule> ruleConsumer);

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
