package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

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

        // Make the most non-square piece last
        var pieces = new ArrayList<>(List.of(target, part1, part2));
        var sorted = pieces.stream()
                .sorted(Comparator.comparingDouble(letter -> (double) Math.max(letter.getWidth(), letter.getHeight()) / (double) Math.min(letter.getWidth(), letter.getHeight())))
                .collect(Collectors.toList());

        var base = sorted.get(2);
        var dot1 = sorted.get(0);
        var dot2 = sorted.get(1);
        if (!isBase(base) || !isDot(dot1) || !isDot(dot2)) return Optional.empty();

        if (!base.isOverlappingY(dot1) || !base.isOverlappingY(dot2)) return Optional.empty();

        base.merge(dot1);
        base.merge(dot2);

        base.setModifier(0);
        base.setLetter('%');

        sorted.remove(base);

        return Optional.of(sorted);
    }

    private boolean isDot(ImageLetter imageLetter) {
        return PERCENT_LDOT.matches(imageLetter) || PERCENT_RDOT.matches(imageLetter) || o.matches(imageLetter);
    }

    private boolean isBase(ImageLetter imageLetter) {
        return PERCENT_BASE.matches(imageLetter) || FORWARD_SLASH.matches(imageLetter);
    }
}
