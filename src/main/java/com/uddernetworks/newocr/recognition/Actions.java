package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.SearchImage;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseCharacter;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.List;
import java.util.Optional;

public interface Actions {

    /**
     * Gets the {@link SearchCharacter} characters found in the given {@link SearchImage}. This works by dividing it up
     * into lines, then horizontally. Each individual section then has vertical padding removed. Any 'characters' that
     * are 2x2 pixels or less are discarded. More information on this method can be found on page 55 of <a href=https://www.researchgate.net/publication/260405352_OPTICAL_CHARACTER_RECOGNITION_OCR_SYSTEM_FOR_MULTIFONT_ENGLISH_TEXTS_USING_DCT_WAVELET_TRANSFORM>this paper</a>.
     *
     * @param searchImage The image to scan
     * @param searchCharacters The list that will have all of the {@link SearchCharacter}s added to
     */
    void getLetters(SearchImage searchImage, List<SearchCharacter> searchCharacters);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, List<TrainedCharacterData> data);

    /**
     * Actually matches the {@link SearchCharacter} object to a real character from the database.
     *
     * @param searchCharacter The input {@link SearchCharacter} to match to
     * @return The {@link ImageLetter} object with the {@link DatabaseCharacter} inside it containing the found character
     */
    Optional<ImageLetter> getCharacterFor(SearchCharacter searchCharacter, Object2DoubleMap<ImageLetter> diffs);

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
}
