package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultMergenceManager implements MergenceManager {

    private List<MergeRule> mergeRules = new CopyOnWriteArrayList<>();

    @Override
    public void addRule(MergeRule rule) {
        this.mergeRules.add(rule);
    }

    @Override
    public void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines) {
        this.mergeRules.sort(Comparator.comparingInt(rule -> rule.getPriority().getPriorityIndex()));

        // TODO: Horizontal/vertical calculation and then merge loop
    }
}
