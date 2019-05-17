package com.uddernetworks.newocr.configuration;

import com.typesafe.config.*;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.rules.BasicSimilarityRule;
import com.uddernetworks.newocr.train.OCROptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reads a HOCON Configuration file and derives options from it.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class HOCONFontConfiguration implements FontConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(HOCONFontConfiguration.class);

    private String fileName;
    private Config config;
    private ReflectionCacher reflectionCacher;

    private String systemName;
    private String friendlyName;

    /**
     * Creates a {@link HOCONFontConfiguration} with a given file name and {@link ReflectionCacher} (Which should be
     * global across all instances of this current class). This also includes a {@link SimilarityManager} and a
     * {@link MergenceManager} to automatically invoke the methods
     * {@link FontConfiguration#fetchAndApplySimilarities(SimilarityManager)} and
     * {@link FontConfiguration#fetchAndApplyMergeRules(MergenceManager)} in their respective order.
     *
     * @param fileName          The name of the file
     * @param reflectionCacher  The {@link ReflectionCacher} to use
     * @param similarityManager The {@link SimilarityManager} to invoke
     *                          {@link FontConfiguration#fetchAndApplySimilarities(SimilarityManager)} on
     * @param mergenceManager   The {@link MergenceManager} to invoke
     *                          {@link FontConfiguration#fetchAndApplyMergeRules(MergenceManager)} on
     */
    public HOCONFontConfiguration(String fileName, ReflectionCacher reflectionCacher, SimilarityManager similarityManager, MergenceManager mergenceManager) {
        this(fileName, reflectionCacher);
        fetchAndApplySimilarities(similarityManager);
        fetchAndApplyMergeRules(mergenceManager);
    }

    /**
     * Creates a {@link HOCONFontConfiguration} with a given file name and {@link ReflectionCacher} (Which should be
     * global across all instances of this current class)
     *
     * @param fileName         The name of the file
     * @param reflectionCacher The {@link ReflectionCacher} to use
     */
    public HOCONFontConfiguration(String fileName, ReflectionCacher reflectionCacher) {
        this.fileName = fileName;
        this.config = ConfigFactory.load(fileName);
        this.reflectionCacher = reflectionCacher;

        var langProperties = this.config.getConfig("language.properties");
        this.systemName = langProperties.getString("system-name");
        this.friendlyName = langProperties.getString("friendly-name");

        LOGGER.info("[{}] Loading font configuration...", this.friendlyName);
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String getFriendlyName() {
        return this.friendlyName;
    }

    @Override
    public String getSystemName() {
        return this.systemName;
    }

    @Override
    public OCROptions fetchOptions(SimilarityManager similarityManager) {
        var options = this.config.getConfig("language.options");
        var ocrOptions = new OCROptions();

        ocrOptions.setSpecialSpaces(options.getStringList("special-spaces")
                .stream()
                .map(string -> string.charAt(0))
                .collect(Collectors.toSet()));

        ocrOptions.setMaxPercentDiffToMerge(options.getDouble("max-percent-diff-to-merge"));
        ocrOptions.setSizeRatioWeight(options.getDouble("size-ratio-weight"));

        var weights = this.config.getConfigList("language.ratio-weights");
        weights.forEach(item -> {
            var weight = item.getDouble("weight");
            var letterList = item.getEnumList(Letter.class, "letters");
            var ruleList = item.getStringList("similarities")
                    .stream()
                    .map(similarityManager::getRule)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            ocrOptions.addRatioWeights(letterList, weight);
            ocrOptions.addRatioWeightsFromRules(ruleList, weight);
        });

        LOGGER.info("[{}] Processed {} custom ratio weights...", this.friendlyName, weights.size());

        LOGGER.info("[{}] Generated OCROptions", this.friendlyName);

        return ocrOptions;
    }

    @Override
    public void fetchAndApplySimilarities(SimilarityManager similarityManager) {
        var similarities = this.config.getConfig("language.similarities");

        var entries = similarities.entrySet();

        var collected = entries.stream().collect(Collectors.groupingBy(t -> getNthPath(t.getKey(), 0)));

        collected.forEach((root, entryList) -> {
            var children = entryList.stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(getNthPath(entry.getKey(), 1), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var configList = (ConfigList) children.get("letters");

            var letters = configList.stream().map(value -> getEnumValue(Letter.class, value))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());

            similarityManager.addSimilarity(new BasicSimilarityRule((String) children.get("name").unwrapped(), letters));
        });

        LOGGER.info("[{}] Generated and added {} similarities...", this.friendlyName, entries.size());
    }

    @Override
    public void fetchAndApplyMergeRules(MergenceManager mergenceManager) {
        var mergence = this.config.getConfig("language.mergence");
        var ruleList = mergence.getStringList("rules");
        ruleList.forEach(className -> loadMergeClass(className).ifPresentOrElse(ruleClass ->
                this.reflectionCacher.getOrLookupConstructor(ruleClass, () -> ruleClass.getConstructor(DatabaseManager.class, SimilarityManager.class)).ifPresentOrElse(constructor ->
                        mergenceManager.addRule((databaseManager, similarityManager) -> {
                            try {
                                return constructor.newInstance(databaseManager, similarityManager);
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                LOGGER.error("[" + this.friendlyName + "] Error while creating an instance of " + className, e);
                                return null;
                            }
                        }), () -> LOGGER.warn("[{}] No constructor found for {}", this.friendlyName, className)), () -> LOGGER.warn("[{}] Couldn't find rule with name of {}", this.friendlyName, className)));
        LOGGER.info("[{}] Generated and added {} merge rules...", this.friendlyName, ruleList.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Class<MergeRule>> loadMergeClass(String className) {
        try {
            var got = Class.forName(className);
            if (got.getClass().isInstance(MergeRule.class)) return Optional.of((Class<MergeRule>) got);
        } catch (ClassNotFoundException ignored) {
        }
        return Optional.empty();
    }

    /**
     * Slightly adapted from SimpleConfig#getEnumValue(String, Class, ConfigValue)
     *
     * @param enumClass       The class of the enum
     * @param enumConfigValue The value of the enum in the config
     * @param <T>             The enum type
     * @return The enum value of the key
     */
    private <T extends Enum<T>> Optional<T> getEnumValue(Class<T> enumClass, ConfigValue enumConfigValue) {
        String enumName = (String) enumConfigValue.unwrapped();
        try {
            return Optional.of(Enum.valueOf(enumClass, enumName));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid enum value {} for enum {}", enumName, enumClass.getSimpleName());
            return Optional.empty();
        }
    }

    private String getNthPath(String pathString, int index) {
        var path = ConfigUtil.splitPath(pathString);
        if (path.size() < index + 1) return "null";
        return path.get(index);
    }

}
