package com.uddernetworks.newocr.character;

import java.util.Objects;

/**
 * The superclass of all characters.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public abstract class Character implements Comparable<Character> {

    // Coordinate data
    int x;
    int y;
    int width;
    int height;

    // Letter data
    char letter;
    int modifier;

    Character() {}

    Character(char letter) {
        this(letter, 0);
    }

    Character(char letter, int modifier) {
        this.letter = letter;
        this.modifier = modifier;
    }


    /**
     * Gets the X coordinate of the current {@link Character}.
     *
     * @return The X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the X coordinate of the current {@link Character}.
     *
     * @param x The X coordinate to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Gets the Y coordinate of the current {@link Character}.
     *
     * @return The Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the Y coordinate of the current {@link Character}.
     *
     * @param y The Y coordinate to set
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Gets the width of the current {@link Character}.
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the current {@link Character}.
     *
     * @param width The width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of the current {@link Character}.
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the current {@link Character}.
     *
     * @param height The height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the char letter of the current {@link Character}.
     *
     * @return The letter (Will default to '0' if not found for whatever reason)
     */
    public char getLetter() {
        return letter;
    }

    /**
     * Sets the char letter to the current {@link Character}.
     *
     * @param letter The letter to set
     */
    public void setLetter(char letter) {
        this.letter = letter;
    }

    /**
     * Gets the modifier for the character.
     *
     * @return The character's modifier
     */
    public int getModifier() {
        return modifier;
    }

    /**
     * Sets the modifier for the character.
     *
     * @param modifier The modifier to set
     */
    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    /**
     * Gets if another {@link Character} is overlapping the current {@link Character} at all in the X axis.
     *
     * @param searchCharacter The {@link Character} to check for overlapping
     * @return If the given {@link Character} is overlapping the current {@link Character}
     */
    public boolean isOverlappingX(Character searchCharacter) {
        // Thanks https://nedbatchelder.com/blog/201310/range_overlap_in_two_compares.html :)
        return getX() + getWidth() > searchCharacter.getX() && searchCharacter.getX() + searchCharacter.getWidth() > getX();
    }

    /**
     * Gets if another {@link Character} is overlapping the current {@link Character} at all in the X axis.
     *
     * @param searchCharacter The {@link Character} to check for overlapping
     * @return If the given {@link Character} is overlapping the current {@link Character}
     */
    public boolean isOverlappingY(Character searchCharacter) {
        // Thanks https://nedbatchelder.com/blog/201310/range_overlap_in_two_compares.html :)
        return getY() + getHeight() > searchCharacter.getY() && searchCharacter.getY() + searchCharacter.getHeight() > getY();
    }

    /**
     * Gets if the given coordinate is within the bounds of this character.
     *
     * @param x The X coordinate to check
     * @param y The Y coordinate to check
     * @return If the coordinate is within this character
     */
    public boolean isInBounds(int x, int y) {
        return x <= this.x + this.width
                && x >= this.x
                && y <= this.y + this.height
                && y >= this.y;
    }

    /**
     * Gets if the given Y position is within the Y bounds of the current character.
     *
     * @param y The Y position to check
     * @return If the given Y position is within the Y bounds of the current character
     */
    public boolean isInYBounds(int y) {
        return y <= this.y + this.height
                && y >= this.y;
    }

    /**
     * Gets if the given Y position is within the X bounds of the current character.
     *
     * @param x The Y position to check
     * @return If the given Y position is within the X bounds of the current character
     */
    public boolean isInXBounds(int x) {
        return x <= this.x + this.width
                && x >= this.x;
    }

    @Override
    public String toString() {
        return String.valueOf(this.letter);
    }

    @Override
    public int compareTo(Character searchCharacter) {
        return x - searchCharacter.x;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.letter, this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Character)) return false;
        var character = (Character) obj;
        return character.letter == this.letter
                && character.x == this.x
                && character.y == this.y
                && character.width == this.width
                && character.height == this.height;
    }
}
