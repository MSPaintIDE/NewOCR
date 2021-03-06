package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

/**
 * Merges : and = pieces.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class EqualVerticalMergeRule extends MergeRule {

    private double colonDistance;
    private double equalsDistance;
    private SimilarRule dotRule;
    private SimilarRule horizontalLineRule;

    public EqualVerticalMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getSafeRule("dot", rule -> this.dotRule = rule);
        similarityManager.getSafeRule("horizontal-line", rule -> this.horizontalLineRule = rule);

        try {
            this.colonDistance = this.databaseManager.getAveragedData("colonDistance").get();
            this.equalsDistance = this.databaseManager.getAveragedData("equalsDistance").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isHorizontal() {
        return false;
    }

    @Override
    public MergePriority getPriority() {
        return MergePriority.LOW;
    }

    @Override
    public Optional<List<ImageLetter>> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        var index = letterData.indexOf(target) + 1;

        if (letterData.size() <= index) return Optional.empty();

        var above = letterData.get(index);

        if (target.getAmountOfMerges() > 0 || above.getAmountOfMerges() > 0) return Optional.empty();

        var bottomOfCharacterY = above.getY();
        var difference = bottomOfCharacterY - target.getY() - target.getHeight();

        var isPartAbove = above.getHeight() < target.getHeight();
        double minHeight = Math.min(above.getHeight(), target.getHeight());
        double projectedDifference;
        var colon = true;

        if (this.horizontalLineRule.matchesLetter(target) && this.horizontalLineRule.matchesLetter(above)) { //   =
            projectedDifference = this.equalsDistance * minHeight;
            colon = false;
        } else if (this.dotRule.matchesLetter(target) && this.dotRule.matchesLetter(above)) { //   :
            projectedDifference = this.colonDistance * minHeight;
        } else {
            return Optional.empty();
        }

        var delta = projectedDifference * 0.5D;

        if (diff(difference, projectedDifference) <= delta) {
            var base = !isPartAbove ? above : target;
            var adding = !isPartAbove ? target : above;
            base.merge(adding);
            base.setLetter(colon ? ':' : '=');
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
