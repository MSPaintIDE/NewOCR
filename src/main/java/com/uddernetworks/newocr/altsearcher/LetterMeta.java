package com.uddernetworks.newocr.altsearcher;

import java.util.Arrays;

public enum LetterMeta {
    NONE(0),
    EVEN_DOTS(1), // : =
    DOT_UNDER(2), // ! ?
    DOT_ABOVE(3), // ;
    PERCENT(4),   // %
    QUOTE(5);     // "

    private int id;

    LetterMeta(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public static LetterMeta fromID(int id) {
        return Arrays.stream(values()).filter(letterMeta -> letterMeta.id == id).findFirst().orElse(null);
    }
}
