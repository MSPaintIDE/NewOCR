package com.uddernetworks.newocr.altsearcher;

import com.uddernetworks.newocr.altsearcher.feature.TrainedCharacterData;
import javafx.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static double AFFECT_BACKWARDS = 1D; // Originally 2
    public static final boolean ALL_INPUTS_EQUAL = true;
    public static final boolean DRAW_PROBES = true;
    public static final boolean DRAW_FULL_PROBES = true;
    public static final String trainString = "!#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghjiklmnopqrstuvwxyz{|}~";

    private static Histogram first;
    private static int letterIndex = 0;
//    private static char letter = 'a';
    //    private static Map<Character, SearchCharacter> searchCharacters = new HashMap<>();
    private static int trainWidth = 0;
    private static List<TrainedCharacterData> trainedCharacterData = new ArrayList<>();
    private static double[] segmentPercentages;

    private static int testIndex = 0;

    private static SortedMap<Double, Double> averageToBack = new TreeMap<>();

    private static DecimalFormat percent = new DecimalFormat(".##");

    private static BufferedImage testImageShit;

    public static void main(String[] args) throws IOException { // alphabet48

        for (int i = 0; i < 100; i++) {
            letterIndex = 0;
            trainedCharacterData = new ArrayList<>();
            System.out.println("AFFECT_BACKWARDS = " + AFFECT_BACKWARDS);
            System.out.println("Generating features...");
            long start = System.currentTimeMillis();
//            generateFeatures(new File("E:\\NewOCR\\ij.png"));
//            generateFeatures(new File("E:\\NewOCR\\tttapos.png"));
            generateFeatures(new File("E:\\NewOCR\\training.png"));
//            generateFeatures(new File("E:\\NewOCR\\testshittttt.png"));
//            generateFeatures(new File("E:\\NewOCR\\pecent.png"));
//        generateFeatures(new File("E:\\NewOCR\\letter.png"));
            System.out.println("Finished in " + (System.currentTimeMillis() - start) + "ms");


//        System.exit(0);
//        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\lipsum_lower.png"));
//        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\alphabet72.png"));
            BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\letter.png"));
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
                    searchImage.scanFrom(x, y, coordinates);

                    if (coordinates.size() != 0) {
                        SearchCharacter searchCharacter = new SearchCharacter(coordinates);

                        if (doDotStuff(searchCharacter, coordinates, searchCharacters)) continue;

//                    System.out.println("\nDOING TESTING ONE NOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n");

//                    printOut(searchCharacter.getValues()); // [0.5143769968051118, 0.6233333333333333, 0.5238095238095238, 0.44477611940298506, 0.6538461538461539, 0.2585669781931464, 0.45918367346938777, 0.7792207792207793]

                        searchCharacter.applySections();
                        searchCharacter.analyzeSlices();

//                    System.out.println("Generated segment percentages: " + Arrays.toString(searchCharacter.getSegmentPercentages()));

//                    System.out.println("Similarity with real: " + percent.format(Main.searchCharacters.get('a').getSimilarityWith(searchCharacter) * 100) + "%");

                        searchCharacters.add(searchCharacter);
                        coordinates.clear();
                    }
                }
            }

//        searchCharacters.stream().sorted().forEach(searchCharacter -> {
//
//
//
////            System.out.println("Closest is '" + answer + "' with a similarity of " + percent.format(answerSimilarity * 100) + "%");
//        });

            List<Double> percentages = new ArrayList<>();
            StringBuilder result = new StringBuilder();

            List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);
            for (int y = 0; y < input.getHeight(); y++) {
                List<SearchCharacter> line = findCharacterAtY(y, searchCharacters);

                if (!line.isEmpty()) {
//                System.out.println("");
//                System.out.println(line);
                    line.forEach(searchCharacter -> {
//                    System.out.print(getCharacterFor(searchCharacter));
                        CharData charData = getCharacterFor(searchCharacter);
//                        if (pair == null) {
//                            percentages.add(0D);
//                            System.out.print('?');
//                            result.append('?');
//                        } else {
//                            percentages.add(pair.getValue());
//                            System.out.print(pair.getKey());
//                            result.append(pair.getKey());
//                        }
                    });

                    searchCharacters.removeAll(line);
                }
            }

//            if (!result.toString().equals("abcdefghijklmnopqrstuvwxyz")) {
//            if (!result.toString().trim().equals("dsfjhsdfknefiusjfdlkneoiwejdmsdkljfoweoijfmosuehrmseoruimnhurdignidousenmfiuerfjnseiufhosjiefnisdurofjmsdrofjsieorjfcmsrojifsmefjiosdrefmrnsehoimzapyoiyluknjvmxbchdneywtqraesdzcsgfcbdhfjrueikdhs")) {
//                System.out.println("Not equals! Got: \n\t" + result);

//                System.exit(0);
//            }

            System.out.println(result);

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


            AFFECT_BACKWARDS += 0.3D;
        }

        List<Map.Entry<Double, Double>> entries = new ArrayList<>(averageToBack.entrySet());
        Collections.reverse(entries);
//        System.out.println(averageToBack);
        System.out.println("\n\n====================");
        entries.forEach(entry -> {
            System.out.println(percent.format(entry.getKey() * 100) + "% \t\t| " + entry.getValue());
        });

//        ImageIO.write(temp, "png", new File("E:\\NewOCR\\tempout.png"));
    }

    public static void generateFeatures(File file) throws IOException {
        BufferedImage input = ImageIO.read(file); // Full alphabet in 72 font
//        BufferedImage histogramVisual = new BufferedImage(500, 2000, BufferedImage.TYPE_INT_ARGB);
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

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        testIndex = 0;

        List<Double> doubles = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (coordinates.size() != 0) {
                    SearchCharacter searchCharacter = new SearchCharacter(coordinates);

                    double res = ((double) searchCharacter.getWidth()) / ((double) searchCharacter.getHeight());
                    doubles.add(res);
                    System.out.println("\t\t]" + res);

                    if (doDotStuff(searchCharacter, coordinates, searchCharacters)) continue;
                    if (doPercentStuff(searchCharacter, coordinates, searchCharacters)) continue;
//                    if (doApostropheStuff(searchCharacter, coordinates, searchCharacters)) continue;
//                    if (doColonStuff(searchCharacter, coordinates, searchCharacters)) continue;

                    Optional<SearchCharacter> possibleDot = getBaseForPercent(searchCharacters, searchCharacter);
                    if (possibleDot.isPresent()) {
                        combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.PERCENTAGE_CIRCLE);
                        searchCharacters.remove(searchCharacter);
                        continue;
                    }

                    if (searchCharacter.isProbablyDot() && !searchCharacter.hasDot()) {
                        possibleDot = getBaseOfDot(searchCharacters, searchCharacter);
//                        System.out.println("possibleDot = " + possibleDot);
                        if (possibleDot.isPresent()) {
                            combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT);
                            searchCharacters.remove(searchCharacter);
                            continue;
                        }
                    }

                    // For ! or ?
                    possibleDot = getDotUnderLetter(searchCharacters, searchCharacter);
                    if (possibleDot.isPresent()) {
                        combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.DOT);
                        searchCharacters.remove(searchCharacter);
                        continue;
                    }

                    /*if (searchCharacter.isProbablyApostraphe() && !searchCharacter.hasDot()) {
                        System.out.println("Is apos");
                        possibleDot = getLeftApostrophe(searchCharacters, searchCharacter);
                        System.out.println("possibleDot = " + possibleDot);
                        if (possibleDot.isPresent()) {
                            combine(searchCharacter, possibleDot.get(), coordinates, CombineMethod.APOSTROPHE);
                            searchCharacters.remove(searchCharacter);
                            continue;
                        }
                    }*/

//                    System.out.println("Is colon: " + searchCharacter.isProbablyColon());
                    if (searchCharacter.isProbablyColon() && isAllBlack(searchCharacter) && !searchCharacter.hasDot()) {
//                        System.out.println("Found part of a colon");
                        possibleDot = getBottomColon(searchCharacters, searchCharacter);
                        if (possibleDot.isPresent()) {
//                            System.out.println("PRESENT!");
                            combine(possibleDot.get(), searchCharacter, coordinates, CombineMethod.COLON);
                            searchCharacters.remove(searchCharacter);
                            continue;
                        }
                    }

//                    System.out.println("===== END =====");


                    searchCharacter.applySections();
                    searchCharacter.analyzeSlices();

                    segmentPercentages = searchCharacter.getSegmentPercentages();
//                    System.out.println("Trained segmentPercentages = " + Arrays.toString(segmentPercentages));

//                    try {
//                        if (!new File("E:\\NewOCR\\" + ("output\\charcater_" + testIndex) + ".png").exists()) {
//                            makeImage(searchCharacter.getValues(), "output\\charcater_" + testIndex);
//                        }
//                        System.out.println(searchCharacter.getY());
//                        testIndex++;
//                    } catch (Exception ignore) {
////                        ignore.printStackTrace();
//                    }

                    searchCharacters.add(searchCharacter);
                    coordinates.clear();
                }
            }
        }

//        searchCharacters.forEach(searchCharacter -> searchCharacter.drawTo());
        System.out.println("CHARS: " + searchCharacters.size());

        IntStream.range('!', '~' + 1).forEach(letter -> trainedCharacterData.add(new TrainedCharacterData((char) letter)));

//        int maxWidth = searchCharacters.stream().mapToInt(SearchCharacter::getHeight).max().getAsInt();

        BufferedImage finalInput = input;
        searchCharacters.stream().sorted().forEach(searchCharacter -> searchCharacter.drawTo(finalInput));
        Collections.sort(searchCharacters);

        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));


        System.out.println("searchCharacters = " + searchCharacters.size());

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);
        for (int y = 0; y < input.getHeight(); y++) {
            List<SearchCharacter> line = findCharacterAtY(y, searchCharacters);

//            System.out.println("line = " + line.size());

            if (!line.isEmpty()) {
                line.forEach(searchCharacter -> {
//                    char current = Main.letter++;
                    char current = trainString.charAt(letterIndex++);
                    if (letterIndex >= trainString.length()) letterIndex = 0;
                    searchCharacter.setKnownChar(current);

                    try {
                        if (!new File("E:\\NewOCR\\" + ("output\\charcater_" + testIndex) + ".png").exists()) {
                            makeImage(searchCharacter.getValues(), "output\\charcater_" + testIndex);
                        }
//                        System.out.println(searchCharacter.getY());
                        testIndex++;
                    } catch (Exception ignore) {
                    }

                    TrainedCharacterData trainedCharacterData = getTrainedData(current);
                    trainedCharacterData.setHasDot(searchCharacter.hasDot());
                    trainedCharacterData.recalculateTo(searchCharacter);
                });

                searchCharacters.removeAll(line);
//                System.exit(0);
            }
        }

        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));

        System.exit(0);

        trainedCharacterData.forEach(TrainedCharacterData::finishRecalculations);

//        trainedCharacterData.forEach(TrainedCharacterData::preformRecalculations);

        searchCharacters = searchCharactersCopy;

        System.out.println(searchCharacters.size() + " characters found");

        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));
    }

    static class CharData implements Comparable<CharData> {
        private TrainedCharacterData characterData;
        private double similarity;
        private double ratioDifference;

        public CharData(TrainedCharacterData characterData, double similarity, double ratioDifference) {
            this.characterData = characterData;
            this.similarity = similarity;
            this.ratioDifference = ratioDifference;
        }

        public TrainedCharacterData getCharacterData() {
            return characterData;
        }

        public double getSimilarity() {
            return similarity;
        }

        public double getRatioDifference() {
            return ratioDifference;
        }

        @Override
        public String toString() {
            return "[" + this.characterData + "|" + percent.format(similarity * 100) + "|" + percent.format(ratioDifference * 100) + "]";
        }

        @Override
        public int compareTo(CharData charData) {
            return Double.compare(this.similarity, charData.similarity);
        }
    }

    private static CharData getCharacterFor(SearchCharacter searchCharacter) {
        Map<Character, Double> results = new HashMap<>();
//        double answerSimilarity = -1;
//        TrainedCharacterData answer = null;
        List<CharData> charDataList = new ArrayList<>();
        for (TrainedCharacterData characterData : trainedCharacterData) {
            Pair<Double, Double> pair = characterData.getSimilarityWith(searchCharacter);
            double similarity = pair.getKey();
//            if (similarity > answerSimilarity && characterData.hasDot() == searchCharacter.hasDot()) {
//                answerSimilarity = similarity;
//                answer = characterData;
//            }

            if (characterData.hasDot() == searchCharacter.hasDot()) {
                charDataList.add(new CharData(characterData, pair.getKey(), pair.getValue()));
            }

            results.put(characterData.getValue(), similarity);
        }

        Collections.sort(charDataList);
        Collections.reverse(charDataList);

        System.out.println("answers = " + charDataList);

        CharData answer = charDataList.get(0);

        System.out.println("Closest is '" + answer.getCharacterData() + "' with a similarity of " + percent.format(answer.getSimilarity() * 100) + "% \t\tOther: " + sortByValue(results));
//        System.out.println("Unknown: " + ((double) searchCharacter.getWidth() / (double) searchCharacter.getHeight()) + " known: " + answer);

        return answer;
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

    private static TrainedCharacterData getTrainedData(char cha) {
        return trainedCharacterData.stream().filter(characterData -> characterData.getValue() == cha).findFirst().get();
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

//        Optional<SearchCharacter> maxOneOptional = temp.stream().sorted(Comparator.comparingInt(SearchCharacter::getHeight)).reduce((first, second) -> second);
        Optional<SearchCharacter> maxOneOptional = temp.stream().sorted((o1, o2) -> o2.getHeight() - o1.getHeight()).findFirst();
//        System.out.println("maxOneOptional = " + maxOneOptional.get().getHeight() + " > " + betterYCharacter.getHeight());
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

    public static Optional<SearchCharacter> getBaseOfDot(List<SearchCharacter> characters, SearchCharacter dotCharacter) {
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
                        if (DRAW_PROBES) drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.RED);
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
    public static Optional<SearchCharacter> getDotUnderLetter(List<SearchCharacter> characters, SearchCharacter baseCharacter) {
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
                        if (DRAW_PROBES) drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.BLUE);
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
    public static Optional<SearchCharacter> getBottomColon(List<SearchCharacter> characters, SearchCharacter topDot) {
        return characters.stream()
                .filter(character -> !character.equals(topDot))
                .filter(character -> !character.hasDot())
//                .filter(character -> character.isProbablyDot() || character.isProbablyApostraphe())
//                .filter(character -> topDot.getHeight() == character.getHeight() && topDot.getWidth() == character.getWidth())
                .filter(character -> topDot.isInXBounds(character.getX() + (character.getWidth() / 2)))
//                .filter(Main::isAllBlack)
                .filter(character -> {
                    double ratio = (double) topDot.getHeight() / (double) character.getHeight();
                    return (ratio >= 0.3 && ratio <= 0.5) || (topDot.getHeight() == character.getHeight() && topDot.getWidth() == character.getWidth());
                })
                .filter(dotCharacter -> {
                    System.out.println("Bottom");
                    int below = dotCharacter.getY() - dotCharacter.getHeight() * 2;
                    int mod = dotCharacter.getHeight() * 2;
                    boolean got = false;
                    for (int i = 0; i < dotCharacter.getHeight() * 3; i++) {
                        if (DRAW_PROBES) drawGuides(dotCharacter.getX() + (dotCharacter.getWidth() / 2), below + mod, Color.GREEN);
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

    public static Optional<SearchCharacter> getBaseForPercent(List<SearchCharacter> characters, SearchCharacter circleOfPercent) {
        return characters.parallelStream()
                .filter(searchCharacter -> searchCharacter.isOverlaping(circleOfPercent))
                .findFirst();
    }

    /*private static Optional<SearchCharacter> getLeftApostrophe(List<SearchCharacter> characters, SearchCharacter rightApostrophe) {
        return characters.parallelStream()
                .filter(SearchCharacter::isProbablyApostraphe)
                .filter(character -> character.getY() == rightApostrophe.getY())
                .filter(character -> character.getWidth() == rightApostrophe.getWidth() && character.getHeight() == rightApostrophe.getHeight())
                .filter(character -> {
                    double xDiff = Math.max(character.getX(), rightApostrophe.getX()) - Math.min(character.getX(), rightApostrophe.getX()) - rightApostrophe.getWidth();
                    double acceptedDiff = Math.pow((double) rightApostrophe.getWidth(), 1.2);
                    if (xDiff < acceptedDiff) return true;
                    xDiff = Math.max(character.getX(), rightApostrophe.getX()) - Math.min(character.getX(), rightApostrophe.getX()) - rightApostrophe.getWidth() + 4D;
                    acceptedDiff = Math.pow(((double) rightApostrophe.getWidth() + 4D), 1.5);
                    return xDiff < acceptedDiff;
                })
                .findFirst();
    }*/

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

    public static boolean[][] createGrid(BufferedImage bufferedImage) {
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

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }

    public static void filter(BufferedImage bufferedImage) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                bufferedImage.setRGB(x, y, isBlack(bufferedImage, x, y) ? new Color(0, 0, 0, 255).getRGB() : new Color(255, 255, 255, 255).getRGB());
            }
        }
    }

    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
//            System.out.println(pixel);
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 255 * 0.75;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
