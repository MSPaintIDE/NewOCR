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
 * Merges dots above base characters for the letter i, j, and ;
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class OverDotMergeRule extends MergeRule {

    private double distancei;
    private double distancej;
    private double semicolonDistance;
    private SimilarRule dotRule;
    private SimilarRule verticalLineRule;

    public OverDotMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getSafeRule("dot", rule -> this.dotRule = rule);
        similarityManager.getSafeRule("vertical-line", rule -> this.verticalLineRule = rule);

        try {
            this.distancei = this.databaseManager.getAveragedData("distancei").get();
            this.distancej = this.databaseManager.getAveragedData("distancej").get();
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
        var index = letterData.indexOf(target) - 1;

        if (index < 0 || letterData.size() <= index) return Optional.empty();

        var targetLetter = target.getLetter();

        var semicolon = (targetLetter == ';' && target.getModifier() == 1) || targetLetter == ',';

        var verticalTarget = this.verticalLineRule.matchesLetter(target);

        // Base
        if (!semicolon &&
                !verticalTarget &&
                !(targetLetter == 'j' && target.getModifier() == 1) &&
                (targetLetter != 'J')) {
            return Optional.empty();
        }

        // Dot
        var above = letterData.get(index);
        if (!this.dotRule.matchesLetter(above)) return Optional.empty();

        if (target.getAmountOfMerges() > 0 || above.getAmountOfMerges() > 0) return Optional.empty();

        double distance;

        if (semicolon) {
            distance = this.semicolonDistance;
        } else if (targetLetter == 'j' || targetLetter == 'J') {
            distance = this.distancej;
        } else {
            distance = this.distancei;
        }

        var bottomOfCharacterY = above.getY() + above.getHeight();
        var difference = Math.abs(bottomOfCharacterY - target.getY());
        var isPartAbove = above.getHeight() < target.getHeight();
        double maxHeight = Math.max(above.getHeight(), target.getHeight());
        double projectedDifference = distance * maxHeight;
        double delta = projectedDifference * 0.5D;

        if (diff(difference, projectedDifference) <= delta) {
            var base = !isPartAbove ? above : target;
            var adding = !isPartAbove ? target : above;
            base.merge(adding);
            var usingChar = targetLetter;
            if (usingChar == 'J') {
                usingChar = 'j';
            } else if (usingChar == ',') {
                usingChar = ';';
            } else if (verticalTarget) {
                usingChar = 'i';
            }

            base.setLetter(usingChar);
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
