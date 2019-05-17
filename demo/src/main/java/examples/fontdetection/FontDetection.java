package examples.fontdetection;

import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRActions;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.ScannedImage;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.utils.ConversionUtils;

import java.io.File;
import java.io.IOException;

/**
 * From https://wiki.newocr.dev/examples/font-detection
 *
 * @author Adam Yarris
 * @version 2.0.0
 * @since April 25, 2019
 */
public class FontDetection {
    public static void main(String[] args) throws IOException {
        var databaseManager = new OCRDatabaseManager(new File("database\\ocr_basictraining"));
        var similarityManager = new DefaultSimilarityManager();
        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);

        var fontConfiguration = new HOCONFontConfiguration("fonts/ComicSans", new ConfigReflectionCacher(), similarityManager, mergenceManager);

        var actions = new OCRActions(similarityManager, databaseManager, fontConfiguration.fetchOptions(similarityManager));

        var ocrScan = new OCRScan(databaseManager, similarityManager, mergenceManager, actions);

        ScannedImage scannedImage = ocrScan.scanImage(new File("demo\\src\\main\\resources\\basic-scanning.png"));
        System.out.println(scannedImage.stripLeadingSpaces().getPrettyString());

        var first = scannedImage.letterAt(0).get();

        System.out.println("\nFirst letter is " + first);

        var size = (int) actions.getFontSize(first).orElse(0);

        System.out.println("Estimated font size is " + ConversionUtils.pixelToPoint(size) + "pt or " + size + "px");

        databaseManager.shutdown();
    }
}
