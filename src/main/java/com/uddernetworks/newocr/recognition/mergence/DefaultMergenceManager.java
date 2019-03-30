package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.rules.*;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMergenceManager implements MergenceManager {

    private List<MergeRule> mergeRules = new CopyOnWriteArrayList<>();

    // Concurrent from parallel streams
    private Map<ImageLetter, List<ImageLetter>> horizontalLetterRelations = new ConcurrentHashMap<>();
    private Map<ImageLetter, List<ImageLetter>> verticalLetterRelations = new ConcurrentHashMap<>();

    /**
     * Adds the default {@link MergeRule}s, otherwise all rules will need to be added manually via
     * {@link MergenceManager#addRule(MergeRule)}.
     *
     * @return The current {@link MergenceManager}
     */
    public MergenceManager loadDefaults(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        addRule(new OverDotMergeRule(databaseManager, similarityManager));
        addRule(new UnderDotMergeRule(databaseManager, similarityManager));
        addRule(new ApostropheMergeRule(databaseManager, similarityManager));
        addRule(new PercentMergeRule(databaseManager, similarityManager));
        addRule(new EqualVerticalMergeRule(databaseManager, similarityManager));
        return this;
    }

    @Override
    public void addRule(MergeRule rule) {
        this.mergeRules.add(rule);
    }

    @Override
    public void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines) {
        this.mergeRules.sort(Comparator.comparingInt(rule -> rule.getPriority().getPriorityIndex()));

        flatKeys(sortedLines).forEach(letter -> {
            System.out.println(letter + " at (" + letter.getX() + ", " + letter.getY() + ") wh is (" + letter.getWidth() + " x " + letter.getHeight() + ")");
        });

        var tt = flatKeys(sortedLines).findFirst().get();

        System.out.println("Vertical for first is " + tt);
        System.out.println(getVerticalTo(tt, sortedLines));

        long start = System.currentTimeMillis();
        flatKeys(sortedLines).forEach(imageLetter -> verticalLetterRelations.put(imageLetter, getVerticalTo(imageLetter, sortedLines)));

        sortedLines.forEach((y, line) -> line.forEach(imageLetter -> horizontalLetterRelations.put(imageLetter, line)));
        System.out.println("Finished first in " + (System.currentTimeMillis() - start) + "ms");

        System.out.println("horizontalLetterRelations = " + horizontalLetterRelations);
        System.out.println("verticalLetterRelations = " + verticalLetterRelations);

        System.out.println("mergeRules = " + mergeRules);

        start = System.currentTimeMillis();
        this.mergeRules.stream().map(this::processRule).flatMap(Set::stream).forEach(imageLetter -> removeFromSorted(imageLetter, sortedLines));

        System.out.println("Finished in " + (System.currentTimeMillis() - start));
    }

    private Set<ImageLetter> processRule(MergeRule rule) {
        var iterating = rule.isHorizontal() ? horizontalLetterRelations : verticalLetterRelations;
        var removing = new HashSet<ImageLetter>();
        iterating.forEach((base, context) -> {
            if (removing.contains(base)) return;
            rule.mergeCharacters(base, context).ifPresent(remove -> {
                removing.addAll(remove);
                iterating.forEach((key, list) -> list.removeAll(remove));
            });
        });

        System.out.println("removing = " + removing);

        removing.forEach(horizontalLetterRelations::remove);
        removing.forEach(verticalLetterRelations::remove);

        return removing;
    }

    private void removeFromSorted(ImageLetter imageLetter, Map<Integer, List<ImageLetter>> sortedLines) {
        var iterator = sortedLines.entrySet().iterator();
        while (iterator.hasNext()) {
            var currentEntry = iterator.next();
            var currentLine = currentEntry.getValue();
            var removed = currentLine.remove(imageLetter);
            if (removed) {
                if (currentLine.isEmpty()) iterator.remove();
                break;
            }
        }
    }

    private List<ImageLetter> getVerticalTo(ImageLetter imageLetter, Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines) {
        return flatKeys(sortedLines)
                .filter(filterChar -> filterChar.isOverlappingX(imageLetter))
                .sorted(Comparator.comparingInt(ImageLetter::getY))
//                .collect(Collectors.toCollection(LinkedList::new));
                .collect(Collectors.toList());
    }

    private <V, K> Stream<V> flatKeys(Map<K, List<V>> map) {
        return map.values()
                .stream()
                .flatMap(List::stream)
                .parallel();
    }
}
