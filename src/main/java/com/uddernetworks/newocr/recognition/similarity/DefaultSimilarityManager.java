package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.similarity.rules.DotSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.HorizontalLineSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.PercentDotSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.VerticalLineSimilarityRule;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DefaultSimilarityManager implements SimilarityManager {

    private List<SimilarRule> similarRules = new ArrayList<>();

    /**
     * Load the default {@link SimilarRule}s, otherwise all rules will need to be added manually via
     * {@link SimilarityManager#addSimilarity(SimilarRule)}.
     *
     * @return The current {@link SimilarityManager}
     */
    public SimilarityManager loadDefaults() {
        addSimilarity(new DotSimilarityRule());
        addSimilarity(new VerticalLineSimilarityRule());
        addSimilarity(new HorizontalLineSimilarityRule());
        addSimilarity(new PercentDotSimilarityRule());
        return this;
    }

    @Override
    public void addSimilarity(SimilarRule rule) {
        this.similarRules.add(rule);
    }

    @Override
    public boolean isSimilar(ImageLetter first, ImageLetter second) {
        return this.similarRules.stream()
                .filter(rule -> rule.matchesLetter(first))
                .anyMatch(rule -> rule.matchesLetter(second));
    }

    @Override
    public Optional<SimilarRule> getRule(Class<? extends SimilarRule> similarityRuleClass) {
        return this.similarRules.stream()
                .filter(rule -> rule.getClass().equals(similarityRuleClass))
                .findFirst();
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
