package com.uddernetworks.newocr.altsearcher;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Histogram {

    private int[] left;
    private int[] bottom;

    public Histogram(boolean[][] values) {
         left = new int[values.length];
         bottom = new int[values[0].length];

        for (int y = 0; y < left.length; y++) {
            int current = 0;
            for (int x = 0; x < bottom.length; x++) {
                if (values[y][x]) current++;
            }

            left[y] = current;
        }

        for (int x = 0; x < bottom.length; x++) {
            int current = 0;
            for (int y = 0; y < left.length; y++) {
                if (values[y][x]) current++;
            }

            bottom[x] = current;
        }
    }

    private double heightOf(int[] array) {
        return Arrays.stream(array).max().getAsInt();
    }

    private double minOf(int[] array) {
        return Arrays.stream(array).min().getAsInt();
    }

    public int getHeight() {
        return (int) Math.max(heightOf(left), heightOf(bottom));
    }

    public void drawTo(BufferedImage image, int xOffset, int yOffset, Color color) {
        int height = (int) Math.max(heightOf(left), heightOf(bottom));

        int[] left = Arrays.copyOf(this.left, this.left.length);
        int[] bottom = Arrays.copyOf(this.bottom, this.bottom.length);

        for (int i = 0; i < height; i++) {
            for (int x = 0; x < left.length; x++) {
                if (left[x]-- > 0) {
                    image.setRGB(x + xOffset, height - i + yOffset, color.getRGB());
                }
            }

            for (int x = 0; x < bottom.length; x++) {
                if (bottom[x]-- > 0) {
                    image.setRGB(x + xOffset + left.length + 5, height - i + yOffset, color.getRGB());
                }
            }
        }
    }

    public double getSimilarity(Histogram comparing) {
        double comparingMinL = minOf(comparing.left) / heightOf(comparing.left);
        double comparingMinB = minOf(comparing.bottom) / heightOf(comparing.bottom);

        double currMinL = minOf(this.left) / heightOf(this.left);
        double currMinB = minOf(this.bottom) / heightOf(this.bottom);

//        System.out.println("Current: [" + currMinL + ", " + currMinB + "]  Comparing: [" + comparingMinL + ", " + comparingMinB + "]");
//        System.out.println("L difference: " + (Math.min(currMinL, comparingMinL) / Math.max(comparingMinL, currMinL)));
//        System.out.println("B difference: " + (Math.min(currMinB, comparingMinB) / Math.max(comparingMinB, currMinB)));

        return ((Math.min(currMinL, comparingMinL) / Math.max(comparingMinL, currMinL)) + (Math.min(currMinB, comparingMinB) / Math.max(comparingMinB, currMinB))) / 2;
    }

    @Override
    public String toString() {
        int[] left = Arrays.copyOf(this.left, this.left.length);
        int[] bottom = Arrays.copyOf(this.bottom, this.bottom.length);

        List<String> lines = new ArrayList<>();

        for (int i = 0; i < getHeight(); i++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < left.length; x++) {
                line.append(left[x]-- > 0 ? "ｘ" : "　");
            }

            line.append("\t");

            for (int x = 0; x < bottom.length; x++) {
                line.append(bottom[x]-- > 0 ? "ｘ" : "　");
            }

            lines.add(line.toString());
        }

        Collections.reverse(lines);

        StringBuilder stringBuilder = new StringBuilder();
        lines.forEach(line -> stringBuilder.append(line).append("\n"));
        return stringBuilder.toString();
    }
}
