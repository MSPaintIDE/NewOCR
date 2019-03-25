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
    public Optional<ImageLetter> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        var index = letterData.indexOf(target) + 1;

        if (letterData.size() <= index) return Optional.empty();

        // Base
        if (!this.verticalLineRule.matchesLetter(target)) return Optional.empty();

        // Dot
        var below = letterData.get(index);
        if (!this.dotRule.matchesLetter(below)) return Optional.empty();

        var bottomOfCharacterY = below.getY();
        var aboveY = target.getY() + target.getHeight();
        var difference = Math.abs(bottomOfCharacterY - aboveY);
        var isBelowBase = below.getHeight() < target.getHeight();
        double minHeight = Math.min(below.getHeight(), target.getHeight());
        double projectedDifference = this.distanceBelow * minHeight;
        double delta = projectedDifference * 0.25;
        System.out.println("difference = " + difference);
        System.out.println("projectedDifference = " + projectedDifference);
        System.out.println("Delta = " + delta);
// Definitely can be improved
        if (diff(difference, projectedDifference) <= delta) {
            System.out.println("Merging");
            var base = !isBelowBase ? below : target;
            var adding = !isBelowBase ? target : below;
            base.merge(adding);
            return Optional.of(adding);
        }

        return Optional.empty();
    }
}
