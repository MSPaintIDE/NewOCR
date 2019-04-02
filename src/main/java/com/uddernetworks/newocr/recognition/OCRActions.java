package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class OCRActions implements Actions {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRActions.class);

    private DatabaseManager databaseManager;
    private SimilarityManager similarityManager;
    private OCROptions options;

    private double distanceAbove = -1;
    private double distanceBelow = -1;

    public OCRActions(DatabaseManager databaseManager, SimilarityManager similarityManager, OCROptions options) {
        this.databaseManager = databaseManager;
        this.similarityManager = similarityManager;
        this.options = options;
    }

    @Override
    public void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters) {
        try {
            if (this.distanceAbove == -1)
                this.distanceAbove = this.databaseManager.getAveragedData("distanceAbove").get();
            if (this.distanceBelow == -1)
                this.distanceBelow = this.databaseManager.getAveragedData("distanceBelow").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

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
                '=', "equalsDistance"
        );

        // By default 0
        var configurableBases = Map.of(
                Set.of('i', 'j', ':', ';', '='), 1 // The base is the second character (Bottom part)
        );

        var first = true;

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

//          Get the characters with horizontal overlap
            var ignored = new HashSet<SearchCharacter>();

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
                        .sorted(Comparator.comparingInt(SearchCharacter::getY))
//                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toCollection(LinkedList::new));
//                Collections.reverse(list);

                list.forEach(searchCharacter -> {
                    System.out.println("SearchCharacter in list at (" + searchCharacter.getX() + ", " + searchCharacter.getY() + ")");
                });

                var currentChar = OCRTrain.TRAIN_STRING.charAt(i1);

                // If this is 1, it gets the second character
                var index = configurableBases.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().contains(currentChar))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(0);

                System.out.println("Index of using is " + index);

                var base = list.get(index);

                boolean finalFirst = first;
                list.forEach(part2 -> {
//                    double maxHeight = Math.max(base.getHeight(), part2.getHeight());

                    if (!base.equals(part2)) { // If part2 is NOT the base
                        if (currentChar == '!' || currentChar == '?') { // ! ?
                            double diff = (double) (part2.getY() - (base.getY() + base.getHeight()));
                            var distance = diff / (double) base.getHeight();
                            base.setTrainingMeta("distanceBelow", distance);
                        } else if (currentChar == 'i' || currentChar == 'j' || currentChar == ':' || currentChar == ';' || currentChar == '=') { // i j   base below
                            double diff = (double) (base.getY() - (part2.getY() + part2.getHeight()));
                            var distance = diff / (double) base.getHeight();

                            if (currentChar == ':') {
                                System.out.println(base.getY() + " - (" + part2.getY() + " + " + part2.getHeight() + ") = " + diff);
                                System.out.println("Colon distance = " + distance);
                            }

                            base.setTrainingMeta(charMetaMap.getOrDefault(currentChar, "distanceAbove"), distance);
                        }
                    }

                    var i = increment.getAndIncrement();
                    part2.setModifier(i);
                    ignored.add(part2);

                    if (finalFirst) {
                        var lett = Letter.getLetter(currentChar, i);
//                        OCRUtils.makeImage(part2.getValues(), "ind\\" + lett.name() + ".png");
                    }
                });
            }

            ret.add(new TrainLine(found, fromY, toY));
            first = false;
        }

        return ret;
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        try {
            System.out.println("Character values = " + Arrays.toString(searchCharacter.getSegmentPercentages()));
            var diffs = new Object2DoubleOpenHashMap<ImageLetter>(); // The lower value the better

            var data = new ArrayList<>(databaseManager.getAllCharacterSegments().get());

            data.stream()
                    .filter(character -> character.hasDot() == searchCharacter.hasDot())
                    .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
                    .forEach(character ->
                            OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData()).ifPresent(charDifference -> {
                                // Gets the difference of the database character and searchCharacter (Lower is better)
                                var imageLetter = new ImageLetter(character.getLetter(), character.getModifier(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getAvgWidth(), character.getAvgHeight(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates());
                                imageLetter.setMaxCenter(character.getMaxCenter());
                                imageLetter.setMinCenter(character.getMinCenter());
                                diffs.put(imageLetter, charDifference);
                            }));

            return getCharacterFor(searchCharacter, diffs);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data) {
        Object2DoubleMap<ImageLetter> diffs = new Object2DoubleOpenHashMap<>(); // The lower value the better

        data.stream()
                .filter(character -> character.hasDot() == searchCharacter.hasDot())
                .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
                .forEach(character -> {
                    character.finishRecalculations();
                    OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getSegmentPercentages()).ifPresent(charDifference -> {
                        // Gets the difference of the database character and searchCharacter (Lower is better)
                        diffs.put(new ImageLetter(character.getValue(), character.getModifier(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getWidthAverage(), character.getHeightAverage(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates()), charDifference);
                    });
                });

        return getCharacterFor(searchCharacter, diffs);
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs) {
        System.out.println("===========================================================");
        // TODO: The following code can definitely be improved
        var entries = diffs.object2DoubleEntrySet()
                .stream()
                .sorted(Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue))
                .limit(10)
                .collect(Collectors.toList());

        // If there's no characters found, don't continue
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        var firstEntry = entries.get(0);

        double ratio = firstEntry.getKey().getAverageWidth() / firstEntry.getKey().getAverageHeight();
        double searchRatio = (double) searchCharacter.getWidth() / (double) searchCharacter.getHeight();

        var ratioDifference = diff(ratio, searchRatio);

//        System.out.println("entries = " + entries);

        OCRUtils.makeImage(searchCharacter.getValues(), "ind\\pot.png");

        // This makes the secondEntry anything that can't be interchanged, so it won't try something else
        // if the top 2 options are % mod 0 and % mod 1 (The dots of a %) or a " mod 0 and " mod 2

        this.similarityManager.getSecondHighest(entries).ifPresent(secondEntry -> {
            // Skip everything if it has a very high confidence of the character (Much higher than the closest one OR is <= 0.01 in confidence),
            // and make sure that the difference in width/height is very low, or else it will continue and sort by width/height difference.
            // TODO: Make 1.5 an option
            var bigDifference = firstEntry.getDoubleValue() * 1.5 > secondEntry.getDoubleValue(); // If true, SORT
            var verySmallDifference = ratioDifference <= 0.01 || (firstEntry.getDoubleValue() <= 0.01 && secondEntry.getDoubleValue() > 0.1); // If true, skip sorting
            var ratioDiff = ratioDifference > 0.1; // If true, SORT

            // TODO: This should probably be in some options
            // This is because characters - and | have *extremely* similar section data, but the ratios are very different,
            // which in most characters high ratio differences means a very different character. This is just a check to compare
            // ratios between the two if they are a - or | or _
            var letter = firstEntry.getKey().getLetter();
            var secondLetter = secondEntry.getKey().getLetter();
            var sorting = !verySmallDifference && (bigDifference || ratioDiff);
            var sizeCheck = this.options.getRequireSizeCheck();

            var firstLetterEnum = Letter.getLetter(firstEntry.getKey());
            var secondLetterEnum = Letter.getLetter(secondEntry.getKey());

            System.out.println("First " + firstEntry.getDoubleValue() + " * 2 >  " + secondEntry.getDoubleValue());
            System.out.println("ratioDifference = " + ratioDifference);

            System.out.println("sorting = " + sorting + "\t\t\t!" + verySmallDifference + " && (" + bigDifference + " || " + ratioDiff + ")");
            System.out.println("sizeCheck = " + sizeCheck);
            System.out.println("letter = " + firstLetterEnum.name());
            System.out.println("secondLetter = " + secondLetterEnum.name());
            System.out.println("firstEntry = " + firstEntry);

            if (sizeCheck.contains(firstLetterEnum) && sizeCheck.contains(secondLetterEnum)) {
                System.out.println("Contains here!");
//            if ((letter == '-' || letter == '|' || letter == '_' || letter == '=' || letter == '.')
//                    && (secondLetter == '-' || secondLetter == '|' || secondLetter == '_' || secondLetter == '=' || secondLetter == '.')) {
                sorting = true;
                entries.removeIf(entry -> !sizeCheck.contains(Letter.getLetter(entry.getKey())));
            }

            if (sorting) {
                entries.sort(Comparator.comparingDouble(entry -> {
                    // Lower is more similar
                    return compareSizes(searchCharacter.getWidth(), searchCharacter.getHeight(), entry.getKey().getAverageWidth(), entry.getKey().getAverageHeight());
                }));

                System.out.println("entries = " + entries);
            }
        });

        ImageLetter first = entries.get(0).getKey();
        first.setValues(searchCharacter.getValues());
        return Optional.of(first);
    }

    @Override
    public double compareSizes(double width1, double height1, double width2, double height2) {
        var res = 0D;

        double ratio1 = width1 / height1;
        double ratio2 = width2 / height2;
        double ratioDiff = diff(ratio1, ratio2);

        if ((width1 > height1 && width2 < height2)
                || (width1 < height1 && width2 > height2)) {
            // If they aren't rotated the right way (E.g. tall rectangle isn't similar to a wide one)
            if (ratioDiff > 0.5) {
                res += 300D;
            }
        }

        res += ratioDiff;
        return res;
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

                LOGGER.debug("Height of " + height);
                lines.add(new IntPair(y - height, y));
                height = 0;
            } else {
                if (height == 0) continue;
                LOGGER.debug("Height: " + height);
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

        LOGGER.debug("After removal:");

        for (IntPair line : lines) {
            LOGGER.debug("Height: " + (line.getValue() - line.getKey()));
        }

        return lines;
    }

    private boolean characterAt(int index, char character) {
        return OCRTrain.TRAIN_STRING.charAt(index) == character;
    }

    private int index(char character) {
        return OCRTrain.TRAIN_STRING.indexOf(character);
    }

}
