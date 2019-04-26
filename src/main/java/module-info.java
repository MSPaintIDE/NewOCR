module NewOCR {
    requires java.desktop;
    requires java.sql;

    requires com.zaxxer.hikari;
    requires it.unimi.dsi.fastutil;
    requires slf4j.api;
    requires typesafe.config;

    exports com.uddernetworks.newocr.character;
    exports com.uddernetworks.newocr.configuration;
    exports com.uddernetworks.newocr.database;
    exports com.uddernetworks.newocr.detection;
    exports com.uddernetworks.newocr.recognition;
    exports com.uddernetworks.newocr.recognition.mergence;
    exports com.uddernetworks.newocr.recognition.mergence.rules;
    exports com.uddernetworks.newocr.recognition.similarity;
    exports com.uddernetworks.newocr.recognition.similarity.rules;
    exports com.uddernetworks.newocr.train;
    exports com.uddernetworks.newocr.utils;
}