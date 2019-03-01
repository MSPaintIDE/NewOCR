package com.uddernetworks.newocr.train;

import java.io.File;

public interface TrainGenerator {

    /**
     * Generates an image that can be used while training the OCR using default options of font bounds 90-30, and a font
     * family of Comic Sans MS.
     *
     * @param file The file to write to
     */
    void generateTrainingImage(File file);

    /**
     * Generates an image that can be used while training the OCR using the given options.
     *
     * @param file The file to write to
     * @param options The options used during image generation
     */
    void generateTrainingImage(File file, TrainGeneratorOptions options);
}
