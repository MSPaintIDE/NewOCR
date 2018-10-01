package com.uddernetworks.newocr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    private static short[][] values;
    private static BufferedImage input;
    private static int outputIndex = 0;

    public static void main(String[] args) throws IOException {
        input = ImageIO.read(new File("E:\\NewOCR\\training.png"));
        values = createGrid(input);

        // Pre-filter

        filter(input);

        // End pre-filters

        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                values[arrY][arrX++] = (short) (255 - getBlackness(x, y));
            }

            arrX = 0;
            arrY++;
        }


        ParsingImage parsingImage = new ParsingImage(input, values);
        parsingImage.parseLines();
        parsingImage.graphLines();
//        parsingImage.getLines().add(new ParsingLine(parsingImage, 3, 21));

        parsingImage.getLines().forEach(ParsingLine::parseCharacterLRB);
//        parsingImage.getLines().forEach(ParsingLine::graphLR);

        System.out.println("Total lines: " + parsingImage.getLines().size());

        ImageIO.write(input, "png", new File("E:\\NewOCR\\step1.png"));

        parsingImage.getLines().forEach(ParsingLine::parseCharacterTBB);
//        parsingImage.getLines().forEach(ParsingLine::graphTB);

        parsingImage.getLines().forEach(ParsingLine::graphCharacterBoundingBox);

        ImageIO.write(input, "png", new File("E:\\NewOCR\\step3.png"));
    }

    public static void colorRow(BufferedImage image, Color color, int y) {
        for (int x = 0; x < image.getWidth(); x++) {
            image.setRGB(x, y, color.getRGB());
        }
    }

    public static void colorRow(BufferedImage image, Color color, int y, int x, int width) {
        for (int x2 = 0; x2 < width; x2++) {
            image.setRGB(x2 + x, y, color.getRGB());
        }
    }

    public static void colorColumn(BufferedImage image, Color color, int x, int y, int height) {
        for (int y2 = 0; y2 < height; y2++) {
            image.setRGB(x, y + y2, color.getRGB());
        }
    }

    public static boolean isRowPopulated(short[][] values, int y) {
        for (int x = 0; x < values[y].length; x++) {
            if (values[y][x] == 255) return true; // 255 displays as 0 in printing
        }

        return false;
    }

    private static short[] lastColumn;

    public static void resetLastColumn() {
        lastColumn = null;
    }

    public static boolean isColumnPopulated(short[][] values, int x) {
        short[] currentColumn = new short[values.length];
        for (int y = 0; y < values.length; y++) {
            currentColumn[y] = values[y][x];
        }

        for (int y = 0; y < values.length; y++) {
            if (values[y][x] == 255) {

                if (lastColumn != null) {
//                    printOut(values);

                    System.out.println("Last:");
                    System.out.println(Arrays.toString(lastColumn));
                    System.out.println("Current:");
                    System.out.println(Arrays.toString(currentColumn));
                    System.out.println("==================================");

                    int currentContains = 0;
                    int lastContains = 0;

                    int bothClear = 0;
                    for (int y2 = 0; y2 < currentColumn.length; y2++) {
                        if (currentColumn[y2] == 255) {
                            currentContains++;
                        } else {
                            System.out.println("111 Current: " + currentContains  + " Last: " + lastContains);
//                            if (currentContains > 2) return false;
                            currentContains = 0;
                        }

                        if (lastColumn[y2] == 255) {
                            lastContains++;
                        } else {
                            System.out.println("222 Current: " + currentContains  + " Last: " + lastContains);
                            lastContains = 0;
                        }
                    }
                }

                lastColumn = currentColumn;
                return true; // Is part of a character
            }
        }

        lastColumn = currentColumn;
        return false; // Makes a new character
    }

    public static short[][] createGrid(BufferedImage bufferedImage) {
        short[][] values = new short[bufferedImage.getHeight()][];
        for (int i = 0; i < values.length; i++) {
            short[] row = new short[bufferedImage.getWidth()];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = 0;

            values[i] = row;
        }

        return values;
    }

    public static short[][] makeEven(short[][] input) {
        if (input.length % 2 != 0) {
            short[][] temp = new short[input.length + 1][];
            System.arraycopy(input, 0, temp, 0, input.length);
            System.out.println(Arrays.toString(input));
            System.out.println(Arrays.toString(temp));

            input = temp;
        }

        if (input[0].length % 2 != 0) {
            for (int i = 0; i < input.length; i++) {
                short[] oldRow = input[i];
                short[] newRow = new short[oldRow.length + 1];

                System.arraycopy(oldRow, 0, newRow, 0, oldRow.length);

                input[i] = newRow;
            }
        }

        return input;
    }

    public static BufferedImage createImage(short[][] data) throws IOException {
        BufferedImage output = new BufferedImage(data[0].length, data.length, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < data.length; y++) {
            for (int x = 0; x < data[0].length; x++) {
                short color = data[y][x];
                output.setRGB(x, y, new Color(color, color, color).getRGB());
            }
        }

        ImageIO.write(output, "png", new File("E:\\NewOCR\\output_" + (outputIndex++) + ".png"));
        return output;
    }

    public static void printOut(short[][] values) {
        for (short[] row : values) {
            for (short b : row) {
                System.out.print(fixedLengthString(String.valueOf(255 - b), 4));
            }

            System.out.println("");
        }
    }

    private static short[][] minimize(short[][] original) {
        short[][] result = new short[original.length / 2][];
        for (int i = 0; i < result.length; i++) {
            short[] row = new short[original[0].length / 2];
            for (int i1 = 0; i1 < row.length; i1++) row[i1] = 0;

            result[i] = row;
        }

        int arrX = 0;
        int arrY = 0;
        for (int y = 0; y < original.length; y += 2) {
            for (int x = 0; x < original[0].length; x += 2) {
                double total = ((double) original[y][x]
                        + (double) original[y + 1][x]
                        + (double) original[y][x + 1]
                        + (double) original[y + 1][x + 1]) / 4;

                result[arrY][arrX++] = (short) total;
            }

            arrX = 0;
            arrY++;
        }

        return result;
    }

    public static String fixedLengthString(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    public static double getBlackness(int x, int y) {
        return new Color(input.getRGB(x, y)).getRed();
    }

    public static void filter(BufferedImage bufferedImage) {
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                bufferedImage.setRGB(x, y, isBlack(bufferedImage, x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
    }

    public static boolean isBlack(BufferedImage image, int x, int y) {
        try {
            Color pixel = new Color(image.getRGB(x, y));
            return (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 128;
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
    }

}
