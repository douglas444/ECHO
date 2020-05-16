package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.*;
import java.util.stream.Collectors;

public class ECHO {

    private boolean warmed;
    private List<Sample> filteredOutlierBuffer;
    private List<Model> ensemble;
    private List<Double> confidenceWindow;
    private List<Integer> knownLabels;

    private int filteredOutlierBufferMaxSize;

    public ECHO(int filteredOutlierBufferMaxSize) {
        this.warmed = false;
        this.ensemble = new ArrayList<>();
        this.filteredOutlierBuffer = new ArrayList<>();
        this.knownLabels = new ArrayList<>();
        this.confidenceWindow = new ArrayList<>();

        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
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
        return Optional.empty();
    }

    private void novelClassDetection() {}

    private void updateClassifier(int changePoint) {}

    private void warmUp(final Sample sample) {
        this.warmed = true;
    }

}
