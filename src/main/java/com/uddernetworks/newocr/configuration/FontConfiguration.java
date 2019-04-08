package com.uddernetworks.newocr.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.mergence.rules.ApostropheMergeRule;
import com.uddernetworks.newocr.recognition.similarity.Letter;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.rules.BasicSimilarityRule;
import com.uddernetworks.newocr.train.OCROptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;

public class FontConfiguration {

    private static Logger LOGGER = LoggerFactory.getLogger(FontConfiguration.class);

    private String fileName;
    private Config config;
    private ReflectionCacher reflectionCacher;

    private String systemName;
    private String friendlyName;

    public FontConfiguration(String fileName, ReflectionCacher reflectionCacher) {
        this.fileName = fileName;
        this.config = ConfigFactory.load(fileName);
        this.reflectionCacher = reflectionCacher;

        var properties = this.config.atPath("language.properties");
        this.systemName = properties.getString("system-name");
        this.friendlyName = properties.getString("friendly-name");

        LOGGER.info("[{}] Loading font configuration...", this.friendlyName);
    }

    public OCROptions fetchOptions() {
        var options = this.config.atPath("language.options");
        var ocrOptions = new OCROptions();

        ocrOptions.setSpecialSpaces(options.getStringList("special-spaces")
                .stream()
                .map(string -> string.charAt(0))
                .collect(Collectors.toSet()));

        ocrOptions.setRequireSizeCheck(EnumSet.copyOf(options.getEnumList(Letter.class, "require-size-check")));

        ocrOptions.setMaxCorrectionIterations(options.getInt("max-correction-iterations"));
        ocrOptions.setMaxPercentDiffToMerge(options.getDouble("max-percent-diff-to-merge"));
        ocrOptions.setMaxPercentDistanceToMerge(options.getDouble("max-percent-distance-to-merge"));

        LOGGER.info("[{}] Generated OCROptions", this.friendlyName);

        return ocrOptions;
    }

    public void fetchAndApplySimilarities(SimilarityManager similarityManager) {
        var similarities = this.config.atPath("language.similarities");
        var entries = similarities.entrySet();
        entries.forEach(entry -> {
            var letters = EnumSet.copyOf(similarities.getEnumList(Letter.class, entry.getKey() + ".letters"));
            similarityManager.addSimilarity(new BasicSimilarityRule(letters));
        });

        LOGGER.info("[{}] Generated and added {} similarities...", this.friendlyName, entries.size());
    }

    public void fetchAndApplyMergeRules(MergenceManager mergenceManager) {
        var mergence = this.config.atPath("language.mergence");
        var ruleList = mergence.getStringList("rules");
        ruleList.forEach(className -> {
            loadClass(className).ifPresentOrElse(ruleClass -> {
                if (!ruleClass.isInstance(MergeRule.class)) {
                    return;
                }

                var clazz = ApostropheMergeRule.class;
                this.reflectionCacher.getOrLookupConstructor(clazz, () -> clazz.getConstructor(DatabaseManager.class, SimilarityManager.class)).ifPresentOrElse(constructor -> {
                        mergenceManager.addRule((databaseManager, similarityManager) -> {
                            try {
                                return constructor.newInstance(databaseManager, similarityManager);
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                LOGGER.error("[" + this.friendlyName + "] Error while creating an instance of " + className, e);
                                return null;
                            }
                        });
                }, () -> LOGGER.warn("[{}] No constructor found for {}", this.friendlyName, className));
            }, () -> LOGGER.warn("[{}] Couldn't find rule with name of {}", this.friendlyName, className));
        });

        LOGGER.info("[{}] Generated and added {} similarities...", this.friendlyName, ruleList.size());
    }

    private Optional<Class> loadClass(String className) {
        try {
            return Optional.ofNullable(Class.forName(className));
        } catch (ClassNotFoundException ignored) {}
        return Optional.empty();
    }

}
