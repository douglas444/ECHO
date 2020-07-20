package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierController;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.Optional;

public class ECHOController implements DSClassifierController {

    private final ECHO echo;

    public ECHOController(ECHO echo) {
        this.echo = echo;
    }

    @Override
    public Optional<Integer> process(final Sample sample) {
        final ClassificationResult classificationResult = this.echo.process(sample);
        return classificationResult.getLabel();
    }

    @Override
    public String getLog() {

        return String.format("Timestamp = %d, Labeled = %d, Mean conf = %f, CD = %d",
                this.echo.getTimestamp(),
                this.echo.getNumberOfLabeledSamples(),
                this.echo.getMeanConfidence(),
                this.echo.getNumberOfConceptDrifts())
                + "\n" + echo.getConfusionMatrix().toString();
    }
}
