package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

class ClassificationResult {

    private final boolean explained;
    private final Integer label;
    private final double confidence;
    private final Sample sample;

    ClassificationResult(Integer label, Sample sample, double confidence, boolean explained) {

        if (explained && label == null) {
            throw new IllegalArgumentException();
        }

        this.sample = sample;
        this.label = label;
        this.confidence = confidence;
        this.explained = explained;
    }

    public void ifExplained(final BiConsumer<Integer, Double> action) {

        if (this.explained) {
            action.accept(this.label, this.confidence);
        }

    }

    public void ifExplainedOrElse(final BiConsumer<Integer, Double> action, final Runnable runnable) {

        if (this.explained) {
            action.accept(this.label, this.confidence);
        } else {
            runnable.run();
        }

    }

    public static List<Double> getConfidenceList(final List<ClassificationResult> classificationResults) {
        return classificationResults.stream()
                .map(ClassificationResult::getConfidence)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Optional<Integer> getLabel() {
        if (this.label == null) {
            return Optional.empty();
        } else {
            return Optional.of(this.label);
        }
    }

    double getConfidence() {
        return confidence;
    }

    Sample getSample() {
        return sample;
    }

    boolean isExplained() {
        return explained;
    }

}
