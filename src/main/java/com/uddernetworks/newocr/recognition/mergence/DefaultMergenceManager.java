package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMergenceManager implements MergenceManager {

    private List<MergeRule> mergeRules = new CopyOnWriteArrayList<>();

    // Concurrent from parallel streams
    private Map<ImageLetter, List<ImageLetter>> horizontalLetterRelations = new ConcurrentHashMap<>();
    private Map<ImageLetter, List<ImageLetter>> verticalLetterRelations = new ConcurrentHashMap<>();

    @Override
    public void addRule(MergeRule rule) {
        this.mergeRules.add(rule);
    }

    @Override
    public void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines) {
        this.mergeRules.sort(Comparator.comparingInt(rule -> rule.getPriority().getPriorityIndex()));

        long start = System.currentTimeMillis();
        flatKeys(sortedLines).forEach(imageLetter -> verticalLetterRelations.put(imageLetter, getVerticalTo(imageLetter, sortedLines)));

        sortedLines.forEach((y, line) -> line.forEach(imageLetter -> horizontalLetterRelations.put(imageLetter, line)));
        System.out.println("Finished first in " + (System.currentTimeMillis() - start) + "ms");

        var removed = new HashSet<ImageLetter>();

        this.mergeRules.forEach(rule -> {
            if (rule.isHorizontal()) {
                // TODO: Merge characters matching rules and then *remove them from all lists* (This is where I'm wondering about the best solution)
            } else {
                // TODO: Here as well
            }
        });
    }

    private void removeFromAll(ImageLetter imageLetter) {
        horizontalLetterRelations.values().forEach(line -> line.remove(imageLetter));
        verticalLetterRelations.values().forEach(line -> line.remove(imageLetter));
    }

    private List<ImageLetter> getVerticalTo(ImageLetter imageLetter, Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines) {
        return flatKeys(sortedLines)
                .filter(filterChar -> isOverlappingX(filterChar, imageLetter))
                .collect(Collectors.toList());
    }

    private <V, K> Stream<V> flatKeys(Map<K, List<V>> map) {
        return map.values()
                .stream()
                .flatMap(List::stream)
                .parallel();
    }

    // TODO: Updated migrated methods' docs from SearchCharacter

    /**
     * Gets if another {@link SearchCharacter} is overlapping the current {@link SearchCharacter} at all in the X axis.
     *
     * @return If the given {@link SearchCharacter} is overlapping the current {@link SearchCharacter}
     */
    public boolean isOverlappingX(ImageLetter letter1, ImageLetter letter2) {
        if (isInXBounds(letter1, letter2.getX())) return true;
        if (isInXBounds(letter2, letter2.getX() + letter2.getWidth())) return true;
        return false;
    }

    /**
     * Gets if the given Y position is within the Y bounds of the current character.
     *
     * @param y The Y position to check
     * @return If the given Y position is within the Y bounds of the current character
     */
    public boolean isInYBounds(ImageLetter base, int y) {
        return y <= base.getY() + base.getHeight()
                && y >= base.getY();
    }

    /**
     * Gets if the given Y position is within the X bounds of the current character.
     *
     * @param x The Y position to check
     * @return If the given Y position is within the X bounds of the current character
     */
    public boolean isInXBounds(ImageLetter base, int x) {
        return x <= base.getX() + base.getWidth()
                && x >= base.getX();
    }
}
