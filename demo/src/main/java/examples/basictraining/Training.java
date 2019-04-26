package examples.basictraining;

import com.uddernetworks.newocr.configuration.ConfigReflectionCacher;
import com.uddernetworks.newocr.configuration.HOCONFontConfiguration;
import com.uddernetworks.newocr.database.OCRDatabaseManager;
import com.uddernetworks.newocr.recognition.OCRTrain;
import com.uddernetworks.newocr.recognition.mergence.DefaultMergenceManager;
import com.uddernetworks.newocr.recognition.similarity.DefaultSimilarityManager;
import com.uddernetworks.newocr.train.ComputerTrainGenerator;
import com.uddernetworks.newocr.train.TrainGeneratorOptions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * From https://wiki.newocr.dev/examples/basic-training
 */
public class Training {
    public static void main(String[] args) throws IOException {
        new ComputerTrainGenerator().generateTrainingImage(new File("train_comicsans.png"),
                new TrainGeneratorOptions().setFontFamily("Comic Sans MS"));

        var databaseManager = new OCRDatabaseManager(new File("database\\ocr_basictraining"));
        var similarityManager = new DefaultSimilarityManager();
        var mergenceManager = new DefaultMergenceManager(databaseManager, similarityManager);

        var fontConfiguration = new HOCONFontConfiguration("fonts/ComicSans", new ConfigReflectionCacher(), similarityManager, mergenceManager);

        var ocrTrain = new OCRTrain(databaseManager, fontConfiguration.fetchOptions());

        ocrTrain.trainImage(new File("train_comicsans.png"));

        databaseManager.shutdown(TimeUnit.SECONDS, 1L);
    }
}
