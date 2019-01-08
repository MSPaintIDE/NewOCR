package com.uddernetworks.newocr;

import com.uddernetworks.newocr.character.ImageLetter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * An object to store line data for {@link ImageLetter}s on a scanned image.
 */
public class ScannedImage {

    private transient File originalFile;
    private transient BufferedImage originalImage;
    private Map<Integer, List<ImageLetter>> grid = new LinkedHashMap<>();

    public ScannedImage(File originalFile, BufferedImage originalImage) {
        this.originalFile = originalFile;
        this.originalImage = originalImage;
    }

    /**
     * Gets the string of a scanned image
     * @return The string of a scanned image
     */
    public String getPrettyString() {
        StringBuilder stringBuilder = new StringBuilder();

        grid.values().forEach(line -> {
            line.stream()
                    .map(ImageLetter::getLetter)
                    .forEach(stringBuilder::append);
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
    }

    /**
     * Gets the first font size found in points.
     *
     * @param ocrHandle The OCRHandle used
     * @return The font size in points
     */
    public int getFirstFontSize(OCRHandle ocrHandle) {
        try {
            for (Map.Entry<Integer, List<ImageLetter>> entry : grid.entrySet()) {
                for (ImageLetter imageLetter : entry.getValue()) {
                    int size = ocrHandle.getFontSize(imageLetter).get();
                    if (size != -1) return size;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * gets the amount of lines in the image.
     * @return The amount of lines in the image
     */
    public int getLineCount() {
        return this.grid.size();
    }

    /**
     * Returns the raw, mutable grid of {@link ImageLetter}s internally used with the key of the mpa being the exact Y
     * position of the line.
     * @return The raw, mutable grid of values
     */
    public Map<Integer, List<ImageLetter>> getGrid() {
        return this.grid;
    }

    /**
     * Adds a line containing {@link ImageLetter}s
     * @param y The exact Y position of the line
     * @param databaseCharacterList A list of {@link ImageLetter}s as the line
     */
    public void addLine(int y, List<ImageLetter> databaseCharacterList) {
        grid.put(y, databaseCharacterList);
    }

    /**
     * Gets the line at the given index value.
     * @param y The index of the line
     * @return The line at the given index value
     */
    public List<ImageLetter> getLine(int y) {
        return grid.get(new ArrayList<>(grid.keySet()).get(y));
    }

    /**
     * Gets both the line Y and values at the given index value.
     * @param y The index of the line
     * @return The line at the given Y index value
     */
    public Map.Entry<Integer, List<ImageLetter>> getLineEntry(int y) {
        return new ArrayList<>(grid.entrySet()).get(y);
    }

    /**
     * Gets the original image scanned by the OCR.
     *
     * @return The original image, which may be null if pulled from caches
     */
    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    /**
     * Gets the original {@link File} scanned by the OCR.
     *
     * @return The original File, which may be null if pulled from caches
     */
    public File getOriginalFile() {
        return originalFile;
    }
}
