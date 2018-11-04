package com.uddernetworks.newocr;

import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.uddernetworks.newocr.CharacterGettingUtils.*;

public class OCRHandle {

    public static final FontBounds[] FONT_BOUNDS = {
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

    public ScannedImage scanImage(File file) throws IOException {
        long start = System.currentTimeMillis();

        BufferedImage input = ImageIO.read(file);
        boolean[][] values = OCRUtils.createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rewriteImage(temp, input);
        input = temp;

        filter(input);
        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                if (getLetterFrom(searchImage, input, x, y, coordinates, searchCharacters)) continue;
            }
        }

        Map<FontBounds, List<SearchCharacter>> searchLines = new HashMap<>();

        searchCharacters.forEach(searchCharacter -> {
            FontBounds bounds = matchNearestFontSize(searchCharacter.getHeight());
            searchLines.putIfAbsent(bounds, new ArrayList<>());
            searchLines.get(bounds).add(searchCharacter);
        });

        searchLines.keySet().parallelStream().forEach(fontBounds -> {
            try {
                databaseManager.getAllCharacterSegments(fontBounds).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        Map<Integer, List<ImageLetter>> lines = new LinkedHashMap<>();

        List<ImageLetter> firstList = new LinkedList<>();
        List<ImageLetter> secondList = new LinkedList<>();

        searchLines.values()
                .stream()
                .flatMap(List::stream)
                .parallel()
                .map(this::getCharacterFor)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ImageLetter::getX))
                .forEachOrdered(imageLetter -> {
                    if (       imageLetter.getLetter() == ','
                            || imageLetter.getLetter() == '.'
                            || imageLetter.getLetter() == '_'
                            || imageLetter.getLetter() == '`'
                            || imageLetter.getLetter() == '\''
                            || imageLetter.getLetter() == '"'
                            || imageLetter.getLetter() == '*'
                    ) {
                        secondList.add(imageLetter);
                    } else {
                        firstList.add(imageLetter);
                    }
                });

        Arrays.asList(firstList, secondList)
                .forEach(list -> {
                    list.forEach(imageLetter -> {
                        double maxCenter = imageLetter.getDatabaseCharacter().getMaxCenter();
                        double minCenter = imageLetter.getDatabaseCharacter().getMinCenter();
                        boolean subtract = maxCenter < 0 && imageLetter.getDatabaseCharacter().getMinCenter() < 0;
                        double centerDiff = subtract ?
                                maxCenter + minCenter :
                                maxCenter - minCenter;
                        centerDiff /= 2;
                        double threashold = Math.max(Math.abs(centerDiff * 1.1), 2D);


                        int potentialY = (int) (imageLetter.getY() + centerDiff + (minCenter > 0 ? minCenter : 0));

                        Optional<Integer> tempp = lines.keySet()
                                .stream()
                                .filter(y -> OCRUtils.isWithin(y, potentialY, threashold))
                                .sorted(Comparator.comparing(y -> getDiff(y, potentialY)))
                                .findFirst();

                        int center = tempp.orElseGet(() -> {
                            lines.put(potentialY, new LinkedList<>());

                            return potentialY;
                        });

                        double ratio = imageLetter.getDatabaseCharacter().getAvgWidth() / imageLetter.getDatabaseCharacter().getAvgHeight();

                        double diff = Math.max(ratio, imageLetter.getRatio()) - Math.min(ratio, imageLetter.getRatio());
                        if (diff > 0.2D) {
                            System.err.println("Questionable ratio diff of " + diff + " on letter: " + imageLetter.getLetter() + " at (" + imageLetter.getX() + ", " + imageLetter.getY() + ")");
                        }

                        lines.get(center).add(imageLetter);
                    });
                });

        // End ordering

        Map<Integer, List<ImageLetter>> sortedLines = new LinkedHashMap<>();

        lines.keySet()
                .stream()
                .sorted()
                .forEach(y -> {
                    List<ImageLetter> databaseCharacters = lines.get(y);
                    if (databaseCharacters.isEmpty()) return;
                    databaseCharacters.sort(Comparator.comparingInt(ImageLetter::getX));
                    sortedLines.put(y, databaseCharacters);
                });

        sortedLines.values().forEach(line -> line.addAll(getSpacesFor(line, line.stream().mapToInt(ImageLetter::getHeight).max().getAsInt())));

        lines.clear();
        sortedLines.keySet().stream().sorted().forEach(y -> {
            List<ImageLetter> line = sortedLines.get(y);
            lines.put(y, line.stream().sorted(Comparator.comparingInt(ImageLetter::getX)).collect(Collectors.toList()));
        });

        System.out.println("Finished in " + (System.currentTimeMillis() - start) + "ms");

        ScannedImage scannedImage = new ScannedImage();

        lines.values().forEach(scannedImage::addLine);

        return scannedImage;
    }

    public void trainImage(File file) throws IOException {
        Map<FontBounds, List<TrainedCharacterData>> trainedCharacterDataList = new HashMap<>();
        Arrays.stream(FONT_BOUNDS).forEach(fontBounds -> trainedCharacterDataList.put(fontBounds, new ArrayList<>()));
        BufferedImage input = ImageIO.read(file);
        boolean[][] values = OCRUtils.createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rewriteImage(temp, input);
        input = temp;

        filter(input);
        OCRUtils.toGrid(input, values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        boolean[][] valuesClone = new boolean[values.length][values[0].length];
        for (int y = 0; y < values.length; y++) {
            boolean[] row = new boolean[values[y].length];
            if (values[y].length >= 0) System.arraycopy(values[y], 0, row, 0, values[y].length);

            valuesClone[y] = row;
        }

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                getLetterFrom(searchImage, input, x, y, coordinates, searchCharacters);
            }
        }

        trainedCharacterDataList.values().forEach(dataList -> {
            IntStream.range('!', '~' + 1).forEach(letter -> dataList.add(new TrainedCharacterData((char) letter)));
            dataList.add(new TrainedCharacterData(' '));
        });

        Collections.sort(searchCharacters);

        // topY, bottomY
        List<Pair<Integer, Integer>> lineBounds = getLineBoundsForTesting(valuesClone);

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);

        for (Pair<Integer, Integer> lineBound : lineBounds) {
            int lineHeight = lineBound.getValue() - lineBound.getKey();
            List<SearchCharacter> line = findCharacterAtLine(lineBound.getKey(), lineBound.getValue(), searchCharacters);

            if (!line.isEmpty()) {

                AtomicInteger letterIndex = new AtomicInteger(0);
                AtomicInteger beforeSpaceX = new AtomicInteger();

                line.forEach(searchCharacter -> {
                    char current = searchCharacter.getKnownChar() == ' ' ? ' ' : trainString.charAt(letterIndex.getAndIncrement());

                    FontBounds currentFontBounds = trainedCharacterDataList.keySet()
                            .stream()
                            .filter(fontBounds ->
                                    fontBounds.isInbetween(searchCharacter.getHeight()))
                            .findFirst()
                            .orElse(null);

                    TrainedCharacterData trainedSearchCharacter = trainedCharacterDataList.get(currentFontBounds)
                            .stream()
                            .filter(trainedCharacterData -> trainedCharacterData.getValue() == current)
                            .findFirst()
                            .orElse(null);

                    TrainedCharacterData spaceTrainedCharacter = trainedCharacterDataList.get(currentFontBounds)
                            .stream()
                            .filter(trainedCharacterData -> trainedCharacterData.getValue() == ' ')
                            .findFirst()
                            .orElse(null);

                    if (letterIndex.get() == trainString.length() - 2) {
                        beforeSpaceX.set(searchCharacter.getX() + searchCharacter.getWidth());
                        letterIndex.incrementAndGet();
                        return;
                    } else if (letterIndex.get() == trainString.length()) {
                        spaceTrainedCharacter.recalculateTo(searchCharacter.getX() - beforeSpaceX.get(), lineHeight);
                        letterIndex.set(0);
                        return;
                    } else {
                        searchCharacter.setKnownChar(current);
                    }

                    trainedSearchCharacter.recalculateTo(searchCharacter);

                    double halfOfLineHeight = ((double) lineBound.getValue() - (double) lineBound.getKey()) / 2;
                    double middleToTopChar = (double) searchCharacter.getY() - (double) lineBound.getKey();
                    double topOfLetterToCenter = halfOfLineHeight - middleToTopChar;

                    trainedSearchCharacter.recalculateCenter(topOfLetterToCenter); // This NOW gets offset from top of
                    trainedSearchCharacter.setHasDot(searchCharacter.hasDot());
                    trainedSearchCharacter.setLetterMeta(searchCharacter.getLetterMeta());

                    if (letterIndex.get() >= trainString.length()) {
                        letterIndex.set(0);
                    }
                });

                searchCharacters.removeAll(line);
            }
        }

        searchCharacters = searchCharactersCopy;

        System.out.println(searchCharacters.size() + " characters found");

        System.out.println("Writing data to database...");
        long start = System.currentTimeMillis();

        System.out.println("trainedCharacterDataList = " + trainedCharacterDataList);

        trainedCharacterDataList
                .forEach((fontBounds, databaseTrainedCharacters) -> databaseTrainedCharacters
                        .parallelStream().forEach(databaseTrainedCharacter -> {
                            try {
                                if (databaseTrainedCharacter.isEmpty()) return;
                                databaseTrainedCharacter.finishRecalculations();
                                char letter = databaseTrainedCharacter.getValue();

                                Future databaseFuture = databaseManager.clearLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont());
                                while (!databaseFuture.isDone()) {
                                }

                                databaseFuture = databaseManager.createLetterEntry(letter, databaseTrainedCharacter.getWidthAverage(), databaseTrainedCharacter.getHeightAverage(), fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getMinCenter(), databaseTrainedCharacter.getMaxCenter(), databaseTrainedCharacter.hasDot(), databaseTrainedCharacter.getLetterMeta(), letter == ' ');
                                while (!databaseFuture.isDone()) {
                                }

                                if (letter != ' ') {
                                    databaseFuture = databaseManager.addLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getSegmentPercentages());
                                    while (!databaseFuture.isDone()) {
                                    }
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }));

        System.out.println("Finished writing to database in " + (System.currentTimeMillis() - start) + "ms");
    }

    private List<ImageLetter> getSpacesFor(List<ImageLetter> line, int fontSize) {
        List<ImageLetter> ret = new ArrayList<>();
        try {
            FontBounds fontBounds = matchNearestFontSize(fontSize);
            List<DatabaseCharacter> data = databaseManager.getAllCharacterSegments(fontBounds).get();
            DatabaseCharacter space = data.stream().filter(databaseCharacter -> databaseCharacter.getLetter() == ' ').findFirst().orElse(null);
            if (space == null) {
                System.err.println("No space found for current font size: " + fontSize);
                return line;
            }

            ImageLetter prev = null;
            for (ImageLetter searchCharacter : line) {
                int leftX = prev == null ? 0 : prev.getX() + prev.getWidth() + 1;
                int rightX = searchCharacter.getX();
                double gap = rightX - leftX;

                double ratio = space.getAvgWidth() / space.getAvgHeight();
                double usedWidth = ratio * fontSize;

                String noRoundDownSpace = "!"; // Might be more in the future, that's why it's not testing equality of an inline string
                int spaces = noRoundDownSpace.contains(searchCharacter.getLetter() + "") ? (int) Math.floor(gap / usedWidth) : spaceRound(gap / usedWidth);

                for (int i = 0; i < spaces; i++) {
                    ret.add(new ImageLetter(space, (int) (leftX + (usedWidth * i)), searchCharacter.getY(), (int) usedWidth, fontSize, ratio, null));
                }

                prev = searchCharacter;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private int spaceRound(double input) {
        int known = (int) Math.floor(input);
        double extra = input % 1;

        known += OCRUtils.checkDifference(extra, 1, 0.2D) ? 1 : 0;

        return known;
    }

    private double getDiff(double one, double two) {
        return Math.max(one, two) - Math.min(one, two);
    }

    private void scanFrom(BufferedImage image, int x, int y, Color color, List<Map.Entry<Integer, Integer>> coordinates) {
        if (image.getRGB(x, y) == color.getRGB()) {
            image.setRGB(x, y, Color.WHITE.getRGB());
            coordinates.add(new AbstractMap.SimpleEntry<>(x, y));

            scanFrom(image, x, y + 1, color, coordinates);
            scanFrom(image, x, y - 1, color, coordinates);
            scanFrom(image, x + 1, y, color, coordinates);
            scanFrom(image, x - 1, y, color, coordinates);
            scanFrom(image, x + 1, y + 1, color, coordinates);
            scanFrom(image, x + 1, y - 1, color, coordinates);
            scanFrom(image, x - 1, y + 1, color, coordinates);
            scanFrom(image, x - 1, y - 1, color, coordinates);
        }
    }

    private boolean getLetterFrom(SearchImage searchImage, BufferedImage binaryImage, int x, int y, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        SearchCharacter searchCharacter;

        if (binaryImage.getRGB(x, y) == Color.RED.getRGB()) {
            scanFrom(binaryImage, x, y, Color.RED, coordinates);

            searchCharacter = new SearchCharacter(coordinates);
            searchCharacter.setKnownChar(' ');
        } else {
            searchImage.scanFrom(x, y, coordinates);
            if (coordinates.size() == 0) return false;

            searchCharacter = new SearchCharacter(coordinates);

            if (doDotStuff(searchCharacter, coordinates, searchCharacters)) return true;
            if (doPercentStuff(searchCharacter, coordinates, searchCharacters)) return true;
            if (doApostropheStuff(searchCharacter, coordinates, searchCharacters)) return true;

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

            if (searchCharacter.isProbablyColon() && isAllBlack(searchCharacter) && !searchCharacter.hasDot()) {
                possibleDot = getBottomColon(searchCharacters, searchCharacter);
                if (possibleDot.isPresent()) {
                    combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.COLON, LetterMeta.EVEN_DOTS);
                    searchCharacters.remove(searchCharacter);
                    return true;
                }
            }

            if (searchCharacter.isProbablyApostraphe()) {
                SearchCharacter leftApostrophe = getLeftApostrophe(searchCharacters, searchCharacter).orElse(null);
                if (leftApostrophe != null) {
                    combine(leftApostrophe, searchCharacter, coordinates, CombineMethod.APOSTROPHE, LetterMeta.QUOTE);
                    leftApostrophe.setHasDot(true);
                    searchCharacter.setHasDot(true);
                    searchCharacters.remove(searchCharacter);
                    return true;
                }
            }
        }

        searchCharacter.applySections();
        searchCharacter.analyzeSlices();

        searchCharacters.add(searchCharacter);
        coordinates.clear();

        return false;
    }

    private FontBounds matchNearestFontSize(int fontSize) {
        return Arrays.stream(FONT_BOUNDS).filter(fontBounds -> fontBounds.isInbetween(fontSize)).findFirst().get();
    }

    private ImageLetter getCharacterFor(SearchCharacter searchCharacter) {
        Map<ImageLetter, Double> diffs = new HashMap<>(); // The lower value the better

        try {
            List<DatabaseCharacter> data = new ArrayList<>(databaseManager.getAllCharacterSegments(matchNearestFontSize(searchCharacter.getHeight())).get());

            data.stream()
                    .filter(character -> character.hasDot() == searchCharacter.hasDot())
                    .filter(character -> character.getLetterMeta() == searchCharacter.getLetterMeta())
                    .forEach(character -> {
                        double[] charDifference = OCRUtils.getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData());

                        double value = Arrays.stream(charDifference).average().getAsDouble();

                        ImageLetter imageLetter = new ImageLetter(character, searchCharacter.getX(), searchCharacter.getY(), searchCharacter.getWidth(), searchCharacter.getHeight(), ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()), searchCharacter.getSegments());


//                        DatabaseCharacter using = character.copy();
//                        using.setX(searchCharacter.getX());
//                        using.setY(searchCharacter.getY());
//                        using.setWidth(searchCharacter.getWidth());
//                        using.setHeight(searchCharacter.getHeight());
//                        using.setRatio(((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight()));

//                using.setCenterExact(searchCharacter.getY() + using.getCenter());

                        diffs.put(imageLetter, value);
                    });

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // TODO: The following code can definitely be improved

        List<Map.Entry<ImageLetter, Double>> entries = new ArrayList<>(OCRUtils.sortByValue(diffs).entrySet());

        if (entries.size() == 0) return null;
        Map.Entry<ImageLetter, Double> firstEntry = entries.get(0);
        entries.removeIf(value -> !value.getValue().equals(firstEntry.getValue()));

        double searchRatio = (double) searchCharacter.getWidth() / (double) searchCharacter.getHeight();

        entries.sort(Comparator.comparingDouble(entry -> getDiff(searchRatio, entry.getKey().getDatabaseCharacter().getAvgWidth() / entry.getKey().getDatabaseCharacter().getAvgHeight())));

        return entries.get(0).getKey();
    }

    private List<Pair<Integer, Integer>> getLineBoundsForTesting(boolean[][] values) {
        // topY, bottomY
        List<Pair<Integer, Integer>> lines = new ArrayList<>();

        int height = 0;
        for (int y = 0; y < values.length; y++) {
            if (isRowPopulated(values, y)) {
                height++;
            } else if (height > 0) {
                int heightUntil = 0;
                int finalSpace = -1;

                // Seeing if the gap under the character is <= the height of the above piece. This is mainly for seeing
                // if the dot on an 'i' is <= is above the rest of the character the same amount as its height (Making it a proper 'i' in Verdana
                for (int i = 0; i < height; i++) {
                    if (y + i >= values.length) {
                        finalSpace = 0;
                        break;
                    }

                    if (isRowPopulated(values, y + i)) {
                        if (finalSpace == -1) finalSpace = heightUntil;
                    } else {
                        heightUntil++;
                    }
                }

                if (finalSpace > 0) {
//                    System.out.println("Vertical separation was " + finalSpace + " yet height was " + height);
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

    private boolean isRowPopulated(boolean[][] values, int y) {
        for (int x = 0; x < values[y].length; x++) {
            if (values[y][x]) return true;
        }

        return false;
    }

    private TrainedCharacterData getTrainedData(char cha, List<TrainedCharacterData> trainedCharacterDataList) {
        return trainedCharacterDataList.stream().filter(characterData -> characterData.getValue() == cha).findFirst().get();
    }

    private List<SearchCharacter> findCharacterAtY(int y, List<SearchCharacter> searchCharacters) {
        return findCharacterAtY(y, searchCharacters, -1);
    }

    private List<SearchCharacter> findCharacterAtY(int y, List<SearchCharacter> searchCharacters, int addY) {
        Optional<SearchCharacter> optionalSearchCharacter = searchCharacters
                .stream()
                .filter(searchCharacter -> searchCharacter.isInYBounds(y))
                .findFirst();

        if (!optionalSearchCharacter.isPresent()) return new ArrayList<>();
        SearchCharacter betterYCharacter = optionalSearchCharacter.get();
        int betterY = addY == -1 ? betterYCharacter.getY() + betterYCharacter.getHeight() / 2 : addY;

        List<SearchCharacter> temp = searchCharacters
                .stream()
                .sorted()
                .filter(searchCharacter -> searchCharacter.isInYBounds(betterY))
                .collect(Collectors.toCollection(LinkedList::new));

        Optional<SearchCharacter> maxOneOptional = temp.stream().sorted((o1, o2) -> o2.getHeight() - o1.getHeight()).findFirst();
        if (maxOneOptional.isPresent()) {
            SearchCharacter maxOne = maxOneOptional.get();
            int otherBetterY = maxOne.getY() + maxOne.getHeight() / 2;
            temp = searchCharacters
                    .stream()
                    .sorted()
                    .filter(searchCharacter -> searchCharacter.isInYBounds(betterY) || searchCharacter.isInYBounds(otherBetterY))
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        return temp;
    }

    private List<SearchCharacter> findCharacterAtLine(int topY, int bottomY, List<SearchCharacter> searchCharacters) {
        return searchCharacters
                .stream()
                .sorted()
                .filter(searchCharacter -> OCRUtils.isWithin(topY, bottomY, searchCharacter.getY()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private void makeImage(boolean[][] values, String name) {
        try {
            BufferedImage image = new BufferedImage(values[0].length, values.length, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setRGB(x, y, (values[y][x] ? Color.BLACK : Color.WHITE).getRGB());
                }
            }

            ImageIO.write(image, "png", new File("E:\\NewOCR\\" + name + ".png"));
        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    private void rewriteImage(BufferedImage temp, BufferedImage input) {
        for (int y = 0; y < temp.getHeight(); y++) {
            for (int x = 0; x < temp.getWidth(); x++) {
                temp.setRGB(x, y, input.getRGB(x, y));
            }
        }
    }

    private boolean isAllBlack(SearchCharacter searchCharacter) {
        for (boolean[] row : searchCharacter.getValues()) {
            for (boolean bool : row) {
                if (!bool) return false;
            }
        }

        return true;
    }

    public void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    public void colorColumn(BufferedImage image, Color color, int x, int y, int height) {
        for (int y2 = 0; y2 < height; y2++) {
            image.setRGB(x, y + y2, color.getRGB());
        }
    }

    public void printOut(boolean[][] values) {
        for (boolean[] row : values) {
            for (boolean bool : row) {
                System.out.print(bool ? "＃" : "　");
            }

            System.out.println("");
        }
    }

    private void filter(BufferedImage bufferedImage) {
        int red = Color.RED.getRGB();
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                Color writeColor = isBlack(bufferedImage, x, y) ? new Color(0, 0, 0, 255) : new Color(255, 255, 255, 255);
                if (bufferedImage.getRGB(x, y) == red) {
                    writeColor = Color.RED;
                }

                bufferedImage.setRGB(x, y, writeColor.getRGB());
            }
        }
    }

    private boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 255 * 0.75;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
