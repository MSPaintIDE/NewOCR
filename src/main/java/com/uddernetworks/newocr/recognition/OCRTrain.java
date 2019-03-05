package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.train.TrainOptions;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class OCRTrain implements Train {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRTrain.class);

    public static final String TRAIN_STRING = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;
    private Actions actions;

    public OCRTrain(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        ImageIO.setUseCache(false);

        this.actions = new OCRActions(databaseManager);
    }

    @Override
    public void trainImage(File file) {
        trainImage(file, new TrainOptions());
    }

    @Override
    public void trainImage(File file, TrainOptions options) {

        // First clear the database
        databaseManager.clearData();

        List<TrainedCharacterData> trainedCharacterDataList = new ArrayList<>();

        // Preparing image

        var input = OCRUtils.readImage(file);
        var values = OCRUtils.createGrid(input);
        var searchCharacters = new ArrayList<SearchCharacter>();

        if (Boolean.getBoolean("newocr.rewrite")) {
            var temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
            OCRUtils.rewriteImage(temp, input);
            input = temp;
        }

        input = OCRUtils.filter(input).orElseThrow();

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);

        this.actions.getLettersDuringTraining(searchImage, searchCharacters);

        System.exit(0);

        TrainedCharacterData spaceTrainedCharacter = new TrainedCharacterData(' ');
        trainedCharacterDataList.add(spaceTrainedCharacter);

        Collections.sort(searchCharacters);

        // Pair<topY, bottomY> (Absolute coordinates)
        // Gets the top and bottom line bounds of every line
        var lineBounds = getLineBoundsForTesting(values, options);

//        for (IntPair lineBound : lineBounds) {
//            OCRUtils.colorRow(input, Color.MAGENTA, lineBound.getKey(), 0, input.getWidth());
//            OCRUtils.colorRow(input, Color.MAGENTA, lineBound.getValue(), 0, input.getWidth());
//        }
//
//        try {
//            ImageIO.write(input, "png", new File("E:\\NewOCR\\bounds.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        System.exit(0);

        // Stores the height/distance ratio for apostrophe parts
        var apostropheRatios = new ArrayList<Double>();

        var searchCharactersCopy = new ArrayList<>(searchCharacters);
        var searchCharacterLines = new ArrayList<List<SearchCharacter>>();
        var customSpaces = new HashMap<Character, List<Double>>();

        // Goes through each line found
        for (var lineBound : lineBounds) {
            int lineHeight = lineBound.getValue() - lineBound.getKey();

            // Gets all characters found at the line bounds from the searchCharacters (Collected from the double for loops)
            var line = OCRUtils.findCharactersAtLine(lineBound.getKey(), lineBound.getValue(), searchCharacters);
            searchCharacterLines.add(line);

            SearchCharacter nextMeasuringSpace = null;

            if (!line.isEmpty()) {
                var letterIndex = 0;
                var beforeSpaceX = 0;
                SearchCharacter firstQuote = null;

                for (SearchCharacter searchCharacter : line) {
                    // Gets the next character it knows it will be
                    char current = searchCharacter.getKnownChar() == ' ' ? ' ' : TRAIN_STRING.charAt(letterIndex++);
                    var modifier = 0;
                    var revertIndex = false;

                    // If the index is on the quote
                    if (letterIndex == 2) {
                        searchCharacter.setKnownChar('"');
                        if (firstQuote == null) {
                            firstQuote = searchCharacter;

                            // Make sure to subtract 1 from the letterIndex at the end, so it can process the " again
                            revertIndex = true;
                        } else {
                            var distance = searchCharacter.getX() - firstQuote.getX() - firstQuote.getWidth();
                            var ratio = (double) firstQuote.getHeight() / (double) distance;
                            apostropheRatios.add(ratio);

                            modifier = 1;
                        }

                        // If the current character is the FIRST `W`, sets beforeSpaceX to the current far right coordinate
                        // of the space (X + width), and go up another character (Skipping the space in TRAIN_STRING)
                    } else if (letterIndex == TRAIN_STRING.length() - 2) {
                        searchCharacter.setKnownChar('W');
                        beforeSpaceX = searchCharacter.getX() + searchCharacter.getWidth();
                        letterIndex++;
                        continue;

                        // If it's the last character, add the space based on beforeSpaceX and the current X, (Getting the
                        // width of the space) and reset the line
                    } else if (letterIndex == TRAIN_STRING.length()) {
                        searchCharacter.setKnownChar('W');
                        spaceTrainedCharacter.recalculateTo(searchCharacter.getX() - beforeSpaceX, lineHeight);
                        letterIndex = 0;
                        continue;
                    } else {
                        searchCharacter.setKnownChar(current);
                    }

                    if (nextMeasuringSpace != null) {
                        double width = searchCharacter.getX() - (nextMeasuringSpace.getX() + nextMeasuringSpace.getWidth());
                        double ratio = width / (double) nextMeasuringSpace.getHeight();
                        customSpaces.computeIfAbsent(nextMeasuringSpace.getKnownChar(), x -> new ArrayList<>()).add(ratio);
                        nextMeasuringSpace = null;
                    }

                    if (options.getSpecialSpaces().contains(current)) {
                        nextMeasuringSpace = searchCharacter;
                    }

                    searchCharacter.setModifier(modifier);
                    var trainedSearchCharacter = getTrainedCharacter(trainedCharacterDataList, current, modifier);

                    // Adds the current segment values of the current searchCharacter to the trainedSearchCharacter
                    trainedSearchCharacter.recalculateTo(searchCharacter);

                    double halfOfLineHeight = ((double) lineBound.getValue() - (double) lineBound.getKey()) / 2;
                    double middleToTopChar = (double) searchCharacter.getY() - (double) lineBound.getKey();
                    double topOfLetterToCenter = halfOfLineHeight - middleToTopChar;

                    // Sets the current center to be calculated, along with any meta it may have
                    trainedSearchCharacter.recalculateCenter(topOfLetterToCenter); // This NOW gets offset from top of
                    trainedSearchCharacter.setHasDot(searchCharacter.hasDot());
                    trainedSearchCharacter.setLetterMeta(searchCharacter.getLetterMeta());

                    if (revertIndex) letterIndex--;

                    // Resets the current letter
                    if (letterIndex >= TRAIN_STRING.length()) {
                        letterIndex = 0;
                    }
                }

                // Removes any used letters from the line in searchCharacters, so none will be duplicated and to
                // increase performance.
                searchCharacters.removeAll(line);
            }
        }

        searchCharacters = searchCharactersCopy;

        LOGGER.debug(searchCharacters.size() + " characters found");


        // Before writing the data to the database and finalizing the data, it needs to read the training image and
        // detect any differences, and then modify the collected data accordingly.
        var exclude = new HashMap<Character, List<Integer>>();

        // This is how many errors exist per character to know if a potential fix has actually worked or not
        Map<Character, Integer> errorsForCharacter = new HashMap<>();

        for (int i2 = 0; i2 < options.getMaxCorrectionIterations(); i2++) {
            System.out.println("========================== [" + i2 + "] ==========================");
            var foundThisRun = new ArrayList<Character>();

            var changes = new LongAdder();
            for (int i = 0; i < searchCharacterLines.size(); i++) {
                var line = searchCharacterLines.get(i);
                int lineNumber = i;
                line.forEach(searchCharacter -> {
                    // Because the second and third W will be for space testing
                    var foundW = new LongAdder();
                    this.actions.getCharacterFor(searchCharacter, trainedCharacterDataList).ifPresentOrElse(imageLetter -> {
                        var correct = searchCharacter.getKnownChar();
                        var calculatedChar = imageLetter.getLetter();
                        System.out.print(calculatedChar);

                        if (correct == 'W') foundW.increment();
                        if (foundW.intValue() == 2) return;

                        if (correct == calculatedChar) return;
                        if ((correct == '"' && calculatedChar == '\'') || (correct == '\'' && calculatedChar == '"')) return;

                        // Don't try and fix the same correct character twice in one run
                        if (foundThisRun.contains(correct)) return;

                        if (exclude.containsKey(correct) && exclude.get(correct).contains(lineNumber)) return;

                        LOGGER.debug("Incorrect character for " + correct + " (Found as " + calculatedChar + " on line " + lineNumber + ")");
                        var trainedSearchCharacter = getTrainedCharacter(trainedCharacterDataList, correct, searchCharacter.getModifier());

                        errorsForCharacter.putIfAbsent(correct, getErrorsForCharacter(searchCharacterLines, trainedCharacterDataList, correct));

                        // Try and correct the training data by invoking TrainedCharacterData#recalculateTo until it works
                        var maxIterations = 1000;

                        int recalcAttempts;
                        for (recalcAttempts = 0; recalcAttempts < maxIterations; recalcAttempts++) {
                            trainedSearchCharacter.recalculateTo(searchCharacter);

                            var gotten = this.actions.getCharacterFor(searchCharacter, trainedCharacterDataList);
                            if (gotten.isEmpty()) break;
                            var gottenCharacter = gotten.get();

                            if (gottenCharacter.getLetter() == correct) break;
                        }


                        // If it reached maxIterations then just undo it, because it probably didn't work
                        if (recalcAttempts == maxIterations) {
                            System.err.println("Reached max iterations on " + correct);
                            trainedSearchCharacter.undoLastRecalculations(recalcAttempts + 1);
                            exclude.computeIfAbsent(correct, x -> new ArrayList<>()).add(lineNumber);
                            return;
                        }

                        // If there is more errors than before, OR if it maxed out on attempts, undo it and add to exclusions
                        var errors = getErrorsForCharacter(searchCharacterLines, trainedCharacterDataList, correct);
                        var previousErrors = errorsForCharacter.getOrDefault(correct, Integer.MAX_VALUE);


                        LOGGER.debug("Recalculated " + correct + " after " + recalcAttempts + " attempts");

                        // If this recalculation creates more errors, undo it. This can probably be improved in the future, but works well enough for now.
                        if (errors > previousErrors) {
                            LOGGER.debug("The previous recalculation created " + (errors - previousErrors) + " more errors, so " + recalcAttempts + " recalculations are being undone on line " + lineNumber + ".");
                            trainedSearchCharacter.undoLastRecalculations(recalcAttempts + 1);
                            exclude.computeIfAbsent(correct, x -> new ArrayList<>()).add(lineNumber);

                            LOGGER.debug("There were " + previousErrors + " before, and " + errors + " after the previous calculations. This next number should be the previous errors: " + getErrorsForCharacter(searchCharacterLines, trainedCharacterDataList, correct));
                            return;
                        }

                        errorsForCharacter.put(correct, errors);

                        foundThisRun.add(correct);
                        changes.increment();

                    }, () -> LOGGER.debug("Couldn't find a value for the SearchCharacter at (" + searchCharacter.getX() + "x" + searchCharacter.getY() + ")"));
                });

                System.out.print("\n");
            }

            if (changes.intValue() == 0) {
                LOGGER.debug("Nothing changed, so stopping correction.");
                break;
            }
        }

        LOGGER.debug("Writing data to database...");
        long start = System.currentTimeMillis();

        // Add the apostropheRatios data into the database
        CompletableFuture.runAsync(() -> databaseManager.addAveragedData("apostropheRatio", apostropheRatios.stream().mapToDouble(Double::doubleValue).toArray()))
                .thenRunAsync(() -> customSpaces.forEach((character, ratios) -> databaseManager.addCustomSpace(character, ratios.stream().mapToDouble(Double::doubleValue).average().orElse(0))));

        // Inserts all character data into the database after recalculating the
        trainedCharacterDataList.forEach(databaseTrainedCharacter -> {
            try {
                databaseTrainedCharacter.finishRecalculations();

                char letter = databaseTrainedCharacter.getValue();

                CompletableFuture.runAsync(() -> databaseManager.createLetterEntry(letter, databaseTrainedCharacter.getModifier(), databaseTrainedCharacter.getWidthAverage(), databaseTrainedCharacter.getHeightAverage(), databaseTrainedCharacter.getMinCenter(), databaseTrainedCharacter.getMaxCenter(), databaseTrainedCharacter.hasDot(), databaseTrainedCharacter.getLetterMeta(), letter == ' '))
                        .thenRunAsync(() -> {
                            if (letter != ' ') {
                                databaseManager.addLetterSegments(letter, databaseTrainedCharacter.getModifier(), databaseTrainedCharacter.getSegmentPercentages());
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        LOGGER.debug("Finished writing to database in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public int getErrorsForCharacter(List<List<SearchCharacter>> charData, List<TrainedCharacterData> trainedCharacterDataList, char checking) {
        var errors = new AtomicInteger(0);
        charData.parallelStream().forEach(line -> line.parallelStream()
                .forEach(searchCharacter -> this.actions.getCharacterFor(searchCharacter, trainedCharacterDataList).ifPresent(imageLetter -> {
                    var correct = searchCharacter.getKnownChar();
                    var calculatedChar = imageLetter.getLetter();
                    if (correct == checking && calculatedChar != correct) errors.incrementAndGet();
                })));

        return errors.get();
    }

    @Override
    public TrainedCharacterData getTrainedCharacter(List<TrainedCharacterData> trainedCharacterDataList, char current, int finalModifier) {
        return trainedCharacterDataList
                .stream()
                .filter(trainedCharacterData -> trainedCharacterData.getValue() == current
                        && trainedCharacterData.getModifier() == finalModifier)
                .findFirst()
                .orElseGet(() -> {
                    var trained = new TrainedCharacterData(current, finalModifier);
                    trainedCharacterDataList.add(trained);
                    return trained;
                });
    }

    @Override
    public List<IntPair> getLineBoundsForTesting(boolean[][] values, TrainOptions options) {
        // Pair<topY, bottomY>
        List<IntPair> lines = new ArrayList<>();

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
}
