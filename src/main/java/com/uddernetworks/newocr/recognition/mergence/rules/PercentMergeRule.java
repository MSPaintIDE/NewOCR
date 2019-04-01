package com.uddernetworks.newocr.recognition.mergence.rules;

import com.uddernetworks.newocr.character.ImageLetter;
import com.uddernetworks.newocr.database.DatabaseManager;
import com.uddernetworks.newocr.recognition.mergence.MergePriority;
import com.uddernetworks.newocr.recognition.mergence.MergeRule;
import com.uddernetworks.newocr.recognition.similarity.SimilarityManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.uddernetworks.newocr.recognition.similarity.Letter.*;

/**
 * Merges all pieces of a percent sign
 */
public class PercentMergeRule extends MergeRule {

    public PercentMergeRule(DatabaseManager databaseManager, SimilarityManager similarityManager) {
        super(databaseManager, similarityManager);
    }

    @Override
    public boolean isHorizontal() {
        return true;
    }

    @Override
    public MergePriority getPriority() {
        return MergePriority.HIGH;
    }

    // TODO: REMOVE THIS
    private static boolean first = true;

    @Override
    public Optional<List<ImageLetter>> mergeCharacters(ImageLetter target, List<ImageLetter> letterData) {
        System.out.println("====================================");
        var baseIndex = letterData.indexOf(target);

        if (baseIndex - 1 < 0 || baseIndex + 1 >= letterData.size()) return Optional.empty();

        var part1 = letterData.get(baseIndex - 1);
        var part2 = letterData.get(baseIndex + 1);

//        System.out.println("target = " + target + " (" + target.getX() + ", " + target.getY() + ") wh = " + target.getWidth() + "x" + target.getHeight());
//        System.out.println("part1 = " + part1 + " (" + part1.getX() + ", " + part1.getY() + ") wh = " + part1.getWidth() + "x" + part1.getHeight());
//        System.out.println("part2 = " + part2 + " (" + part2.getX() + ", " + part2.getY() + ") wh = " + part2.getWidth() + "x" + part2.getHeight());

        if (target.getAmountOfMerges() > 0 || part1.getAmountOfMerges() > 0 || part2.getAmountOfMerges() > 0) return Optional.empty();

        // Make the most non-square piece last

        var pieces = new ArrayList<>(List.of(target, part1, part2));
        var sorted = pieces.stream()
                .sorted(Comparator.comparingDouble(letter -> (double) Math.max(letter.getWidth(), letter.getHeight()) / (double) Math.min(letter.getWidth(), letter.getHeight())))
                .collect(Collectors.toList());

        var base = sorted.get(2);
        var dot1 = sorted.get(0);
        var dot2 = sorted.get(1);

//        System.out.println("SORTED!");
//
//        System.out.println("base = " + base + " (" + base.getX() + ", " + base.getY() + ") wh = " + base.getWidth() + "x" + base.getHeight());
//        System.out.println("dot1 = " + dot1 + " (" + dot1.getX() + ", " + dot1.getY() + ") wh = " + dot1.getWidth() + "x" + dot1.getHeight());
//        System.out.println("dot2 = " + dot2 + " (" + dot2.getX() + ", " + dot2.getY() + ") wh = " + dot2.getWidth() + "x" + dot2.getHeight());


//        if (dots.size() != 2) return Optional.empty();

//        pieces.removeAll(dots);
//        var base = pieces.get(0);
        if (!isBase(base) || !isDot(dot1) || !isDot(dot2)) {
//            System.out.println("111 " + isBase(base) + " " + isDot(dot1) + " " + isDot(dot2));
            return Optional.empty();
        }

        if (!base.isOverlappingY(dot1) || !base.isOverlappingY(dot2)) {
//            System.out.println("222");
            return Optional.empty();
        }

//        if (first) {
//            String dir = "2";
//            OCRUtils.makeImage(base.getValues(), "ind\\" + dir + "\\base.png");
//            OCRUtils.makeImage(dot1.getValues(), "ind\\" + dir + "\\dot1.png");
//            OCRUtils.makeImage(dot2.getValues(), "ind\\" + dir + "\\dot2.png");
//            System.exit(0);
//        }

        base.merge(dot1);
        base.merge(dot2);

        base.setModifier(0);
        base.setLetter('%');

        sorted.remove(base);

        first = false;

        return Optional.of(sorted);
    }

    private boolean isDot(ImageLetter imageLetter) {
        return PERCENT_LDOT.matches(imageLetter) || PERCENT_RDOT.matches(imageLetter) || o.matches(imageLetter);
    }

    private boolean isBase(ImageLetter imageLetter) {
        return PERCENT_BASE.matches(imageLetter) || FORWARD_SLASH.matches(imageLetter);
    }
}
