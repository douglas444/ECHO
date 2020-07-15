package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierBuilder;

public class ECHOBuilder  implements DSClassifierBuilder {

    private final ECHO echo;

    public ECHOBuilder(int q,
                       int k,
                       double gamma,
                       double sensitivity,
                       double confidenceThreshold,
                       int filteredOutlierBufferMaxSize,
                       int confidenceWindowMaxSize,
                       int ensembleSize,
                       int randomGeneratorSeed,
                       int chunkSize) {

        echo = new ECHO(
                q,
                k,
                gamma,
                sensitivity,
                confidenceThreshold,
                filteredOutlierBufferMaxSize,
                confidenceWindowMaxSize,
                ensembleSize,
                randomGeneratorSeed,
                chunkSize);

    }

    @Override
    public ECHOController build() {
        return new ECHOController(echo);
    }
}
