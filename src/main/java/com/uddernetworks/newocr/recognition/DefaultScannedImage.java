package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * An object to store line data for {@link ImageLetter}s on a scanned image.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class DefaultScannedImage implements ScannedImage {

    private transient File originalFile;

    private transient BufferedImage originalImage;

    private final Int2ObjectMap<List<ImageLetter>> grid = new Int2ObjectLinkedOpenHashMap<>();

    public DefaultScannedImage(File originalFile, BufferedImage originalImage) {
        this.originalFile = originalFile;
        this.originalImage = originalImage;
    }

    @Override
    public String getPrettyString() {
        StringBuilder stringBuilder = new StringBuilder();

        grid.forEach((y, line) -> {
            line.stream()
                    .map(ImageLetter::getLetter)
                    .forEach(stringBuilder::append);
            stringBuilder.append("\n");
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    @Override
    public ScannedImage stripLeadingSpaces() {
        var commonSpaces = grid.values().stream().mapToInt(line -> {
            for (int i = 0; i < line.size(); i++) if (line.get(i).getLetter() != ' ') return i;
            return line.size();
        }).min().orElse(0);
        if (commonSpaces > 0)
            grid.values().forEach(values -> values.subList(0, commonSpaces).clear());
        return this;
    }

    @Override
    public Optional<ImageLetter> letterAt(int index) {
        var firstLineOptional = getGridLineAtIndex(0);
        if (firstLineOptional.isEmpty()) return Optional.empty();
        var last = firstLineOptional.get();

        var i = 0;
        while (last.size() + 1 <= index) {
            index -= last.size() + 1;
            var nextLine = getGridLineAtIndex(++i);
            if (nextLine.isEmpty()) break;
            last = nextLine.get();
        }

        return Optional.ofNullable(last.size() <= index ? null : last.get(index));
    }

    @Override
    public Optional<List<ImageLetter>> getGridLineAtIndex(int index) throws IndexOutOfBoundsException {
        return grid.keySet().stream().skip(index).limit(1).findFirst().map(t -> grid.get(t.intValue()));
    }

    @Override
    public int getLineCount() {
        return grid.size();
    }

    @Override
    public Int2ObjectMap<List<ImageLetter>> getGrid() {
        return grid;
    }

    @Override
    public void addLine(int y, List<ImageLetter> databaseCharacterList) {
        grid.put(y, databaseCharacterList);
    }

    @Override
    public List<ImageLetter> getLine(int y) {
        return grid.values().stream().skip(y).findFirst().orElse(null);
    }

    @Override
    public Int2ObjectMap.Entry<List<ImageLetter>> getLineEntry(int y) {
        return grid.int2ObjectEntrySet().stream().skip(y).findFirst().orElse(null);
    }

    @Override
    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    @Override
    public File getOriginalFile() {
        return originalFile;
    }

}
