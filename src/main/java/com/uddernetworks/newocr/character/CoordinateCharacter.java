package com.uddernetworks.newocr.character;

import com.uddernetworks.newocr.utils.IntPair;

import java.util.List;
import java.util.Objects;

/**
 * The superclass for characters containing data from the input image.
 */
public abstract class CoordinateCharacter extends Character {

    List<IntPair> coordinates;
    boolean[][] values;
    int amountOfMerges = 0;

    /**
     * Merges the given {@link CoordinateCharacter} with the current one, possibly changing width, height, X and Y
     * values, along with combining the current and given {@link CoordinateCharacter}'s coordinates and values
     * (Accessible via {@link CoordinateCharacter#getCoordinates()} and {@link CoordinateCharacter#getValues()}
     * respectively).
     *
     * @param character The {@link CoordinateCharacter} to merge into the current one
     */
    public void merge(CoordinateCharacter character) {
        this.amountOfMerges++;
        this.coordinates.addAll(character.coordinates);
        int maxX = Integer.MIN_VALUE, minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;

        for (var pair : this.coordinates) {
            int key = pair.getKey(), value = pair.getValue();

            if (key > maxX) {
                maxX = key;
            }

            if (key < minX) {
                minX = key;
            }

            if (value > maxY) {
                maxY = value;
            }

            if (value < minY) {
                minY = value;
            }
        }

        this.x = minX;
        this.y = minY;

        this.width = maxX - minX;
        this.height = maxY - minY;

        values = new boolean[this.height + 1][];

        for (int i = 0; i < values.length; i++) {
            values[i] = new boolean[width + 1];
        }

        this.coordinates.forEach(pair -> values[pair.getValue() - this.y][pair.getKey() - this.x] = true);
    }

    /**
     * Gets the coordinates of the character.
     *
     * @return The coordinates
     */
    public List<IntPair> getCoordinates() {
        return coordinates;
    }

    /**
     * Gets the black (true) and white (false) pixels of the scanned character.
     *
     * @return The grid of black or white values
     */
    public boolean[][] getValues() {
        return values;
    }

    /**
     * Sets the black (true) and white (false) pixels of the scanned character.
     *
     * @param values The grid of black or white values. Will return `null` for spaces
     */
    public void setValues(boolean[][] values) {
        this.values = values;
    }

    /**
     * Gets how many times the current {@link ImageLetter} has been merged via
     * {@link CoordinateCharacter#merge(CoordinateCharacter)} with another {@link ImageLetter}. This value is added
     * every time {@link CoordinateCharacter#merge(CoordinateCharacter)} is invoked, and adds the amount of merges the
     * argument of that method to the current merge value, as well as
     * incrementing normally.
     *
     * @return The amount of merge operations affecting the current {@link ImageLetter}
     */
    public int getAmountOfMerges() {
        return amountOfMerges;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.letter, this.x, this.y, this.width, this.height, this.coordinates, this.values, this.amountOfMerges);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CoordinateCharacter)) return false;
        var character = (CoordinateCharacter) obj;
        return character.letter == this.letter
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height
                && character.coordinates == this.coordinates
                && character.values == this.values
                && character.amountOfMerges == this.amountOfMerges;
    }

}
