package br.com.douglas444.echo;

import br.com.douglas444.streams.datastructures.DynamicConfusionMatrix;
import br.com.douglas444.streams.datastructures.DynamicConfusionMatrixCompatible;
import br.com.douglas444.streams.datastructures.Sample;
import br.com.douglas444.streams.processor.StreamsProcessor;

import java.util.Optional;

public class ECHOController implements StreamsProcessor, DynamicConfusionMatrixCompatible {

    private final ECHO echo;

    public ECHOController(ECHO echo) {
        this.echo = echo;
    }

    @Override
    public Optional<Integer> process(final Sample sample) {
        final Classification classification = this.echo.process(sample);
        if (classification.isExplained()) {
            return Optional.of(classification.getLabel());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getLog() {

        return String.format("Timestamp = %d, Labeled = %d, Mean conf = %f, CD = %d, CER = %f, UnkR = %f, Novelty count = %d",
                this.echo.getTimestamp(),
                this.echo.getLabeledSamplesCount(),
                this.echo.getMeanConfidence(),
                this.echo.getConceptDriftsCount(),
                this.echo.calculateCER(),
                this.echo.calculateUnkR(),
                this.echo.getNoveltyCount());
    }

    @Override
    public DynamicConfusionMatrix getDynamicConfusionMatrix() {
        return this.echo.getConfusionMatrix();
    }

    public int getNoveltyCount() {
        return this.echo.getNoveltyCount();
    }
}
