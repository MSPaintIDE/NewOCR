package com.uddernetworks.newocr.train;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainOptions {
    private Set<Character> specialSpaces = new HashSet<>();

    public Set<Character> getSpecialSpaces() {
        return specialSpaces;
    }

    public void setSpecialSpaces(char... specialSpaces) {
        this.specialSpaces = IntStream.range(0, specialSpaces.length)
                .mapToObj(x -> specialSpaces[x])
                .collect(Collectors.toSet());
    }
}
