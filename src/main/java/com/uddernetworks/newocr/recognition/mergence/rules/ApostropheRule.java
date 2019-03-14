package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;
import java.util.Optional;

public class ApostropheRule implements MergeRule {

    @Override
    public boolean isHorizontal() {
        return true;
    }

    @Override
    public MergePriority getPriority() {
        return MergePriority.HIGH;
    }

    @Override
    public Optional<ImageLetter> mergeCharacters(List<ImageLetter> letterData) {
        // TODO: Implement method

        return Optional.empty();
    }
}
