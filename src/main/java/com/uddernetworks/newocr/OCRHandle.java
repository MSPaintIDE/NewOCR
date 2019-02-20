package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.train.TrainGenerator;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.Histogram;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class OCRHandle {

    private static String trainString = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjiklmnopqrstuvwxyz{|}~W W";

    private DatabaseManager databaseManager;

    public OCRHandle(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        ImageIO.setUseCache(false);
    }

    /**
     * Scans the input image and returns a {@link ScannedImage} containing all the characters and their info.
     *
     * @param file The input image to be scanned
     * @return A {@link ScannedImage} containing all scanned character data
     */
    public ScannedImage scanImage(File file) throws ExecutionException, InterruptedException {
        var start = System.currentTimeMillis();

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

        getLetters(searchImage, searchCharacters);

        // Gets all needed character data from the database based on the currently used font sizes

        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.getAllCharacterSegments().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        // Key = Entry<MinCenter, MaxCenter>  centers are ABSOLUTE
        Map<IntPair, List<ImageLetter>> lines = new LinkedHashMap<>();

        // Gets the closest matching character (According to the database values) using OCRHandle#getCharacterFor(SearchCharacter),
        // then it orders them by their X values, and then sorts the ImageLetters so certain ones go first, allowing the
        // characters to go to the correct lines

        searchCharacters
                .stream()
                .map(this::getCharacterFor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(ImageLetter::getX))
                .sorted((o1, o2) -> {
                    char cha = o1.getLetter();
                    char cha2 = o2.getLetter();

                    if (cha == cha2) return 0;
                    if (cha == ',' ^ cha2 == ',') return cha2 == ',' ? 1 : -1;
                    if (cha == '.' ^ cha2 == '.') return cha2 == '.' ? 1 : -1;
                    if (cha == '_' ^ cha2 == '_') return cha2 == '_' ? 1 : -1;
                    if (cha == '`' ^ cha2 == '`') return cha2 == '`' ? 1 : -1;
                    if (cha == '\'' ^ cha2 == '\'') return cha2 == '\'' ? 1 : -1;
                    if (cha == '"' ^ cha2 == '"') return cha2 == '"' ? 1 : -1;
                    if (cha == '*' ^ cha2 == '*') return cha2 == '*' ? 1 : -1;
                    return -1;
                })
                .forEach(imageLetter -> {
                    double maxCenter = imageLetter.getDatabaseCharacter().getMaxCenter();
                    double minCenter = imageLetter.getDatabaseCharacter().getMinCenter();
                    boolean subtract = maxCenter < 0 && imageLetter.getDatabaseCharacter().getMinCenter() < 0;
                    double centerDiff = subtract ?
                            maxCenter + minCenter :
                            maxCenter - minCenter;
                    // The tolerance of how far away a character can be from the line's center for it to be included
                    double tolerance = (int) Math.round(Math.max(Math.abs(centerDiff / 2 * 1.1), 2D));

                    int exactMin = (int) Math.round(imageLetter.getY() + minCenter);
                    int exactMax = (int) Math.round(imageLetter.getY() + maxCenter);

                    int exactTolerantMin = (int) Math.max(exactMin - tolerance, 0);
                    int exactTolerantMax = (int) (exactMax + tolerance);

                    int potentialY = (int) Math.round(imageLetter.getY() + centerDiff);

                    // Gets the nearest line and its Y value, if any
                    var possibleCenter = lines.keySet()
                            .stream()
                            .filter(centers -> {
                                int x1 = centers.getKey();
                                int y1 = centers.getValue();
                                int x2 = exactTolerantMin;
                                int y2 = exactTolerantMax;
                                return Math.max(y1, y2) - Math.min(x1, x2) < (y1 - x1) + (y2 - x2);
                            })
                            .min(Comparator.comparing(centers -> {
                                double min = centers.getKey();
                                double max = centers.getValue();
                                double centerBeginningY = ((max - min) / 2) + min;
                                return OCRUtils.getDiff(centerBeginningY, potentialY);
                            }));

                    var center = possibleCenter.orElseGet(() -> {
                        var pair = new IntPair(exactTolerantMin, exactTolerantMax); // Included tolerance
                        lines.put(pair, new LinkedList<>());
                        return pair;
                    });

                    double ratio = imageLetter.getDatabaseCharacter().getAvgWidth() / imageLetter.getDatabaseCharacter().getAvgHeight();
                    double diff = Math.max(ratio, imageLetter.getRatio()) - Math.min(ratio, imageLetter.getRatio());

                    // This is signaled when the difference of the ratios are a value that is probably incorrect.
                    // If the ratio is very different, it should be looked into, as it could be from faulty detection.
                    if (diff > 0.2D) {
                        error("Questionable ratio diff of " + diff + " on letter: " + imageLetter.getLetter() + " at (" + imageLetter.getX() + ", " + imageLetter.getY() + ")");
                    }

                    lines.get(center).add(imageLetter);
                });

        // End ordering
        var sortedLines = new Int2ObjectLinkedOpenHashMap<List<ImageLetter>>();

        // Sorts the characters again based on their X value in their respective lines. This must be done again because
        // the two different lists (firstList and secondList) will have caused a mixup of X positions from normal
        // characters, and the ones in secondList

        lines.keySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry, (int) Math.round(((double) entry.getValue() - (double) entry.getKey()) / 2D + entry.getKey())))
                .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
                .forEach(nestedEntry -> {
                    var linesEntry = nestedEntry.getKey();
                    int y = nestedEntry.getValue();

                    List<ImageLetter> databaseCharacters = lines.get(linesEntry);

                    if (databaseCharacters.isEmpty()) {
                        return;
                    }

                    databaseCharacters.sort(Comparator.comparingInt(ImageLetter::getX));
                    sortedLines.put(y, databaseCharacters);
                });

        var apostropheRatio = databaseManager.getAveragedData("apostropheRatio").get();

        // Combine " characters that are next to each other which would equate to a full " instead of the ' parts
        sortedLines.forEach((y, line) -> {
            final ImageLetter[] last = {null};
            line.removeIf(imageLetter -> {
                if (last[0] == null) {
                    last[0] = imageLetter;
                    return false;
                }

                // TODO: This logic with the apostropheRatio seems a bit sketchy... it may need to be looked at/tested
                var avgLength = (double) last[0].getHeight() / apostropheRatio;
                if (imageLetter.getX() - last[0].getX() <= avgLength) {
                    // If the ' (Represented as ") are close enough to each other, they are put into a single " and the second (current) character is removed
                    last[0].merge(imageLetter);
                    return true;
                }

                return false;
            });
        });

        // Inserts all the spaces in the line. This is based on the first character of the line's height, and will be
        // derived from that font size.
        sortedLines.values().forEach(line -> line.addAll(getSpacesFor(line, line.stream().mapToInt(ImageLetter::getHeight).max().getAsInt())));

        // Sorts the lines again based on X values, to move spaces from the back to their proper locations in the line.

        ScannedImage scannedImage = new ScannedImage(file, input);

        sortedLines.keySet().stream().sorted().forEach(y -> {
            List<ImageLetter> line = sortedLines.get(y);
            scannedImage.addLine(y, line.stream().sorted(Comparator.comparingInt(ImageLetter::getX)).collect(Collectors.toList()));
        });

        debug("Finished in " + (System.currentTimeMillis() - start) + "ms");
        return scannedImage;
    }

    private boolean[][] clone2DArray(boolean[][] input) {
        boolean[][] clone = new boolean[input.length][input[0].length];

        for (int y = 0; y < input.length; y++) {
            clone[y] = Arrays.copyOf(input[y], input[y].length);
        }

        return clone;
    }

    /**
     * Scans the input image and creates training data based off of it. It must be an input image created from
     * {@link TrainGenerator} or something of a similar format.
     *
     * @param file The input image to be trained from
     */
    public void trainImage(File file) throws ExecutionException, InterruptedException {

        // First clear the database
        var clearStart = System.currentTimeMillis();
        databaseManager.clearData();
        System.out.println("Cleared in " + (System.currentTimeMillis() - clearStart) + "ms");

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

        getLetters(searchImage, searchCharacters);

        TrainedCharacterData spaceTrainedCharacter = new TrainedCharacterData(' ');
        trainedCharacterDataList.add(spaceTrainedCharacter);

        Collections.sort(searchCharacters);

        // Pair<topY, bottomY> (Absolute coordinates)
        // Gets the top and bottom line bounds of every line
        var lineBounds = getLineBoundsForTesting(values);

        // Stores the height/distance ratio for apostrophe parts
        var apostropheRatios = new ArrayList<Double>();

        var searchCharactersCopy = new ArrayList<>(searchCharacters);

        // Goes through each line found
        for (var lineBound : lineBounds) {
            int lineHeight = lineBound.getValue() - lineBound.getKey();

            // Gets all characters found at the line bounds from the searchCharacters (Collected from the double for loops)
            var line = OCRUtils.findCharactersAtLine(lineBound.getKey(), lineBound.getValue(), searchCharacters);

            if (!line.isEmpty()) {
                var letterIndex = 0;
                var beforeSpaceX = new AtomicInteger();
                SearchCharacter firstQuote = null;

                for (SearchCharacter searchCharacter : line) {
                    // Gets the next character it knows it will be
                    char current = searchCharacter.getKnownChar() == ' ' ? ' ' : trainString.charAt(letterIndex++);
                    var modifier = 0;
                    var revertIndex = false;

                    // If the index is on the quote
                    if (letterIndex == 2) {
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
                        // of the space (X + width), and go up another character (Skipping the space in trainString)
                    } else if (letterIndex == trainString.length() - 2) {
                        beforeSpaceX.set(searchCharacter.getX() + searchCharacter.getWidth());
                        letterIndex++;
                        continue;

                        // If it's the last character, add the space based on beforeSpaceX and the current X, (Getting the
                        // width of the space) and reset the line
                    } else if (letterIndex == trainString.length()) {
                        spaceTrainedCharacter.recalculateTo(searchCharacter.getX() - beforeSpaceX.get(), lineHeight);
                        letterIndex = 0;
                        continue;
                    } else {
                        searchCharacter.setKnownChar(current);
                    }

                    int finalModifier = modifier;
                    var trainedSearchCharacter = trainedCharacterDataList
                            .parallelStream()
                            .filter(trainedCharacterData -> trainedCharacterData.getValue() == current
                                    && trainedCharacterData.getModifier() == finalModifier)
                            .findFirst()
                            .orElseGet(() -> {
                                var trained = new TrainedCharacterData(current, finalModifier);
                                trainedCharacterDataList.add(trained);
                                return trained;
                            });

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
                    if (letterIndex >= trainString.length()) {
                        letterIndex = 0;
                    }
                }

                // Removes any used letters from the line in searchCharacters, so none will be duplicated and to
                // increase performance.
                searchCharacters.removeAll(line);
            }
        }

        searchCharacters = searchCharactersCopy;

        debug(searchCharacters.size() + " characters found");

        debug("Writing data to database...");
        long start = System.currentTimeMillis();

        debug("trainedCharacterDataList = " + trainedCharacterDataList);

        // Add the apostropheRatios data into the database
        CompletableFuture.runAsync(() -> databaseManager.addAveragedData("apostropheRatio", apostropheRatios.stream().mapToDouble(Double::doubleValue).toArray()));

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

        debug("Finished writing to database in " + (System.currentTimeMillis() - start) + "ms");
    }

    private SearchCharacter combineCharacters(SearchCharacter character1, SearchCharacter character2) {
        return new SearchCharacter(Stream.of(character1.getCoordinates(), character2.getCoordinates()).flatMap(List::stream).collect(Collectors.toList()));
    }

    /**
     * Gets and inserts all the spaces of the current line based on the font size given (The first character of the line
     * by default). This method adds the spaces to the end of the line currently, so a resort is needed.
     *
     * @param line     The line to add spaces to
     * @param fontSize The font size to base the space widths off of
     * @return A copy of the input {@link ImageLetter} List, but with spaces appended to the end
     */
    private List<ImageLetter> getSpacesFor(List<ImageLetter> line, int fontSize) {
        var ret = new ArrayList<ImageLetter>();

        try {
            var data = databaseManager.getAllCharacterSegments().get();

            // Gets the space DatabaseCharacter used for the current font size from the database
            var spaceOptional = data.stream().filter(databaseCharacter -> databaseCharacter.getLetter() == ' ').findFirst();

            if (spaceOptional.isEmpty()) {
                error("No space found for current font size: " + fontSize);
                return line;
            }

            var space = spaceOptional.get();

            ImageLetter prev = null;

            for (var searchCharacter : line) {
                int leftX = prev == null ? 0 : prev.getX() + prev.getWidth() + 1;
                int rightX = searchCharacter.getX();

                var gap = rightX - leftX; // The space between the current character and the last character
                var ratio = space.getAvgWidth() / space.getAvgHeight(); // The ratio of the space DatabaseCharacter
                var usedWidth = ratio * fontSize; // The width of the space for this specific fot size

                var noRoundDownSpace = "!"; // Might be more in the future, that's why it's not testing equality of an inline string

                int spaces = noRoundDownSpace.contains(searchCharacter.getLetter() + "") ? (int) Math.floor(gap / usedWidth) : spaceRound(gap / usedWidth);

                for (int i = 0; i < spaces; i++) {
                    ret.add(new ImageLetter(space, (int) (leftX + (usedWidth * i)), searchCharacter.getY(), (int) usedWidth, fontSize, ratio));
                }

                prev = searchCharacter;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * Gets the full space character count for the blank gap divided by the space width. This is calculated by getting
     * the amount of times the space can fit in evenly (x % 1) and if the remaining value is within 0.2 of 1, it is
     * considered a space.
     *
     * @param input The amount of spaces that fit in the gap (gap / spaceWidth)
     * @return The amount of spaces that is found as a whole number
     */
    private int spaceRound(double input) {
        int known = (int) Math.floor(input);
        double extra = input % 1;
        known += OCRUtils.checkDifference(extra, 1, 0.2D) ? 1 : 0;
        return known;
    }

    private void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters) throws ExecutionException, InterruptedException {
        var histogram = new Histogram(searchImage);
        for (var coords : histogram.getWholeLines()) {
            var fromY = coords.getKey();
            var toY = coords.getValue();
            if (diff(fromY, toY) <= 3) continue;

            var sub = searchImage.getSubimage(0, fromY, searchImage.getWidth(), toY - fromY);

            var subHistogram = new Histogram(sub);

            for (var columnCoords : subHistogram.getWholeColumns()) {
                var fromX = columnCoords.getKey();
                var toX = columnCoords.getValue();
                if (diff(fromX, toX) <= 2) continue; // Don't recognize blobs with a width of <= 2

                var charSub = searchImage.getSubimage(fromX, fromY, toX - fromX, toY - fromY);
                var charHistogram = new Histogram(charSub);

                var padding = charHistogram.getVerticalPadding();
                var newHeight = charSub.getHeight() - padding.getKey() - padding.getValue();
                if (newHeight <= 2) continue; // Don't recognize blobs with a height of <= 2
                charSub = charSub.getSubimage(0, padding.getKey(), charSub.getWidth(), newHeight);

                var coordinates = new ArrayList<IntPair>();

                var values = charSub.getValues();
                for (int y = 0; y < values.length; y++) {
                    for (int x = 0; x < values[0].length; x++) {
                        var val = values[y][x];
                        if (val) coordinates.add(new IntPair(fromX + x, fromY + y));
                    }
                }

                var searchCharacter = new SearchCharacter(coordinates);
                searchCharacter.applySections();
                searchCharacter.analyzeSlices();
                searchCharacters.add(searchCharacter);
            }
        }
    }

    private int diff(int one, int two) {
        return Math.abs(one - two);
    }

    private int getEstimatedLineHeight(SearchImage image) {
        var list = new ArrayList<Integer>();
        var width = image.getWidth();
        var emptyCounter = 0;
        var topBuffer = true;
        for (int y = 0; y < image.getHeight(); y++) {
            int finalY = y;
            var empty = IntStream.range(0, width).allMatch(x -> image.getValue(x, finalY));
            if (empty) {
                if (topBuffer) {
                    topBuffer = false;
                    continue;
                }

                if (emptyCounter > 0) {
                    list.add(emptyCounter);
                    emptyCounter = 0;
                }
            } else if (!topBuffer) {
                emptyCounter++;
            }
        }

        if (emptyCounter > 0) list.add(emptyCounter);

        return (int) Math.round(list.stream().mapToInt(t -> t).average().orElse(10));
    }

    private int countFrequency(SearchImage input, int y) {
        return (int) IntStream.range(0, input.getWidth())
                .filter(x -> input.getValue(x, y))
                .count();
    }

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    private Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        Object2DoubleMap<ImageLetter> diffs = new Object2DoubleOpenHashMap<>(); // The lower value the better

        try {
            // All the possible DatabaseCharacters that `searchCharacter` can be from the database
            List<DatabaseCharacter> data = new ArrayList<>(databaseManager.getAllCharacterSegments().get());

            data.stream()
                    // TODO: Implement these?
//                    .filter(character -> character.hasDot() == searchCharacter.hasDot())
//                    .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
                    .forEach(character ->
                            OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData()).ifPresent(charDifference ->
                                    Arrays.stream(charDifference).average().ifPresent(value -> {
                                        // Gets the difference of the database character and searchCharacter (Lower is better)
                                        diffs.put(new ImageLetter(character, searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getSegments()), value);
                                    })));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // TODO: The following code can definitely be improved
        var entries = diffs.object2DoubleEntrySet().stream().sorted(Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue)).collect(Collectors.toList());

        // If there's no characters found, don't continue
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        var firstEntry = entries.get(0); // The most similar character

        double allowedDouble = firstEntry.getDoubleValue() * 0.1D;

        entries.removeIf(value -> OCRUtils.getDiff(value.getDoubleValue(), firstEntry.getDoubleValue()) > allowedDouble); // Removes any character without the

        // same difference (Most often a similarity of 0)
        double searchRatio = (double) searchCharacter.getWidth() / (double) searchCharacter.getHeight();

        // Sorts the equally matching characters by their width to height ratios, the first being most similar
        entries.sort(Comparator.comparingDouble(entry -> OCRUtils.getDiff(searchRatio, entry.getKey().getDatabaseCharacter().getAvgWidth() / entry.getKey().getDatabaseCharacter().getAvgHeight())));

        ImageLetter first = entries.get(0).getKey();
        first.setValues(searchCharacter.getValues());
        return Optional.of(first);
    }

    /**
     * Gets the top and bottom line bounds found from the value 2D array. This is used for getting characters for
     * training data.
     *
     * @param values The 2D array of values derived from the image to check from
     * @return A list of the absolute top and bottom line values
     */
    public static List<IntPair> getLineBoundsForTesting(boolean[][] values) {
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
                    } else {
                        lines.add(new IntPair(y - height, y));
                        height = 0;
                    }
                } else {
                    lines.add(new IntPair(y - height, y));
                    height = 0;
                }
            } else {
                if (height == 0) continue;
                lines.add(new IntPair(y - height, y));
                height = 0;
            }
        }

        return lines;
    }

    /**
     * Prints out an error message if the System property `newocr.error` is `true`.
     *
     * @param string The error to potentially print out
     */
    private void error(String string) {
        if (Boolean.getBoolean("newocr.error")) {
            System.err.println(string);
        }
    }

    /**
     * Prints out a debug message if the System property `newocr.debug` is `true`.
     *
     * @param string The string to potentially print out
     */
    private void debug(String string) {
        if (Boolean.getBoolean("newocr.debug")) System.out.println(string);
    }

}
