package com.uddernetworks.newocr.detection;

import java.util.List;

public interface KenningResolver {

    /**
     * Separates the different {@link SearchImage}s that have kenning.
     *
     * @param searchImage The {@link SearchImage} to separate
     * @return The original {@link SearchImage} if no separations are created, or the different characters' images
     */
    List<SearchImage> separateKenning(SearchImage searchImage);
}
