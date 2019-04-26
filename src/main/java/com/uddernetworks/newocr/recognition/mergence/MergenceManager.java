package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The manager for {@link MergeRule}s to combine/merge multi part characters.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public interface MergenceManager {

    /**
     * Adds a mergence rule to be ran using the given {@link com.uddernetworks.newocr.database.DatabaseManager} and
     * {@link SimilarityManager} from the constructor.
     *
     * @param rule The rule to add
     * @return The current {@link MergenceManager}
     */
    MergenceManager addRule(BiFunction<DatabaseManager, SimilarityManager, MergeRule> rule);

    /**
     * Orders and invokes all merge rules' {@link MergeRule#mergeCharacters(ImageLetter, List)} method with appropriate data.
     *
     * @param sortedLines       The read image data
     * @param similarityManager The {@link SimilarityManager} used
     */
    void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines, SimilarityManager similarityManager);

}
