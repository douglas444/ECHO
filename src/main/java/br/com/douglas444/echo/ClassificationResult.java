package br.com.douglas444.echo;

import java.util.Optional;
import java.util.function.BiConsumer;

public class ClassificationResult {

    private final Integer label;
    private final boolean insideBoundary;
    private final Double confidence;

    public ClassificationResult(Integer label, boolean insideBoundary, Double confidence) {

        if (insideBoundary && (label == null || confidence == null)) {
            throw new IllegalArgumentException();
        }

        this.label = label;
        this.insideBoundary = insideBoundary;
        this.confidence = confidence;
    }

    public void ifInsideBoundary(final BiConsumer<Integer, Double> action) {

        if (this.insideBoundary) {
            action.accept(this.label, this.confidence);
        }

    }

    public void ifInsideBoundaryOrElse(final BiConsumer<Integer, Double> consumer, final Runnable runnable) {

        if (this.insideBoundary) {
            consumer.accept(this.label, this.confidence);
        } else {
            runnable.run();
        }

    }

    public Optional<Integer> getLabel() {
        if (this.label == null) {
            return Optional.empty();
        } else {
            return Optional.of(this.label);
        }
    }

    public boolean isInsideBoundary() {
        return insideBoundary;
    }

    public Double getConfidence() {
        return confidence;
    }
}
