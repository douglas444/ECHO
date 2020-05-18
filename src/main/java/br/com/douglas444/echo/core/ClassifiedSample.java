package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.datastructure.Sample;

public class ClassifiedSample {

    private final int label;
    private final double confidence;
    private final Sample sample;

    public ClassifiedSample(int label, Sample sample, double confidence) {

        this.sample = sample;
        this.label = label;
        this.confidence = confidence;
    }


    public int getLabel() {
        return label;
    }

    public double getConfidence() {
        return confidence;
    }

    public Sample getSample() {
        return sample;
    }
}
