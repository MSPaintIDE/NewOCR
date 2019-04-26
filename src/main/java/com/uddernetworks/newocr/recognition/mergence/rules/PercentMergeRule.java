package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.List;
import java.util.Optional;

/**
 * Merges all pieces of a percent sign.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class PercentMergeRule extends MergeRule {

    private SimilarRule percentDot;
    private SimilarRule percentBase;

    public PercentMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getSafeRule("percent-dot", rule -> this.percentDot = rule);
        similarityManager.getSafeRule("percent-base", rule -> this.percentBase = rule);
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

        if (target.getAmountOfMerges() > 0 || part1.getAmountOfMerges() > 0 || part2.getAmountOfMerges() > 0)
            return Optional.empty();

        var partsOptional = getParts(target, part1, part2);
        if (partsOptional.isEmpty()) return Optional.empty();

        var parts = partsOptional.get();
        var base = parts[0];
        var dot1 = parts[1];
        var dot2 = parts[2];

        if (!base.isOverlappingY(dot1) || !base.isOverlappingY(dot2)) return Optional.empty();

        base.merge(dot1);
        base.merge(dot2);

        base.setModifier(0);
        base.setLetter('%');

        return Optional.of(List.of(dot1, dot2));
    }

    // base, part1, part2
    private Optional<ImageLetter[]> getParts(ImageLetter one, ImageLetter two, ImageLetter three) {
        var oneDot = isDot(one);
        var twoDot = isDot(two);
        var threeDot = isDot(three);

        var oneBase = isBase(one);
        var twoBase = isBase(two);
        var threeBase = isBase(three);

        // TODO: Improve/shorten logic?
        if (oneDot && twoDot && threeBase) {
            return Optional.of(new ImageLetter[]{three, one, two});
        } else if (oneDot && twoBase && threeDot) {
            return Optional.of(new ImageLetter[]{two, one, three});
        } else if (oneBase && twoDot && threeDot) {
            return Optional.of(new ImageLetter[]{one, two, three});
        }

        return Optional.empty();
    }

    private boolean isDot(ImageLetter imageLetter) {
        return this.percentDot.matchesLetter(imageLetter);
    }

    private boolean isBase(ImageLetter imageLetter) {
        return this.percentBase.matchesLetter(imageLetter);
    }
}
