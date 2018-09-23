package com.uddernetworks.newocr.altsearcher;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    private static Histogram first;
    private static char letter = 'a';
    private static Map<Character, SearchCharacter> searchCharacters = new HashMap<>();
    private static double[] segmentPercentages;

    private static DecimalFormat percent = new DecimalFormat(".##");

    public static void main(String[] args) throws IOException { // alphabet48

        System.out.println("Generating features...");
        long start = System.currentTimeMillis();
        generateFeatures(new File("E:\\NewOCR\\letter.png"));
        System.out.println("Finished in " + (System.currentTimeMillis() - start) + "ms");

        BufferedImage input = ImageIO.read(new File("E:\\NewOCR\\teststuff.png"));
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

                    System.out.println("Similarity with real: " + percent.format(Main.searchCharacters.get('a').getSimilarityWith(searchCharacter) * 100) + "%");

//                    Map.Entry<boolean[][], Integer> firstSegment = searchCharacter.getSegments().entrySet().stream().findFirst().get();
//                    makeImage(firstSegment.getKey(), "firstTest");

                    searchCharacters.add(searchCharacter);
                    coordinates.clear();
                }
            }
        }

//        searchCharacters.stream().sorted().forEach(searchCharacter -> {
//            double maxScore = 0;
//
//
//        });

        BufferedImage finalInput = input;
        searchCharacters.forEach(searchCharacter -> searchCharacter.drawTo(finalInput));

        System.out.println(searchCharacters.size() + " characters found");

        ImageIO.write(temp, "png", new File("E:\\NewOCR\\tempout.png"));
    }

    public static void generateFeatures(File file) throws IOException {
        BufferedImage input = ImageIO.read(file); // Full alphabet in 72 font
//        BufferedImage histogramVisual = new BufferedImage(500, 2000, BufferedImage.TYPE_INT_ARGB);
        boolean[][] values = createGrid(input);
        List<SearchCharacter> searchCharacters = new ArrayList<>();

        BufferedImage temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rewriteImage(temp, input);
        input = temp;

        filter(input);
        toGrid(input, values);

        printOut(values);

        SearchImage searchImage = new SearchImage(values, input.getWidth(), input.getHeight());

        List<Map.Entry<Integer, Integer>> coordinates = new ArrayList<>();

        for (int y = input.getHeight(); 0 <= --y; ) {
            for (int x = 0; x < input.getWidth(); x++) {
                searchImage.scanFrom(x, y, coordinates);

                if (coordinates.size() != 0) {
                    SearchCharacter searchCharacter = new SearchCharacter(coordinates);

                    if (doDotStuff(searchCharacter, coordinates, searchCharacters)) continue;

//                    printOut(searchCharacter.getValues());

                    searchCharacter.applySections();
                    searchCharacter.analyzeSlices();

                    segmentPercentages = searchCharacter.getSegmentPercentages();
                    System.out.println("Trained segmentPercentages = " + Arrays.toString(segmentPercentages));

//                    Map.Entry<boolean[][], Integer> firstSegment = searchCharacter.getSegments().entrySet().stream().findFirst().get();
//                    makeImage(firstSegment.getKey(), "firstGen");

                    searchCharacters.add(searchCharacter);
                    coordinates.clear();
                }
            }
        }

        BufferedImage finalInput = input;
        searchCharacters.stream().sorted().forEach(searchCharacter -> {
            Main.searchCharacters.put(letter++, searchCharacter);

            searchCharacter.drawTo(finalInput);
        });

        System.out.println(searchCharacters.size() + " characters found");

//        ImageIO.write(histogramVisual, "png", new File("E:\\NewOCR\\output.png"));
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
