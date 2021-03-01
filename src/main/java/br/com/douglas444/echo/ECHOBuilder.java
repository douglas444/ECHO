package br.com.douglas444.echo;

import br.com.douglas444.ndc.processor.StreamsProcessorBuilder;

public class ECHOBuilder implements StreamsProcessorBuilder {

    private final ECHO echo;

    public ECHOBuilder(int q,
                       int k,
                       double gamma,
                       double sensitivity,
                       double confidenceThreshold,
                       double activeLearningThreshold,
                       int filteredOutlierBufferMaxSize,
                       int confidenceWindowMaxSize,
                       int ensembleSize,
                       int randomGeneratorSeed,
                       int chunkSize,
                       boolean keepNoveltyDecisionModel) {

        echo = new ECHO(
                q,
                k,
                gamma,
                sensitivity,
                confidenceThreshold,
                activeLearningThreshold,
                filteredOutlierBufferMaxSize,
                confidenceWindowMaxSize,
                ensembleSize,
                randomGeneratorSeed,
                chunkSize,
                keepNoveltyDecisionModel);

    }

    @Override
    public ECHOController build() {
        return new ECHOController(echo);
    }

}
