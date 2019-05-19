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
import java.util.Optional;
import java.util.function.Function;

public enum ImageReadMethod implements Function<File, Optional<BufferedImage>> {
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
    IMAGE_ICON {
        @Override
        public Optional<BufferedImage> apply(File file) {
            try {
                return Optional.ofNullable(OCRUtils.readImage(file));
            } catch (IOException e) {
                LOGGER.error("Error reading file with ImageReadMethod.IMAGE_ICON", e);
                return Optional.empty();
            }
        }
    },
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
