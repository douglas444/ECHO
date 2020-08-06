package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Classification {

    private final boolean explained;
    private final boolean novelty;
    private final Integer label;
    private final double confidence;
    private final Sample sample;

    Classification(Integer label, Sample sample, double confidence, boolean explained, boolean novelty) {

        if (explained && label == null) {
            throw new IllegalArgumentException();
        }

        this.sample = sample;
        this.label = label;
        this.confidence = confidence;
        this.explained = explained;
        this.novelty = novelty;
    }
    public static List<Double> getConfidenceList(final List<Classification> classifications) {
        return classifications.stream()
                .map(Classification::getConfidence)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Integer getLabel() {
        return this.label;
    }

    double getConfidence() {
        return confidence;
    }

    Sample getSample() {
        return sample;
    }

    boolean isExplained() {
        return explained;
    }

    public boolean isNovelty() {
        return novelty;
    }
}
