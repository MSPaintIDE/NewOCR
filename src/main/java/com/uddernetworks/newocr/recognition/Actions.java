package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.DatabaseCharacter;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.character.TrainedCharacterData;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.utils.IntPair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;

public interface Actions {

    /**
     * Gets the {@link SearchCharacter} characters found in the given {@link SearchImage}. This works by dividing it up
     * into lines, then horizontally. Each individual section then has vertical padding removed. Any 'characters' that
     * are 2x2 pixels or less are discarded. More information on this method can be found on page 55 of <a href=https://www.researchgate.net/publication/260405352_OPTICAL_CHARACTER_RECOGNITION_OCR_SYSTEM_FOR_MULTIFONT_ENGLISH_TEXTS_USING_DCT_WAVELET_TRANSFORM>this paper</a>.
     *
     * @param searchImage      The image to scan
     * @param searchCharacters The list that will have all of the {@link SearchCharacter}s added to
     */
    void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters);

    /**
     * Gets the {@link SearchCharacter}s found in training. This is different because it assumes that there are whole
     * lines to help group characters.
     *
     * @param searchImage The training image to scan
     * @return A collection of a list contianing the characters in a line
     */
    List<CharacterLine> getLettersDuringTraining(SearchImage searchImage);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database with line bounds for
     * improved accuracy.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @param lineBounds      The line bounds (Key/value is top/bottom Y values respectively) for improved accuracy
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, IntPair lineBounds);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @param data            The potential trained {@link TrainedCharacterData} to use
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database with line bounds for
     * improved accuracy.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @param data            The potential trained {@link TrainedCharacterData} to use
     * @param lineBounds      The line bounds (Key/value is top/bottom Y values respectively) for improved accuracy
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data, IntPair lineBounds);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @param diffs           The potential {@link ImageLetter}s
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database with line bounds for
     * improved accuracy.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @param diffs           The potential {@link ImageLetter}s
     * @param lineBounds      The line bounds (Key/value is top/bottom Y values respectively) for improved accuracy
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs, IntPair lineBounds);

    /**
     * Compares the ratios of the given width and heights
     *
     * @param width1  Character from image
     * @param height1 Character from image
     * @param width2  Character from database
     * @param height2 Character from database
     * @return The similarity of rectangles, lower being more similar
     */
    double compareSizes(double width1, double height1, double width2, double height2);

    /**
     * Gets the top and bottom line bounds found from the value 2D array. This is used for getting characters for
     * training data.
     *
     * @param image The image to get the line bounds from
     * @return A list of the absolute top and bottom line values
     */
    List<IntPair> getLineBoundsForTraining(SearchImage image);

}
