package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.List;
import java.util.Optional;

/**
 * Merges all pieces of a percent sign
 */
public class PercentMergeRule extends MergeRule {

    public PercentMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);
    }

    @Override
    public boolean isHorizontal() {
        return true;
    }

    @Override
    public MergePriority getPriority() {
        return MergePriority.HIGH;
    }

    @Override
    public Optional<List<ImageLetter>> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        var baseIndex = letterData.indexOf(target);

        if (baseIndex - 1 < 0 || baseIndex + 1 >= letterData.size()) return Optional.empty();

        var dot = letterData.get(baseIndex - 1);
        var dot2 = letterData.get(baseIndex + 1);

        if (target.getAmountOfMerges() > 0 || dot.getAmountOfMerges() > 0 || dot2.getAmountOfMerges() > 0) return Optional.empty();

        if (target.getLetter() != '%' && target.getLetter() != '/') return Optional.empty();

        if (!(dot.getLetter() == '%' && dot.getModifier() != 2) &&
                dot.getLetter() != 'o') return Optional.empty();

        if (!(dot2.getLetter() == '%' && dot2.getModifier() != 2) &&
                dot.getLetter() != 'o') return Optional.empty();

        if (!target.isOverlappingY(dot) || !target.isOverlappingY(dot2)) return Optional.empty();

        target.merge(dot);
        target.merge(dot2);

        return Optional.of(List.of(dot, dot2));
    }
}
