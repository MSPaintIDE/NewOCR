package com.uddernetworks.newocr.altsearcher.feature;

import com.uddernetworks.newocr.altsearcher.Main;
import com.uddernetworks.newocr.altsearcher.SearchCharacter;

import java.util.Arrays;

public class TrainedCharacterData {

    private char value;
    private boolean hasDot;
    private double[] segmentPercentages;

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

    public void recalculateTo(double[] segmentPercentages) {
        if (this.segmentPercentages == null) {
            this.segmentPercentages = segmentPercentages;
            return;
        }

        for (int i = 0; i < segmentPercentages.length; i++) {
            this.segmentPercentages[i] += (segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS; // Default 2
//            System.out.println((segmentPercentages[i] - this.segmentPercentages[i]) / Main.AFFECT_BACKWARDS);
//            System.out.println("Changed by " + ((segmentPercentages[i] - this.segmentPercentages[i]) / 2));
        }

//        System.out.println(Arrays.toString(this.segmentPercentages));
    }

    public double getSimilarityWith(SearchCharacter searchCharacter) {
        double[] otherPercentages = searchCharacter.getSegmentPercentages();
        double[] differences = new double[8 + 9];
        for (int i = 0; i < 8 + 9; i++) {
            differences[i] = Math.max(this.segmentPercentages[i], otherPercentages[i]) - Math.min(otherPercentages[i], this.segmentPercentages[i]);
        }

        return 1 - Arrays.stream(differences).average().getAsDouble();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
