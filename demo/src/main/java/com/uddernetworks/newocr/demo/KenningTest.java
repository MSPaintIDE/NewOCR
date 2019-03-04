package com.uddernetworks.newocr.demo;

import com.uddernetworks.newocr.detection.KenningSeparator;
import com.uddernetworks.newocr.detection.SearchImage;
import com.uddernetworks.newocr.utils.OCRUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class KenningTest {

    public static void main(String[] args) throws IOException {
        var input = new File("E:\\NewOCR\\frames\\input.png");
        var image = ImageIO.read(input);

        var grid = OCRUtils.createGrid(image);
        OCRUtils.toGrid(image, grid);

        var searchImage = new SearchImage(grid);
        System.out.println("searchImage = " + searchImage);

        var kenningSeparator = new KenningSeparator();
        kenningSeparator.separateKenning(searchImage);
    }

}
