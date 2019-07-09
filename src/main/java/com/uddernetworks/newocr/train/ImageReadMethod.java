package com.uddernetworks.newocr.train;

import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.utils.OCRUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

public enum ImageReadMethod implements Function<File, Optional<BufferedImage>> {
    /**
     * This read method simply uses {@link ImageIO#read(File)} to read the given file. This method does have unexpected
     * caching during quick and repetitive reading the same image in some scenarios.
     */
    IMAGEIO {
        @Override
        public Optional<BufferedImage> apply(File file) {
            try {
                return Optional.ofNullable(ImageIO.read(file));
            } catch (IOException e) {
                LOGGER.error("Error reading file with ImageReadMethod.IMAGEIO", e);
                return Optional.empty();
            }
        }
    },
    /**
     * This read method uses the {@link OCRUtils#readImage(File)} to read the given file. It is the fastest of all 3
     * methods, but has unexpected caching during quick and repetitive reading of the same image in some scenarios.
     */
    IMAGE_ICON {
        @Override
        public Optional<BufferedImage> apply(File file) {
            try {
                return Optional.of(OCRUtils.readImage(file));
            } catch (IOException e) {
                LOGGER.error("Error reading file with ImageReadMethod.IMAGE_ICON", e);
                return Optional.empty();
            }
        }
    },
    /**
     * This read method uses a {@link FileInputStream} along with {@link ImageIO#read(InputStream)} to read the mage.
     * This method is the slowest, but does not produce the unexpected caching that the other read methods have.
     */
    IMAGEIO_STREAM {
        @Override
        public Optional<BufferedImage> apply(File file) {
            try {
                try (var stream = new FileInputStream(file)) {
                    return Optional.ofNullable(ImageIO.read(stream));
                }
            } catch (IOException e) {
                LOGGER.error("Error reading file with ImageReadMethod.IMAGEIO_STREAM", e);
                return Optional.empty();
            }
        }
    };

    private static Logger LOGGER = LoggerFactory.getLogger(OCRScan.class);
}
