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

import static com.uddernetworks.newocr.recognition.similarity.Letter.QUESTION_MARK_BOTTOM;
import static com.uddernetworks.newocr.recognition.similarity.Letter.QUESTION_MARK_TOP;
import static com.uddernetworks.newocr.utils.OCRUtils.diff;

/**
 * Merges the dot under a character for ! and ?
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class UnderDotMergeRule extends MergeRule {

    private double distanceExclamation;
    private double distanceQuestion;
    private SimilarRule dotRule;
    private SimilarRule verticalLineRule;

    public UnderDotMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getSafeRule("dot", rule -> this.dotRule = rule);
        similarityManager.getSafeRule("vertical-line", rule -> this.verticalLineRule = rule);

        try {
            this.distanceExclamation = this.databaseManager.getAveragedData("distanceExclamation").get();
            this.distanceQuestion = this.databaseManager.getAveragedData("distanceQuestion").get();
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

        // Base, we want this to be a line
        if (QUESTION_MARK_BOTTOM.matches(target)
                && !this.verticalLineRule.matchesLetter(target)) return Optional.empty();

        var question = QUESTION_MARK_TOP.matches(target);

        // Dot
        var below = letterData.get(index);
        if (!this.dotRule.matchesLetter(below)) return Optional.empty();

        if (target.getAmountOfMerges() > 0 || below.getAmountOfMerges() > 0) return Optional.empty();

        var bottomOfCharacterY = below.getY();
        var aboveY = target.getY() + target.getHeight();
        var difference = Math.abs(bottomOfCharacterY - aboveY);
        var isBelowBase = below.getHeight() < target.getHeight();
        double minHeight = target.getHeight();
        double distanceUsed = question ? this.distanceQuestion : this.distanceExclamation;

        double projectedDifference = distanceUsed * minHeight;
        double delta = projectedDifference * 0.75D;

        if (diff(difference, projectedDifference) <= delta) {
            var base = !isBelowBase ? below : target;
            var adding = !isBelowBase ? target : below;
            base.merge(adding);
            if (base.getLetter() != '?') base.setLetter('!');
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
