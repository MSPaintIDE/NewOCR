package com.uddernetworks.newocr.altsearcher.feature;

import java.util.List;

public class VerticalLineFeature implements Feature {

    private static double SNAP_HEIGHT_INCREMENTS = 0.2; // 5 in total
//    private static double MIN_HEIGHT_THRESHOLD = SNAP_HEIGHT_INCREMENTS *

    // ALL fields here are *percentages*
    private double x;
    private double y;
    private double height;

    @Override
    public boolean createFeature(boolean[][] grid, List<Feature> featureList) {
        return false;
    }

    @Override
    public boolean hasFeature(boolean[][] grid) {
        return false;
    }
}
