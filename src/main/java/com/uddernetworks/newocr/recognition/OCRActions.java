package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.TrainOptions;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class OCRActions implements Actions {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRActions.class);

    private DatabaseManager databaseManager;
    private SimilarityManager similarityManager;

    private double distanceAbove = -1;
    private double distanceBelow = -1;
    private int index = 0;

    public OCRActions(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        this.databaseManager = databaseManager;
        this.similarityManager = similarityManager;
    }

    @Override
    public void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters) {
        try {
            if (this.distanceAbove == -1) this.distanceAbove = this.databaseManager.getAveragedData("distanceAbove").get();
            if (this.distanceBelow == -1) this.distanceBelow = this.databaseManager.getAveragedData("distanceBelow").get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        System.out.println("distanceAbove = " + distanceAbove);
        System.out.println("distanceBelow = " + distanceBelow);

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
    public List<CharacterLine> getLettersDuringTraining(SearchImage searchImage, TrainOptions options) {
        var ret = new ArrayList<CharacterLine>();

        var lineBounds = getLineBoundsForTraining(searchImage, options);
        for (var coords : lineBounds) {
            var fromY = coords.getKey();
            var toY = coords.getValue();

            var sub = searchImage.getSubimage(0, fromY, searchImage.getWidth(), toY - fromY);

//                ImageIO.write(sub.toImage(), "png", new File("E:\\NewOCR\\ind\\" + fromY + ".png"));

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

            System.out.println("Found size before: " + found.size());

//          Get the characters with horizontal overlap

            var ignored = new HashSet<SearchCharacter>();

            Collections.sort(found);

            // These values represent the indices of characters that require multiple parts
            var multipleParts = Arrays.asList(0, 7, 29, 31, 34, 37, 80, 82);

            for (int i1 = 0; i1 < found.size(); i1++) {
                var part1 = found.get(i1);

//                OCRUtils.makeImage(part1.getValues(), "ind\\" + i1 + ".png");

                if (ignored.contains(part1)) continue;
                if (!multipleParts.contains(i1)) continue;
                System.out.println("here: " + i1);

                var increment = new AtomicInteger(0);
                int finalI = i1;
                AtomicBoolean first = new AtomicBoolean(true);
                var list = found.stream()
//                        .filter(Predicate.not(part1::equals))
                        .filter(part1::isOverlappingX)
                        .sorted(Comparator.comparingInt(SearchCharacter::getY))
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());

                SearchCharacter tempBase;

                if (finalI == index('!') || finalI == index('?')) { // part1 above
                    tempBase = list.get(0);
                } else if (finalI == index('i') || finalI == index('j')) { // part1 below
                    tempBase = list.get(list.size() - 1);
                } else if (finalI == index('%')) {
                    tempBase = list.get(0);
                } else {
                    tempBase = list.get(0);
                }

                var base = tempBase;

                list.forEach(part2 -> {
                    double maxHeight = Math.max(base.getHeight(), part2.getHeight());

                    if (!base.equals(part2)) {
                        if (finalI == index('!') || finalI == index('?')) { // ! ?
                            double diff = (double) (part2.getY() - (base.getY() + base.getHeight())) - 1;
//                                System.out.println("2 diff = " + diff);
                            var distance = maxHeight / diff;
                            base.setTrainingMeta("distanceBelow", distance);
                        } else if (finalI == index('i') || finalI == index('j')) { // i j   base below
                            double diff = (double) (base.getY() - (part2.getY() + part2.getHeight())) - 1;
//                                System.out.println("1 diff = " + diff);
                            var distance = maxHeight / diff;
                            base.setTrainingMeta("distanceAbove", distance);
                        }
                    }

//                            System.out.println("GOT");
                    var i = increment.getAndIncrement();
                    System.out.println("i = " + i);
                    part2.setModifier(i);
                    ignored.add(part2);

                    if (first.get()) {
                        first.set(false);
//                                OCRUtils.makeImage(base.getValues(), "ind\\" + finalI1 + ".png");
                    }

//                            OCRUtils.makeImage(part2.getValues(), "ind\\" + finalI + "_m" + i + ".png");
                });
            }

            ret.add(new TrainLine(found, fromY, toY));
//            System.exit(0);

        }

        return ret;
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        try {
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

        // TODO: Modulize this
        // This makes the secondEntry anything that can't be interchanged, so it won't try something else
        // if the top 2 options are % mod 0 and % mod 1 (The dots of a %) or a " mod 0 and " mod 2

        this.similarityManager.getSecondHighest(entries).ifPresentOrElse(secondEntry -> {
            // Skip everything if it has a very high confidence of the character (Much higher than the closest one OR is <= 0.01 in confidence),
            // and make sure that the difference in width/height is very low, or else it will continue and sort by width/height difference.
            var bigDifference = firstEntry.getDoubleValue() * 2 > secondEntry.getDoubleValue(); // If true, SORT
            System.out.println((firstEntry.getDoubleValue()) + " * 2 > " + secondEntry.getDoubleValue());
            var verySmallDifference = ratioDifference <= 0.01 || firstEntry.getDoubleValue() <= 0.01; // If true, skip sorting
            var ratioDiff = ratioDifference > 0.1; // If true, SORT

            System.out.println("verySmallDifference = " + verySmallDifference);
            System.out.println("bigDifference = " + bigDifference);
            System.out.println("ratioDiff = " + ratioDiff);

            if (!verySmallDifference && (bigDifference || ratioDiff)) {
                System.out.println("Comparing sizes");
                entries.sort(Comparator.comparingDouble(entry -> {
                    // Lower is more similar
                    return compareSizes(searchCharacter.getWidth(), searchCharacter.getHeight(), entry.getKey().getAverageWidth(), entry.getKey().getAverageHeight());
                }));
            }
        }, () -> {
            System.out.println("Perfect match!");
        });

        System.out.println("entries = " + entries);

        ImageLetter first = entries.get(0).getKey();
        first.setValues(searchCharacter.getValues());
        return Optional.of(first);
    }

    // TODO: Remove this
    private Optional<Object2DoubleMap.Entry<ImageLetter>> getSecondHighest(ImageLetter first, List<Object2DoubleMap.Entry<ImageLetter>> data) {
        var firstLetter = first.getLetter();
        var firstMod = first.getModifier();

        System.out.println("firstLetter = " + firstLetter);

        // TODO: Java 12 update? This can also definitely be optimised/minified in the future, this is mainly for seeing if this will work
        switch (firstLetter) {
            case '%':
                if (firstMod <= 1) {
                    for (int i = 1; i < data.size(); i++) {
                        var entry = data.get(i);
                        var letter = entry.getKey();

                        var cha = letter.getLetter();
                        var mod = letter.getModifier();

                        if ((cha == firstLetter && (mod == 0 || mod == 1))
                                || (cha == 'o')) continue;
                        return Optional.of(entry);
                    }
                }

                return Optional.empty();
            case '\'':
            case '"':
            case 'i':
                if (firstLetter == 'i' && firstMod != 1) break;
                for (int i = 1; i < data.size(); i++) {
                    var entry = data.get(i);
                    var letter = entry.getKey();

                    var cha = letter.getLetter();
                    var mod = letter.getModifier();

                    if (cha == '\"' || cha == '\'' || (cha == 'i' && mod == 1)) continue;
                    return Optional.of(entry);
                }

                return Optional.empty();
            case '-':
            case '=':
            case '_':
                for (int i = 1; i < data.size(); i++) {
                    var entry = data.get(i);
                    var letter = entry.getKey();

                    var cha = letter.getLetter();

                    if (cha == '-' || cha == '=' || cha == '_') continue;
                    return Optional.of(entry);
                }

                return Optional.empty();
        }

        // TODO: Add dots too

        return Optional.of(data.get(1));
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
    public List<IntPair> getLineBoundsForTraining(SearchImage image, TrainOptions options) {
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
                if (belowHeight / currentHeight <= options.getMaxPercentDiffToMerge()
                        && ((double) current.getKey() - below.getKey()) / currentHeight <= options.getMaxPercentDiffToMerge()) {
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

    private int index(char character) {
        return OCRTrain.TRAIN_STRING.indexOf(character);
    }


}
