package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.Histogram;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class OCRActions implements Actions {

    private DatabaseManager databaseManager;

    private double distanceAbove = -1;
    private double distanceBelow = -1;
    private int index = 0;

    public OCRActions(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
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
                    searchCharacters.add(new SearchCharacter(new ArrayList<>(coordinates)));
                    coordinates.clear();
                }
            }
        }

/*
        searchCharacters.removeIf(searchCharacter -> searchCharacter.getWidth() == 1);

        // distanceAbove
        var ignoring = new HashSet<SearchCharacter>();

        for (SearchCharacter searchCharacter : searchCharacters) {
            if (ignoring.contains(searchCharacter)) continue;

            System.out.println(searchCharacter.getWidth());
            System.out.println(searchCharacter.getHeight());

            System.out.println("\n\n======================== [" + (++index) + "] ========================");
//            OCRUtils.makeImage(searchCharacter.getValues(), "E:\\NewOCR\\ind\\" + index + ".png");

            System.out.println("ignoring = " + ignoring);

            var charactersAbove = new ArrayList<SearchCharacter>();
            var charactersBelow = new ArrayList<SearchCharacter>();
            var charactersOverlapping = new ArrayList<SearchCharacter>();

            // TODO: Parallel this?
            searchCharacters.stream()
                    .filter(Predicate.not(ignoring::contains))
                    .filter(Predicate.not(searchCharacter::equals))
                    .filter(filterChar -> filterChar.isOverlappingX(searchCharacter))
                    .forEach(filterChar -> {
                        List<SearchCharacter> listToAdd;
                        if (searchCharacter.isOverlappingPixels(filterChar)) {
                            listToAdd = charactersOverlapping;
                        } else
                            if (filterChar.getY() < searchCharacter.getY()) {
                            listToAdd = charactersAbove;
                        } else {
                            listToAdd = charactersBelow;
                        }

                        listToAdd.add(filterChar);
                    });

            System.out.println("charactersAbove = " + charactersAbove);
            System.out.println("charactersBelow = " + charactersBelow);

            var aboveIndex = 0;
            for (SearchCharacter part : charactersAbove) {
                System.out.println("Above!");
//                OCRUtils.makeImage(part.getValues(), "E:\\NewOCR\\ind\\above_" + aboveIndex++ + ".png");

                var bottomOfCharacterY = part.getY() + part.getHeight();
                var difference = Math.abs(bottomOfCharacterY - searchCharacter.getY());
                var isPartAbove = part.getHeight() < searchCharacter.getHeight();
                double minHeight = Math.min(part.getHeight(), searchCharacter.getHeight());
                double projectedDifference = this.distanceBelow * minHeight;
                double delta = projectedDifference * 0.25;

                System.out.println("difference = " + difference);
                System.out.println("projectedDifference = " + projectedDifference);
                System.out.println("Delta = " + delta);

                // Definitely can be improved
                if (diff(difference, projectedDifference) <= delta) {
                    System.out.println("Moving part");
                    var base = !isPartAbove ? part : searchCharacter;
                    var adding = !isPartAbove ? searchCharacter : part;
                    base.merge(adding);
                    ignoring.add(adding);
                }
            }

            var belowIndex = 0;
            for (SearchCharacter part : charactersBelow) {
                System.out.println("Below!");
//                OCRUtils.makeImage(part.getValues(), "E:\\NewOCR\\ind\\below_" + belowIndex++ + ".png");

                var bottomOfCharacterY = part.getY();
                var aboveY = searchCharacter.getY() + searchCharacter.getHeight();
                var difference = Math.abs(bottomOfCharacterY - aboveY);
                var isBelowBase = part.getHeight() < searchCharacter.getHeight();
                double minHeight = Math.min(part.getHeight(), searchCharacter.getHeight());
                double projectedDifference = this.distanceBelow * minHeight;
                double delta = projectedDifference * 0.25;
                System.out.println("difference = " + difference);
                System.out.println("projectedDifference = " + projectedDifference);
                System.out.println("Delta = " + delta);

                // Definitely can be improved
                if (diff(difference, projectedDifference) <= delta) {
                    System.out.println("Merging");
                    var base = !isBelowBase ? part : searchCharacter;
                    var adding = !isBelowBase ? searchCharacter : part;
                    base.merge(adding);
                    ignoring.add(adding);
                }
            }

            for (SearchCharacter overlap : charactersOverlapping) {
                System.out.println("Overlapping");
                var overlapBigger = overlap.getWidth() > searchCharacter.getWidth() && overlap.getHeight() > searchCharacter.getHeight();
                var base = overlapBigger ? overlap : searchCharacter;
                var adding = overlapBigger ? searchCharacter : overlap;

                base.merge(adding);
                ignoring.add(adding);
            }
        }

        searchCharacters.removeAll(ignoring);

        Collections.sort(searchCharacters);

        var i = 0;
        for (SearchCharacter searchCharacter : searchCharacters) {
            OCRUtils.makeImage(searchCharacter.getValues(), "E:\\NewOCR\\ind\\final_" + i++ + ".png");
        }
*/
    }

    @Override
    public void getLettersDuringTraining(SearchImage searchImage, List<SearchCharacter> searchCharacters) {
        var histogram = new Histogram(searchImage);
        for (var coords : histogram.getWholeLines()) {
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
                        found.add(new SearchCharacter(new ArrayList<>(coordinates), 0, fromY));
                        coordinates.clear();
                    }
                }
            }

            System.out.println("Found size before: " + found.size());

//          Get the characters with horizontal overlap

            var ignored = new HashSet<SearchCharacter>();

            for (SearchCharacter part1 : found) {
                if (ignored.contains(part1)) continue;

                for (SearchCharacter part2 : found) {
                    if (part1.equals(part2)) continue;

                    if (part1.isOverlappingX(part2)) {

                        if (part1.isOverlappingY(part2)) {

                        } else {
                            double maxHeight = Math.max(part1.getHeight(), part2.getHeight());
                            if (part2.getY() < part1.getY()) { // If it's a dot on the i or anything
                                double diff = (double) (part1.getY() - (part2.getY() + part2.getHeight())) - 1;
                                System.out.println("1 diff = " + diff);
                                var distance = maxHeight / diff;
                                part1.setTrainingMeta("distanceAbove", distance);
                            } else {
                                // part 2 > part 1
                                double diff = (double) (part2.getY() - (part1.getY() + part1.getHeight())) - 1;
                                System.out.println("2 diff = " + diff);
                                var distance = maxHeight / diff;
                                part1.setTrainingMeta("distanceBelow", distance);
                            }
                        }

                        part1.merge(part2);
                        ignored.add(part2);
                        break;
                    }
                }


            }
//
//
//
//            ignored.forEach(found::remove);
            Collections.sort(found);

            searchCharacters.addAll(found);
        }
    }

    @Override
    public Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        try {
            Object2DoubleMap<ImageLetter> diffs = new Object2DoubleOpenHashMap<>(); // The lower value the better

            var data = new ArrayList<>(databaseManager.getAllCharacterSegments().get());

            data.stream()
                    .filter(character -> character.hasDot() == searchCharacter.hasDot())
                    .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
                    .forEach(character ->
                            OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData()).ifPresent(charDifference -> {
                                var value = Arrays.stream(charDifference).average().orElse(0);
                                // Gets the difference of the database character and searchCharacter (Lower is better)
                                var imageLetter = new ImageLetter(character.getLetter(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getAvgWidth(), character.getAvgHeight(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates());
                                imageLetter.setMaxCenter(character.getMaxCenter());
                                imageLetter.setMinCenter(character.getMinCenter());
                                diffs.put(imageLetter, value);
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
                        var value = Arrays.stream(charDifference).average().orElse(0);
                        // Gets the difference of the database character and searchCharacter (Lower is better)
                        diffs.put(new ImageLetter(character.getValue(), searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), character.getWidthAverage(), character.getHeightAverage(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getCoordinates()), value);
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

        var secondEntry = entries.get(1);

        // Skip everything if it has a very high confidence of the character (Much higher than the closest one OR is <= 0.01 in confidence),
        // and make sure that the difference in width/height is very low, or else it will continue and sort by width/height difference.
        var bigDifference = firstEntry.getDoubleValue() * 2 > secondEntry.getDoubleValue(); // If true, SORT
        var verySmallDifference = ratioDifference <= 0.01 || firstEntry.getDoubleValue() <= 0.01; // If true, skip sorting
        var ratioDiff = ratioDifference > 0.1; // If true, SORT

        if (!verySmallDifference && (bigDifference || ratioDiff)) {
            entries.sort(Comparator.comparingDouble(entry -> {
                // Lower is more similar
                return compareSizes(searchCharacter.getWidth(), searchCharacter.getHeight(), entry.getKey().getAverageWidth(), entry.getKey().getAverageHeight());
            }));
        }

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

}
