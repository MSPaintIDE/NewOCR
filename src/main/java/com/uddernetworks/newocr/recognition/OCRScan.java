package com.uddernetworks.newocr.recognition;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.character.SearchCharacter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.mergence.MergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.utils.IntPair;
import com.uddernetworks.newocr.utils.OCRUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.utils.OCRUtils.diff;

public class OCRScan implements Scan {

    private static Logger LOGGER = LoggerFactory.getLogger(OCRScan.class);

    private DatabaseManager databaseManager;
    private Actions actions;
    private MergenceManager mergenceManager;

    public OCRScan(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        ImageIO.setUseCache(false);

        this.actions = new OCRActions(databaseManager, new DefaultSimilarityManager().loadDefaults());
        this.mergenceManager = new DefaultMergenceManager().loadDefaults(this.databaseManager);
    }

    @Override
    public ScannedImage scanImage(File file) throws ExecutionException, InterruptedException {
        var start = System.currentTimeMillis();

        // Preparing image
        var input = OCRUtils.readImage(file);
        var values = OCRUtils.createGrid(input);
        var searchCharacters = new ArrayList<SearchCharacter>();

        if (Boolean.getBoolean("newocr.rewrite")) {
            var temp = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
            OCRUtils.rewriteImage(temp, input);
            input = temp;
        }

        input = OCRUtils.filter(input).orElseThrow();

        try {
            ImageIO.write(input, "png", new File("E:\\NewOCR\\ind\\binz.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        OCRUtils.toGrid(input, values);

        var searchImage = new SearchImage(values);

        this.actions.getLetters(searchImage, searchCharacters);

        var i2 = new AtomicInteger();

        searchCharacters.stream()
                .sorted(Comparator.comparingInt(SearchCharacter::getX))
                .forEach(searchCharacter -> OCRUtils.makeImage(searchCharacter.getValues(), "ind2\\" + i2.getAndIncrement() + ".png"));
//        this.actions.getLettersDuringTraining(searchImage, searchCharacters);

        System.out.println("DONE");

        // Gets all needed character data from the database based on the currently used font sizes

        CompletableFuture.runAsync(() -> {
            try {
                databaseManager.getAllCharacterSegments().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        // Key = Entry<MinCenter, MaxCenter>  centers are ABSOLUTE
        Map<IntPair, List<ImageLetter>> lines = new LinkedHashMap<>();

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
                    double maxCenter = imageLetter.getMaxCenter();
                    double minCenter = imageLetter.getMinCenter();
                    boolean subtract = maxCenter < 0 && imageLetter.getMinCenter() < 0;
                    double centerDiff = subtract ?
                            maxCenter + minCenter :
                            maxCenter - minCenter;
                    // The tolerance of how far away a character can be from the line's center for it to be included
                    double tolerance = (int) Math.round(Math.max(Math.abs(centerDiff / 2 * 1.5), 2D));

                    int exactMin = (int) Math.round(imageLetter.getY() + minCenter);
                    int exactMax = (int) Math.round(imageLetter.getY() + maxCenter);

                    int exactTolerantMin = (int) Math.max(exactMin - tolerance, 0);
                    int exactTolerantMax = (int) (exactMax + tolerance);

                    int potentialY = (int) Math.round(imageLetter.getY() + centerDiff);

                    // Gets the nearest line and its Y value, if any
                    var possibleCenter = lines.keySet()
                            .stream()
                            .filter(centers -> {
                                int x1 = centers.getKey();
                                int y1 = centers.getValue();
                                int x2 = exactTolerantMin;
                                int y2 = exactTolerantMax;
                                return Math.max(y1, y2) - Math.min(x1, x2) < (y1 - x1) + (y2 - x2);
                            })
                            .min(Comparator.comparing(centers -> {
                                double min = centers.getKey();
                                double max = centers.getValue();
                                double centerBeginningY = ((max - min) / 2) + min;
                                return diff(centerBeginningY, potentialY);
                            }));

                    var center = possibleCenter.orElseGet(() -> {
                        var pair = new IntPair(exactTolerantMin, exactTolerantMax); // Included tolerance
                        lines.put(pair, new LinkedList<>());
                        return pair;
                    });

                    double ratio = imageLetter.getAverageWidth() / imageLetter.getAverageHeight();
                    double diff = Math.max(ratio, imageLetter.getRatio()) - Math.min(ratio, imageLetter.getRatio());

                    // This is signaled when the difference of the ratios are a value that is probably incorrect.
                    // If the ratio is very different, it should be looked into, as it could be from faulty detection.
                    if (diff > 0.2D) {
                        LOGGER.warn("Questionable ratio diff of " + diff + " on letter: " + imageLetter.getLetter() + " at (" + imageLetter.getX() + ", " + imageLetter.getY() + ")");
                    }

                    lines.get(center).add(imageLetter);
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

//        sortedLines.forEach((key, value) -> {
//            var i = 0;
//            for (ImageLetter imageLetter : value) {
//                OCRUtils.makeImage(imageLetter.getValues(), "E:\\NewOCR\\ind\\" + (i++) + ".png");
//            }
//            System.exit(0);
//        });

//        this.mergenceManager.beginMergence(sortedLines);

//        sortedLines.values().stream().flatMap(List::stream).forEach(searchCharacter -> OCRUtils.makeImage(searchCharacter.getValues(), "ind\\2character_" + searchCharacter.getX() + ".png"));

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
        known += OCRUtils.checkDifference(extra, 1, 0.2D) ? 1 : 0;
        return known;
    }
}
