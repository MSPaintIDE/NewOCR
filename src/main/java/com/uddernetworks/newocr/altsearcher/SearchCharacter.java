package com.uddernetworks.newocr.altsearcher;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SearchCharacter implements Comparable<SearchCharacter> {

    private char knownChar = '?';
    private boolean[][] values;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean hasDot;
    private LetterMeta letterMeta = LetterMeta.NONE;
    private List<Map.Entry<Integer, Integer>> segments = new LinkedList<>();
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
        return diff <= 3;
    }

    public boolean isProbablyCircleOfPercent() {
        double ratio = (double) width + 1 / (double) height + 1;
        return ratio <= 0.9 && ratio >= 0.7;
    }

    public boolean isProbablyApostraphe() {
        double ratio = (double) width / (double) height;
        return (ratio <= 0.375 && ratio >= 0.166) || (width == 1 && (height == 4 || height == 5));
    }

    public boolean isProbablyColon() {
        double ratio = (Math.min(this.width, this.height) + 1D) / (Math.max(this.width, this.height) + 1D);
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
            return false;
        }
    }

    public void applySections() {
        AtomicInteger index = new AtomicInteger();
        OCRUtils.getHorizontalHalf(this.values)
                .flatMap(OCRUtils::getVerticalHalf)
                .forEach(section -> OCRUtils.getDiagonal(section, index.get() == 1 || index.getAndIncrement() == 2).forEach(this::addSegment));

        OCRUtils.getHorizontalThird(this.values).forEach(values ->
                OCRUtils.getVerticalThird(values).forEach(this::addSegment));
    }

    public void analyzeSlices() {
        AtomicInteger temp = new AtomicInteger();
        this.segmentPercentages = new double[segments.size()];
        this.segments.forEach((entry) -> {
            double amountTrue = entry.getKey();
            double total = entry.getValue();

            double val = total == 0 ? 1 : amountTrue / total;

            this.segmentPercentages[temp.getAndIncrement()] = val;
        });
    }

    public void addSegment(Map.Entry<Integer, Integer> entry) {
        this.segments.add(entry);
    }

    public double[] getSegmentPercentages() {
        return this.segmentPercentages;
    }

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

    public List<Map.Entry<Integer, Integer>> getSegments() {
        return segments;
    }
}
