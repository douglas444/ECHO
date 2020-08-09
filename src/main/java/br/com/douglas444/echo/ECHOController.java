package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierController;
import br.com.douglas444.mltk.datastructure.DynamicConfusionMatrix;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.Optional;

public class ECHOController implements DSClassifierController {

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
                this.echo.getNoveltyCount())
                + "\n" + echo.getConfusionMatrix().toString();
    }

    public DynamicConfusionMatrix getDynamicConfusionMatrix() {
        return this.echo.getConfusionMatrix();
    }
}
