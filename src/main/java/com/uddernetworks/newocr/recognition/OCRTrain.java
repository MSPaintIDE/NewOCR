package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.character.TrainedCharacterData;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OCRTrain implements Train {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRTrain.class);

    public static final String TRAIN_STRING = "!!\"#$%%%&'()*+,-./0123456789::;;<==>??@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghiijjklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;
    private OCROptions options;
    private Actions actions;

    /**
     * Creates a new {@link OCRTrain}.
     *
     * @param databaseManager The {@link DatabaseManager} to use
     * @param options The {@link OCROptions} to use
     */
    public OCRTrain(DatabaseManager databaseManager, OCROptions options) {
        this.databaseManager = databaseManager;
        this.options = options;
        ImageIO.setUseCache(false);

        this.actions = new OCRActions(databaseManager, options);
    }

    @Override
    public void trainImage(File file) {

        if (this.databaseManager.isTrainedSync()) {
            databaseManager.clearData();
            this.databaseManager.setTrained(false);
        }

        List<TrainedCharacterData> trainedCharacterDataList = new ArrayList<>();

        // Preparing image

        var input = OCRUtils.readImage(file);
        var values = OCRUtils.createGrid(input);

        input = OCRUtils.filter(input).orElseThrow();

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);

        TrainedCharacterData spaceTrainedCharacter = new TrainedCharacterData(' ');
        trainedCharacterDataList.add(spaceTrainedCharacter);

        // Stores the height/distance ratio for apostrophe parts
        var apostropheRatios = new DoubleArrayList();

        var customSpaces = new HashMap<Character, List<Double>>();

        var metaMapping = Stream.of("distanceAbove", "distancei", "distancej", "colonDistance", "semicolonDistance", "equalsDistance", "distanceQuestion", "distanceExclamation")
                .collect(Collectors.toMap(name -> name, name -> new DoubleArrayList()));

        // Goes through each line found
        for (var line : this.actions.getLettersDuringTraining(searchImage)) {

            // Gets all characters found at the line bounds from the searchCharacters (Collected from the double for loops)
            SearchCharacter nextMeasuringSpace = null;

            if (!line.getLetters().isEmpty()) {
                var letterIndex = 0;
                var beforeSpaceX = 0;
                SearchCharacter firstQuote = null;

                for (SearchCharacter searchCharacter : line.getLetters()) {
                    // Gets the next character it knows it will be
                    char current = searchCharacter.getLetter() == ' ' ? ' ' : TRAIN_STRING.charAt(letterIndex++);
                    var modifier = searchCharacter.getModifier();
                    var revertIndex = false;

                    // If the index is on the quote
                    if (letterIndex == 3) {
                        searchCharacter.setLetter('"');
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
                        searchCharacter.setLetter('W');
                        beforeSpaceX = searchCharacter.getX() + searchCharacter.getWidth();
                        letterIndex++;
                        continue;

                        // If it's the last character, add the space based on beforeSpaceX and the current X, (Getting the
                        // width of the space) and reset the line
                    } else if (letterIndex == TRAIN_STRING.length()) {
                        searchCharacter.setLetter('W');
                        spaceTrainedCharacter.recalculateTo(searchCharacter.getX() - beforeSpaceX, line.bottomY() - line.topY());
                        letterIndex = 0;
                        continue;
                    } else {
                        searchCharacter.setLetter(current);
                    }

                    if (nextMeasuringSpace != null) {
                        double width = searchCharacter.getX() - (nextMeasuringSpace.getX() + nextMeasuringSpace.getWidth());
                        double ratio = width / (double) nextMeasuringSpace.getHeight();
                        customSpaces.computeIfAbsent(nextMeasuringSpace.getLetter(), x -> new ArrayList<>()).add(ratio);
                        nextMeasuringSpace = null;
                    }

                    if (this.options.getSpecialSpaces().contains(current)) {
                        nextMeasuringSpace = searchCharacter;
                    }

                    metaMapping.forEach((meta, list) -> searchCharacter.getTrainingMeta(meta).ifPresent(list::add));

                    searchCharacter.setModifier(modifier);
                    var trainedSearchCharacter = getTrainedCharacter(trainedCharacterDataList, current, modifier);

                    // Adds the current segment values of the current searchCharacter to the trainedSearchCharacter
                    trainedSearchCharacter.recalculateTo(searchCharacter);

                    double halfOfLineHeight = ((double) line.bottomY() - (double) line.topY()) / 2;
                    double middleOfLineToTopChar = (double) searchCharacter.getY() - (double) line.topY();
                    double topOfLetterToCenter = halfOfLineHeight - middleOfLineToTopChar;

                    // Sets the current center to be calculated, along with any meta it may have
                    trainedSearchCharacter.recalculateCenter(topOfLetterToCenter); // This NOW gets offset from top of

                    if (revertIndex) letterIndex--;

                    // Resets the current letter
                    if (letterIndex >= TRAIN_STRING.length()) {
                        letterIndex = 0;
                    }
                }
            }
        }

        LOGGER.debug("Writing data to database...");
        long start = System.currentTimeMillis();

        // Add the apostropheRatios data into the database
        CompletableFuture.runAsync(() -> metaMapping.forEach(databaseManager::addAveragedData))
                .thenRunAsync(() -> databaseManager.addAveragedData("apostropheRatio", apostropheRatios))
                .thenRunAsync(() -> customSpaces.forEach((character, ratios) -> databaseManager.addCustomSpace(character, ratios.stream().mapToDouble(Double::doubleValue).average().orElse(0))));

        // Inserts all character data into the database after recalculating the
        trainedCharacterDataList.forEach(databaseTrainedCharacter -> {
            try {
                databaseTrainedCharacter.finishRecalculations();

                char letter = databaseTrainedCharacter.getLetter();

                CompletableFuture.runAsync(() -> databaseManager.createLetterEntry(letter, databaseTrainedCharacter.getModifier(), databaseTrainedCharacter.getWidthAverage(), databaseTrainedCharacter.getHeightAverage(), databaseTrainedCharacter.getMinCenter(), databaseTrainedCharacter.getMaxCenter(), letter == ' '))
                        .thenRunAsync(() -> {
                            if (letter != ' ') {
                                databaseManager.addLetterSegments(letter, databaseTrainedCharacter.getModifier(), databaseTrainedCharacter.getSegmentPercentages());
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        this.databaseManager.setTrained(true);

        LOGGER.debug("Finished writing to database in " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public TrainedCharacterData getTrainedCharacter(List<TrainedCharacterData> trainedCharacterDataList, char current, int finalModifier) {
        return trainedCharacterDataList
                .stream()
                .filter(trainedCharacterData -> trainedCharacterData.getLetter() == current
                        && trainedCharacterData.getModifier() == finalModifier)
                .findFirst()
                .orElseGet(() -> {
                    var trained = new TrainedCharacterData(current, finalModifier);
                    trainedCharacterDataList.add(trained);
                    return trained;
                });
    }
}
