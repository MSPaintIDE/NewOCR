package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        var part1 = letterData.get(baseIndex - 1);
        var part2 = letterData.get(baseIndex + 1);

        if (target.getAmountOfMerges() > 0 || part1.getAmountOfMerges() > 0 || part2.getAmountOfMerges() > 0) return Optional.empty();

        var pieces = new ArrayList<>(List.of(target, part1, part2));
        var dots = pieces.stream().filter(this::isDot).collect(Collectors.toList());
        if (dots.size() != 2) return Optional.empty();

        pieces.removeAll(dots);
        var base = pieces.get(0);
        if (!isBase(base)) return Optional.empty();

        var dot1 = dots.get(0);
        var dot2 = dots.get(1);

        if (!base.isOverlappingY(dot1) || !base.isOverlappingY(dot2)) return Optional.empty();

        base.merge(dot1);
        base.merge(dot2);

        return Optional.of(dots);
    }

    private boolean isDot(ImageLetter imageLetter) {
        return (imageLetter.getLetter() == '%' && imageLetter.getModifier() != 2) ||
                imageLetter.getLetter() == 'o';
    }

    private boolean isBase(ImageLetter imageLetter) {
        return imageLetter.getLetter() == '%' || imageLetter.getLetter() == '/';
    }
}
