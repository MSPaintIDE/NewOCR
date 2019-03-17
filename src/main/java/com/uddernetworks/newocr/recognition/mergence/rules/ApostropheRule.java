package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class ApostropheRule extends MergeRule {

    private double apostropheRatio;

    public ApostropheRule(DatabaseManager databaseManager) {
        super(databaseManager);

        try {
            this.apostropheRatio = databaseManager.getAveragedData("apostropheRatio").get();
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
    public Optional<ImageLetter> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        var letter = target.getLetter();

        if (letter != '\'' && letter != '"') return Optional.empty();

        var index = letterData.indexOf(target) - 1;

        if (letterData.size() <= index || index < 0) return Optional.empty();
        var before = letterData.get(index);

        if (before == null) return Optional.empty();
        if (before.getLetter() != '\'' && before.getLetter() != '\'') { // 0 1 2 3
            before.setLetter('\'');
            return Optional.empty();
        }

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
