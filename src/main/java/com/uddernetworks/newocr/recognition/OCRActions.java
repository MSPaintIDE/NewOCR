package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.CoordinateCharacter;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.character.TrainedCharacterData;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Provides general OCR actions.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class OCRActions implements Actions {

    private SimilarityManager similarityManager;
    private DatabaseManager databaseManager;
    private OCROptions options;

    /**
     * Creates a new {@link OCRActions} with a {@link DatabaseManager} and {@link OCROptions}. The
     * {@link OCRActions#OCRActions(SimilarityManager, DatabaseManager, OCROptions)} constructor is preferred, as
     * without the {@link SimilarityManager} there is less-accurate character fetching.
     *
     * @param databaseManager The {@link DatabaseManager} to use
     * @param options         The {@link OCROptions} to use
     */
    public OCRActions(DatabaseManager databaseManager, OCROptions options) {
        this.databaseManager = databaseManager;
        this.options = options;
    }

    /**
     * Creates a new {@link OCRActions} with a {@link DatabaseManager} and {@link OCROptions}.
     *
     * @param similarityManager The {@link SimilarityManager} to use
     * @param databaseManager The {@link DatabaseManager} to use
     * @param options         The {@link OCROptions} to use
     */
    public OCRActions(SimilarityManager similarityManager, DatabaseManager databaseManager, OCROptions options) {
        this.similarityManager = similarityManager;
        this.databaseManager = databaseManager;
        this.options = options;
    }

    @Override
    public void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters) {
        var coordinates = new ArrayList<IntPair>();

        var width = searchImage.getWidth();
        var height = searchImage.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (!coordinates.isEmpty()) {
                    var foundCharacter = new SearchCharacter(new ArrayList<>(coordinates));
                    foundCharacter.applySections();
                    foundCharacter.analyzeSlices();
                    searchCharacters.add(foundCharacter);
                    coordinates.clear();
                }
            }
        }
    }

    @Override
    public List<CharacterLine> getLettersDuringTraining(SearchImage searchImage) {
        var ret = new ArrayList<CharacterLine>();

        var charMetaMap = Map.of(
                ';', "semicolonDistance",
                ':', "colonDistance",
                '=', "equalsDistance",
                'i', "distancei",
                'j', "distancej"
        );

        // By default 0
        var configurableBases = Map.of(
                Set.of('i', 'j', ':', ';', '='), 1 // The base is the second character (Bottom part)
        );

        var lineBounds = getLineBoundsForTraining(searchImage);
        for (var coords : lineBounds) {
            var fromY = coords.getKey();
            var toY = coords.getValue();

            var sub = searchImage.getSubimage(0, fromY, searchImage.getWidth(), toY - fromY);

            var width = sub.getWidth();
            var height = sub.getHeight();

            var coordinates = new ArrayList<IntPair>();
            var found = new ArrayList<SearchCharacter>();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    sub.scanFrom(x, y, coordinates);

                    if (!coordinates.isEmpty()) {
                        var foundCharacter = new SearchCharacter(new ArrayList<>(coordinates), 0, fromY);
                        foundCharacter.applySections();
                        foundCharacter.analyzeSlices();
                        found.add(foundCharacter);
                        coordinates.clear();
                    }
                }
            }

            // Get the characters with horizontal overlap
            var ignored = new HashSet<CoordinateCharacter>();

            Collections.sort(found);

            // These values represent the indices of characters that require multiple parts
            var multipleParts = Arrays.asList(0, 7, 29, 31, 34, 37, 80, 82);

            for (int i1 = 0; i1 < found.size(); i1++) {
                var part1 = found.get(i1);

                if (ignored.contains(part1)) continue;
                if (!multipleParts.contains(i1)) continue;

                var increment = new AtomicInteger(0);
                var list = found.stream()
                        .filter(part1::isOverlappingX)
                        .sorted(Comparator.comparingInt(CoordinateCharacter::getY))
                        .collect(Collectors.toList());

                var currentChar = OCRTrain.TRAIN_STRING.charAt(i1);

                if (currentChar == '%') {
                    list.sort(Comparator.comparingDouble(character -> (double) character.getWidth() * (double) character.getHeight()));
                }

                // If this is 1, it gets the second character
                var index = configurableBases.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().contains(currentChar))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(0);

                var base = list.get(Math.min(index, list.size() - 1));

                list.forEach(part2 -> {
                    if (!base.equals(part2)) { // If part2 is NOT the base
                        if (currentChar == '!' || currentChar == '?') { // ! ?
                            double diff = (double) (part2.getY() - (base.getY() + base.getHeight()));
                            var distance = diff / (double) base.getHeight();

                            base.setTrainingMeta(currentChar == '?' ? "distanceQuestion" : "distanceExclamation", distance);
                        } else if (currentChar == 'i' || currentChar == 'j' || currentChar == ':' || currentChar == ';' || currentChar == '=') { // i j   base below
                            double diff = (double) (base.getY() - (part2.getY() + part2.getHeight()));
                            var distance = diff / (double) base.getHeight();

                            base.setTrainingMeta(charMetaMap.getOrDefault(currentChar, "distanceAbove"), distance);
                        }
                    }

                    var i = increment.getAndIncrement();
                    part2.setModifier(i);
                    ignored.add(part2);
                });
            }

            ret.add(new TrainLine(found, fromY, toY));
        }

        return ret;
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        return getCharacterFor(searchCharacter, (IntPair) null);
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, IntPair lineBounds) {
        try {
            var diffs = new Object2DoubleOpenHashMap<ImageLetter>(); // The lower value the better

            var data = new ArrayList<>(databaseManager.getAllCharacterSegments().get());


            data.forEach(character -> OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData()).ifPresent(charDifference -> {
                // Gets the difference of the database character and searchCharacter (Lower is better)
                var imageLetter = new ImageLetter(character.getLetter(), character.getModifier(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getAvgWidth(), character.getAvgHeight(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates());
                imageLetter.setMaxCenter(character.getMaxCenter());
                imageLetter.setMinCenter(character.getMinCenter());
                diffs.put(imageLetter, charDifference);
            }));

            return getCharacterFor(searchCharacter, diffs, lineBounds);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data) {
        return getCharacterFor(searchCharacter, data, null);
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data, IntPair lineBounds) {
        Object2DoubleMap<ImageLetter> diffs = new Object2DoubleOpenHashMap<>(); // The lower value the better

        data.forEach(character -> {
            character.finishRecalculations();
            OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getSegmentPercentages()).ifPresent(charDifference -> {
                // Gets the difference of the database character and searchCharacter (Lower is better)
                diffs.put(new ImageLetter(character.getLetter(), character.getModifier(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getWidthAverage(), character.getHeightAverage(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates()), charDifference);
            });
        });

        return getCharacterFor(searchCharacter, diffs, lineBounds);
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs) {
        return getCharacterFor(searchCharacter, diffs, null);
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs, IntPair lineBounds) {
        double searchRatio = (double) searchCharacter.getWidth() / searchCharacter.getHeight();
        var orderedDifferences = diffs.object2DoubleEntrySet().stream()
                .peek(entry -> {
                    var imageLetter = entry.getKey();

                    double ratio = imageLetter.getAverageWidth() / imageLetter.getAverageHeight();
                    double ratioDiff = Math.pow(ratio - searchRatio, 2);
                    ratioDiff *= this.similarityManager == null ? this.options.getSizeRatioWeight() : this.options.getSizeRatioWeight(Letter.getLetter(imageLetter));

                    entry.setValue(ratioDiff + entry.getDoubleValue());
                })
                .sorted(Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue))
                .collect(Collectors.toList());

        if (orderedDifferences.isEmpty()) return Optional.empty();

        var imageLetter = orderedDifferences.remove(0).getKey();
        imageLetter.setClosestMatches(orderedDifferences);
        imageLetter.setValues(searchCharacter.getValues());

        return Optional.of(imageLetter);
    }

    @Override
    public OptionalDouble getFontSize(ImageLetter imageLetter) {
        var charactersToSize = imageLetter.getMergedPieces().orElse(Map.of(Letter.getLetter(imageLetter), imageLetter));
        var sizesGot = new DoubleArrayList();
        charactersToSize.forEach((letter, character) -> {
            try {
                var characterSizeRatio = this.databaseManager.getFontSize(character.getLetter(), character.getModifier()).get();

                double realCharacterSize = character.getHeight();
                var fontSize = characterSizeRatio * realCharacterSize;

                sizesGot.add(fontSize);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        return sizesGot.stream().mapToDouble(Double::valueOf).average();
    }

    @Override
    public List<IntPair> getLineBoundsForTraining(SearchImage image) {
        // Pair<topY, bottomY>
        List<IntPair> lines = new ArrayList<>();
        var values = image.getValues();

        int height = 0;

        for (int y = 0; y < values.length; y++) {
            // If there's something on the line, add to their height of it.
            if (OCRUtils.isRowPopulated(values, y)) {
                height++;
            } else if (height > 0) { // If the row has nothing on it and the line is populated, add it to the values
                int heightUntil = 0;
                int finalSpace = -1;

                // Seeing if the gap under the character is <= the height of the above piece. This is mainly for seeing
                // if the dot on an 'i' (And other similar characters) is <= is above the rest of the character the same
                // amount as its height (Making it a proper 'i' in Verdana and other fonts)
                for (int i = 0; i < height; i++) {
                    if (y + i >= values.length) {
                        finalSpace = 0;
                        break;
                    }

                    if (OCRUtils.isRowPopulated(values, y + i)) {
                        if (finalSpace == -1) {
                            finalSpace = heightUntil;
                        }
                    } else {
                        heightUntil++;
                    }
                }

                if (finalSpace > 0) {
                    if (height == finalSpace) {
                        y += finalSpace;
                        height += finalSpace;
                        continue;
                    }
                }

                lines.add(new IntPair(y - height, y));
                height = 0;
            } else {
                if (height == 0) continue;
                lines.add(new IntPair(y - height, y));
                height = 0;
            }
        }

        // <topY, bottomY>

        var remove = new ArrayList<Integer>();
        for (int i = 0; i < lines.size(); i++) {
            var current = lines.get(i);
            double currentHeight = current.getValue() - current.getKey();

            var onLast = i == lines.size() - 1;

            if (!onLast) {
                var below = lines.get(i + 1);
                double belowHeight = below.getValue() - below.getKey();
                if (belowHeight / currentHeight <= this.options.getMaxPercentDiffToMerge()
                        && ((double) current.getKey() - below.getKey()) / currentHeight <= this.options.getMaxPercentDiffToMerge()) {
                    remove.add(++i);
                    current.setValue(below.getValue());
                }
            }
        }

        remove.stream().sorted(Collections.reverseOrder()).forEach(i -> lines.remove(i.intValue()));

        return lines;
    }

    @Override
    public OCROptions getOptions() {
        return this.options;
    }
}
