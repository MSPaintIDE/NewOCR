package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;
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
        // TODO: Add defaults
        return this;
    }

    @Override
    public void addSimilarity(SimilarRule rule) {
        this.similarRules.add(rule);
    }

    @Override
    public Optional<Object2DoubleMap.Entry<ImageLetter>> getSecondHighest(List<Object2DoubleMap.Entry<ImageLetter>> data) {
        var first = data.get(0);
        return this.similarRules.stream().filter(rule -> rule.matchesFirst(first.getKey())).findFirst().flatMap(rule -> rule.process(data));
    }
}
