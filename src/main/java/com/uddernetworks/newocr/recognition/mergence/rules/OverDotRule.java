package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class OverDotRule extends MergeRule {

    private double distanceAbove;

    public OverDotRule(DatabaseManager databaseManager) {
        super(databaseManager);

        try {
            this.distanceAbove = this.databaseManager.getAveragedData("distanceAbove").get();
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

        var letter = target.getLetter();
        // If the base is a straight line (Common misconceptions for the bottoms of characters requiring dots)
        if (letter != 'i' && letter != 'j' && letter != 'l' && letter != '!' && letter != '|') return Optional.empty();

        var above = letterData.get(index);
        if (above.getLetter() != '.' && above.getLetter() != '.') return Optional.empty();

        var bottomOfCharacterY = above.getY() + above.getHeight();
        var difference = Math.abs(bottomOfCharacterY - target.getY());
        var isPartAbove = above.getHeight() < target.getHeight();
        double minHeight = Math.min(above.getHeight(), target.getHeight());
        double projectedDifference = this.distanceAbove * minHeight;
        double delta = projectedDifference * 0.25;
        System.out.println("difference = " + difference);
        System.out.println("projectedDifference = " + projectedDifference);
        System.out.println("Delta = " + delta);

        // Definitely can be improved
        if (diff(difference, projectedDifference) <= delta) {
            System.out.println("Moving above");
            var base = !isPartAbove ? above : target;
            var adding = !isPartAbove ? target : above;
            base.merge(adding);
            return Optional.of(adding);
        }

        return Optional.empty();
    }
}
