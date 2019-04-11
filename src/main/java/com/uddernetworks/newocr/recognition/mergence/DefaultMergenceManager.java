package com.uddernetworks.newocr.recognition.mergence;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.rules.*;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultMergenceManager implements MergenceManager {

    private static Logger LOGGER = LoggerFactory.getLogger(DefaultMergenceManager.class);
    private DatabaseManager databaseManager;
    private SimilarityManager similarityManager;

    private List<MergeRule> mergeRules = new CopyOnWriteArrayList<>();

    // Concurrent from parallel streams
    private Map<ImageLetter, List<ImageLetter>> horizontalLetterRelations = new ConcurrentHashMap<>();
    private Map<ImageLetter, List<ImageLetter>> verticalLetterRelations = new ConcurrentHashMap<>();

    public DefaultMergenceManager(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        this.databaseManager = databaseManager;
        this.similarityManager = similarityManager;
    }

    /**
     * Adds the default {@link MergeRule}s, otherwise all rules will need to be added manually via
     * {@link MergenceManager#addRule(BiFunction)}
     *
     * @return The current {@link MergenceManager}
     */
    public MergenceManager loadDefaults() {
        return addRule(OverDotMergeRule::new)
                .addRule(UnderDotMergeRule::new)
                .addRule(ApostropheMergeRule::new)
                .addRule(PercentMergeRule::new)
                .addRule(EqualVerticalMergeRule::new);
    }

    @Override
    public MergenceManager addRule(BiFunction<DatabaseManager, SimilarityManager, MergeRule> rule) {
        this.mergeRules.add(rule.apply(this.databaseManager, this.similarityManager));
        return this;
    }

    @Override
    public void beginMergence(Int2ObjectLinkedOpenHashMap<List<ImageLetter>> sortedLines, SimilarityManager similarityManager) {
        this.mergeRules.sort(Comparator.comparingInt(rule -> rule.getPriority().getPriorityIndex()));

        long start = System.currentTimeMillis();
        flatKeys(sortedLines).forEach(imageLetter -> verticalLetterRelations.put(imageLetter, getVerticalTo(imageLetter, sortedLines)));

        sortedLines.forEach((y, line) -> line.forEach(imageLetter -> horizontalLetterRelations.put(imageLetter, line)));

        this.mergeRules.stream().map(this::processRule).flatMap(Set::stream).forEach(imageLetter -> removeFromSorted(imageLetter, sortedLines));

        var dotSimilarity = similarityManager.getRule("dot").orElseThrow();

        // Cleaning up
        // TODO: Make these options
        flatKeys(sortedLines).forEach(imageLetter -> {
            if (imageLetter.getAmountOfMerges() > 0) return;
            var letter = imageLetter.getLetter();
            var mod = imageLetter.getModifier();
            if (dotSimilarity.matchesLetter(imageLetter)) {
                imageLetter.setLetter('.');
            } else if (letter == '=') {
                imageLetter.setLetter('-');
            } else if (letter == ';' && mod == 1) {
                imageLetter.setLetter(',');
            } else if (letter == 'j') { // This can happen because it has no merges here
                imageLetter.setLetter('J');
            } else if (letter == '"') { // This can happen because it has no merges here
                imageLetter.setLetter('\'');
            } else if (letter == '%') {
                imageLetter.setLetter('/');
            }
        });

        LOGGER.debug("Finished merging in " + (System.currentTimeMillis() - start));
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
                .collect(Collectors.toList());
    }

    private <V, K> Stream<V> flatKeys(Map<K, List<V>> map) {
        return map.values()
                .stream()
                .flatMap(List::stream)
                .parallel();
    }
}
