package com.uddernetworks.newocr.detection;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KenningSeparator implements KenningResolver {

    private static int SLIDING_WIDTH = 10;

    @Override
    public List<SearchImage> separateKenning(SearchImage searchImage) {
        if (searchImage.getWidth() < SLIDING_WIDTH) return Collections.singletonList(searchImage);
        var res = new ArrayList<SearchImage>();

        var iters = searchImage.getWidth() - SLIDING_WIDTH + 1;
        for (int i = 0; i < iters; i++) {
            var currentFrame = searchImage.getSubimage(i, 0, SLIDING_WIDTH, searchImage.getHeight());

            // TODO: Test if the bottom and top of the grid meet at all.
            //  This may be a DFS, though a better algorithm may need to be used. If it uses a standard fill instead,
            //  it may just be more efficient to connect similar pixels like v1, but with keeping vertical
            //  relations (Unlike v1, where purely thresholds were used for vertical connection)

            try {
                ImageIO.write(currentFrame.toImage(), "png", new File("E:\\NewOCR\\frames\\" + i + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return res;
    }

}
