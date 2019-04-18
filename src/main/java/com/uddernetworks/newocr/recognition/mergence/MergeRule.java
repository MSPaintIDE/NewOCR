package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.List;
import java.util.Optional;

public abstract class MergeRule {

    protected DatabaseManager databaseManager;
    protected SimilarityManager similarityManager;

    public MergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        this.databaseManager = databaseManager;
        this.similarityManager = similarityManager;
    }

    /**
     * Gets if the current merge rule is based on horizontally aligned letters (true) or vertically aligned letters
     * (true).
     *
     * @return If the data given to {@link MergeRule#mergeCharacters(ImageLetter, List)} is horizontal (Full line) data
     */
    public abstract boolean isHorizontal();

    /**
     * Gets the priority of the current rule
     *
     * @return The priority of the rule
     */
    public abstract MergePriority getPriority();

    /**
     * Preforms the merging action with the current rule against the given data. If {@link MergeRule#isHorizontal()} is
     * true, the given data will be a full line of data. If it returns false, the data will be all characters with a
     * horizontal overlap in a column.
     *
     * @param target     The base charcater
     * @param letterData The letter data
     * @return The {@link ImageLetter} that should be removed if the merge was successful
     */
    public abstract Optional<List<ImageLetter>> mergeCharacters(ImageLetter target, List<ImageLetter> letterData);
}
