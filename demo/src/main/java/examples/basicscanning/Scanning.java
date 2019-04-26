package examples.basicscanning;

import com.uddernetworks.newocr.ScannedImage;
import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRScan;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.utils.OCRUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * From https://wiki.newocr.dev/examples/basic-scanning
 */
public class Scanning {
    public static void main(String[] args) throws IOException {
        var databaseManager = new OCRDatabaseManager(new File("database\\ocr_basictraining"));
        var similarityManager = new DefaultSimilarityManager();
        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);

        if (!databaseManager.isTrainedSync()) {
            System.err.println("The database has not been trained yet! Please run com.uddernetworks.newocr.demo.TrainDemo to train it and try again.");
            databaseManager.shutdown(TimeUnit.SECONDS, 1L);
            return;
        }

        var fontConfiguration = new HOCONFontConfiguration("fonts/ComicSans", new ConfigReflectionCacher(), similarityManager, mergenceManager);
        var ocrScan = new OCRScan(databaseManager, fontConfiguration.fetchOptions(), similarityManager);

        ScannedImage scannedImage = ocrScan.scanImage(new File("basic-scanning.png"));
        System.out.println(OCRUtils.removeLeadingSpaces(scannedImage.getPrettyString()));

        databaseManager.shutdown();
    }
}
