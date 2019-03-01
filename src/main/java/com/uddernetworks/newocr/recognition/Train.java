package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.TrainOptions;
import com.uddernetworks.newocr.train.TrainedCharacterData;
import com.uddernetworks.newocr.utils.IntPair;

import java.io.File;
import java.util.List;

public interface Train {

    /**
     * Scans the input image and creates training data based off of it. It must be an input image created from
     * {@link ComputerTrainGenerator} or something of a similar format.
     *
     * @param file The input image to be trained from
     */
    void trainImage(File file);

    /**
     * Scans the input image and creates training data based off of it. It must be an input image created from
     * {@link ComputerTrainGenerator} or something of a similar format.
     *
     * @param file    The input image to be trained from
     * @param options The options used by the system
     */
    void trainImage(File file, TrainOptions options);

    /**
     * Gets the amount of false detections for a given character using the given training data.
     *
     * @param charData A 2D List of {@link SearchCharacter}s containing the data to compare with
     * @param trainedCharacterDataList A list of {@link TrainedCharacterData} to be tested
     * @param checking The actual character to keep track of
     * @return The amount of false detections/problems with detecting the given character
     */
    int getErrorsForCharacter(List<List<SearchCharacter>> charData, List<TrainedCharacterData> trainedCharacterDataList, char checking);

    /**
     * Gets the {@link TrainedCharacterData} with the known letter value of the given character, with the same modifier.
     * If a character is not found, it will be created and added to the given list.
     *
     * @param trainedCharacterDataList The list of {@link TrainedCharacterData}s to search though
     * @param current The character to find
     * @param finalModifier The modifier for the character to find
     * @return The {@link TrainedCharacterData} with the same character and modifier
     */
    TrainedCharacterData getTrainedCharacter(List<TrainedCharacterData> trainedCharacterDataList, char current, int finalModifier);

    /**
     * Gets the top and bottom line bounds found from the value 2D array. This is used for getting characters for
     * training data.
     *
     * @param values The 2D array of values derived from the image to check from
     * @return A list of the absolute top and bottom line values
     */
    List<IntPair> getLineBoundsForTesting(boolean[][] values);
}
