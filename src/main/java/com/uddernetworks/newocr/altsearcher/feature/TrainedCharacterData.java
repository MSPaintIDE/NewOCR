package com.uddernetworks.newocr.altsearcher.feature;

import com.uddernetworks.newocr.altsearcher.SearchCharacter;

import java.util.Arrays;

public class TrainedCharacterData {

    private char value;
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

    public void recalculateTo(double[] segmentPercentages) {
        if (this.segmentPercentages == null) {
            this.segmentPercentages = segmentPercentages;
            return;
        }

        for (int i = 0; i < segmentPercentages.length; i++) {
            segmentPercentages[i] += (segmentPercentages[i] - this.segmentPercentages[i]) / 2;
//            System.out.println("Changed by " + ((segmentPercentages[i] - this.segmentPercentages[i]) / 2));
        }
    }

    public double getSimilarityWith(SearchCharacter searchCharacter) {
        double[] otherPercentages = searchCharacter.getSegmentPercentages();
        double[] differences = new double[8];
        for (int i = 0; i < 8; i++) {
            differences[i] = Math.max(this.segmentPercentages[i], otherPercentages[i]) - Math.min(otherPercentages[i], this.segmentPercentages[i]);
        }

        return 1 - Arrays.stream(differences).average().getAsDouble();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
