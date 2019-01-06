package com.uddernetworks.newocr;

import java.util.Arrays;
import java.util.Optional;

/**
 * Meta for letters that is inserted into the database to help distinguish characters.
 */
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

    /**
     * Gets the LetterMeta's ID
     * @return The ID
     */
    public int getID() {
        return id;
    }

    /**
     * Gets a {@link LetterMeta} from the given ID.
     * @param id The ID to get
     * @return The {@link LetterMeta} with the same ID as the one given
     */
    public static Optional<LetterMeta> fromID(int id) {
        return Arrays.stream(values()).filter(letterMeta -> letterMeta.id == id).findFirst();
    }
}
