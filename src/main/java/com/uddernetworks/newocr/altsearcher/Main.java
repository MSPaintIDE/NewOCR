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

    private static Histogram first;
    private static char letter = 'a';
    //    private static Map<Character, SearchCharacter> searchCharacters = new HashMap<>();
    private static int trainWidth = 0;
    private static List<TrainedCharacterData> trainedCharacterData = new ArrayList<>();
    private static double[] segmentPercentages;

    private static SortedMap<Double, Double> averageToBack = new TreeMap<>();

    private static DecimalFormat percent = new DecimalFormat(".##");

    public static void main(String[] args) throws IOException { // alphabet48

        for (int i = 0; i < 100; i++) {
            letter = 'a';
            trainedCharacterData = new ArrayList<>();
            System.out.println("AFFECT_BACKWARDS = " + AFFECT_BACKWARDS);
//            System.out.println("Generating features...");
            long start = System.currentTimeMillis();
            generateFeatures(new File("E:\\NewOCR\\input.png"));
//        generateFeatures(new File("E:\\NewOCR\\letter.png"));
//            System.out.println("Finished in " + (System.currentTimeMillis() - start) + "ms");


//        System.exit(0);
//        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\lipsum_lower.png"));
//        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\alphabet72.png"));
            BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\shit.png"));
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
                        Pair<Character, Double> pair = getCharacterFor(searchCharacter);
                        if (pair == null) {
                            percentages.add(0D);
                            System.out.print('?');
                            result.append('?');
                        } else {
                            percentages.add(pair.getValue());
                            System.out.print(pair.getKey());
                            result.append(pair.getKey());
                        }
                    });

                    searchCharacters.removeAll(line);
                }
            }

//            if (!result.toString().equals("abcdefghijklmnopqrstuvwxyz")) {
            if (!result.toString().trim().equals("dsfjhsdfknefiusjfdlkneoiwejdmsdkljfoweoijfmosuehrmseoruimnhurdignidousenmfiuerfjnseiufhosjiefnisdurofjmsdrofjsieorjfcmsrojifsmefjiosdrefmrnsehoimzapyoiyluknjvmxbchdneywtqraesdzcsgfcbdhfjrueikdhs")) {
                System.out.println("Not equals! Got: \n\t" + result);

//                System.exit(0);
            }

            double average = percentages.stream().mapToDouble(t -> t).average().getAsDouble();
            System.out.println("\nAverage: " + average);

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

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rewriteImage(temp, input);
        input = temp;

        filter(input);
        toGrid(input, values);

//        ImageIO.write(input, "png", new File("E:\\NewOCR\\binariazed.png"));

//        printOut(values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (coordinates.size() != 0) {
                    SearchCharacter searchCharacter = new SearchCharacter(coordinates);

                    if (doDotStuff(searchCharacter, coordinates, searchCharacters)) continue;
                    searchCharacter.applySections();
                    searchCharacter.analyzeSlices();

                    segmentPercentages = searchCharacter.getSegmentPercentages();
//                    System.out.println("Trained segmentPercentages = " + Arrays.toString(segmentPercentages));

                    searchCharacters.add(searchCharacter);
                    coordinates.clear();
                }
            }
        }

        IntStream.range('a', 'z' + 1).forEach(letter -> trainedCharacterData.add(new TrainedCharacterData((char) letter)));

        BufferedImage finalInput = input;
        searchCharacters.stream().sorted().forEach(searchCharacter -> searchCharacter.drawTo(finalInput));

        List<SearchCharacter> searchCharactersCopy = new ArrayList<>(searchCharacters);
        for (int y = 0; y < input.getHeight(); y++) {
            List<SearchCharacter> line = findCharacterAtY(y, searchCharacters);

            if (!line.isEmpty()) {
                line.forEach(searchCharacter -> {
                    char current = Main.letter++;
                    if (Main.letter > 'z') Main.letter = 'a';
                    searchCharacter.setKnownChar(current);

                    TrainedCharacterData trainedCharacterData = getTrainedData(current);
                    trainedCharacterData.setHasDot(searchCharacter.hasDot());
                    trainedCharacterData.recalculateTo(searchCharacter.getSegmentPercentages());
                });

                searchCharacters.removeAll(line);
            }
        }

//        trainedCharacterData.forEach(TrainedCharacterData::preformRecalculations);

        searchCharacters = searchCharactersCopy;

        System.out.println(searchCharacters.size() + " characters found");

//        ImageIO.write(input, "png", new File("E:\\NewOCR\\output.png"));
    }

    private static Pair<Character, Double> getCharacterFor(SearchCharacter searchCharacter) {
        Map<Character, Double> results = new HashMap<>();
        double answerSimilarity = -1;
        TrainedCharacterData answer = null;
        for (TrainedCharacterData characterData : trainedCharacterData) {
            double similarity = characterData.getSimilarityWith(searchCharacter);
            if (similarity > answerSimilarity && characterData.hasDot() == searchCharacter.hasDot()) {
                answerSimilarity = similarity;
                answer = characterData;
            }

            results.put(characterData.getValue(), similarity);
        }

//        System.out.println("Closest is '" + answer + "' with a similarity of " + percent.format(answerSimilarity * 100) + "% \t\tOther: " + sortByValue(results));
//        System.out.print(answer);
//        if (answer == null) return ' ';
//        return answer.getValue();
//        System.out.println("searchCharacter = " + searchCharacter);
//        System.out.println("answer = " + answer);
        if (answer == null) return null;
        return new Pair<>(answer.getValue(), answerSimilarity);
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
        Optional<SearchCharacter> optionalSearchCharacter = searchCharacters
                .stream()
                .filter(searchCharacter -> searchCharacter.isInYBounds(y))
                .findFirst();

        if (!optionalSearchCharacter.isPresent()) return new ArrayList<>();
        SearchCharacter betterYCharacter = optionalSearchCharacter.get();
        int betterY = betterYCharacter.getY() + betterYCharacter.getHeight() / 2;

        return searchCharacters
                .stream()
                .sorted()
                .filter(searchCharacter -> searchCharacter.isInYBounds(betterY))
                .collect(Collectors.toCollection(LinkedList::new));
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
            e.printStackTrace();
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
        SearchCharacter baseCharacter = getDotOverLetter(searchCharacters, dotCharacter).orElse(null);
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
            return true;
        }

        return false;
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
                        SearchCharacter baseCharacter = getDotOverLetter(searchCharcaters, dotCharacter).orElse(null);
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

    public static Optional<SearchCharacter> getDotOverLetter(List<SearchCharacter> characters, SearchCharacter searchCharacter) {
        int below = searchCharacter.getY() + (searchCharacter.getHeight() * 2) + 2;
        return characters.parallelStream()
                .filter(character -> character.getX() <= searchCharacter.getX() && character.getX() + character.getWidth() + 1 >= searchCharacter.getX() + searchCharacter.getWidth())
                .filter(character -> {
                    int mod = -1;
                    for (int i = 0; i < 3; i++) {
                        if (below + (mod++) == character.getY()) return true;
                    }

                    return false;
                })
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
                bufferedImage.setRGB(x, y, isBlack(bufferedImage, x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
    }

    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 128;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
