package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.rules.DotSimilarityRule;
import com.uddernetworks.newocr.recognition.similarity.rules.VerticalLineSimilarityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.recognition.similarity.Letter.QUESTION_MARK_BOTTOM;
import static com.uddernetworks.newocr.utils.OCRUtils.diff;

/**
 * Merges the dot under a character for ! and ?
 */
public class UnderDotMergeRule extends MergeRule {

    private static Logger LOGGER = LoggerFactory.getLogger(UnderDotMergeRule.class);

    private double distanceBelow;
    private SimilarRule dotRule;
    private SimilarRule verticalLineRule;

    public UnderDotMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getRule(DotSimilarityRule.class).ifPresentOrElse(rule ->
                this.dotRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

        similarityManager.getRule(VerticalLineSimilarityRule.class).ifPresentOrElse(rule ->
                this.verticalLineRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

        try {
            this.distanceBelow = this.databaseManager.getAveragedData("distanceBelow").get();
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
        System.out.println("==================================================");
        var index = letterData.indexOf(target) + 1;

        if (letterData.size() <= index) {
            System.out.println("Can't have below");
            return Optional.empty();
        }

        // Base, we want this to be a line
//        System.out.println("target = " + target);
//        System.out.println("nyc = " + letterData.get(index));
//        OCRUtils.makeImage(target.getValues(), "ind\\target.png");
        if (QUESTION_MARK_BOTTOM.matches(target)
                && !this.verticalLineRule.matchesLetter(target)) {
            System.out.println("Doesn't match here");
            return Optional.empty();
        }

        // Dot
        var below = letterData.get(index);
        if (!this.dotRule.matchesLetter(below)) {
            System.out.println("Dot is bad " + below);
            return Optional.empty();
        }

        if (target.getAmountOfMerges() > 0 || below.getAmountOfMerges() > 0) {
            System.out.println("Already merged");
            return Optional.empty();
        }

        var bottomOfCharacterY = below.getY();
        var aboveY = target.getY() + target.getHeight();
        var difference = Math.abs(bottomOfCharacterY - aboveY);
        var isBelowBase = below.getHeight() < target.getHeight();
        double minHeight = target.getHeight();
        double projectedDifference = this.distanceBelow * minHeight;
        double delta = projectedDifference * 0.5D;

        System.out.println("difference = " + difference);
        System.out.println("projectedDifference = " + projectedDifference);
        System.out.println("distanceBelow = " + distanceBelow);
        System.out.println(diff(difference, projectedDifference) + " <= " + delta);

        if (diff(difference, projectedDifference) <= delta) {
            System.out.println("Merging now");
            var base = !isBelowBase ? below : target;
            var adding = !isBelowBase ? target : below;
            base.merge(adding);
            if (base.getLetter() != '?') base.setLetter('!');
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
