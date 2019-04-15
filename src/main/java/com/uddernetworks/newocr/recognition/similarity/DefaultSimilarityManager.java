package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.rules.DotSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.HorizontalLineSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.PercentDotSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.VerticalLineSimilarityRule;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class DefaultSimilarityManager implements SimilarityManager {

    private static Logger LOGGER = LoggerFactory.getLogger(DefaultSimilarityManager.class);

    private List<SimilarRule> similarRules = new ArrayList<>();

    /**
     * Load the default {@link SimilarRule}s, otherwise all rules will need to be added manually via
     * {@link SimilarityManager#addSimilarity(SimilarRule)}.
     *
     * @return The current {@link SimilarityManager}
     */
    public SimilarityManager loadDefaults() {
        return addSimilarity(new DotSimilarityRule())
                .addSimilarity(new VerticalLineSimilarityRule())
                .addSimilarity(new HorizontalLineSimilarityRule())
                .addSimilarity(new PercentDotSimilarityRule());
    }

    @Override
    public SimilarityManager addSimilarity(SimilarRule rule) {
        this.similarRules.add(rule);
        return this;
    }

    @Override
    public SimilarityManager removeSimilarity(Class<? extends SimilarRule> ruleClass) {
        this.similarRules.removeIf(rule -> rule.getClass().equals(ruleClass));
        return this;
    }

    @Override
    public boolean isSimilar(ImageLetter first, ImageLetter second) {
        return this.similarRules.stream()
                .filter(rule -> rule.matchesLetter(first))
                .anyMatch(rule -> rule.matchesLetter(second));
    }

    @Override
    public Optional<SimilarRule> getRule(String similarityRuleName) {
        return this.similarRules.stream()
                .filter(rule -> rule.getName().equals(similarityRuleName))
                .findFirst();
    }

    @Override
    public void getSafeRule(String similarityRuleName, Consumer<SimilarRule> ruleConsumer) {
        this.similarRules.stream()
                .filter(rule -> rule.getName().equals(similarityRuleName))
                .findFirst()
                .ifPresentOrElse(ruleConsumer, () -> LOGGER.error("Tried to use uninitialized rule of name " + similarityRuleName));
    }

    @Override
    public Optional<Object2DoubleMap.Entry<ImageLetter>> getSecondHighest(List<Object2DoubleMap.Entry<ImageLetter>> data) {
        var first = data.get(0);
        return this.similarRules.stream()
                .filter(rule -> rule.matchesLetter(first.getKey()))
                .findFirst()
                .flatMap(rule -> rule.process(data));
    }
}
