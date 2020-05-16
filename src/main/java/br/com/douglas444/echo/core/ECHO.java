package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.*;
import java.util.stream.Collectors;

public class ECHO {

    private boolean warmed;
    private List<Sample> outlierBuffer;
    private List<Model> ensemble;
    private List<Double> confidenceWindow;
    private List<Integer> knownLabels;

    public ECHO() {
        this.warmed = false;
        this.ensemble = new ArrayList<>();
        this.outlierBuffer = new ArrayList<>();
        this.knownLabels = new ArrayList<>();
        this.confidenceWindow = new ArrayList<>();
    }

    public ClassificationResult process(final Sample sample) {

        if (!this.warmed) {
            this.warmUp(sample);
            return new ClassificationResult(null, false);
        }

        List<ClassificationResult> classificationResults = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .collect(Collectors.toCollection(ArrayList::new));

        HashMap<Integer, Integer> votesByLabel = new HashMap<>();

        classificationResults.forEach(classificationResult -> {

            classificationResult.ifInsideBoundary(label -> {
                votesByLabel.putIfAbsent(label, 0);
                Integer votes = votesByLabel.get(label);
                votesByLabel.put(label, votes + 1);
            });

        });

        Map.Entry<Integer, Integer> maxEntry = votesByLabel
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue()).orElse(null);

        if (maxEntry != null) {
            return new ClassificationResult(maxEntry.getKey(), true);
        } else {
            return new ClassificationResult(null, false);
        }
    }

    private void changeDetection() {}

    private void warmUp(final Sample sample) {
        this.warmed = true;
    }

}
