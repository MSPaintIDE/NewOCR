package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class UnderDotRule extends MergeRule {

    private double distanceBelow;

    public UnderDotRule(DatabaseManager databaseManager) {
        super(databaseManager);

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

        var letter = target.getLetter();
        // If the base is a straight line (Common misconceptions for the bottoms of characters requiring dots)
        if (letter != 'i' && letter != 'j' && letter != 'l' && letter != '!' && letter != '|') return Optional.empty();

        var below =  letterData.get(index);
        if (below.getLetter() != '.' && below.getLetter() != '.') return Optional.empty();

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
