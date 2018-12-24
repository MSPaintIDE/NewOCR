package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.utils.CharacterGettingUtils;

/**
 * Different ways of combining two {@link SearchCharacter}s.
 * @see CharacterGettingUtils
 */
public enum CombineMethod {
    DOT,
    COLON,
    PERCENTAGE_CIRCLE,
    APOSTROPHE
}