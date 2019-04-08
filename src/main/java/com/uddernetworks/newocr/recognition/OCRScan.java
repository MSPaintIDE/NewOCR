package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;
import com.uddernetworks.newocr.train.OCROptions;
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

public class OCRScan implements Scan {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRScan.class);

    // This is the same as OCRTrain.TRAIN_STRING but without duplicates used in training
    public static final String RAW_STRING = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~W W";
    private DatabaseManager databaseManager;
    private Actions actions;
    private SimilarityManager similarityManager;
    private MergenceManager mergenceManager;

    public OCRScan(DatabaseManager databaseManager, OCROptions options) {
        this(databaseManager, options, new DefaultSimilarityManager().loadDefaults());
    }

    public OCRScan(DatabaseManager databaseManager, OCROptions options, SimilarityManager similarityManager) {
        this(databaseManager, options, similarityManager, new DefaultMergenceManager(databaseManager, similarityManager).loadDefaults());
    }

    public OCRScan(DatabaseManager databaseManager, OCROptions options, SimilarityManager similarityManager, MergenceManager mergenceManager) {
        this.databaseManager = databaseManager;
        this.mergenceManager = mergenceManager;
        this.similarityManager = similarityManager;
        ImageIO.setUseCache(false);

        this.actions = new OCRActions(databaseManager, similarityManager, options);
    }

    @Override
    public ScannedImage scanImage(File file) throws ExecutionException, InterruptedException {
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

        searchCharacters
                .stream()
                .map(actions::getCharacterFor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(ImageLetter::getX))
                .sorted((o1, o2) -> {
                    char cha = o1.getLetter();
                    char cha2 = o2.getLetter();

                    if (cha == cha2) return 0;
                    if (cha == ',' ^ cha2 == ',') return cha2 == ',' ? 1 : -1;
                    if (cha == '.' ^ cha2 == '.') return cha2 == '.' ? 1 : -1;
                    if (cha == '_' ^ cha2 == '_') return cha2 == '_' ? 1 : -1;
                    if (cha == '`' ^ cha2 == '`') return cha2 == '`' ? 1 : -1;
                    if (cha == '\'' ^ cha2 == '\'') return cha2 == '\'' ? 1 : -1;
                    if (cha == '"' ^ cha2 == '"') return cha2 == '"' ? 1 : -1;
                    if (cha == '*' ^ cha2 == '*') return cha2 == '*' ? 1 : -1;
                    return -1;
                })
                .forEach(imageLetter -> {

                    var center = imageLetter.getY() + ((double) imageLetter.getHeight() / 2);

                    // Get the place where it fits
                    lines.entrySet().stream().filter(entry -> {
                        var pair = entry.getKey();
                        var topX = pair.getKey(); // Less than bottom
                        var bottomX = pair.getValue();

                        return OCRUtils.isWithin(topX, bottomX, center);
                    }).findFirst().ifPresentOrElse(matchingPair -> {
                        matchingPair.getValue().add(imageLetter);
                    }, () -> LOGGER.warn("Found a letter not conforming to any bounds at (" + imageLetter.getX() + ", " + imageLetter.getY() + ") with a center Y of " + center));
                });

        // End ordering
        var sortedLines = new Int2ObjectLinkedOpenHashMap<List<ImageLetter>>();

        // Sorts the characters again based on their X value in their respective lines. This must be done again because
        // the two different lists (firstList and secondList) will have caused a mixup of X positions from normal
        // characters, and the ones in secondList

        lines.keySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry, (int) Math.round(((double) entry.getValue() - (double) entry.getKey()) / 2D + entry.getKey())))
                .sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getValue))
                .forEach(nestedEntry -> {
                    var linesEntry = nestedEntry.getKey();
                    int y = nestedEntry.getValue();

                    List<ImageLetter> databaseCharacters = lines.get(linesEntry);

                    if (databaseCharacters.isEmpty()) {
                        return;
                    }

                    databaseCharacters.sort(Comparator.comparingInt(ImageLetter::getX));
                    sortedLines.put(y, databaseCharacters);
                });

        this.mergenceManager.beginMergence(sortedLines, this.similarityManager);

        // Inserts all the spaces in the line. This is based on the first character of the line's height, and will be
        // derived from that font size.
        sortedLines.values().forEach(line -> line.addAll(getSpacesFor(line, line.stream().mapToInt(ImageLetter::getHeight).max().getAsInt())));

        // Sorts the lines again based on X values, to move spaces from the back to their proper locations in the line.

        ScannedImage scannedImage = new ScannedImage(file, input);

        sortedLines.keySet().stream().sorted().forEach(y -> {
            List<ImageLetter> line = sortedLines.get(y);
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
                var ratio = spaceRatio; // The ratio of the space DatabaseCharacter
                var usedWidth = ratio * fontSize; // The width of the space for this specific fot size
                usedWidth += spaceRatioOverride * fontSize;

                int spaces = '!' == searchCharacter.getLetter() ? (int) Math.floor(gap / usedWidth) : spaceRound(gap / usedWidth);

                for (int i = 0; i < spaces; i++) {
                    ret.add(new ImageLetter(' ', 0, (int) (leftX + (usedWidth * i)), searchCharacter.getY(), (int) usedWidth, fontSize, usedWidth, fontSize, ratio));
                }

                prev = searchCharacter;
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public MergenceManager getMergenceManager() {
        return this.mergenceManager;
    }

    @Override
    public int spaceRound(double input) {
        int known = (int) Math.floor(input);
        double extra = input % 1;
        known += OCRUtils.diff(extra, 1) < 0.2D ? 1 : 0;
        return known;
    }
}
