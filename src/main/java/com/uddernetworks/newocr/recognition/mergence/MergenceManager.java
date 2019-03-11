package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.List;

public interface MergenceManager {

    /**
     * Adds a mergence rule to be ran.
     *
     * @param rule The rule to add
     */
    void addRule(MergeRule rule);

    /**
     * Orders and invokes all merge rules' {@link MergeRule#mergeCharacters(List)} method with appropriate data.
     *
     * @param sortedLines The read image data
     */
    void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines);

}
