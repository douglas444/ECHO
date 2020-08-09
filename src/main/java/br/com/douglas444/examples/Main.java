package br.com.douglas444.examples;

import br.com.douglas444.dsframework.DSClassifierExecutor;
import br.com.douglas444.dsframework.DSFileReader;
import br.com.douglas444.echo.ECHOBuilder;
import br.com.douglas444.echo.ECHOController;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class Main {

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

    public static void main(String[] args) throws IOException {

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

        URL url = Main.class.getClassLoader().getResource("MOA3_fold1_ini");
        assert url != null;
        File file = new File(url.getFile());

        FileReader fileReader = new FileReader(file);
        DSFileReader dsFileReader = new DSFileReader(",", fileReader);
        DSClassifierExecutor.start(echoController, dsFileReader, false);

        url = Main.class.getClassLoader().getResource("MOA3_fold1_onl");
        assert url != null;
        file = new File(url.getFile());

        fileReader = new FileReader(file);
        dsFileReader = new DSFileReader(",", fileReader);
        DSClassifierExecutor.start(echoController, dsFileReader, true, 1);


    }

}
