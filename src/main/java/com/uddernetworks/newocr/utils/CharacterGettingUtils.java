package com.uddernetworks.newocr.utils;

import com.uddernetworks.newocr.CombineMethod;
import com.uddernetworks.newocr.LetterMeta;
import com.uddernetworks.newocr.character.SearchCharacter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CharacterGettingUtils {

    /**
     * Gets the base of a character with a dot on top of it and combines it with the found character.
     * @param dotCharacter The dot character to search from
     * @param coordinates The coordinates used by the dotCharacter currently
     * @param searchCharacters The SearchCharacter list to check for the base
     * @return If a successful combination was made
     */
    public static boolean doDotStuff(SearchCharacter dotCharacter, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!dotCharacter.isProbablyDot()) return false;
        Optional<SearchCharacter> baseCharacterOptional = getBaseOfDot(searchCharacters, dotCharacter);
        baseCharacterOptional.ifPresent(baseCharacter -> {
            combine(baseCharacter, dotCharacter, coordinates, CombineMethod.DOT, LetterMeta.DOT_ABOVE);
            baseCharacter.setHasDot(true);
            dotCharacter.setHasDot(true);
        });

        return baseCharacterOptional.isPresent();
    }

    /**
     * Gets the base of the percent and adds the given circle/dot to it.
     * @param percentDotCharacter The dot character of the percentage
     * @param coordinates The coordinates used by the percentDotCharacter currently
     * @param searchCharacters The SearchCharacter list to check for the base
     * @return If a successful combination was made
     */
    public static boolean doPercentStuff(SearchCharacter percentDotCharacter, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!percentDotCharacter.isProbablyCircleOfPercent()) return false;
        Optional<SearchCharacter> baseCharacterOptional = getBaseForPercent(searchCharacters, percentDotCharacter);
        baseCharacterOptional.ifPresent(baseCharacter -> {
            combine(baseCharacter, percentDotCharacter, coordinates, CombineMethod.PERCENTAGE_CIRCLE, LetterMeta.PERCENT);
            baseCharacter.setHasDot(true);
            percentDotCharacter.setHasDot(true);
        });

        return baseCharacterOptional.isPresent();
    }

    /**
     * Gets the left apostrophe and adds the given left apostrophe with it.
     * @param rightApostrophe The apostrophe on the right side
     * @param coordinates The coordinates used by the rightApostrophe currently
     * @param searchCharacters The SearchCharacter list to check for the base
     * @return If a successful combination was made
     */
    public static boolean doApostropheStuff(SearchCharacter rightApostrophe, List<Map.Entry<Integer, Integer>> coordinates, List<SearchCharacter> searchCharacters) {
        if (!rightApostrophe.isProbablyApostraphe()) return false;
        Optional<SearchCharacter> leftApostropheOptional = getLeftApostrophe(searchCharacters, rightApostrophe);
        leftApostropheOptional.ifPresent(leftApostrophe -> {
            combine(leftApostrophe, rightApostrophe, coordinates, CombineMethod.APOSTROPHE, LetterMeta.QUOTE);
            leftApostrophe.setHasDot(true);
            rightApostrophe.setHasDot(true);
        });

        return leftApostropheOptional.isPresent();
    }

    /**
     * Combines a given {@link SearchCharacter} with another using one of several methods.
     * @param baseCharacter The {@link SearchCharacter} that will be added to
     * @param adding The {@link SearchCharacter} that will be added to the baseCharacter
     * @param coordinates The coordinates used by the `adding` parameter
     * @param combineMethod The method to be used when combining the characters. {@link CombineMethod#DOT} and {@link CombineMethod#COLON} do the same thing
     * @param letterMeta The {@link LetterMeta} to add to the base character
     */
    public static void combine(SearchCharacter baseCharacter, SearchCharacter adding, List<Map.Entry<Integer, Integer>> coordinates, CombineMethod combineMethod, LetterMeta letterMeta) {
        int minX = Math.min(baseCharacter.getX(), adding.getX());
        int minY = Math.min(baseCharacter.getY(), adding.getY());
        int maxX = Math.max(baseCharacter.getX() + baseCharacter.getWidth(), adding.getX() + adding.getWidth());
        int maxY = Math.max(baseCharacter.getY() + baseCharacter.getHeight(), adding.getY() + adding.getHeight());
        baseCharacter.setWidth(maxX - minX);
        baseCharacter.setHeight(maxY - minY);
        baseCharacter.setX(minX);
        baseCharacter.setY(minY);
        baseCharacter.setLetterMeta(letterMeta);

        switch (combineMethod) {
            case DOT:
            case COLON:
                maxX = baseCharacter.getX() + baseCharacter.getWidth();
                maxY = baseCharacter.getY() + baseCharacter.getHeight();
                baseCharacter.setHeight(maxY - adding.getY());
                baseCharacter.setY(adding.getY());

                int dotMaxX = adding.getX() + adding.getWidth();

                if (dotMaxX > maxX) {
                    baseCharacter.setWidth(dotMaxX - baseCharacter.getX());
                }

                baseCharacter.addDot(coordinates);
                break;
            case PERCENTAGE_CIRCLE:
                baseCharacter.addPercentageCircle(coordinates, OCRUtils.isWithin(adding.getY(), baseCharacter.getY(), (double) baseCharacter.getHeight() / 10D));
                break;
            case APOSTROPHE:
                baseCharacter.addPercentageCircle(coordinates, false);
                break;
        }

        coordinates.clear();
    }

    /**
     * Gets the base of character like i and j from a dot character
     * @param characters The list of {@link SearchCharacter}s to search from
     * @param dotCharacter The dot character to search from
     * @return The {@link SearchCharacter} base Optional
     */
    public static Optional<SearchCharacter> getBaseOfDot(List<SearchCharacter> characters, SearchCharacter dotCharacter) {
        return characters.parallelStream()
                .filter(character -> !character.equals(dotCharacter))
                .filter(character -> !character.hasDot())
                .filter(character -> character.isInBounds(dotCharacter.getX() + (dotCharacter.getWidth() / 2), character.getY() + 4))
                .filter(character -> character.getHeight() > dotCharacter.getHeight() * 5)
                .filter(baseCharacter -> {
                    int below = dotCharacter.getY() + dotCharacter.getHeight() + 1;

                    return OCRUtils.checkDifference(below, baseCharacter.getY(), dotCharacter.getHeight() + 2);
                })
                .findFirst();
    }

    /**
     * Gets the dot of a character like ! and ? from a base character
     * @param characters The list of {@link SearchCharacter}s to search from
     * @param baseCharacter The base character to search from
     * @return The {@link SearchCharacter} dot Optional
     */
    public static Optional<SearchCharacter> getDotUnderLetter(List<SearchCharacter> characters, SearchCharacter baseCharacter) {
        return characters.parallelStream()
                .filter(character -> !character.equals(baseCharacter))
                .filter(character -> !character.hasDot())
                .filter(SearchCharacter::isProbablyDot)
                .filter(character -> baseCharacter.isInBounds(character.getX() + (character.getWidth() / 2), baseCharacter.getY() + 4))
                .filter(character -> baseCharacter.getHeight() > character.getHeight() * 2)
                .filter(dotCharacter -> {
                    int below = dotCharacter.getY() - dotCharacter.getHeight();
                    int mod = dotCharacter.getHeight();
                    return OCRUtils.checkDifference(below, baseCharacter.getY() + baseCharacter.getHeight(), mod + 2);
                })
                .findFirst();
    }

    /**
     * Gets the bottom dot of a character like : and ; from its top dot
     * @param characters The list of {@link SearchCharacter}s to search from
     * @param topDot The bottom dot to search from
     * @return The {@link SearchCharacter} dot Optional
     */
    public static Optional<SearchCharacter> getBottomColon(List<SearchCharacter> characters, SearchCharacter topDot) {
        return characters.stream()
                .filter(character -> !character.equals(topDot))
                .filter(character -> !character.hasDot())
                .filter(character -> topDot.isInXBounds(character.getX() + (character.getWidth() / 2)))
                .filter(character -> {
                    double ratio = (double) topDot.getHeight() / (double) character.getHeight();
                    if (character.getWidth() * 2 < topDot.getWidth()) return false;
                    return (ratio >= 0.25 && ratio <= 0.5) || (topDot.getHeight() == character.getHeight() && topDot.getWidth() == character.getWidth());
                })
                .filter(bottomCharacter -> {
                    double mult = ((double) bottomCharacter.getWidth() / (double) bottomCharacter.getHeight() > 3 && Arrays.deepEquals(bottomCharacter.getValues(), topDot.getValues())) ? 5 : 5;
                    int mod = (int) (topDot.getHeight() * mult);

                    return OCRUtils.checkDifference(bottomCharacter.getY(), topDot.getY() + topDot.getHeight(), mod + 1);
                })
                .findFirst();
    }

    /**
     * Gets the left apostrophe from the given left apostrophe
     * @param characters The list of {@link SearchCharacter}s to search from
     * @param rightApostrophe The right apostrophe to search from
     * @return The {@link SearchCharacter} dot Optional
     */
    public static Optional<SearchCharacter> getLeftApostrophe(List<SearchCharacter> characters, SearchCharacter rightApostrophe) {
        return characters.parallelStream()
                .filter(SearchCharacter::isProbablyApostraphe)
                .filter(character -> character.getY() == rightApostrophe.getY())
                .filter(character -> {
                    boolean[][] values = character.getValues();
                    boolean[][] values2 = rightApostrophe.getValues();
                    if (values.length != values2.length || values[0].length != values2[0].length) return false;

                    double diff = OCRUtils.getDifferencesFrom2D(values, values2);
                    return diff <= 0.05; // If it's at least 5% similar
                })
                .filter(character -> OCRUtils.isWithin(character.getX() + character.getWidth(), rightApostrophe.getX(), rightApostrophe.getWidth() - 1D, ((double) rightApostrophe.getWidth() * 1.1D) + 4D))
                .findFirst();
    }

    /**
     * Gets the base character for the given percent circle/dot character
     * @param characters The list of {@link SearchCharacter}s to search from
     * @param circleOfPercent The circle/dot of the percent to search from
     * @return The {@link SearchCharacter} dot Optional
     */
    public static Optional<SearchCharacter> getBaseForPercent(List<SearchCharacter> characters, SearchCharacter circleOfPercent) {
        return characters.parallelStream()
                .filter(searchCharacter -> searchCharacter.isOverlaping(circleOfPercent))
                .findFirst();
    }
}
