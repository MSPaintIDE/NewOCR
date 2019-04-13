package com.uddernetworks.newocr.configuration;

import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;

import java.util.Optional;

public interface FontConfiguration {

    /**
     * Gets the file name the {@link HOCONFontConfiguration} was generated from.
     *
     * @return The file name
     */
    String getFileName();

    /**
     * The friendly version of the font name that should be displayed in messages/options.
     *
     * @return The friendly font name
     */
    String getFriendlyName();

    /**
     * Gets the font name used by the OS to define it. This shouldn't really be displayed to the user, unless the
     * specific application requires it.
     *
     * @return The font name by the OS
     */
    String getSystemName();

    /**
     * Generates and returns the {@link OCROptions} from the file configuration.
     *
     * @return The {@link OCROptions} from the settings
     */
    OCROptions fetchOptions();

    /**
     * Fetches the similarities' settings from the configuration and applies them to the given
     * {@link SimilarityManager}. All similarities will be an instance of
     * {@link com.uddernetworks.newocr.recognition.similarity.rules.BasicSimilarityRule}.
     *
     * @param similarityManager The {@link SimilarityManager} to apply all similarities to
     */
    void fetchAndApplySimilarities(SimilarityManager similarityManager);

    /**
     * Fetches the mergence rules' settings from the configuration and applies them to the given
     * {@link MergenceManager}.
     *
     * @param mergenceManager The {@link MergenceManager} to apply all mergence rules to.
     */
    void fetchAndApplyMergeRules(MergenceManager mergenceManager);

    /**
     * Loads a class that's an instance of {@link MergeRule} from the given fully qualified name .
     *
     * @param className The fully qualified name of the class to load
     * @return The class, if found
     */
    Optional<Class<MergeRule>> loadMergeClass(String className);
}
