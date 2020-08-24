package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierBuilder;
import br.com.douglas444.echo.interceptor.ECHOInterceptor;

public class ECHOBuilder  implements DSClassifierBuilder {

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
                       boolean keepNoveltyDecisionModel,
                       ECHOInterceptor interceptor) {

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
                keepNoveltyDecisionModel,
                interceptor);

    }

    @Override
    public ECHOController build() {
        return new ECHOController(echo);
    }
}
