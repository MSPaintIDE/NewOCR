package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TrainImageGeneration {

    private static Logger LOGGER = LoggerFactory.getLogger(TrainImageGeneration.class);

    public static void main(String[] args) {
        LOGGER.info("Generating a training image with font bounds of 90-30, and a font family of Monospaced.plain");

        var start = System.currentTimeMillis();

        new ComputerTrainGenerator().generateTrainingImage(new File("training_mono.png"), new TrainGeneratorOptions()
                .setFontFamily("Monospaced.plain")
                .setMaxFontSize(90)
                .setMinFontSize(30));

        LOGGER.info("Finished in " + (System.currentTimeMillis() - start) + "ms");
    }

}
