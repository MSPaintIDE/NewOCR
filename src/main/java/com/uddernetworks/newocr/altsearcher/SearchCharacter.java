package com.uddernetworks.newocr.altsearcher;

import com.uddernetworks.newocr.altsearcher.feature.Feature;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SearchCharacter implements Comparable<SearchCharacter> {

    private char knownChar;
    private boolean[][] values;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean hasDot;
    private Histogram histogram;
    private List<Feature> features = new ArrayList<>();
    private Map<boolean[][], Integer> segments = new LinkedHashMap<>();
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
        return width == height
                || width - 1 == height
                || width == height - 1;
    }

    public boolean isProbablyCircleOfPercent() {
        double ratio = (double) width / (double) height;
        return ratio <= 0.9 && ratio >= 0.7;
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

        // Top
        Main.colorRow(image, Color.MAGENTA, y, x, width);

        // Bottom
        Main.colorRow(image, Color.MAGENTA, y + height, x, width + 1);

        // Left
        Main.colorColumn(image, Color.MAGENTA, x, y, height);

        // Right
        Main.colorColumn(image, Color.MAGENTA, x + width, y, height);
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

    public Histogram getHistogram() {
        return histogram;
    }

    public void setHistogram(Histogram histogram) {
        this.histogram = histogram;
    }

    public boolean isInBounds(int x, int y) {
        return x <= this.x + this.width
                && x >= this.x
                && y <= this.y + this.height
                && y >= this.y;
    }

    public boolean isOverlaping(SearchCharacter searchCharacter) {
//        if (this.x > searchCharacter.x + searchCharacter.width || searchCharacter.x > this.x + this.width)
//            return false;

        // If one rectangle is above other
//        if (this.y < searchCharacter.y + searchCharacter.height || searchCharacter.y < this.y + this.height)
//            return false;

//        return true;
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

    public void addFeature(Feature feature) {
        features.add(feature);
    }

    // Returns % of completed features (<= 1)
    public double hasFeatures(List<Feature> features) {
        AtomicInteger completed = new AtomicInteger(0);
        features.stream().filter(feature -> feature.hasFeature(this.values)).forEach(t -> completed.getAndIncrement());
        return (double) completed.get() / (double) features.size();
    }

    public void applySections() {
//        System.out.println("===========================");
//        Main.printOut(this.values);
        AtomicInteger index = new AtomicInteger();
        Main.getHorizontalHalf(this.values)
                .flatMap(Main::getVerticalHalf)
                .forEach(section -> {
                    int i = index.getAndIncrement();
                    if (section == null) return;
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
                    if (nineth == null) return;
                    addSegment(nineth, nineth.length * nineth[0].length);
                }));
    }

    public void analyzeSlices() {
        AtomicInteger temp = new AtomicInteger();
        this.segments.forEach((segment, size) -> {
            double amountTrue = Arrays.stream(segment)
                    .flatMap(array -> IntStream.range(0, array.length)
                            .mapToObj(i -> array[i]))
                    .filter(Boolean::booleanValue)
                    .count();

            this.segmentPercentages[temp.getAndIncrement()] = size == 0 ? 0.5 : amountTrue / (double) size; // TODO: Not sure if 0.5 is the best
        });
    }

    public void addSegment(boolean[][] segment, int size) {
        this.segments.put(segment, size);
    }

    public Map<boolean[][], Integer> getSegments() {
        return this.segments;
    }

    public double[] getSegmentPercentages() {
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
}
