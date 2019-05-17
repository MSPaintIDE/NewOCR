package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;
import com.uddernetworks.newocr.train.UntrainedDatabaseException;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * The base class for actually scanning an image.
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class OCRScan implements Scan {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRScan.class);

    // This is the same as OCRTrain.TRAIN_STRING but without duplicates used in training
    public static final String RAW_STRING = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;
    private Actions actions;
    private SimilarityManager similarityManager;
    private MergenceManager mergenceManager;

    /**
     * Creates a new {@link OCRScan} with a default {@link SimilarityManager} and {@link MergenceManager}.
     *
     * @param databaseManager The {@link DatabaseManager} to use
     * @param options         The {@link OCROptions} to use
     */
    public OCRScan(DatabaseManager databaseManager, OCROptions options) {
        this(databaseManager, options, new DefaultSimilarityManager().loadDefaults());
    }

    /**
     * Creates a new {@link OCRScan} with a default {@link MergenceManager}.
     *
     * @param databaseManager   The {@link DatabaseManager} to use
     * @param options           The {@link OCROptions} to use
     * @param similarityManager The {@link SimilarityManager} to use
     */
    public OCRScan(DatabaseManager databaseManager, OCROptions options, SimilarityManager similarityManager) {
        this(databaseManager, options, similarityManager, new DefaultMergenceManager(databaseManager, similarityManager).loadDefaults());
    }

    /**
     * Creates a new {@link OCRScan}.
     *
     * @param databaseManager   The {@link DatabaseManager} to use
     * @param options           The {@link OCROptions} to use
     * @param similarityManager The {@link SimilarityManager} to use
     * @param mergenceManager   The {@link MergenceManager} to use
     */
    public OCRScan(DatabaseManager databaseManager, OCROptions options, SimilarityManager similarityManager, MergenceManager mergenceManager) {
        this(databaseManager, similarityManager, mergenceManager, new OCRActions(similarityManager, databaseManager, options));
    }

    /**
     * Creates a new {@link OCRScan}.
     *
     * @param databaseManager   The {@link DatabaseManager} to use
     * @param similarityManager The {@link SimilarityManager} to use
     * @param mergenceManager   The {@link MergenceManager} to use
     * @param actions           The {@link Actions} to use
     */
    public OCRScan(DatabaseManager databaseManager, SimilarityManager similarityManager, MergenceManager mergenceManager, Actions actions) {
        this.databaseManager = databaseManager;
        this.mergenceManager = mergenceManager;
        this.similarityManager = similarityManager;
        this.actions = actions;
        ImageIO.setUseCache(false);
    }

    @Override
    public ScannedImage scanImage(File file) {

        if (!this.databaseManager.isTrainedSync()) throw new UntrainedDatabaseException(this.databaseManager);

        var start = System.currentTimeMillis();

        // Preparing image
        var input = OCRUtils.readImage(file);
        var values = OCRUtils.createGrid(input);
        var searchCharacters = new ArrayList<SearchCharacter>();

        input = OCRUtils.filter(input).orElseThrow();

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);


        // Moved from below
        // Key = Entry<MinCenter, MaxCenter>  centers are ABSOLUTE
        Map<IntPair, List<ImageLetter>> lines = new LinkedHashMap<>();

        this.actions.getLineBoundsForTraining(searchImage).forEach(pair -> lines.put(pair, new LinkedList<>()));
        this.actions.getLetters(searchImage, searchCharacters);

        // Gets all needed character data from the database based on the currently used font sizes

        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.getAllCharacterSegments().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        // Gets the closest matching character (According to the database values) using OCRActions#getCharacterFor(SearchCharacter),
        // then it orders them by their X values, and then sorts the ImageLetters so certain ones go first, allowing the
        // characters to go to the correct lines

        var sortedLines = new Int2ObjectLinkedOpenHashMap<List<ImageLetter>>();

        // New method: First orders SearchCharacters
        lines.keySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry, (int) Math.round(((double) entry.getValue() - (double) entry.getKey()) / 2D + entry.getKey())))
                .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
                .forEach(nestedEntry -> {
                    var linesEntry = nestedEntry.getKey();
                    int y = nestedEntry.getValue();

                    var databaseCharacters = lines.get(linesEntry);

                    searchCharacters.removeIf(searchCharacter -> {
                        var center = searchCharacter.getY() + ((double) searchCharacter.getHeight() / 2);
                        if (!OCRUtils.isWithin(linesEntry.getKey(), linesEntry.getValue(), center)) return false;
                        searchCharacter.setCenterOffset(center - searchCharacter.getY());
                        this.actions.getCharacterFor(searchCharacter, linesEntry).ifPresent(databaseCharacters::add);
                        return true;
                    });

                    if (databaseCharacters.isEmpty()) {
                        return;
                    }

                    databaseCharacters.sort(Comparator.comparingInt(ImageLetter::getX));
                    sortedLines.put(y, databaseCharacters);
                });

        this.mergenceManager.beginMergence(sortedLines, this.similarityManager);

        // Inserts all the spaces in the line. This is based on the first character of the line's height, and will be
        // derived from that font size.
        sortedLines.values().forEach(line -> line.stream().mapToInt(ImageLetter::getHeight).max().ifPresent(max -> line.addAll(getSpacesFor(line, max))));

        // Sorts the lines again based on X values, to move spaces from the back to their proper locations in the line.

        ScannedImage scannedImage = new DefaultScannedImage(file, input);

        sortedLines.keySet().stream().sorted().forEach(y -> {
            List<ImageLetter> line = sortedLines.get(y.intValue());
            scannedImage.addLine(y, line.stream().sorted(Comparator.comparingInt(ImageLetter::getX)).collect(Collectors.toList()));
        });

        LOGGER.debug("Finished in " + (System.currentTimeMillis() - start) + "ms");
        return scannedImage;
    }

    @Override
    public List<ImageLetter> getSpacesFor(List<ImageLetter> line, int fontSize) {
        var ret = new ArrayList<ImageLetter>();

        try {
            var data = databaseManager.getAllCharacterSegments().get();

            // Gets the space DatabaseCharacter used for the current font size from the database
            var spaceOptional = data.stream().filter(databaseCharacter -> databaseCharacter.getLetter() == ' ').findFirst();

            if (spaceOptional.isEmpty()) {
                LOGGER.error("No space found for current font size: " + fontSize);
                return line;
            }

            var space = spaceOptional.get();
            var spaceRatio = space.getAvgWidth() / space.getAvgHeight();

            ImageLetter prev = null;

            for (var searchCharacter : line) {
                var spaceRatioOverride = prev == null ? 0 : databaseManager.getCustomSpace(prev.getLetter()).get();
                int leftX = prev == null ? 0 : prev.getX() + prev.getWidth() + 1;
                int rightX = searchCharacter.getX();

                var gap = rightX - leftX; // The space between the current character and the last character
                var usedWidth = spaceRatio * fontSize; // The width of the space for this specific fot size
                usedWidth += spaceRatioOverride * fontSize;

                int spaces = '!' == searchCharacter.getLetter() ? (int) Math.floor(gap / usedWidth) : spaceRound(gap / usedWidth);

                for (int i = 0; i < spaces; i++) {
                    ret.add(new ImageLetter(' ', 0, (int) (leftX + (usedWidth * i)), searchCharacter.getY(), (int) usedWidth, fontSize, usedWidth, fontSize, spaceRatio));
                }

                prev = searchCharacter;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public int spaceRound(double input) {
        int known = (int) Math.floor(input);
        double extra = input % 1;
        known += OCRUtils.diff(extra, 1) < 0.2D ? 1 : 0;
        return known;
    }
}
