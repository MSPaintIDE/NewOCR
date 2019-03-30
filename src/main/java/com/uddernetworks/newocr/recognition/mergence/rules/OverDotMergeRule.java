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

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

/**
 * Merges dots above base characters for the letter i
 */
public class OverDotMergeRule extends MergeRule {

    private static Logger LOGGER = LoggerFactory.getLogger(OverDotMergeRule.class);

    private double distanceAbove;
    private double semicolonDistance;
    private SimilarRule dotRule;
    private SimilarRule verticalLineRule;

    public OverDotMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getRule(DotSimilarityRule.class).ifPresentOrElse(rule ->
                this.dotRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

        similarityManager.getRule(VerticalLineSimilarityRule.class).ifPresentOrElse(rule ->
                this.verticalLineRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

        try {
            this.distanceAbove = this.databaseManager.getAveragedData("distanceAbove").get();
            this.semicolonDistance = this.databaseManager.getAveragedData("semicolonDistance").get();
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
        return MergePriority.HIGH;
    }

    @Override
    public Optional<List<ImageLetter>> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        System.out.println("----------------------------------------------------");

        var index = letterData.indexOf(target) - 1;
        System.out.println("index = " + index + " of " + target + " " + letterData);

        System.out.println("111");

        if (index < 0 || letterData.size() <= index) {
            System.out.println("Badd");
            return Optional.empty();
        }

        System.out.println("222");

        var targetLetter = target.getLetter();

        var semicolon = (targetLetter == ';' && target.getModifier() == 1) || targetLetter == ',';
        System.out.println("semicolon = " + semicolon);
        System.out.println("targetLetter = " + targetLetter);

        // Base
        if (!semicolon &&
                !this.verticalLineRule.matchesLetter(target) &&
                !(targetLetter == 'j' && target.getModifier() == 1) &&
                (targetLetter != 'J')) {
            System.out.println("Don't match: " + target);
            return Optional.empty();
        }

        System.out.println("333");

        // Dot
        var above = letterData.get(index);
        if (!this.dotRule.matchesLetter(above)) return Optional.empty();

        if (target.getAmountOfMerges() > 0 || above.getAmountOfMerges() > 0) return Optional.empty();

        System.out.println("444");

        var distanceUsing = semicolon ? this.semicolonDistance : this.distanceAbove;

        var bottomOfCharacterY = above.getY() + above.getHeight();
        var difference = Math.abs(bottomOfCharacterY - target.getY());
        var isPartAbove = above.getHeight() < target.getHeight();
        double maxHeight = Math.max(above.getHeight(), target.getHeight());
        double projectedDifference = distanceUsing * maxHeight;
        double delta = projectedDifference * 0.5D;
        System.out.println("maxHeight = " + maxHeight);
        System.out.println("distanceUsing = " + distanceUsing);
        System.out.println("difference = " + difference);
        System.out.println("projectedDifference = " + projectedDifference);
        System.out.println("Delta = " + delta);

        System.out.println(diff(difference, projectedDifference)  + " <= " + delta);

        // Definitely can be improved
        if (diff(difference, projectedDifference) <= delta) {
            System.out.println("Moving above");
            var base = !isPartAbove ? above : target;
            var adding = !isPartAbove ? target : above;
            base.merge(adding);
            var usingChar = targetLetter;
            if (usingChar == 'J') {
                usingChar = 'j';
            } else if (usingChar == ',') {
                usingChar = ';';
            }

            base.setLetter(usingChar);
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
