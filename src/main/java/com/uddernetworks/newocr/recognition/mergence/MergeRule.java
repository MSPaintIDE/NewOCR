package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;

import java.util.List;
import java.util.Optional;

public interface MergeRule {

    /**
     * Gets if the current merge rule is based on horizontally aligned letters (true) or vertically aligned letters
     * (true).
     *
     * @return If the data given to {@link MergeRule#mergeCharacters(List)} is horizontal (Full line) data
     */
    boolean isHorizontal();

    /**
     * Gets the priority of the current rule
     *
     * @return The priority of the rule
     */
    MergePriority getPriority();

    /**
     * Preforms the merging action with the current rule against the given data. If {@link MergeRule#isHorizontal()} is
     * true, the given data will be a full line of data. If it returns false, the data will be all characters with a
     * horizontal overlap in a column.
     *
     * @param letterData The letter data
     * @return The {@link ImageLetter} that should be removed if the merge was successful
     */
    Optional<ImageLetter> mergeCharacters(List<ImageLetter> letterData);
}
