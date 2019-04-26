package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

/**
 * Merges pieces of apostrophes.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class ApostropheMergeRule extends MergeRule {

    private double apostropheRatio;
    private SimilarRule apostropheRule;

    public ApostropheMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getSafeRule("vertical-line", rule -> this.apostropheRule = rule);

        try {
            this.apostropheRatio = databaseManager.getAveragedData("apostropheRatio").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
        if (!this.apostropheRule.matchesLetter(target)) return Optional.empty();

        var index = letterData.indexOf(target) - 1;

        if (letterData.size() <= index || index < 0) return Optional.empty();

        var before = letterData.get(index);

        if (before == null) return Optional.empty();

        if (target.getAmountOfMerges() > 0 || before.getAmountOfMerges() > 0) return Optional.empty();

        if (!this.apostropheRule.matchesLetter(before)) return Optional.empty();

        // If the size of the apostrophes are not similar, or are not around 50% of their neighbor's
        // height, it's probably not actually an apostrophe
        var diff = percentDiff(target.getHeight(), before.getHeight());
        if (diff >= 0.25D) return Optional.empty();

        var compare = Arrays.asList(EXCLAMATION_DOT, QUOTE_LEFT, QUOTE_RIGHT, PERCENT_LDOT, PERCENT_RDOT, APOSTROPHE, ASTERISK, PLUS, COMMA, MINUS, PERIOD, COLON_TOP, COLON_BOTTOM, SEMICOLON_TOP, SEMICOLON_BOTTOM, EQUALS_TOP, EQUALS_BOTTOM, QUESTION_MARK_BOTTOM, CARROT, UNDERSCORE, GRAVE, i_DOT, j_DOT, TILDE, SPACE);

        ImageLetter compareCharacter = null;
        for (ImageLetter current : letterData) {
            var currentLetter = getLetter(current);
            if (target.equals(current) || before.equals(current) || compare.contains(currentLetter)) continue;
            compareCharacter = current;
            break;
        }

        if (compareCharacter != null) {
            var sizeDiff = percentDiff(compareCharacter.getHeight(), target.getHeight());

            if (sizeDiff <= 0.5D) return Optional.empty();
        }

        var avgLength = (double) before.getHeight() * apostropheRatio;
        if (target.getX() - before.getX() <= avgLength) {
            // If the ' (Represented as ") are close enough to each other, they are put into a single " and the second (current) character is removed
            before.setLetter('"');
            before.merge(target);
            return Optional.of(List.of(target));
        }

        return Optional.empty();
    }

    private double percentDiff(double one, double two) {
        return 1D - (Math.min(one, two) / Math.max(one, two));
    }
}
