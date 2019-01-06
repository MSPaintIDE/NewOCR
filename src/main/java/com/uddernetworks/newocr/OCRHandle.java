package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.train.TrainGenerator;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.OCRUtils;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.uddernetworks.newocr.utils.CharacterGettingUtils.*;

public class OCRHandle {

    private static final FontBounds[] FONT_BOUNDS = {
            new FontBounds(0, 12),
            new FontBounds(13, 20),
            new FontBounds(21, 30),
            new FontBounds(31, 100),
    };

    private static String trainString = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjiklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;

    public OCRHandle(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Scans the input image and returns a {@link ScannedImage} containing all the characters and their info.
     *
     * @param file The input image to be scanned
     * @return A {@link ScannedImage} containing all scanned character data
     * @throws IOException
     */
    public ScannedImage scanImage(File file) throws IOException {
        long start = System.currentTimeMillis();

        // Preparing image
        BufferedImage input = ImageIO.read(file);
        boolean[][] values = OCRUtils.createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        if (Boolean.getBoolean("newocr.rewrite")) {
            BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
            OCRUtils.rewriteImage(temp, input);
            input = temp;
        }

        OCRUtils.filter(input);
        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values);

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        // Goes through coordinates of image and adds any connecting pixels to `coordinates`

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                getLetterFrom(searchImage, x, y, coordinates, searchCharacters);
            }
        }

        Map<FontBounds, List<SearchCharacter>> searchLines = new HashMap<>();

        // Puts all found characters into font size groupings

        searchCharacters.forEach(searchCharacter -> {
            FontBounds bounds = matchNearestFontSize(searchCharacter.getHeight());
            searchLines.putIfAbsent(bounds, new ArrayList<>());
            searchLines.get(bounds).add(searchCharacter);
        });

        // Gets all needed character data from the database based on the currently used font sizes

        searchLines.keySet().parallelStream().forEach(fontBounds -> {
            try {
                databaseManager.getAllCharacterSegments(fontBounds).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        // Key = Entry<MinCenter, MaxCenter>  centers are ABSOLUTE
        Map<Map.Entry<Integer, Integer>, List<ImageLetter>> lines = new LinkedHashMap<>();

        // Gets the closest matching character (According to the database values) using OCRHandle#getCharacterFor(SearchCharacter),
        // then it orders them by their X values, and then sorts the ImageLetters so certain ones go first, allowing the
        // characters to go to the correct lines

        searchLines.values()
                .stream()
                .flatMap(List::stream)
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
                    Optional<Map.Entry<Integer, Integer>> tempp = lines.keySet()
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

                    Map.Entry<Integer, Integer> center = tempp.orElseGet(() -> {
                        Map.Entry<Integer, Integer> entry = new AbstractMap.SimpleEntry<>(exactTolerantMin, exactTolerantMax); // Included tolerance
                        lines.put(entry, new LinkedList<>());

                        return entry;
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

        Map<Integer, List<ImageLetter>> sortedLines = new LinkedHashMap<>();

        // Sorts the characters again based on their X value in their respective lines. This must be done again because
        // the two different lists (firstList and secondList) will have caused a mixup of X positions from normal
        // characters, and the ones in secondList

        lines.keySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry, (int) Math.round(((double) entry.getValue() - (double) entry.getKey()) / 2D + entry.getKey())))
                .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
                .forEach(nestedEntry -> {
                    Map.Entry<Integer, Integer> linesEntry = nestedEntry.getKey();
                    int y = nestedEntry.getValue();

                    List<ImageLetter> databaseCharacters = lines.get(linesEntry);
                    if (databaseCharacters.isEmpty()) return;
                    databaseCharacters.sort(Comparator.comparingInt(ImageLetter::getX));
                    sortedLines.put(y, databaseCharacters);
                });

        // Inserts all the spaces in the line. This is based on the first character of the line's height, and will be
        // derived from that font size.
        sortedLines.values().forEach(line -> line.addAll(getSpacesFor(line, line.stream().mapToInt(ImageLetter::getHeight).max().getAsInt())));

        // Sorts the lines again based on X values, to move spaces from the back to their proper locations in the line.

        ScannedImage scannedImage = new ScannedImage();

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
            boolean[] row = new boolean[input[y].length];
            if (input[y].length >= 0) System.arraycopy(input[y], 0, row, 0, input[y].length);

            clone[y] = row;
        }

        return clone;
    }

    /**
     * Scans the input image and creates training data based off of it. It must be an input image created from
     * {@link TrainGenerator} or something of a similar format.
     *
     * @param file The input image to be trained from
     * @throws IOException
     */
    public void trainImage(File file) throws IOException {
        Map<FontBounds, List<TrainedCharacterData>> trainedCharacterDataList = new HashMap<>();
        Arrays.stream(FONT_BOUNDS).forEach(fontBounds -> trainedCharacterDataList.put(fontBounds, new ArrayList<>()));

        // Preparing image

        BufferedImage input = ImageIO.read(file);
        boolean[][] values = OCRUtils.createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        if (Boolean.getBoolean("newocr.rewrite")) {
            BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
            OCRUtils.rewriteImage(temp, input);
            input = temp;
        }

        OCRUtils.filter(input);
        OCRUtils.toGrid(input, values);

        boolean[][] valuesClone = clone2DArray(values);

        SearchImage searchImage = new SearchImage(values);

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        // Goes through coordinates of image and adds any connecting pixels to `coordinates`

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                getLetterFrom(searchImage, x, y, coordinates, searchCharacters);
            }
        }

        trainedCharacterDataList.values().forEach(dataList -> {
            IntStream.range('!', '~' + 1).forEach(letter -> dataList.add(new TrainedCharacterData((char) letter)));
            dataList.add(new TrainedCharacterData(' '));
        });

        Collections.sort(searchCharacters);

        // Pair<topY, bottomY> (Absolute coordinates)
        // Gets the top and bottom line bounds of every line
        List<Pair<Integer, Integer>> lineBounds = getLineBoundsForTesting(valuesClone);

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);

        int startingSize = 90;

        // Goes through each line found
        for (Pair<Integer, Integer> lineBound : lineBounds) {
            int lineHeight = lineBound.getValue() - lineBound.getKey();
            // Gets all characters found at the line bounds from the searchCharacters (Collected from the double for loops)
            List<SearchCharacter> line = OCRUtils.findCharactersAtLine(lineBound.getKey(), lineBound.getValue(), searchCharacters);

            if (!line.isEmpty()) {
                AtomicInteger letterIndex = new AtomicInteger(0);
                AtomicInteger beforeSpaceX = new AtomicInteger();

                line.forEach(searchCharacter -> {
                    // Gets the next character it knows it will be
                    char current = searchCharacter.getKnownChar() == ' ' ? ' ' : trainString.charAt(letterIndex.getAndIncrement());

                    // TODO: Improve and cache these following 3 variables
                    Optional<FontBounds> currentFontBoundsOptional = trainedCharacterDataList.keySet()
                            .stream()
                            .filter(fontBounds ->
                                    fontBounds.isInbetween(searchCharacter.getHeight()))
                            .findFirst();

                    if (currentFontBoundsOptional.isPresent()) return;

                    Optional<TrainedCharacterData> trainedSearchCharacterOptional = trainedCharacterDataList.get(currentFontBoundsOptional.get())
                            .stream()
                            .filter(trainedCharacterData -> trainedCharacterData.getValue() == current)
                            .findFirst();

                    Optional<TrainedCharacterData> spaceTrainedCharacterOptional = trainedCharacterDataList.get(currentFontBoundsOptional.get())
                            .stream()
                            .filter(trainedCharacterData -> trainedCharacterData.getValue() == ' ')
                            .findFirst();

                    // If the current character is the FIRST `W`, sets beforeSpaceX to the current far right coordinate
                    // of the space (X + width), and go up another character (Skipping the space in trainString)
                    if (letterIndex.get() == trainString.length() - 2) {
                        beforeSpaceX.set(searchCharacter.getX() + searchCharacter.getWidth());
                        letterIndex.incrementAndGet();
                        return;

                        // If it's the last character, add the space based on beforeSpaceX and the current X, (Getting the
                        // width of the space) and reset the line
                    } else if (letterIndex.get() == trainString.length()) {
                        spaceTrainedCharacterOptional.ifPresent(trainedCharacterData -> trainedCharacterData.recalculateTo(searchCharacter.getX() - beforeSpaceX.get(), lineHeight));
                        letterIndex.set(0);
                        return;
                    } else {
                        searchCharacter.setKnownChar(current);
                    }

                    trainedSearchCharacterOptional.ifPresent(trainedSearchCharacter -> {
                        // Adds the current segment values of the current searchCharacter to the trainedSearchCharacter
                        trainedSearchCharacter.recalculateTo(searchCharacter);

                        double halfOfLineHeight = ((double) lineBound.getValue() - (double) lineBound.getKey()) / 2;
                        double middleToTopChar = (double) searchCharacter.getY() - (double) lineBound.getKey();
                        double topOfLetterToCenter = halfOfLineHeight - middleToTopChar;

                        // Sets the current center to be calculated, along with any meta it may have
                        trainedSearchCharacter.recalculateCenter(topOfLetterToCenter); // This NOW gets offset from top of
                        trainedSearchCharacter.setHasDot(searchCharacter.hasDot());
                        trainedSearchCharacter.setLetterMeta(searchCharacter.getLetterMeta());
                    });

                    // Resets the current letter
                    if (letterIndex.get() >= trainString.length()) {
                        letterIndex.set(0);
                    }
                });

                databaseManager.addLetterSize(startingSize--, line);

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

        // Inserts all character data into the database after recalculating the
        trainedCharacterDataList
                .forEach((fontBounds, databaseTrainedCharacters) -> databaseTrainedCharacters.forEach(databaseTrainedCharacter -> {
                    try {
                        if (databaseTrainedCharacter.isEmpty()) return;
                        databaseTrainedCharacter.finishRecalculations();
                        char letter = databaseTrainedCharacter.getValue();

                        CompletableFuture.runAsync(() -> databaseManager.clearLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont()))
                                .thenRunAsync(() -> databaseManager.createLetterEntry(letter, databaseTrainedCharacter.getWidthAverage(), databaseTrainedCharacter.getHeightAverage(), fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getMinCenter(), databaseTrainedCharacter.getMaxCenter(), databaseTrainedCharacter.hasDot(), databaseTrainedCharacter.getLetterMeta(), letter == ' '))
                                .thenRunAsync(() -> {
                                    if (letter != ' ') {
                                        databaseManager.addLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getSegmentPercentages());
                                    }
                                });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));

        debug("Finished writing to database in " + (System.currentTimeMillis() - start) + "ms");
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
        List<ImageLetter> ret = new ArrayList<>();
        try {
            FontBounds fontBounds = matchNearestFontSize(fontSize);
            List<DatabaseCharacter> data = databaseManager.getAllCharacterSegments(fontBounds).get();

            // Gets the space DatabaseCharacter used for the current font size from the database
            Optional<DatabaseCharacter> spaceOptional = data.stream().filter(databaseCharacter -> databaseCharacter.getLetter() == ' ').findFirst();

            if (!spaceOptional.isPresent()) {
                error("No space found for current font size: " + fontSize);
                return line;
            }

            DatabaseCharacter space = spaceOptional.get();

            ImageLetter prev = null;
            for (ImageLetter searchCharacter : line) {
                int leftX = prev == null ? 0 : prev.getX() + prev.getWidth() + 1;
                int rightX = searchCharacter.getX();
                double gap = rightX - leftX; // The space between the current character and the last character

                double ratio = space.getAvgWidth() / space.getAvgHeight(); // The ratio of the space DatabaseCharacter
                double usedWidth = ratio * fontSize; // The width of the space for this specific fot size

                String noRoundDownSpace = "!"; // Might be more in the future, that's why it's not testing equality of an inline string
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

    /**
     * Puts all touching black characters together and adds them to `coordinates`. This is the method where most incorrect
     * detections will result from.
     *
     * @param searchImage      The SearchImage to read from
     * @param x                The X coordinate to start at
     * @param y                The Y coordinate to start at
     * @param coordinates      The mutable list of coordinate values that will be added to when a new black pixel is found
     * @param searchCharacters The mutable list of SearchCharacters that will be added to when a group of pixels is found
     * @return If it count a group of pixels
     */
    public static boolean getLetterFrom(SearchImage searchImage, int x, int y, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        SearchCharacter searchCharacter;

        searchImage.scanFrom(x, y, coordinates);
        if (coordinates.size() == 0) return false;

        searchCharacter = new SearchCharacter(coordinates);

        // Gets any grouping of pixels with this character being added to, whereas the ones below will have pixels added
        // to it
        if (doDotStuff(searchCharacter, coordinates, searchCharacters)) return true;
        if (doPercentStuff(searchCharacter, coordinates, searchCharacters)) return true;
        if (doApostropheStuff(searchCharacter, coordinates, searchCharacters)) return true;

        // Adds groupings of pixels found where they are connected. An example of this is the letter i or j, things like
        // ! or %, etc.

        Optional<SearchCharacter> possibleDot = getBaseForPercent(searchCharacters, /* Is the circle > */ searchCharacter);
        if (possibleDot.isPresent()) {
            combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.PERCENTAGE_CIRCLE, LetterMeta.PERCENT);
            searchCharacters.remove(searchCharacter);
            return true;
        }

        if (searchCharacter.isProbablyDot() && !searchCharacter.hasDot()) {
            possibleDot = getBaseOfDot(searchCharacters, searchCharacter);
            if (possibleDot.isPresent()) {
                combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT, LetterMeta.DOT_ABOVE);
                searchCharacters.remove(searchCharacter);
                return true;
            }
        }

        // For ! or ?
        possibleDot = getDotUnderLetter(searchCharacters, searchCharacter);
        if (possibleDot.isPresent()) {
            combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT, LetterMeta.DOT_UNDER);
            searchCharacters.remove(searchCharacter);
            return true;
        }

        if (searchCharacter.isProbablyColon() && OCRUtils.isAllBlack(searchCharacter) && !searchCharacter.hasDot()) {
            possibleDot = getBottomColon(searchCharacters, searchCharacter);
            if (possibleDot.isPresent()) {
                combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.COLON, LetterMeta.EVEN_DOTS);
                searchCharacters.remove(searchCharacter);
                return true;
            }
        }

        if (searchCharacter.isProbablyApostraphe()) {
            Optional<SearchCharacter> leftApostropheOptional = getLeftApostrophe(searchCharacters, searchCharacter);
            leftApostropheOptional.ifPresent(leftApostrophe -> {
                combine(leftApostrophe, searchCharacter, coordinates, CombineMethod.APOSTROPHE, LetterMeta.QUOTE);
                leftApostrophe.setHasDot(true);
                searchCharacter.setHasDot(true);
                searchCharacters.remove(searchCharacter);
            });

            if (leftApostropheOptional.isPresent()) return true;
        }

        searchCharacter.applySections();
        searchCharacter.analyzeSlices();

        searchCharacters.add(searchCharacter);
        coordinates.clear();

        return false;
    }

    /**
     * Gets the estimated font size based on the given letter's character and dimensions from the stored values in the
     * database after training.
     *
     * @param imageLetter The {@link ImageLetter} to check the size of
     * @return The estimated font size
     */
    public Future<Integer> getFontSize(ImageLetter imageLetter) {
        return this.databaseManager.getLetterSize(imageLetter.getLetter(), imageLetter.getHeight());
    }

    /**
     * Gets the nearest {@link FontBounds} object for the exact font size (Height) given
     *
     * @param fontSize The exact font size (Height)
     * @return The nearest matching {@link FontBounds} object
     */
    private FontBounds matchNearestFontSize(int fontSize) {
        return Arrays.stream(FONT_BOUNDS).filter(fontBounds -> fontBounds.isInbetween(fontSize)).findFirst().get();
    }

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    private Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter) {
        Map<ImageLetter, Double> diffs = new HashMap<>(); // The lower value the better

        try {
            // All the possible DatabaseCharacters that `searchCharacter` can be from the database
            List<DatabaseCharacter> data = new ArrayList<>(databaseManager.getAllCharacterSegments(matchNearestFontSize(searchCharacter.getHeight())).get());

            data.stream()
                    .filter(character -> character.hasDot() == searchCharacter.hasDot())
                    .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
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

        List<Map.Entry<ImageLetter, Double>> entries = new ArrayList<>(OCRUtils.sortByValue(diffs).entrySet());

        // If there's no characters found, don't continue
        if (entries.size() == 0) return Optional.empty();
        Map.Entry<ImageLetter, Double> firstEntry = entries.get(0); // The most similar character
        double allowedDouble = firstEntry.getValue() * 0.1D;
        entries.removeIf(value -> OCRUtils.getDiff(value.getValue(), firstEntry.getValue()) > allowedDouble); // Removes any character without the
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
    public static List<Pair<Integer, Integer>> getLineBoundsForTesting(boolean[][] values) {
        // Pair<topY, bottomY>
        List<Pair<Integer, Integer>> lines = new ArrayList<>();

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
                        if (finalSpace == -1) finalSpace = heightUntil;
                    } else {
                        heightUntil++;
                    }
                }

                if (finalSpace > 0) {
                    if (height == finalSpace) {
                        y += finalSpace;
                        height += finalSpace;
                    } else {
                        lines.add(new Pair<>(y - height, y));
                        height = 0;
                    }
                } else {
                    lines.add(new Pair<>(y - height, y));
                    height = 0;
                }
            } else {
                if (height == 0) continue;
                lines.add(new Pair<>(y - height, y));
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
        if (Boolean.getBoolean("newocr.error")) System.err.println(string);
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
