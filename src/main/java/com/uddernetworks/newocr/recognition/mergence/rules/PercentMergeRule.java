package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;
import java.util.Optional;

public class PercentMergeRule extends MergeRule {

    public PercentMergeRule(DatabaseManager databaseManager) {
        super(databaseManager);
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
        // TODO: Implement method

        return Optional.empty();
    }
}
