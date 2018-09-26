package com.uddernetworks.newocr.altsearcher.feature;

import com.uddernetworks.newocr.altsearcher.Main;
import com.uddernetworks.newocr.altsearcher.SearchCharacter;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrainedCharacterData {

    private char value;
    private boolean hasDot;
    private double[] segmentPercentages;
    private double sizeRatio = -1; //        Width / Height

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
        this.hasDot = hasDot;
    }

    public double getSizeRatio() {
        return sizeRatio;
    }

    private List<double[]> recalculatingList = new ArrayList<>();
    private List<Double> recalculatingWidths = new ArrayList<>();
    private List<Double> recalculatingHeights = new ArrayList<>();

    public void recalculateTo(SearchCharacter searchCharacter) {
        double[] segmentPercentages = searchCharacter.getSegmentPercentages();
//        if (this.segmentPercentages == null) {
//            this.segmentPercentages = segmentPercentages;
//            return;
//        }

//        double[] temp = new double[segmentPercentages.length]
        if (!Main.ALL_INPUTS_EQUAL) {
            for (int i = 0; i < segmentPercentages.length; i++) {
                this.segmentPercentages[i] += (segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS; // Default 2
//            System.out.println((segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS);
//            System.out.println("Changed by " + ((segmentPercentages[i] - this.segmentPercentages[i]) / 2));
            }
        } else {
//            System.out.println("segmentPercentages = " + segmentPercentages.length);
            recalculatingList.add(segmentPercentages);
            if (searchCharacter.getWidth() != 0 && searchCharacter.getHeight() != 0) {
//                System.out.println(searchCharacter.getWidth() + " / " + searchCharacter.getHeight());
//                recalculatingRatios.add((double) searchCharacter.getWidth() / (double) searchCharacter.getHeight());
                recalculatingWidths.add((double) searchCharacter.getWidth());
                recalculatingHeights.add((double) searchCharacter.getHeight());
            }
        }


//        System.out.println(Arrays.toString(this.segmentPercentages));
    }

    public void finishRecalculations() {
        if (!Main.ALL_INPUTS_EQUAL) return;

        this.segmentPercentages = new double[8 + 9];
        for (int i = 0; i < 8 + 9; i++) {
            int finalI = i;
            this.segmentPercentages[i] = recalculatingList.stream().mapToDouble(t -> t[finalI]).average().getAsDouble();
        }

        this.sizeRatio = recalculatingWidths.stream().mapToDouble(t -> t).average().getAsDouble() / recalculatingHeights.stream().mapToDouble(t -> t).average().getAsDouble();
//        System.out.println("sizeRatio = " + sizeRatio);
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

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
