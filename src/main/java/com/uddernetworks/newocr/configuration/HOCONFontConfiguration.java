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
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class HOCONFontConfiguration implements FontConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(HOCONFontConfiguration.class);

    private String fileName;
    private Config config;
    private ReflectionCacher reflectionCacher;

    private String systemName;
    private String friendlyName;

    public HOCONFontConfiguration(String fileName, ReflectionCacher reflectionCacher) {
        this.fileName = fileName;
        this.config = ConfigFactory.load(fileName);
        this.reflectionCacher = reflectionCacher;

        var tt = this.config.getConfig("language.properties");
        this.systemName = tt.getString("system-name");
        this.friendlyName = tt.getString("friendly-name");

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
    public OCROptions fetchOptions() {
        var options = this.config.getConfig("language.options");
        var ocrOptions = new OCROptions();

        ocrOptions.setSpecialSpaces(options.getStringList("special-spaces")
                .stream()
                .map(string -> string.charAt(0))
                .collect(Collectors.toSet()));

        ocrOptions.setRequireSizeCheck(EnumSet.copyOf(options.getEnumList(Letter.class, "require-size-check")));

        ocrOptions.setMaxCorrectionIterations(options.getInt("max-correction-iterations"));
        ocrOptions.setMaxPercentDiffToMerge(options.getDouble("max-percent-diff-to-merge"));
        ocrOptions.setMaxPercentDistanceToMerge(options.getDouble("max-percent-distance-to-merge"));
        ocrOptions.setSizeRatioWeight(options.getDouble("size-ratio-weight"));

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
        LOGGER.info("[{}] Generated and added {} similarities...", this.friendlyName, ruleList.size());
    }

    @Override
    public Optional<Class<MergeRule>> loadMergeClass(String className) {
        try {
            var got = Class.forName(className);
            if (got.isInstance(MergeRule.class)) return Optional.of((Class<MergeRule>) got);
        } catch (ClassNotFoundException ignored) {}
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
