package br.com.douglas444.echo;

import br.com.douglas444.streams.processor.StreamsFileReader;
import br.com.douglas444.streams.processor.StreamsProcessorExecutor;
import br.ufu.facom.pcf.core.Configurable;
import br.ufu.facom.pcf.core.Interceptable;
import br.ufu.facom.pcf.core.Interceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class ECHOInterceptable implements Interceptable, Configurable {

    private StreamsProcessorExecutor executor;

    private static final String Q = "Q";
    private static final String K = "K";
    private static final String GAMMA = "Gamma";
    private static final String SENSITIVITY = "Sensitivity";
    private static final String CONFIDENCE_THRESHOLD = "Confidence threshold";
    private static final String AL_THRESHOLD = "AL threshold";
    private static final String OUTLIER_BUFFER_MAX_SIZE = "Outlier buffer max size";
    private static final String WINDOW_MAX_SIZE = "Window max size";
    private static final String ENSEMBLE_SIZE = "Ensemble size";
    private static final String RANDOM_GENERATOR_SEED = "Seed";
    private static final String CHUNK_SIZE = "Chunk size";
    private static final String NOVELTY_DECISION_MODEL = "Novelty decision model {0,1}";
    private static final String DATASET_FILE_PATH = "Dataset CSV's (separated by ';')";
    private static final String LOG_INTERVAL = "Log interval";

    private static final double DEFAULT_Q = 400;
    private static final double DEFAULT_K = 50;
    private static final double DEFAULT_GAMMA = 0.5;
    private static final double DEFAULT_SENSITIVITY = 0.001;
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.6;
    private static final double DEFAULT_AL_THRESHOLD = 0.5;
    private static final double DEFAULT_OUTLIER_BUFFER_MAX_SIZE = 2000;
    private static final double DEFAULT_WINDOW_MAX_SIZE = 1000;
    private static final double DEFAULT_ENSEMBLE_SIZE = 5;
    private static final double DEFAULT_RANDOM_GENERATOR_SEED = 0;
    private static final double DEFAULT_CHUNK_SIZE = 2000;
    private static final double DEFAULT_NOVELTY_DECISION_MODEL = 1;
    private static final double DEFAULT_LOG_INTERVAL = 1000;

    final private HashMap<String, Double> numericParameters;
    final private HashMap<String, String> nominalParameters;

    public ECHOInterceptable() {

        this.numericParameters = new HashMap<>();

        this.numericParameters.put(Q, DEFAULT_Q);
        this.numericParameters.put(K, DEFAULT_K);
        this.numericParameters.put(GAMMA, DEFAULT_GAMMA);
        this.numericParameters.put(SENSITIVITY, DEFAULT_SENSITIVITY);
        this.numericParameters.put(CONFIDENCE_THRESHOLD, DEFAULT_CONFIDENCE_THRESHOLD);
        this.numericParameters.put(AL_THRESHOLD, DEFAULT_AL_THRESHOLD);
        this.numericParameters.put(OUTLIER_BUFFER_MAX_SIZE, DEFAULT_OUTLIER_BUFFER_MAX_SIZE);
        this.numericParameters.put(WINDOW_MAX_SIZE, DEFAULT_WINDOW_MAX_SIZE);
        this.numericParameters.put(ENSEMBLE_SIZE, DEFAULT_ENSEMBLE_SIZE);
        this.numericParameters.put(RANDOM_GENERATOR_SEED, DEFAULT_RANDOM_GENERATOR_SEED);
        this.numericParameters.put(CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
        this.numericParameters.put(NOVELTY_DECISION_MODEL, DEFAULT_NOVELTY_DECISION_MODEL);
        this.numericParameters.put(LOG_INTERVAL, DEFAULT_LOG_INTERVAL);

        this.nominalParameters = new HashMap<>();
        this.nominalParameters.put(DATASET_FILE_PATH, "");
    }

    @Override
    public boolean execute(Interceptor interceptor) {

        final boolean noveltyDetectionDecisionModel;
        if (this.numericParameters.get(NOVELTY_DECISION_MODEL).equals(0d)) {
            noveltyDetectionDecisionModel = false;
        } else if (this.numericParameters.get(NOVELTY_DECISION_MODEL).equals(1d)) {
            noveltyDetectionDecisionModel = true;
        } else {
            throw new IllegalArgumentException();
        }

        final ECHOBuilder echoBuilder = new ECHOBuilder(
                this.numericParameters.get(Q).intValue(),
                this.numericParameters.get(K).intValue(),
                this.numericParameters.get(GAMMA),
                this.numericParameters.get(SENSITIVITY),
                this.numericParameters.get(CONFIDENCE_THRESHOLD),
                this.numericParameters.get(AL_THRESHOLD),
                this.numericParameters.get(OUTLIER_BUFFER_MAX_SIZE).intValue(),
                this.numericParameters.get(WINDOW_MAX_SIZE).intValue(),
                this.numericParameters.get(ENSEMBLE_SIZE).intValue(),
                this.numericParameters.get(RANDOM_GENERATOR_SEED).intValue(),
                this.numericParameters.get(CHUNK_SIZE).intValue(),
                noveltyDetectionDecisionModel,
                interceptor);

        final ECHOController controller = echoBuilder.build();
        this.executor = new StreamsProcessorExecutor();

        final String[] files = Arrays.stream(this.nominalParameters.get(DATASET_FILE_PATH).split(";"))
                .map(file -> file.replace(" ", ""))
                .filter(file -> !file.isEmpty())
                .toArray(String[]::new);

        final StreamsFileReader[] fileReaders = new StreamsFileReader[files.length];

        for (int i = 0; i < files.length; i++) {
            fileReaders[i] = new StreamsFileReader(",", FileUtil.getFileReader(files[i]));
        }

        boolean success;
        try {

            success = executor.start(
                    controller,
                    this.getNumericParameters().get(LOG_INTERVAL).intValue(),
                    fileReaders);

        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        if (success) {
            System.out.println(controller.getDynamicConfusionMatrix());
        }

        return success;

    }

    @Override
    public void stop() {
        if (this.executor != null) {
            this.executor.interrupt();
        }
    }

    @Override
    public HashMap<String, String> getNominalParameters() {
        return this.nominalParameters;
    }

    @Override
    public HashMap<String, Double> getNumericParameters() {
        return this.numericParameters;
    }
}