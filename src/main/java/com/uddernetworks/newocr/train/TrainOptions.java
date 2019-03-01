package com.uddernetworks.newocr.train;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainOptions {
    private Set<Character> specialSpaces = new HashSet<>();
    private int maxCorrectionIterations = 10;

    public Set<Character> getSpecialSpaces() {
        return specialSpaces;
    }

    public TrainOptions setSpecialSpaces(char... specialSpaces) {
        this.specialSpaces = IntStream.range(0, specialSpaces.length)
                .mapToObj(x -> specialSpaces[x])
                .collect(Collectors.toSet());
        return this;
    }

    public int getMaxCorrectionIterations() {
        return maxCorrectionIterations;
    }

    public TrainOptions setMaxCorrectionIterations(int maxCorrectionIterations) {
        this.maxCorrectionIterations = maxCorrectionIterations;
        return this;
    }
}
