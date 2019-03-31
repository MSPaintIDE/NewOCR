package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.train.TrainOptions;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class OCRTrain implements Train {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRTrain.class);

    public static final String TRAIN_STRING = "!!\"#$%%%&'()*+,-./0123456789::;;<==>??@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghiijjklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;
    private Actions actions;

    public OCRTrain(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        ImageIO.setUseCache(false);

        this.actions = new OCRActions(databaseManager, new DefaultSimilarityManager().loadDefaults());
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

        input = OCRUtils.filter(input).orElseThrow();

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);

        TrainedCharacterData spaceTrainedCharacter = new TrainedCharacterData(' ');
        trainedCharacterDataList.add(spaceTrainedCharacter);

        Collections.sort(searchCharacters);

        // Stores the height/distance ratio for apostrophe parts
        var apostropheRatios = new DoubleArrayList();
        var distancesAbove = new DoubleArrayList();
        var distancesBelow = new DoubleArrayList();
        var colonDistance = new DoubleArrayList();
        var semicolonDistance = new DoubleArrayList();
        var equalsDistance = new DoubleArrayList();

        var searchCharactersCopy = new ArrayList<>(searchCharacters);
        var customSpaces = new HashMap<Character, List<Double>>();

        var metaMapping = Map.of(
                "distanceAbove", distancesAbove,
                "distanceBelow", distancesBelow,
                "colonDistance", colonDistance,
                "semicolonDistance", semicolonDistance,
                "equalsDistance", equalsDistance
        );

        // Goes through each line found
        for (var line : this.actions.getLettersDuringTraining(searchImage, options)) {

            // Gets all characters found at the line bounds from the searchCharacters (Collected from the double for loops)
            SearchCharacter nextMeasuringSpace = null;

            if (!line.getLetters().isEmpty()) {
                var letterIndex = 0;
                var beforeSpaceX = 0;
                SearchCharacter firstQuote = null;

                for (SearchCharacter searchCharacter : line.getLetters()) {
                    // Gets the next character it knows it will be
                    char current = searchCharacter.getKnownChar() == ' ' ? ' ' : TRAIN_STRING.charAt(letterIndex++);
                    var modifier = searchCharacter.getModifier();
                    var revertIndex = false;

                    // If the index is on the quote
                    if (letterIndex == 3) {
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
                        spaceTrainedCharacter.recalculateTo(searchCharacter.getX() - beforeSpaceX, line.bottomY() - line.topY());
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

                    metaMapping.forEach((meta, list) -> searchCharacter.getTrainingMeta(meta).ifPresent(list::add));

                    searchCharacter.setModifier(modifier);
                    var trainedSearchCharacter = getTrainedCharacter(trainedCharacterDataList, current, modifier);

                    // Adds the current segment values of the current searchCharacter to the trainedSearchCharacter
                    trainedSearchCharacter.recalculateTo(searchCharacter);

                    double halfOfLineHeight = ((double) line.bottomY() - (double) line.topY()) / 2;
                    double middleToTopChar = (double) searchCharacter.getY() - (double) line.topY();
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
                searchCharacters.removeAll(line.getLetters());
            }
        }

        searchCharacters = searchCharactersCopy;

        LOGGER.debug(searchCharacters.size() + " characters found");

        LOGGER.debug("Writing data to database...");
        long start = System.currentTimeMillis();

        // Add the apostropheRatios data into the database
        CompletableFuture.runAsync(() -> databaseManager.addAveragedData("apostropheRatio", apostropheRatios))
                .thenRunAsync(() -> databaseManager.addAveragedData("distanceAbove", distancesAbove))
                .thenRunAsync(() -> databaseManager.addAveragedData("distanceBelow", distancesBelow))
                .thenRunAsync(() -> databaseManager.addAveragedData("colonDistance", colonDistance))
                .thenRunAsync(() -> databaseManager.addAveragedData("semicolonDistance", semicolonDistance))
                .thenRunAsync(() -> databaseManager.addAveragedData("equalsDistance", equalsDistance))
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
}
