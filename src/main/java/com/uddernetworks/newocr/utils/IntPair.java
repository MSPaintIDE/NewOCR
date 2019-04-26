package com.uddernetworks.newocr.utils;

import java.util.Objects;

/**
 * A class that acts as an {@code int}-{@code int} tuple.
 *
 * @author Jacob G.
 * @since January 12, 2019
 */
public final class IntPair {

    /**
     * The key of this {@link IntPair}.
     */
    private int key;

    /**
     * The value of this {@link IntPair}.
     */
    private int value;

    /**
     * Creates a new {@link IntPair} with the specified key and value.
     *
     * @param key   the key.
     * @param value the value.
     */
    public IntPair(int key, int value) {
        this.key = key;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntPair)) {
            return false;
        }

        var pair = (IntPair) o;

        return key == pair.key && value == pair.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "IntPair[key = " + key + ", value = " + value + "]";
    }

    /**
     * Gets this {@link IntPair}'s key.
     *
     * @return the key as an {@code int}.
     */
    public int getKey() {
        return key;
    }

    /**
     * Sets this {@link IntPair}'s key.
     *
     * @param key The key as an {@code int}.
     */
    public void setKey(int key) {
        this.key = key;
    }

    /**
     * Gets this {@link IntPair}'s value.
     *
     * @return the value as an {@code int}.
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets this {@link IntPair}'s value.
     *
     * @param value The value as an {@code int}.
     */
    public void setValue(int value) {
        this.value = value;
    }

}
