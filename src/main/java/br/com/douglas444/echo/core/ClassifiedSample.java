package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.datastructure.Sample;

class ClassifiedSample {

    private final int label;
    private final double confidence;
    private final Sample sample;

    ClassifiedSample(int label, Sample sample, double confidence) {

        this.sample = sample;
        this.label = label;
        this.confidence = confidence;
    }


    int getLabel() {
        return label;
    }

    double getConfidence() {
        return confidence;
    }

    Sample getSample() {
        return sample;
    }
}
