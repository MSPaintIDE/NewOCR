package com.uddernetworks.newocr.altsearcher;

import com.uddernetworks.newocr.altsearcher.feature.TrainedCharacterData;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static final double AFFECT_BACKWARDS = 1D; // Originally 2
    public static final FontBounds[] FONT_BOUNDS = {
            new FontBounds(0, 12),
            new FontBounds(13, 20),
            new FontBounds(21, 30),
            new FontBounds(31, 100),
    };

    public static final boolean ALL_INPUTS_EQUAL = true;
    public static final boolean AVERAGE_DIFF = true; // true for average, false for max and min
    public static final boolean DRAW_PROBES = true;
    public static final boolean DRAW_FULL_PROBES = true;
    public static final String trainString = "!#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjiklmnopqrstuvwxyz{|}~";
    private static DatabaseManager databaseManager;

    private static Histogram first;
    private static int letterIndex = 0;
    //    private static char letter = 'a';
    //    private static Map<Character, SearchCharacter> searchCharacters = new HashMap<>();
    private static int trainWidth = 0;
    private static double[] segmentPercentages;

    private static int testIndex = 0;

    private static SortedMap<Double, Double> averageToBack = new TreeMap<>();

    private static DecimalFormat percent = new DecimalFormat(".##");

    private static BufferedImage testImageShit;

    public static void main(String[] args) throws IOException, InterruptedException { // alphabet48
        databaseManager = new DatabaseManager(args[0], args[1], args[2]);

        Scanner scanner = new Scanner(System.in);

        Arrays.asList("letters.sql", "sectionData.sql").parallelStream().forEach(table -> {
            try {
                URL url = Main.class.getClassLoader().getResource(table);
                String tables = new BufferedReader(new InputStreamReader(url.openStream())).lines().collect(Collectors.joining("\n"));
                try (Connection connection = databaseManager.getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement(tables)) {

                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        databaseManager.initializeStatements();

        System.out.println("Do you want to train? yes/no");

        String inputLine = scanner.nextLine();
        if (inputLine.equalsIgnoreCase("yes") || inputLine.equalsIgnoreCase("y") && false) {
            System.out.println("AFFECT_BACKWARDS = " + AFFECT_BACKWARDS);
            System.out.println("Generating features...");
            long start = System.currentTimeMillis();
            generateFeatures(new File("E:\\NewOCR\\training.png"));
            System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");
        }

        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\HW.png"));
        boolean[][] values = createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rewriteImage(temp, input);
        input = temp;

        filter(input);
        toGrid(input, values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                if (getLetterFrom(searchImage, x, y, coordinates, searchCharacters)) continue;
            }
        }

//        System.out.println("Got letters?");

        List<Double> percentages = new ArrayList<>();

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);
        List<DatabaseCharacter> found = searchCharacters.stream()
                .map(Main::getCharacterFor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Integer, List<DatabaseCharacter>> lines = new LinkedHashMap<>();

        System.out.println("lines = " + lines);

        found = found.stream()
                .sorted(Comparator.comparingInt(DatabaseCharacter::getX))
                .collect(Collectors.toList());

//        System.out.println("111");
//        found.forEach(databaseCharacter -> System.out.println(databaseCharacter.getLetter() + " = [" + databaseCharacter.getX()));
//        System.out.println("222");

        // Ordering

        found.stream()
                .sorted(Comparator.comparingInt(DatabaseCharacter::getX))
                .forEach(databaseCharacter -> {
                    double threashold = Math.max(databaseCharacter.getAvgHeight() / 2, 2D);
//                    System.out.println("threashold = " + threashold);

                    System.out.println("========================================== [" + databaseCharacter.getLetter() + "] Exact center: " + ((int) (databaseCharacter.getY() - databaseCharacter.getCenter())) + " (" + databaseCharacter.getY() + " - " + databaseCharacter.getCenter() + ")");
                    System.out.println(lines.keySet());

                    int center = lines.keySet()
                            .stream()
                            .filter(y -> isWithin(y, (int) (databaseCharacter.getY() - databaseCharacter.getCenter()), threashold)).findFirst().orElseGet(() -> {
//                                System.out.println("----------------------- Creating at: " + (databaseCharacter.getY() - databaseCharacter.getCenter()));
                                lines.put((int) (databaseCharacter.getY() - databaseCharacter.getCenter()), new LinkedList<>());
                                return (int) (databaseCharacter.getY() - databaseCharacter.getCenter());
                            });

//                    System.out.println("----------------------- center = " + center);
                    lines.get(center).add(databaseCharacter);
                });

        // End ordering

        System.out.println("lines = " + lines);

        lines.forEach((center, line) -> line.forEach(System.out::println));

        OptionalDouble averageOptional = percentages.stream().mapToDouble(t -> t).average();
        if (!averageOptional.isPresent()) {
            System.out.println("No average found");
            System.exit(0);
        }

        double average = averageOptional.getAsDouble();
        System.out.println("\nAverage: " + average);

        System.exit(0);
        averageToBack.put(average, AFFECT_BACKWARDS);

        searchCharacters = searchCharactersCopy;

        BufferedImage finalInput = input;
        searchCharacters.forEach(searchCharacter -> searchCharacter.drawTo(finalInput));

//            System.out.println(searchCharacters.size() + " characters found");


//            AFFECT_BACKWARDS += 0.3D;
//        }

        List<Map.Entry<Double, Double>> entries = new ArrayList<>(averageToBack.entrySet());
        Collections.reverse(entries);
//        System.out.println(averageToBack);
        System.out.println("\n\n====================");
        entries.forEach(entry -> {
            System.out.println(percent.format(entry.getKey() * 100) + "% \t\t| " + entry.getValue());
        });

//        ImageIO.write(temp, "png", new File("E:\\NewOCR\\tempout.png"));
    }

    static class FontBounds {
        private int minFont;
        private int maxFont;

        public FontBounds(int minFont, int maxFont) {
            this.minFont = minFont;
            this.maxFont = maxFont;
        }

        public int getMinFont() {
            return minFont;
        }

        public int getMaxFont() {
            return maxFont;
        }

        public boolean isInbetween(int font) {
            return minFont <= font && font <= maxFont;
        }
    }

    public static void generateFeatures(File file) throws IOException {
        Map<FontBounds, List<TrainedCharacterData>> trainedCharacterDataList = new HashMap<>();
        Arrays.stream(FONT_BOUNDS).forEach(fontBounds -> trainedCharacterDataList.put(fontBounds, new ArrayList<>()));
        BufferedImage input = ImageIO.read(file);
        boolean[][] values = createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        trainWidth = input.getWidth();

//        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
//        rewriteImage(temp, input);
//        input = temp;

        filter(input);
        toGrid(input, values);

        ImageIO.write(input, "png", new File("E:\\NewOCR\\binariazed.png"));

//        printOut(values);
        Main.testImageShit = input;

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        boolean[][] valuesClone = new boolean[values.length][values[0].length];
        for (int y = 0; y < values.length; y++) {
            boolean[] row = new boolean[values[y].length];
            if (values[y].length >= 0) System.arraycopy(values[y], 0, row, 0, values[y].length);

            valuesClone[y] = row;
        }

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        testIndex = 0;

//        List<Double> doubles = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                if (getLetterFrom(searchImage, x, y, coordinates, searchCharacters)) continue;
            }
        }

//        searchCharacters.forEach(searchCharacter -> searchCharacter.drawTo());
        System.out.println("CHARS: " + searchCharacters.size());

        trainedCharacterDataList.values().forEach(dataList -> IntStream.range('!', '~' + 1).forEach(letter -> dataList.add(new TrainedCharacterData((char) letter))));

//        int maxWidth = searchCharacters.stream().mapToInt(SearchCharacter::getHeight).max().getAsInt();

        searchCharacters.stream().sorted().forEach(searchCharacter -> searchCharacter.drawTo(input));
        Collections.sort(searchCharacters);

        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));


        System.out.println("searchCharacters = " + searchCharacters.size());

//        printOut(valuesClone);

        // topY, bottomY
        List<Pair<Integer, Integer>> lineBounds = getLineBoundsForTesting(valuesClone);
        System.out.println("lineBounds = " + lineBounds);

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);
        for (Pair<Integer, Integer> lineBound : lineBounds) {
            List<SearchCharacter> line = findCharacterAtLine(lineBound.getKey(), lineBound.getValue(), searchCharacters);

            System.out.println("line = " + line.size());

            if (!line.isEmpty()) {
                line.forEach(searchCharacter -> {
                    char current = trainString.charAt(letterIndex++);
                    if (letterIndex >= trainString.length()) letterIndex = 0;
                    searchCharacter.setKnownChar(current);

                    TrainedCharacterData trainedCharacterData = getTrainedData(current, trainedCharacterDataList.get(trainedCharacterDataList.keySet().stream().filter(fontBounds -> fontBounds.isInbetween(searchCharacter.getHeight())).findFirst().orElse(null)));
                    trainedCharacterData.setHasDot(searchCharacter.hasDot());
                    trainedCharacterData.recalculateTo(searchCharacter);
                    trainedCharacterData.recalculateCenter((double) searchCharacter.getY() - (double) lineBound.getKey());
//                    trainedCharacterData.recalculateCenter((((double) lineBound.getValue() - (double) lineBound.getKey()) / 2D) - ((double) lineBound.getKey() - (double) searchCharacter.getY()));
                });

                searchCharacters.removeAll(line);
            }
        }

        searchCharacters = searchCharactersCopy;

        System.out.println(searchCharacters.size() + " characters found");

        long start = System.currentTimeMillis();
        System.out.println("Writing output...");
        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));
        System.out.println("Wrote output in " + (System.currentTimeMillis() - start) + "ms");

        System.out.println("Writing data to database...");
        start = System.currentTimeMillis();

//        trainedCharacterDataList.values().stream().flatMap(List::stream).forEach(TrainedCharacterData::finishRecalculations);

        System.out.println("trainedCharacterDataList = " + trainedCharacterDataList);

        trainedCharacterDataList.forEach((fontBounds, databaseTrainedCharacters) -> databaseTrainedCharacters.parallelStream().forEach(databaseTrainedCharacter -> {
            databaseTrainedCharacter.finishRecalculations();
            char letter = databaseTrainedCharacter.getValue();

            Future databaseFuture = databaseManager.clearLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont());
            while (!databaseFuture.isDone()) {
            }

            databaseFuture = databaseManager.createLetterEntry(letter, databaseTrainedCharacter.getWidthAverage(), databaseTrainedCharacter.getHeightAverage(), fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getCenter());
            while (!databaseFuture.isDone()) {
            }

            databaseFuture = databaseManager.addLetterSegments(letter, fontBounds.getMinFont(), fontBounds.getMaxFont(), databaseTrainedCharacter.getSegmentPercentages());
            while (!databaseFuture.isDone()) {
            }
        }));

//        System.out.println("Finished training in " + (System.currentTimeMillis() - start) + "ms");

//        trainedCharacterData.forEach(TrainedCharacterData::preformRecalculations);

        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));
    }

    private static boolean isWithin(int one, int two, double within) {
        double diff = Math.max((double) one, (double) two) - Math.min((double) one, (double) two);
        return diff <= within;
    }

    private static double[] getDifferencesFrom(double[] input1, double[] input2) {
        if (input1.length != input2.length) return null;
        double[] ret = new double[input1.length];

        for (int i = 0; i < input1.length; i++) {
            double one = input1[i];
            double two = input2[i];

            ret[i] = Math.max(one, two) - Math.min(one, two);
        }

        return ret;
    }

    private static boolean getLetterFrom(SearchImage searchImage, int x, int y, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        searchImage.scanFrom(x, y, coordinates);

        if (coordinates.size() != 0) {
            SearchCharacter searchCharacter = new SearchCharacter(coordinates);

            if (doDotStuff(searchCharacter, coordinates, searchCharacters)) return true;
            if (doPercentStuff(searchCharacter, coordinates, searchCharacters)) return true;

            Optional<SearchCharacter> possibleDot = getBaseForPercent(searchCharacters, searchCharacter);
            if (possibleDot.isPresent()) {
                combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.PERCENTAGE_CIRCLE);
                searchCharacters.remove(searchCharacter);
                return true;
            }

            if (searchCharacter.isProbablyDot() && !searchCharacter.hasDot()) {
                possibleDot = getBaseOfDot(searchCharacters, searchCharacter);
                if (possibleDot.isPresent()) {
                    combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT);
                    searchCharacters.remove(searchCharacter);
                    return true;
                }
            }

            // For ! or ?
            possibleDot = getDotUnderLetter(searchCharacters, searchCharacter);
            if (possibleDot.isPresent()) {
                combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT);
                searchCharacters.remove(searchCharacter);
                return true;
            }

            if (searchCharacter.isProbablyColon() && isAllBlack(searchCharacter) && !searchCharacter.hasDot()) {
                possibleDot = getBottomColon(searchCharacters, searchCharacter);
                if (possibleDot.isPresent()) {
                    combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.COLON);
                    searchCharacters.remove(searchCharacter);
                    return true;
                }
            }

            searchCharacter.applySections();
            searchCharacter.analyzeSlices();

            segmentPercentages = searchCharacter.getSegmentPercentages();

            searchCharacters.add(searchCharacter);
            coordinates.clear();
        }

        return false;
    }

    private static DatabaseCharacter getCharacterFor(SearchCharacter searchCharacter) {
        Map<DatabaseCharacter, Double> diffs = new HashMap<>(); // The lower value the better

        try {
            List<DatabaseCharacter> data = databaseManager.getAllCharacterSegments(searchCharacter.getHeight()).get();
//            System.out.println(searchCharacter.getHeight() + "] data = " + data);

            data.parallelStream().forEach(character -> {
                double[] charDifference = getDifferencesFrom(searchCharacter.getSegmentPercentages(), character.getData());

                double value = 1;
                if (AVERAGE_DIFF) {
                    value = Arrays.stream(charDifference).average().getAsDouble();
                }

                DatabaseCharacter using = character.copy();
                using.setX(searchCharacter.getX());
                using.setY(searchCharacter.getY());
//                using.setCenterExact(searchCharacter.getY() + using.getCenter());

                diffs.put(using, value);
            });

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }


//        System.out.println("diffs = " + diffs);

        LinkedList<Map.Entry<DatabaseCharacter, Double>> entries = sortByValue(diffs)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toCollection(LinkedList::new));

//        System.out.println("Got one: " + entries);
        return entries.isEmpty() ? null : entries.getFirst().getKey();
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static List<Pair<Integer, Integer>> getLineBoundsForTesting(boolean[][] values) {
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
                    System.out.println("Vertical separation was " + finalSpace + " yet height was " + height);
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

    private static boolean isRowPopulated(boolean[][] values, int y) {
        for (int x = 0; x < values[y].length; x++) {
            if (values[y][x]) return true;
        }

        return false;
    }

    private static TrainedCharacterData getTrainedData(char cha, List<TrainedCharacterData> trainedCharacterDataList) {
        return trainedCharacterDataList.stream().filter(characterData -> characterData.getValue() == cha).findFirst().get();
    }

    private static List<SearchCharacter> findCharacterAtY(int y, List<SearchCharacter> searchCharacters) {
        return findCharacterAtY(y, searchCharacters, -1);
    }

    private static List<SearchCharacter> findCharacterAtY(int y, List<SearchCharacter> searchCharacters, int addY) {
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
        int otherBetterY = -1;
        if (maxOneOptional.isPresent()) {
            SearchCharacter maxOne = maxOneOptional.get();
            otherBetterY = maxOne.getY() + maxOne.getHeight() / 2;
            int finalOtherBetterY = otherBetterY;
            temp = searchCharacters
                    .stream()
                    .sorted()
                    .filter(searchCharacter -> searchCharacter.isInYBounds(betterY) || searchCharacter.isInYBounds(finalOtherBetterY))
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        if (!temp.isEmpty()) {
            for (int x = 0; x < testImageShit.getWidth(); x++) {
                drawGuides(x, otherBetterY == -1 ? betterY : otherBetterY, Color.GREEN);
            }
        }

        return temp;
    }

    private static List<SearchCharacter> findCharacterAtLine(int topY, int bottomY, List<SearchCharacter> searchCharacters) {
        return searchCharacters
                .stream()
                .sorted()
                .filter(searchCharacter -> isWithin(topY, bottomY, searchCharacter.getY()))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static boolean isWithin(int lowerBound, int upperBound, int value) {
        return lowerBound <= value && value <= upperBound;
    }

    private static void makeImage(boolean[][] values, String name) {
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

    public static Stream<boolean[][]> getHorizontalHalf(boolean[][] values) {
        int topHeight = values.length / 2;
        int bottomHeight = values.length - topHeight;

        boolean[][] topHalf = new boolean[topHeight][];
        boolean[][] bottomHalf = new boolean[bottomHeight][];

        for (int y = 0; y < values.length; y++) {
            if (y < topHeight) {
                topHalf[y] = values[y];
            } else {
                bottomHalf[y - topHeight] = values[y];
            }
        }

        return Stream.of(topHalf, bottomHalf).sequential();
    }

    public static Stream<boolean[][]> getHorizontalThird(boolean[][] values) {
        int topHeight = values.length / 3;
        int middleHeight = values.length - topHeight * 2;
        int bottomHeight = topHeight;

        boolean[][] topThird = new boolean[topHeight][];
        boolean[][] middleThird = new boolean[middleHeight][];
        boolean[][] bottomThird = new boolean[bottomHeight][];

        for (int y = 0; y < values.length; y++) {
            if (y < topHeight) {
                topThird[y] = values[y];
            } else if (y < topHeight + middleHeight) {
                middleThird[y - topHeight] = values[y];
            } else {
                bottomThird[y - topHeight - middleHeight] = values[y];
            }
        }

        return Stream.of(topThird, middleThird, bottomThird).sequential();
    }

    public static Stream<boolean[][]> getVerticalHalf(boolean[][] values) {
        if (values.length == 0) return Stream.of(null, null);
        int leftHeight = values[0].length / 2;
        int rightHeight = values[0].length - leftHeight;

        boolean[][] leftHalf = new boolean[values.length][];
        boolean[][] rightHalf = new boolean[values.length][];

        for (int i = 0; i < values.length; i++) {
            leftHalf[i] = new boolean[leftHeight];
            rightHalf[i] = new boolean[rightHeight];
        }

        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[0].length; x++) {
                if (x < leftHeight) {
                    leftHalf[y][x] = values[y][x];
                } else {
                    rightHalf[y][x - leftHeight] = values[y][x];
                }
            }
        }

        return Stream.of(leftHalf, rightHalf).sequential();
    }

    public static Stream<boolean[][]> getVerticalThird(boolean[][] values) {
        if (values.length == 0) return Stream.of(null, null, null);
        int leftHeight = values[0].length / 3;
        int middleHeight = values[0].length - leftHeight * 2;
        int rightHeight = leftHeight;

        boolean[][] leftHalf = new boolean[values.length][];
        boolean[][] middleHalf = new boolean[values.length][];
        boolean[][] rightHalf = new boolean[values.length][];

        for (int i = 0; i < values.length; i++) {
            leftHalf[i] = new boolean[leftHeight];
            middleHalf[i] = new boolean[middleHeight];
            rightHalf[i] = new boolean[rightHeight];
        }

        for (int y = 0; y < values.length; y++) {
            for (int x = 0; x < values[0].length; x++) {
                if (x < leftHeight) {
                    leftHalf[y][x] = values[y][x];
                } else if (x < middleHeight + leftHeight) {
                    middleHalf[y][x - leftHeight] = values[y][x];
                } else {
                    rightHalf[y][x - leftHeight - middleHeight] = values[y][x];
                }
            }
        }

        return Stream.of(leftHalf, middleHalf, rightHalf).sequential();
    }

    public static Map<boolean[][], Integer> getDiagonal(boolean[][] values, boolean increasing) {
        double slope = (double) values.length / (double) values[0].length;

        List<Integer> yPositions = new ArrayList<>();

        for (int x = 0; x < values[0].length; x++) {
            double y = slope * x;
            if (increasing) y = values.length - y;
            yPositions.add((int) y);
        }

        boolean[][] topHalf = new boolean[values.length][];
        boolean[][] bottomHalf = new boolean[values.length][];
        int topSize = 0;
        int bottomSize = 0;

        for (int i = 0; i < values.length; i++) {
            topHalf[i] = new boolean[values[0].length];
            bottomHalf[i] = new boolean[values[0].length];
        }

        for (int x = 0; x < values[0].length; x++) {
            int yPos = yPositions.get(x);
            for (int y = 0; y < values.length; y++) {
                if (y < yPos) {
                    bottomHalf[y][x] = values[y][x];
                    bottomSize++;
                } else {
                    topHalf[y][x] = values[y][x];
                    topSize++;
                }
            }
        }

        Map<boolean[][], Integer> ret = new LinkedHashMap<>();
        ret.put(topHalf, topSize);
        ret.put(bottomHalf, bottomSize);
        return ret;
    }

    private static void rewriteImage(BufferedImage temp, BufferedImage input) {
        for (int y = 0; y < temp.getHeight(); y++) {
            for (int x = 0; x < temp.getWidth(); x++) {
                temp.setRGB(x, y, input.getRGB(x, y));
            }
        }
    }

    private static void toGrid(BufferedImage input, boolean[][] values) {
        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                values[arrY][arrX++] = new Color(input.getRGB(x, y)).equals(Color.BLACK);
            }

            arrX = 0;
            arrY++;
        }
    }

    private static boolean doDotStuff(SearchCharacter dotCharacter, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!dotCharacter.isProbablyDot()) return false;
        SearchCharacter baseCharacter = getBaseOfDot(searchCharacters, dotCharacter).orElse(null);
        if (baseCharacter != null) {
            combine(baseCharacter, dotCharacter, coordinates, CombineMethod.DOT);
            return true;
        }

        return false;
    }

    private static boolean doColonStuff(SearchCharacter dotCharacter, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!dotCharacter.isProbablyColon()) return false;
        SearchCharacter baseCharacter = getDotUnderLetter(searchCharacters, dotCharacter).orElse(null);
        if (baseCharacter != null) {
            combine(baseCharacter, dotCharacter, coordinates, CombineMethod.COLON);
            return true;
        }

        return false;
    }

    private static boolean doPercentStuff(SearchCharacter percentDotCharacter, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!percentDotCharacter.isProbablyCircleOfPercent()) return false;
        SearchCharacter baseCharacter = getBaseForPercent(searchCharacters, percentDotCharacter).orElse(null);
        if (baseCharacter != null) {
            combine(baseCharacter, percentDotCharacter, coordinates, CombineMethod.PERCENTAGE_CIRCLE);
            return true;
        }

        return false;
    }

    /*private static boolean doApostropheStuff(SearchCharacter rightApostrophe, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!rightApostrophe.isProbablyApostraphe()) return false;
        SearchCharacter leftApostrophe = getLeftApostrophe(searchCharacters, rightApostrophe).orElse(null);
        if (leftApostrophe != null) {
            combine(leftApostrophe, rightApostrophe, coordinates, CombineMethod.APOSTROPHE);
            return true;
        }

        return false;
    }*/

    private static void combine(SearchCharacter baseCharacter, SearchCharacter adding, List<Map.Entry<Integer, Integer>> coordinates, CombineMethod combineMethod) {
        int minX = Math.min(baseCharacter.getX(), adding.getX());
        int minY = Math.min(baseCharacter.getY(), adding.getY());
        int maxX = Math.max(baseCharacter.getX() + baseCharacter.getWidth(), adding.getX() + adding.getWidth());
        int maxY = Math.max(baseCharacter.getY() + baseCharacter.getHeight(), adding.getY() + adding.getHeight());
        baseCharacter.setWidth(maxX - minX);
        baseCharacter.setHeight(maxY - minY);
        baseCharacter.setX(minX);
        baseCharacter.setY(minY);

        switch (combineMethod) {
            case DOT:
            case COLON:
                maxX = baseCharacter.getX() + baseCharacter.getWidth();
                maxY = baseCharacter.getY() + baseCharacter.getHeight();
                baseCharacter.setHeight(maxY - adding.getY());
                baseCharacter.setY(adding.getY());

                int dotMaxX = adding.getX() + adding.getWidth();

                if (dotMaxX > maxX) {
                    baseCharacter.setWidth(dotMaxX - baseCharacter.getX());
                }

                baseCharacter.addDot(coordinates);
                break;
            case PERCENTAGE_CIRCLE:
                baseCharacter.addPercentageCircle(coordinates, adding.getY() + (adding.getHeight() / 2) < baseCharacter.getY() + (baseCharacter.getHeight() / 2));
                break;
            case APOSTROPHE:
                baseCharacter.addPercentageCircle(coordinates, false);
                break;
        }

        coordinates.clear();
    }

    enum CombineMethod {
        DOT,
        COLON,
        PERCENTAGE_CIRCLE,
        APOSTROPHE
    }

/*
    public static void generateHistograms(File file) throws IOException {
        BufferedImage input = ImageIO.read(file); // Full alphabet in 72 font
        BufferedImage histogramVisual = new BufferedImage(500, 2000, BufferedImage.TYPE_INT_ARGB);
        boolean[][] values = createGrid(input);
        List<SearchCharacter> searchCharcaters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                temp.setRGB(x, y, input.getRGB(x, y));
            }
        }

        input = temp;

        // Pre-filter

        filter(input);

        // End pre-filters

        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                values[arrY][arrX++] = new Color(input.getRGB(x, y)).equals(Color.BLACK);
            }

            arrX = 0;
            arrY++;
        }

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();
        List<Histogram> unorderedHistograms = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (coordinates.size() != 0) {
                    SearchCharacter dotCharacter = new SearchCharacter(coordinates);

                    if (dotCharacter.isProbablyDot()) {
                        SearchCharacter baseCharacter = getDotNearLetter(searchCharcaters, dotCharacter).orElse(null);
                        if (baseCharacter != null) {
                            int maxX = baseCharacter.getX() + baseCharacter.getWidth();
                            int maxY = baseCharacter.getY() + baseCharacter.getHeight();
                            baseCharacter.setHeight(maxY - dotCharacter.getY());
                            baseCharacter.setY(dotCharacter.getY());

                            int dotMaxX = dotCharacter.getX() + dotCharacter.getWidth();

                            if (dotMaxX > maxX) {
                                baseCharacter.setWidth(dotMaxX - baseCharacter.getX());
                            }

                            baseCharacter.addDot(coordinates);

                            coordinates.clear();
                            continue;
                        }
                    }

                    Histogram first = new Histogram(dotCharacter.getValues());
                    dotCharacter.setHistogram(first);

                    if (Main.first == null) {
                        Main.first = first;
                    }

                    unorderedHistograms.add(first);

                    searchCharcaters.add(dotCharacter);
                    coordinates.clear();
                }
            }
        }

        searchCharcaters.stream().sorted().forEach(searchCharacter -> {
            searchCharacters.put(letter++, searchCharacter.getHistogram());

            searchCharacter.getHistogram().drawTo(histogramVisual, 10, histogramY, Color.RED);
            histogramY += searchCharacter.getHistogram().getHeight() + 10;
        });

        BufferedImage finalInput = input;
        searchCharcaters.forEach(searchCharacter -> searchCharacter.drawTo(finalInput));

        System.out.println(searchCharcaters.size() + " characters found");

        ImageIO.write(histogramVisual, "png", new File("E:\\NewOCR\\histogramvisual.png"));
    }
*/

    private static Optional<SearchCharacter> getBaseOfDot(List<SearchCharacter> characters, SearchCharacter dotCharacter) {
        return characters.parallelStream()
                .filter(character -> !character.equals(dotCharacter))
                .filter(character -> !character.hasDot())
                .filter(character -> character.isInBounds(dotCharacter.getX() + (dotCharacter.getWidth() / 2), character.getY() + 4))
                .filter(character -> character.getHeight() > dotCharacter.getHeight() * 2)
                .filter(baseCharacter -> {
                    int below = dotCharacter.getY() + (dotCharacter.getHeight() * 2);
                    int mod = -dotCharacter.getHeight();
                    boolean got = false;
                    for (int i = 0; i < dotCharacter.getHeight() * 2; i++) {
                        if (DRAW_PROBES)
                            drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.RED);
                        if (below + (mod++) == baseCharacter.getY()) {
                            if (!DRAW_FULL_PROBES) return true;
                            got = true;
                        }
                    }

                    return got;
                })
                .findFirst();
    }

    // For ! or ?
    private static Optional<SearchCharacter> getDotUnderLetter(List<SearchCharacter> characters, SearchCharacter baseCharacter) {
        return characters.parallelStream()
                .filter(character -> !character.equals(baseCharacter))
                .filter(character -> !character.hasDot())
                .filter(SearchCharacter::isProbablyDot)
                .filter(character -> baseCharacter.isInBounds(character.getX() + (character.getWidth() / 2), baseCharacter.getY() + 4))
                .filter(character -> baseCharacter.getHeight() > character.getHeight() * 2)
                .filter(dotCharacter -> {
                    int below = dotCharacter.getY() - dotCharacter.getHeight();
                    int mod = dotCharacter.getHeight();
                    boolean got = false;
                    for (int i = 0; i < dotCharacter.getHeight() * 2; i++) {
                        if (DRAW_PROBES)
                            drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.BLUE);
                        if (below + (mod--) == baseCharacter.getY() + baseCharacter.getHeight()) {
                            if (!DRAW_FULL_PROBES) return true;
                            got = true;
                        }
                    }

                    return got;
                })
                .findFirst();
    }

    // : or ;
    private static Optional<SearchCharacter> getBottomColon(List<SearchCharacter> characters, SearchCharacter topDot) {
        return characters.stream()
                .filter(character -> !character.equals(topDot))
                .filter(character -> !character.hasDot())
                .filter(character -> topDot.isInXBounds(character.getX() + (character.getWidth() / 2)))
                .filter(character -> {
                    double ratio = (double) topDot.getHeight() / (double) character.getHeight();
                    return (ratio >= 0.3 && ratio <= 0.5) || (topDot.getHeight() == character.getHeight() && topDot.getWidth() == character.getWidth());
                })
                .filter(dotCharacter -> {
//                    System.out.println("Bottom");
                    int below = dotCharacter.getY() - dotCharacter.getHeight() * 2;
                    int mod = dotCharacter.getHeight() * 2;
                    boolean got = false;
                    for (int i = 0; i < dotCharacter.getHeight() * 3; i++) {
                        if (DRAW_PROBES)
                            drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.GREEN);
                        if (below + (mod--) == topDot.getY() + topDot.getHeight()) {
                            if (!DRAW_FULL_PROBES) return true;
                            got = true;
                        }
                    }

                    return got;
                })
                .findFirst();
    }

    private static void drawGuides(int x, int y, Color color) {
        if (x < 0 || y < 0) return;
        if (x >= testImageShit.getWidth() || y >= testImageShit.getWidth()) return;
        testImageShit.setRGB(x, y, color.getRGB());
    }

    private static boolean isAllBlack(SearchCharacter searchCharacter) {
        for (boolean[] row : searchCharacter.getValues()) {
            for (boolean bool : row) {
                if (!bool) return false;
            }
        }

        return true;
    }

    private static Optional<SearchCharacter> getBaseForPercent(List<SearchCharacter> characters, SearchCharacter circleOfPercent) {
        return characters.parallelStream()
                .filter(searchCharacter -> searchCharacter.isOverlaping(circleOfPercent))
                .findFirst();
    }

    public static void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    public static void colorColumn(BufferedImage image, Color color, int x, int y, int height) {
        for (int y2 = 0; y2 < height; y2++) {
            image.setRGB(x, y + y2, color.getRGB());
        }
    }

    private static boolean[][] createGrid(BufferedImage bufferedImage) {
        boolean[][] values = new boolean[bufferedImage.getHeight()][];
        for (int i = 0; i < values.length; i++) {
            boolean[] row = new boolean[bufferedImage.getWidth()];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = false;

            values[i] = row;
        }

        return values;
    }

    public static void printOut(boolean[][] values) {
        for (boolean[] row : values) {
            for (boolean bool : row) {
                System.out.print(bool ? "1" : "0");
            }

            System.out.println("");
        }
    }

    private static void filter(BufferedImage bufferedImage) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                bufferedImage.setRGB(x, y, isBlack(bufferedImage, x, y) ? new Color(0, 0, 0, 255).getRGB() : new Color(255, 255, 255, 255).getRGB());
            }
        }
    }

    private static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
//            System.out.println(pixel);
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 255 * 0.75;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
