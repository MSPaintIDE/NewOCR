package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

/**
 * Merges : and = pieces
 */
public class EqualVerticalMergeRule extends MergeRule {

    private static Logger LOGGER = LoggerFactory.getLogger(EqualVerticalMergeRule.class);

    private double colonDistance;
    private double equalsDistance;
    private SimilarRule dotRule;
    private SimilarRule horizontalLineRule;

    public EqualVerticalMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);

        similarityManager.getRule("dot").ifPresentOrElse(rule ->
                this.dotRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

        similarityManager.getRule("horizontal-line").ifPresentOrElse(rule ->
                this.horizontalLineRule = rule, () ->
                LOGGER.error("Tried to use uninitialized rule from " + similarityManager.getClass().getCanonicalName()));

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
        System.out.println("======================================================");
        var index = letterData.indexOf(target) + 1;

        if (letterData.size() <= index) {
            System.out.println("Can't have above");
            return Optional.empty();
        }

        var above = letterData.get(index);

        if (target.getAmountOfMerges() > 0 || above.getAmountOfMerges() > 0) {
            System.out.println("Already merged!");
            return Optional.empty();
        }

        System.out.println("Past " + Letter.getLetter(target).name() + " and " + Letter.getLetter(above).name());

        var bottomOfCharacterY = above.getY();
        var difference = bottomOfCharacterY - target.getY() - target.getHeight();

        var isPartAbove = above.getHeight() < target.getHeight();
        double minHeight = Math.min(above.getHeight(), target.getHeight());
        double projectedDifference;
        var colon = true;

        if (this.horizontalLineRule.matchesLetter(target) && this.horizontalLineRule.matchesLetter(above)) { //   =
            System.out.println("equals");
            projectedDifference = this.equalsDistance * minHeight;
            System.out.println("equalsDistance = " + equalsDistance);
            colon = false;
        } else if (this.dotRule.matchesLetter(target) && this.dotRule.matchesLetter(above)) { //   :
            System.out.println("Colon");
            projectedDifference = this.colonDistance * minHeight;
            System.out.println("colonDistance = " + colonDistance);
        } else {
            System.out.println("ELSE!");
            return Optional.empty();
        }

        var delta = projectedDifference * 0.5D;
        System.out.println("difference = " + difference);
        System.out.println("projectedDifference = " + projectedDifference);
        System.out.println(diff(difference, projectedDifference) + " <= " + delta);

        if (diff(difference, projectedDifference) <= delta) {
            System.out.println("Merging");
            var base = !isPartAbove ? above : target;
            var adding = !isPartAbove ? target : above;
            base.merge(adding);
            base.setLetter(colon ? ':' : '=');
            return Optional.of(List.of(adding));
        }

        return Optional.empty();
    }
}
