package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class ECHO {

    private boolean warmed;
    private List<Sample> filteredOutlierBuffer;
    private List<Model> ensemble;
    private List<Double> confidenceWindow;
    private List<Integer> knownLabels;

    private double gamma;
    private double sensitivity;
    private int filteredOutlierBufferMaxSize;
    private int confidenceWindowsMaxSize;

    public ECHO(int filteredOutlierBufferMaxSize, int confidenceWindowsMaxSize, double gamma, double sensitivity) {
        this.warmed = false;
        this.ensemble = new ArrayList<>();
        this.filteredOutlierBuffer = new ArrayList<>();
        this.knownLabels = new ArrayList<>();
        this.confidenceWindow = new ArrayList<>();

        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
        this.confidenceWindowsMaxSize = confidenceWindowsMaxSize;
        this.gamma = gamma;
        this.sensitivity = sensitivity;
    }

    public ClassificationResult process(final Sample sample) {

        if (!this.warmed) {
            this.warmUp(sample);
            return new ClassificationResult(null, false, null);
        }

        final ClassificationResult classificationResult = this.classify(sample);

        classificationResult.ifInsideBoundaryOrElse((label, confidence) -> {

            this.confidenceWindow.add(confidence);
            final Optional<Integer> changePoint = this.changeDetection();
            changePoint.ifPresent(this::updateClassifier);

        }, () -> {

            this.filteredOutlierBuffer.add(sample);
            if (this.filteredOutlierBuffer.size() >= this.filteredOutlierBufferMaxSize) {
                this.novelClassDetection();
            }

        });

        return classificationResult;

    }

    private ClassificationResult classify(final Sample sample) {

        final List<ClassificationResult> classificationResults = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .collect(Collectors.toCollection(ArrayList::new));

        double ensembleConfidence = calculateConfidence(classificationResults);

        final HashMap<Integer, Integer> votesByLabel = new HashMap<>();
        classificationResults.forEach(classificationResult -> {

            classificationResult.ifInsideBoundary((label, modelConfidence) -> {
                votesByLabel.putIfAbsent(label, 0);
                Integer votes = votesByLabel.get(label);
                votesByLabel.put(label, votes + 1);
            });

        });

        return votesByLabel
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new ClassificationResult(entry.getKey(), true, ensembleConfidence))
                .orElseGet(() -> new ClassificationResult(null, false, 0.0));

    }

    private static double calculateConfidence(List<ClassificationResult> classificationResults) {

        List<Double> confidenceValues = classificationResults
                .stream()
                .map(ClassificationResult::getConfidence)
                .collect(Collectors.toCollection(ArrayList::new));

        Double maxConfidence = confidenceValues.stream().max(Double::compareTo).orElse(1.0);

        return confidenceValues
                .stream()
                .map(confidenceValue -> confidenceValue / maxConfidence)
                .reduce(0.0, Double::sum) / confidenceValues.size();

    }

    private Optional<Integer> changeDetection() {

        final int n = this.confidenceWindow.size();
        final double meanConfidence = this.confidenceWindow.stream().reduce(0.0, Double::sum) / n;
        final int cushion = Math.max(100, (int) Math.floor(Math.pow(n, this.gamma))) ;

        if ((n > 2 * cushion && meanConfidence <= 0.3) || n >= this.confidenceWindowsMaxSize) {
            return Optional.of(n);
        }

        double maxLLRS = 0; //LLRS stands for Log Likelihood Ratio Sum
        int maxLLRSIndex = -1;

        for (int i = cushion; i <= n - cushion; ++i) {

            final BetaDistribution preBeta = estimateBetaDistribution(
                    this.confidenceWindow.subList(0, i));

            final BetaDistribution postBeta = estimateBetaDistribution(
                    this.confidenceWindow.subList(i, n));

            double lLRS = this.confidenceWindow.subList(i + 1, n)
                    .stream()
                    .map(x -> preBeta.density(x) / postBeta.density(x))
                    .map(Math::log)
                    .reduce(0.0, Double::sum);

            if (lLRS > maxLLRS) {
                maxLLRS = lLRS;
                maxLLRSIndex = i;
            }

        }

        if (maxLLRS > -Math.log(this.sensitivity) && maxLLRSIndex != -1) {
            return Optional.of(maxLLRSIndex);
        } else {
            return Optional.empty();
        }
    }

    public static BetaDistribution estimateBetaDistribution(final List<Double> data) {

        double mean = data.stream().reduce(0.0, Double::sum) / data.size();

        double variance = data
                .stream()
                .map(value -> Math.abs(value - mean))
                .map(value -> value * value)
                .reduce(0.0, Double::sum) / data.size();

        double alpha = ((Math.pow(mean, 2) - Math.pow(mean, 3)) / variance) - mean;
        double beta = alpha * ((1 / mean) - 1);

        return new BetaDistribution(alpha, beta);
    }

    private void novelClassDetection() {}

    private void updateClassifier(int changePoint) {}

    private void warmUp(final Sample sample) {
        this.warmed = true;
    }

}
