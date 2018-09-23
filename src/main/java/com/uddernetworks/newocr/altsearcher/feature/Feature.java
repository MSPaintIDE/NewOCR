package com.uddernetworks.newocr.altsearcher.feature;

import java.util.List;

public interface Feature {
    boolean createFeature(boolean[][] grid, List<Feature> featureList);
    boolean hasFeature(boolean[][] grid);
}
