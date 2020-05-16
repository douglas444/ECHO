package br.com.douglas444.echo;

import java.util.Optional;
import java.util.function.Consumer;

public class ClassificationResult {

    private final Integer label;
    private final boolean insideBoundary;
    private final double confidence;

    public ClassificationResult(Integer label, boolean insideBoundary, double confidence) {

        if (insideBoundary && label == null) {
            throw new IllegalArgumentException();
        }

        this.label = label;
        this.insideBoundary = insideBoundary;
        this.confidence = confidence;
    }

    public void ifInsideBoundary(final Consumer<Integer> action) {

        if (this.insideBoundary) {
            action.accept(this.label);
        }

    }

    public void ifInsideBoundaryOrElse(final Consumer<Integer> consumer, final Runnable runnable) {

        if (this.insideBoundary) {
            consumer.accept(this.label);
        } else {
            runnable.run();
        }

    }

    public void ifInsideBoundaryOrElse(final Consumer<Integer> consumer1,
                                       final Consumer<Optional<Integer>> consumer2) {

        if (this.insideBoundary) {
            consumer1.accept(this.label);
        } else {

            final Optional<Integer> argument;

            if (this.label == null) {
                argument = Optional.empty();
            } else {
                argument = Optional.of(this.label);
            }

            consumer2.accept(argument);
        }

    }

    public Optional<Integer> getLabel() {
        if (this.label == null) {
            return Optional.empty();
        } else {
            return Optional.of(this.label);
        }
    }

    public double getConfidence() {
        return confidence;
    }
}
