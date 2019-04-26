package com.uddernetworks.newocr.utils;

/**
 * Provides simple conversions of units used by the OCR and programs using the library.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class ConversionUtils {

    /**
     * Converts the given pixel to a point
     *
     * @param pixel the pixel to convert
     * @return The point value of the pixel
     */
    public static int pixelToPoint(int pixel) {
        return (int) Math.round(((double) pixel) / (4D / 3D));
    }

    /**
     * Converts the given point to a pixel
     *
     * @param point The point to convert
     * @return The pixel value of the point
     */
    public static int pointToPixel(int point) {
        return (int) Math.round(((double) point) * (4D / 3D));
    }

}
