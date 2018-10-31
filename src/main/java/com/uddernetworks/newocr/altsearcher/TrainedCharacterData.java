package com.uddernetworks.newocr.altsearcher;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

public class TrainedCharacterData {

    private char value;
    private boolean hasDot;
    private double widthAverage;
    private double heightAverage;
    private double[] segmentPercentages;
    private double minCenter = -1;
    private double maxCenter = -1;
    private double sizeRatio = -1; //        Width / Height
    private boolean empty = true;
    private LetterMeta letterMeta = LetterMeta.NONE;

    public TrainedCharacterData(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public double[] getSegmentPercentages() {
        return this.segmentPercentages;
    }

    public void setSegmentPercentages(double[] segmentPercentages) {
        this.segmentPercentages = segmentPercentages;
    }

    public boolean hasDot() {
        return this.hasDot;
    }

    public void setHasDot(boolean hasDot) {
        if (!this.hasDot) this.hasDot = hasDot;
    }

    public double getSizeRatio() {
        return sizeRatio;
    }

    private List<double[]> recalculatingList = new ArrayList<>();
    private List<Double> recalculatingWidths = new ArrayList<>();
    private List<Double> recalculatingHeights = new ArrayList<>();
    private List<Double> recalculatingCenters = new ArrayList<>();

    public void recalculateTo(double width, double height) {
        this.empty = false;

        recalculatingWidths.add(width);
        recalculatingHeights.add(height);
    }

    public void recalculateTo(SearchCharacter searchCharacter) {
        this.empty = false;
        double[] segmentPercentages = searchCharacter.getSegmentPercentages();

        if (value == '-') {
            System.out.println("----------------------------------------------");
            System.out.println("111 Dash");

            System.out.println(Arrays.toString(segmentPercentages));
            System.out.println("Set: " + searchCharacter.segments.size());

            System.out.println("----------------------------------------------");
        }

        recalculatingList.add(segmentPercentages);
        if (searchCharacter.getWidth() != 0 && searchCharacter.getHeight() != 0) {
            recalculatingWidths.add((double) searchCharacter.getWidth());
            recalculatingHeights.add((double) searchCharacter.getHeight());
        }
    }

    public void finishRecalculations() {
        OptionalDouble widthAverageOptional = recalculatingWidths.stream().mapToDouble(t -> t).average();
        this.widthAverage = widthAverageOptional.isPresent() ? widthAverageOptional.getAsDouble() : 0D;

        OptionalDouble heightAverageOptional = recalculatingHeights.stream().mapToDouble(t -> t).average();
        this.heightAverage = heightAverageOptional.isPresent() ? heightAverageOptional.getAsDouble() : 0D;

        this.sizeRatio = this.heightAverage != 0 ? this.widthAverage / this.heightAverage : 0;

        if (value == ' ') return;

//        if (value == '-') {
//            System.out.println("Dash = ");

//        }

        this.segmentPercentages = new double[8 + 9];
        for (int i = 0; i < 8 + 9; i++) {
            int finalI = i;
            OptionalDouble optionalDouble = recalculatingList.stream().mapToDouble(t -> t[finalI]).average();
            this.segmentPercentages[i] = optionalDouble.isPresent() ? optionalDouble.getAsDouble() : 0;
        }

        if (value == '-') {
            System.out.println("=======================================");

            System.out.println("222 Dash");
            for (double[] doubles : recalculatingList) {
                System.out.println(Arrays.toString(doubles));
            }

            System.out.println("segmentPercentages = " + Arrays.toString(segmentPercentages));

            System.out.println("=======================================");
        }

//        this.center = this.recalculatingCenters.stream().mapToDouble(t -> t).average().orElse(0);
    }

    public Pair<Double, Double> getSimilarityWith(SearchCharacter searchCharacter) {
        double[] otherPercentages = searchCharacter.getSegmentPercentages();
        double[] differences = new double[8 + 9];
        for (int i = 0; i < 8 + 9; i++) {
            differences[i] = Math.max(this.segmentPercentages[i], otherPercentages[i]) - Math.min(otherPercentages[i], this.segmentPercentages[i]);
        }

        double checkingRatio = ((double) searchCharacter.getWidth() / (double) searchCharacter.getHeight());
        double ratioDifference = Math.max(checkingRatio, this.sizeRatio) - Math.min(checkingRatio, this.sizeRatio);
//        System.out.println(value + "] " + Math.max(checkingRatio, this.sizeRatio) + " - " + Math.min(checkingRatio, this.sizeRatio));
//        System.out.println(value + "] " + ratioDifference);

        return new Pair<>(1 - Arrays.stream(differences).average().getAsDouble(), ratioDifference); // / ratioDifference
//        return ratioDifference;
    }

    public double getWidthAverage() {
        return widthAverage;
    }

    public double getHeightAverage() {
        return heightAverage;
    }

//    public double getCenter() {
//        return center;
//    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public void recalculateCenter(double center) {
//        if (center < 0) {
//            System.out.println("Pushing bad center: " + center);
//            System.out.println("PRE min: " + minCenter + " max: " + maxCenter);
//        }

        if (minCenter == -1 && maxCenter == -1) {
            minCenter = center;
            maxCenter = center;
        } else {
            if (center > maxCenter) maxCenter = center;
            if (center < minCenter) minCenter = center;
        }

//        if (center < 0) {
//            System.out.println("POST min: " + minCenter + " max: " + maxCenter);
//        }
    }

    public double getMinCenter() {
        return minCenter;
    }

    public double getMaxCenter() {
        return maxCenter;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public LetterMeta getLetterMeta() {
        return letterMeta;
    }

    public void setLetterMeta(LetterMeta letterMeta) {
        this.letterMeta = letterMeta;
    }
}
