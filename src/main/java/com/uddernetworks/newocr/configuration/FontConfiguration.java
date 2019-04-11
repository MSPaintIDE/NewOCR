package com.uddernetworks.newocr.configuration;

import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;

import java.util.Optional;

public interface FontConfiguration {

    // TODO: Docs

    OCROptions fetchOptions();

    void fetchAndApplySimilarities(SimilarityManager similarityManager);

    void fetchAndApplyMergeRules(MergenceManager mergenceManager);

    <T> Optional<Class<T>> loadClass(String className, Class<T> t);
}
