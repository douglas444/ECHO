package br.com.douglas444.echo;

import java.util.Optional;
import java.util.function.Consumer;

public class ClassificationResult {

    private final Integer label;
    private final boolean insideDecisionBoundary;

    public ClassificationResult(Integer label, boolean insideDecisionBoundary) {

        if (insideDecisionBoundary && label == null) {
            throw new IllegalArgumentException();
        }

        this.label = label;
        this.insideDecisionBoundary = insideDecisionBoundary;
    }

    public void ifExplained(final Consumer<Integer> action) {

        if (this.insideDecisionBoundary) {
            action.accept(this.label);
        }

    }

    public void ifExplainedOrElse(final Consumer<Integer> consumer, final Runnable runnable) {

        if (this.insideDecisionBoundary) {
            consumer.accept(this.label);
        } else {
            runnable.run();
        }

    }

    public void ifExplainedOrElse(final Consumer<Integer> consumer1,
                                  final Consumer<Optional<Integer>> consumer2) {

        if (this.insideDecisionBoundary) {
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

}
