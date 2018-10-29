package com.uddernetworks.newocr.altsearcher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchCharacter implements Comparable<SearchCharacter> {

    private char knownChar = '?';
    private boolean[][] values;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean hasDot;
    private LetterMeta letterMeta = LetterMeta.NONE;
    public LinkedHashMap<boolean[][], Integer> segments = new LinkedHashMap<>();
//    private List<Double> segmentPercentages = new LinkedList<>(); // Percentage <= 1 // FIrst 8 are the normal ones, last 9 are for the grid created
//    private double[] segmentPercentagesArray;
    private double[] segmentPercentages = new double[8 + 9]; // Percentage <= 1 // FIrst 8 are the normal ones, last 9 are for the grid created

    public SearchCharacter(List<Map.Entry<Integer, Integer>> coordinates) {
        List<Integer> xStream = coordinates.stream().map(Map.Entry::getKey).collect(Collectors.toList());
        List<Integer> yStream = coordinates.stream().map(Map.Entry::getValue).collect(Collectors.toList());

        int maxX = xStream.stream().max(Integer::compareTo).get();
        int minX = xStream.stream().min(Integer::compareTo).get();

        int maxY = yStream.stream().max(Integer::compareTo).get();
        int minY = yStream.stream().min(Integer::compareTo).get();

        this.x = minX;
        this.y = minY;

        this.width = maxX - minX;
        this.height = maxY - minY;

        values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        coordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);
    }

    public boolean isProbablyDot() {
        int diff = Math.max(width, height) - Math.min(width, height);
//        System.out.println("diff = " + diff);
//        return diff <= 3 && width < height * 5;
        return diff <= 3;
    }

    public boolean isProbablyCircleOfPercent() {
        double ratio = (double) width + 1 / (double) height + 1;
        return ratio <= 0.9 && ratio >= 0.7;
    }

    public boolean isProbablyApostraphe() {
        double ratio = (double) width / (double) height;
//        System.out.println("ratio = " + ratio);
        return (ratio <= 0.375 && ratio >= 0.166) || (width == 1 && (height == 4 || height == 5));
    }

    public boolean isProbablyColon() {
//        System.out.println(width + " x " + height);
//        if (width > height * 5) {
////            System.out.println("False!");
//            return false;
//        }

        double ratio = (Math.min(this.width, this.height) + 1D) / (Math.max(this.width, this.height) + 1D);
//        double ratio = (double) this.width / (double) this.height;
//        System.out.println("ratio = " + ratio);
//        System.out.println("ratio " + ratio + " <= 0.9D is: " + (ratio <= 0.9D));

//        System.out.println("ratio = " + ratio + " (" + (width + 1) + "/" + (height + 1) + ") " + (ratio <= 0.9D) + " and " + (ratio >= 0.8D));
        return (ratio <= 1D && ratio >= 0.7D)
                || (height * 4 < width)
                || ((width == 3 && height == 3)
                    || (width == 2 && height == 3)
                    || (width == 2 && height == 2)
                    || (width == 1 || height == 2));
    }

    public void addDot(List<Map.Entry<Integer, Integer>> dotCoordinates) {
        boolean[][] values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        int yOffset = this.height - this.values.length + 1;

        for (int y = 0; y < this.values.length; y++) {
            System.arraycopy(this.values[y], 0, values[y + yOffset], 0, this.values[0].length);
        }

        dotCoordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);

        this.values = values;
        this.hasDot = true;
    }

    public void addPercentageCircle(List<Map.Entry<Integer, Integer>> dotCoordinates, boolean left) {
        boolean[][] values = new boolean[this.height + 1][];
        for (int i = 0; i < values.length; i++) values[i] = new boolean[width + 1];

        int yOffset = this.height - this.values.length + 1;

        int offset = left ? Math.abs(width + 1 - this.values[0].length) : 0;
        for (int y = 0; y < this.values.length; y++) {
            System.arraycopy(this.values[y], 0, values[y + yOffset], offset, this.values[0].length);
        }

        dotCoordinates.forEach(entry -> values[entry.getValue() - this.y][entry.getKey() - this.x] = true);

        this.values = values;
        this.hasDot = true;
    }

    public void drawTo(BufferedImage image) {
        drawTo(image, Color.MAGENTA);
    }

    public void drawTo(BufferedImage image, Color color) {

        // Top
        Main.colorRow(image, color, y, x, width);

        // Bottom
        Main.colorRow(image, color, y + height, x, width + 1);

        // Left
        Main.colorColumn(image, color, x, y, height);

        // Right
        Main.colorColumn(image, color, x + width, y, height);
    }

    public boolean[][] getValues() {
        return values;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isInBounds(int x, int y) {
        return x <= this.x + this.width
                && x >= this.x
                && y <= this.y + this.height
                && y >= this.y;
    }

    public boolean isOverlaping(SearchCharacter searchCharacter) {
        if (isInBounds(searchCharacter.getX(), searchCharacter.getY())) return true;
        if (isInBounds(searchCharacter.getX(), searchCharacter.getY() + searchCharacter.getHeight())) return true;
        if (isInBounds(searchCharacter.getX() + searchCharacter.getWidth(), searchCharacter.getY())) return true;
        if (isInBounds(searchCharacter.getX() + searchCharacter.getWidth(), searchCharacter.getY() + searchCharacter.getHeight())) return true;
        return false;
    }

    public boolean isInYBounds(int y) {
        return y <= this.y + this.height
                && y >= this.y;
    }

    public boolean isInXBounds(int x) {
        if (x <= this.x + this.width
                && x >= this.x) {
            return true;
        } else {
//            System.out.println(x + " is not between " + this.x + " and " + (this.x + this.width) + " (" + this.width  + ")");
            return false;
        }
    }

    public void applySections() {
//        System.out.println("===========================");
//        Main.printOut(this.values);
        AtomicInteger index = new AtomicInteger();
        Main.getHorizontalHalf(this.values)
                .flatMap(Main::getVerticalHalf)
                .forEach(section -> {
                    int i = index.getAndIncrement();
                    if (section == null) {
//                        addSegment(get2DFilledArray(true, 8 + 9, 1), 8 + 9);
                        addPlaceholder();
                        addPlaceholder();
                        return;
                    }

                    Main.getDiagonal(section, i == 1 || i == 2).forEach(this::addSegment);
                });

//        Main.getDiagonal(this.values, true).forEach((section, size) -> {
//            Main.getDiagonal(section, false).forEach((innerSection, innerSize) -> {
//                System.out.println("size = " + size + " inner = " + innerSize);
//            });
//        });

//        Main.printOut(values);
//        System.out.println("\n");

        Main.getHorizontalThird(this.values).forEach(values ->
                Main.getVerticalThird(values).forEach(nineth -> {
                    if (nineth == null) {
//                        addSegment(get2DFilledArray(true, 8 + 9, 1), 8 + 9);
                        addPlaceholder();
                        return;
                    }

                    addSegment(nineth, nineth.length * nineth[0].length);
                }));
    }

    private boolean[][] get2DFilledArray(boolean value, int width, int height) {
        boolean[][] ret = new boolean[height][];
        for (int y = 0; y < height; y++) {
            ret[y] = getFilledArray(value, width);
        }

        return ret;
    }

    private boolean[] getFilledArray(boolean value, int size) {
        boolean[] ret = new boolean[size];
        Arrays.fill(ret, value);
        return ret;
    }

    public void analyzeSlices() {
        AtomicInteger temp = new AtomicInteger();
        this.segmentPercentages = new double[8 + 9];
        this.segments.forEach((segment, size) -> {
            if (segment == null || size == -1) {
                this.segmentPercentages[temp.getAndIncrement()] = 1;
                return;
            }

            double amountTrue = Arrays.stream(segment)
                    .flatMap(array -> IntStream.range(0, array.length)
                            .mapToObj(i -> array[i]))
                    .filter(Boolean::booleanValue)
                    .count();

//            System.out.println("amountTrue = " + amountTrue + "/" + size);

            double val = size == 0 ? 1 : amountTrue / (double) size;
//            if (val != 1) System.out.println("\t\t\t\tNot 1!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//            this.segmentPercentages.add(val); // TODO: Not sure if 0.5 is the best
            this.segmentPercentages[temp.getAndIncrement()] = val;
        });

//        this.segmentPercentagesArray = this.segmentPercentages.stream().mapToDouble(t -> t).toArray();

//        System.out.println("\t\t\t\t\t\t\tsegmentPercentages = " + Arrays.toString(segmentPercentages));
    }

    public void addSegment(boolean[][] segment, int size) {
//        System.out.println("(" + width + "x" + height + ") Adding size: " + size + " = " + Arrays.deepToString(segment));
        this.segments.put(segment, size);
    }

    public void addPlaceholder() {
        this.segments.put(new boolean[0][], -1);
//        Map.Entry<boolean[][], Integer> first = this.segments.entrySet().stream().findFirst().orElseGet(() -> {
//            return new AbstractMap.SimpleEntry<>(get2DFilledArray(true, , ))
//        });
//        this.segments.put(get2DFilledArray(true, first.getKey()[0].length, first.getKey().length), first.getValue());
    }

    public Map<boolean[][], Integer> getSegments() {
        return this.segments;
    }

    public double[] getSegmentPercentages() {
//        return this.segmentPercentagesArray;
        return this.segmentPercentages;
    }

    /*
        public double getSimilarityWith(SearchCharacter searchCharacter) {
            double[] otherPercentages = searchCharacter.segmentPercentages;
            double[] differences = new double[8 + 9];
            for (int i = 0; i < 8; i++) {
                differences[i] = Math.max(this.segmentPercentages[i], otherPercentages[i]) - Math.min(otherPercentages[i], this.segmentPercentages[i]);
            }

            return 1 - Arrays.stream(differences).average().getAsDouble();
        }
    */
    public char getKnownChar() {
        return knownChar;
    }

    public void setKnownChar(char knownChar) {
        this.knownChar = knownChar;
    }

    public boolean hasDot() {
        return this.hasDot;
    }

    public void setHasDot(boolean hasDot) {
        this.hasDot = hasDot;
    }

    @Override
    public int compareTo(SearchCharacter searchCharacter) {
        return x - searchCharacter.x;
    }

    @Override
    public String toString() {
        return String.valueOf(knownChar);
    }

    public boolean[][] getData() {
        return values;
    }

    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }
}
