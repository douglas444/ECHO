package br.com.douglas444.echo;

import br.com.douglas444.streams.datastructures.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

class Heater {

    private final double centroidsPercentage;
    private final Random random;
    private final List<Sample> chunk;
    private final List<Model> ensemble;
    private final int chunkSize;

    Heater(int chunkSize, double centroidsPercentage, Random random) {
        this.centroidsPercentage = centroidsPercentage;
        this.random = random;
        this.chunk = new ArrayList<>();
        this.ensemble = new ArrayList<>();
        this.chunkSize = chunkSize;
    }

    void process(final Sample sample) {

        this.chunk.add(sample);

        if (this.chunk.size() >= this.chunkSize) {

            final List<PseudoPoint> pseudoPoints = MCIKMeans
                    .execute(this.chunk, new ArrayList<>(), this.centroidsPercentage, this.random)
                    .stream()
                    .filter(cluster -> cluster.size() > 1)
                    .map(PseudoPoint::new)
                    .collect(Collectors.toList());

            this.ensemble.add(Model.fit(this.chunk, pseudoPoints));
            this.chunk.clear();
        }

    }

    List<Model> getResult() {
        return this.ensemble;
    }

    int getEnsembleSize() {
        return this.ensemble.size();
    }
}
