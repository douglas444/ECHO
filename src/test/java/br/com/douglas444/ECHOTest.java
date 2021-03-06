package br.com.douglas444;

import br.com.douglas444.echo.ECHOBuilder;
import br.com.douglas444.echo.ECHOController;
import br.com.douglas444.streams.processor.StreamsFileReader;
import br.com.douglas444.streams.processor.StreamsProcessorExecutor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ECHOTest {

    private static final int Q = 400;
    private static final int K = 50;
    private static final int CENTROIDS_PERCENTAGE = 10;
    private static final int MCIKMEANS_MAX_ITERATIONS = 10;
    private static final int CONDITIONAL_MODE_MAX_ITERATIONS = 10;
    private static final double GAMMA = 0.5;
    private static final double SENSITIVITY = 0.001;
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private static final double ACTIVE_LEARNING_THRESHOLD = 0.4;
    private static final int FILTERED_OUTLIER_BUFFER_MAX_SIZE = 2000;
    private static final int CONFIDENCE_WINDOW_MAX_SIZE = 1000;
    private static final int ENSEMBLE_SIZE = 5;
    private static final int RANDOM_GENERATOR_SEED = 0;
    private static final int CHUNK_SIZE = 2000;
    private static final boolean KEEP_NOVELTY_DECISION_MODEL = true;
    private static final boolean MULTI_CLASS_NOVELTY_DETECTION = true;

    @Test
    public void execute() throws IOException {

        final ECHOBuilder echoBuilder = new ECHOBuilder(
                Q,
                K,
                CENTROIDS_PERCENTAGE,
                MCIKMEANS_MAX_ITERATIONS,
                CONDITIONAL_MODE_MAX_ITERATIONS,
                GAMMA,
                SENSITIVITY,
                CONFIDENCE_THRESHOLD,
                ACTIVE_LEARNING_THRESHOLD,
                FILTERED_OUTLIER_BUFFER_MAX_SIZE,
                CONFIDENCE_WINDOW_MAX_SIZE,
                ENSEMBLE_SIZE,
                RANDOM_GENERATOR_SEED,
                CHUNK_SIZE,
                KEEP_NOVELTY_DECISION_MODEL,
                MULTI_CLASS_NOVELTY_DETECTION);

        final ECHOController echoController = echoBuilder.build();

        URL url = getClass().getClassLoader().getResource("MOA3_fold1_ini");
        if (url == null) {
            throw new MissingResourceException("File not found", ECHOTest.class.getName(), "MOA3_fold1_ini");
        }
        File file1 = new File(url.getFile());
        FileReader fileReader1 = new FileReader(file1);

        url = getClass().getClassLoader().getResource("MOA3_fold1_onl");
        if (url == null) {
            throw new MissingResourceException("File not found", ECHOTest.class.getName(), "MOA3_fold1_onl");
        }
        File file2 = new File(url.getFile());
        FileReader fileReader2 = new FileReader(file2);

        new StreamsProcessorExecutor().start(
                echoController,
                10000,
                new StreamsFileReader(",", fileReader1),
                new StreamsFileReader(",", fileReader2));


        //Asserting UnkR
        double unkR = echoController.getDynamicConfusionMatrix().measureUnkR();
        unkR = (double) Math.round(unkR * 10000) / 10000;
        assertEquals(0.1308, unkR, "The final value of UnkR differs from the expected " +
                "for the dataset MOA3_fold1 with the following parameters configuration:\n" + parameters());

        //Asserting CER
        double cer = echoController.getDynamicConfusionMatrix().measureCER();
        cer = (double) Math.round(cer * 10000) / 10000;
        assertEquals(0.0000, cer, "The final value of CER differs from the expected for the " +
                "dataset MOA3_fold1 with the following parameters configuration:\n" + parameters());

        //Asserting number of novelties
        assertEquals(108, echoController.getNoveltyCount(),
                "The final value of Novelty Count differs from the expected for the dataset " +
                        "MOA3_fold1 with the following parameters configuration:\n" + parameters());


    }

    static String parameters() {

        return
            "\nQ = 400" +
            "\nK = 50" +
            "\nGAMMA = 0.5" +
            "\nSENSITIVITY = 0.001" +
            "\nCONFIDENCE_THRESHOLD = 0.6" +
            "\nACTIVE_LEARNING_THRESHOLD = 0.5" +
            "\nFILTERED_OUTLIER_BUFFER_MAX_SIZE = 2000" +
            "\nCONFIDENCE_WINDOW_MAX_SIZE = 1000" +
            "\nENSEMBLE_SIZE = 5" +
            "\nRANDOM_GENERATOR_SEED = 0" +
            "\nCHUNK_SIZE = 2000";

    }

}