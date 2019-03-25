package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.rules.VerticalLineSimilarityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ApostropheMergeRule extends MergeRule {

    private static Logger LOGGER = LoggerFactory.getLogger(ApostropheMergeRule.class);

    private double apostropheRatio;
    private SimilarRule apostropheRule;

    public ApostropheMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getRule(VerticalLineSimilarityRule.class).ifPresentOrElse(rule ->
                this.apostropheRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

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
    public Optional<ImageLetter> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        System.out.println("=== Merging ===");
        if (!this.apostropheRule.matchesLetter(target)) {
            System.out.println("Base character does not match (" + target + ")");
            return Optional.empty();
        }

        var index = letterData.indexOf(target) - 1;

        if (letterData.size() <= index || index < 0) {
            System.out.println("Can't have a before");
            return Optional.empty();
        }
        var before = letterData.get(index);

        if (before == null) {
            System.out.println("Before is null");
            return Optional.empty();
        }

        if (before.getAmountOfMerges() > 0) {
            System.out.println("Already had merges!");
            return Optional.empty();
        }

        if (!this.apostropheRule.matchesLetter(before)) {
            System.out.println("Before does NOT match!");
//            before.setLetter('\'');
            return Optional.empty();
        }

        System.out.println("Continuing");

        var avgLength = (double) before.getHeight() * apostropheRatio;
        if (target.getX() - before.getX() <= avgLength) {
            System.out.println("Merging");
            // If the ' (Represented as ") are close enough to each other, they are put into a single " and the second (current) character is removed
            before.setLetter('"');
            before.merge(target);
            return Optional.of(target);
        }

        return Optional.empty();
    }
}
