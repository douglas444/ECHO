package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Heater {

    private final int k;
    private final Random random;
    private final List<Sample> chunk;
    private final List<Model> ensemble;
    private final int chunkSize;

    Heater(int chunkSize, int k, Random random) {
        this.k = k;
        this.random = random;
        this.chunk = new ArrayList<>();
        this.ensemble = new ArrayList<>();
        this.chunkSize = chunkSize;
    }

    void process(final Sample sample) {

        this.chunk.add(sample);
        if (this.chunk.size() >= this.chunkSize) {
            this.ensemble.add(Model.fit(this.chunk, this.k, this.random));
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
