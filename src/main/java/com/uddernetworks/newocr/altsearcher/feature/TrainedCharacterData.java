package com.uddernetworks.newocr.altsearcher.feature;

import com.uddernetworks.newocr.altsearcher.SearchCharacter;
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

    public void recalculateTo(SearchCharacter searchCharacter) {
        this.empty = false;
        double[] segmentPercentages = searchCharacter.getSegmentPercentages();
//        if (this.segmentPercentages == null) {
//            this.segmentPercentages = segmentPercentages;
//            return;
//        }

//        double[] temp = new double[segmentPercentages.length]
//        if (!Main.ALL_INPUTS_EQUAL) {
//            for (int i = 0; i < segmentPercentages.length; i++) {
//                this.segmentPercentages[i] += (segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS; // Default 2
////            System.out.println((segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS);
////            System.out.println("Changed by " + ((segmentPercentages[i] - this.segmentPercentages[i]) / 2));
//            }
//        } else {
//            System.out.println("segmentPercentages = " + segmentPercentages.length);
            recalculatingList.add(segmentPercentages);
            if (searchCharacter.getWidth() != 0 && searchCharacter.getHeight() != 0) {
//                System.out.println(searchCharacter.getWidth() + " / " + searchCharacter.getHeight());
//                recalculatingRatios.add((double) searchCharacter.getWidth() / (double) searchCharacter.getHeight());
                recalculatingWidths.add((double) searchCharacter.getWidth());
                recalculatingHeights.add((double) searchCharacter.getHeight());
            }
//        }


//        System.out.println(Arrays.toString(this.segmentPercentages));
    }

    public void finishRecalculations() {
//        if (!Main.ALL_INPUTS_EQUAL) return;

        this.segmentPercentages = new double[8 + 9];
        for (int i = 0; i < 8 + 9; i++) {
            int finalI = i;
            OptionalDouble optionalDouble = recalculatingList.stream().mapToDouble(t -> t[finalI]).average();
            this.segmentPercentages[i] = optionalDouble.isPresent() ? optionalDouble.getAsDouble() : 0;
        }

        OptionalDouble widthAverageOptional = recalculatingWidths.stream().mapToDouble(t -> t).average();
        this.widthAverage = widthAverageOptional.isPresent() ? widthAverageOptional.getAsDouble() : 0D;

        OptionalDouble heightAverageOptional = recalculatingHeights.stream().mapToDouble(t -> t).average();
        this.heightAverage = heightAverageOptional.isPresent() ? heightAverageOptional.getAsDouble() : 0D;

        this.sizeRatio = this.heightAverage != 0 ? this.widthAverage / this.heightAverage : 0;

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
        if (minCenter == -1 && maxCenter == -1) {
            minCenter = center;
            maxCenter = center;
        } else {
            if (center > maxCenter) maxCenter = center;
            if (center < minCenter) minCenter = center;
        }
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
}
