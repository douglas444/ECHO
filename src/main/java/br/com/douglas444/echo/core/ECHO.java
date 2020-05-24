package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.datastructure.Sample;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class ECHO {

    private boolean warmed;
    private final List<Sample> filteredOutlierBuffer;
    private final List<Model> ensemble;
    private final List<ClassifiedSample> window;

    private double gamma;
    private double sensitivity;
    private double confidenceThreshold;
    private int filteredOutlierBufferMaxSize;
    private int confidenceWindowsMaxSize;
    private int chunkSize;

    public ECHO(int filteredOutlierBufferMaxSize, int confidenceWindowsMaxSize, double gamma, double sensitivity,
                double confidenceThreshold, int chunkSize) {
        this.warmed = false;
        this.ensemble = new ArrayList<>();
        this.filteredOutlierBuffer = new ArrayList<>();

        this.window = new ArrayList<>();

        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
        this.confidenceWindowsMaxSize = confidenceWindowsMaxSize;
        this.gamma = gamma;
        this.sensitivity = sensitivity;
        this.confidenceThreshold = confidenceThreshold;
        this.chunkSize = chunkSize;
    }

    public Optional<Integer> process(final Sample sample) {

        if (!this.warmed) {
            this.warmUp(sample);
            return Optional.empty();
        }

        final Optional<ClassifiedSample> classifiedSample = this.classify(sample);

        if (classifiedSample.isPresent()) {

            this.window.add(classifiedSample.get());
            final Optional<Integer> changePoint = this.changeDetection();
            changePoint.ifPresent(this::updateClassifier);
            return Optional.of(classifiedSample.get().getLabel());

        } else {

            this.filteredOutlierBuffer.add(sample);
            if (this.filteredOutlierBuffer.size() >= this.filteredOutlierBufferMaxSize) {
                this.novelClassDetection();
            }
            return  Optional.empty();

        }

    }

    private Optional<ClassifiedSample> classify(final Sample sample) {

        final List<ClassifiedSample> classifiedSamples = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(ArrayList::new));

        double ensembleConfidence = calculateConfidence(classifiedSamples);

        final HashMap<Integer, Integer> votesByLabel = new HashMap<>();
        classifiedSamples.forEach(classifiedSample -> {
            votesByLabel.putIfAbsent(classifiedSample.getLabel(), 0);
            Integer votes = votesByLabel.get(classifiedSample.getLabel());
            votesByLabel.put(classifiedSample.getLabel(), votes + 1);
        });

        return votesByLabel
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new ClassifiedSample(entry.getKey(), sample, ensembleConfidence));

    }

    private static double calculateConfidence(List<ClassifiedSample> classifiedSamples) {

        final List<Double> confidenceValues = getConfidenceList(classifiedSamples);

        final Double maxConfidence = confidenceValues.stream().max(Double::compareTo).orElse(1.0);

        return confidenceValues
                .stream()
                .map(confidenceValue -> confidenceValue / maxConfidence)
                .reduce(0.0, Double::sum) / confidenceValues.size();

    }

    private Optional<Integer> changeDetection() {

        final int n = this.window.size();
        final double meanConfidence = getConfidenceList(this.window).stream().reduce(0.0, Double::sum) / n;
        final int cushion = Math.max(100, (int) Math.floor(Math.pow(n, this.gamma)));

        if ((n > 2 * cushion && meanConfidence <= 0.3) || n > this.confidenceWindowsMaxSize) {
            return Optional.of(n);
        }


        double maxLLRS = 0; //LLRS stands for Log Likelihood Ratio Sum
        int maxLLRSIndex = -1;

        for (int i = cushion; i <= n - cushion; ++i) {

            final BetaDistribution preBeta = estimateBetaDistribution(
                    getConfidenceList(this.window).subList(0, i));

            final BetaDistribution postBeta = estimateBetaDistribution(
                    getConfidenceList(this.window).subList(i, n));

            final double lLRS = getConfidenceList(this.window).subList(i + 1, n)
                    .stream()
                    .map(x -> preBeta.density(x) / postBeta.density(x))
                    .map(Math::log)
                    .reduce(0.0, Double::sum);

            if (lLRS > maxLLRS) {
                maxLLRS = lLRS;
                maxLLRSIndex = i;
            }

        }

        if (maxLLRSIndex != -1 && n >= 100 && meanConfidence < 0.3) {
            return Optional.of(n);
        }

        if (maxLLRSIndex != -1 && maxLLRS > -Math.log(this.sensitivity)) {
            return Optional.of(maxLLRSIndex);
        }

        return Optional.empty();
    }

    private static BetaDistribution estimateBetaDistribution(final List<Double> data) {

        final double mean = data.stream().reduce(0.0, Double::sum) / data.size();

        final double variance = data
                .stream()
                .map(value -> Math.abs(value - mean))
                .map(value -> value * value)
                .reduce(0.0, Double::sum) / data.size();

        final double alpha = ((Math.pow(mean, 2) - Math.pow(mean, 3)) / variance) - mean;
        final double beta = alpha * ((1 / mean) - 1);

        return new BetaDistribution(alpha, beta);
    }

    private void novelClassDetection() {}

    private void updateClassifier(int changePoint) {

        final List<Sample> labeledSamples = new ArrayList<>();
        final List<ClassifiedSample> classifiedSamples = new ArrayList<>();

        this.window.stream()
                .filter(classifiedSample -> classifiedSample.getConfidence() > this.confidenceThreshold)
                .forEach(classifiedSamples::add);

        this.window.stream()
                .filter(classifiedSample -> classifiedSample.getConfidence() <= this.confidenceThreshold)
                .map(ClassifiedSample::getSample)
                .forEach(labeledSamples::add);

        final Model model = Model.fit(labeledSamples, classifiedSamples);
        this.ensemble.remove(0);
        this.ensemble.add(model);
        this.window.removeAll(this.window.subList(0, changePoint));

    }

    private void warmUp(final Sample sample) {
        this.warmed = true;
    }

    private static List<Double> getConfidenceList(List<ClassifiedSample> classifiedSamples) {
        return classifiedSamples.stream()
                .map(ClassifiedSample::getConfidence)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
