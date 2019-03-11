package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;

import java.util.List;

public class UnderDotRule implements MergeRule {

    @Override
    public boolean isHorizontal() {
        return false;
    }

    @Override
    public MergePriority getPriority() {
        return MergePriority.LOW;
    }

    @Override
    public void mergeCharacters(List<ImageLetter> letterData) {
        // TODO: Implement method
    }
}
