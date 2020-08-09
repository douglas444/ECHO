package br.com.douglas444;

import br.com.douglas444.dsframework.DSClassifierExecutor;
import br.com.douglas444.dsframework.DSFileReader;
import br.com.douglas444.echo.ECHOBuilder;
import br.com.douglas444.echo.ECHOController;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ECHOTest {

    private static final int Q = 400;
    private static final int K = 50;
    private static final double GAMMA = 0.5;
    private static final double SENSITIVITY = 0.001;
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private static final double ACTIVE_LEARNING_THRESHOLD = 0.5;
    private static final int FILTERED_OUTLIER_BUFFER_MAX_SIZE = 2000;
    private static final int CONFIDENCE_WINDOW_MAX_SIZE = 1000;
    private static final int ENSEMBLE_SIZE = 5;
    private static final int RANDOM_GENERATOR_SEED = 0;
    private static final int CHUNK_SIZE = 2000;

    @Test
    public void execute() throws IOException {

        final ECHOBuilder echoBuilder = new ECHOBuilder(
                Q,
                K,
                GAMMA,
                SENSITIVITY,
                CONFIDENCE_THRESHOLD,
                ACTIVE_LEARNING_THRESHOLD,
                FILTERED_OUTLIER_BUFFER_MAX_SIZE,
                CONFIDENCE_WINDOW_MAX_SIZE,
                ENSEMBLE_SIZE,
                RANDOM_GENERATOR_SEED,
                CHUNK_SIZE,
                null);

        final ECHOController echoController = echoBuilder.build();

        URL url = getClass().getClassLoader().getResource("MOA3_fold1_ini");
        assert url != null;
        File file = new File(url.getFile());

        FileReader fileReader = new FileReader(file);
        DSFileReader dsFileReader = new DSFileReader(",", fileReader);
        DSClassifierExecutor.start(echoController, dsFileReader, false);

        url = getClass().getClassLoader().getResource("MOA3_fold1_onl");
        assert url != null;
        file = new File(url.getFile());

        fileReader = new FileReader(file);
        dsFileReader = new DSFileReader(",", fileReader);
        DSClassifierExecutor.start(echoController, dsFileReader, true, 1);

        //Asserting UnkR
        double unkR = echoController.getDynamicConfusionMatrix().unkR();
        unkR = (double) Math.round(unkR * 10000) / 10000;
        assertEquals(0.1067, unkR, "The final value of UnkR differs from the expected " +
                "for the dataset MOA3_fold1 with the following parameters configuration:\n" + parameters());

        //Asserting CER
        double cer = echoController.getDynamicConfusionMatrix().cer();
        cer = (double) Math.round(cer * 10000) / 10000;
        assertEquals(0.0077, cer, "The final value of CER differs from the expected for the " +
                "dataset MOA3_fold1 with the following parameters configuration:\n" + parameters());

        //Asserting number of novelties
        assertEquals(4, echoController.getNoveltyCount(),
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