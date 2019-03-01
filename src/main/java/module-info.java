module NewOCR {
    requires java.desktop;
    requires java.sql;

    requires com.zaxxer.hikari;
    requires it.unimi.dsi.fastutil;
    requires lept4j;
    requires slf4j.api;

    exports com.uddernetworks.newocr;
    exports com.uddernetworks.newocr.character;
    exports com.uddernetworks.newocr.database;
    exports com.uddernetworks.newocr.train;
    exports com.uddernetworks.newocr.utils;
}