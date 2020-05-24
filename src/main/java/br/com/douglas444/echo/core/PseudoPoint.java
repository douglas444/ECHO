package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.datastructure.ImpurityBasedCluster;
import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

class PseudoPoint {

    private Sample centroid;
    private double radius;
    private HashMap<Integer, Integer> numberOfSamplesByLabel;
    private int totalNumberOfSamples;
    private int numberOfSampleForMostFrequentLabel;
    private Integer label;

    PseudoPoint(ImpurityBasedCluster cluster) {

        this.centroid = cluster.getCentroid();
        this.radius = cluster.calculateRadius();
        this.totalNumberOfSamples = cluster.getNumberOfLabeledSamples();
        this.label = cluster.getMostFrequentLabel();

        this.numberOfSamplesByLabel = new HashMap<>();

        cluster.getSamplesByLabel().forEach((label, samples) -> {
            this.numberOfSamplesByLabel.put(label, samples.size());
            if (label.equals(this.label)) {
                this.numberOfSampleForMostFrequentLabel = samples.size();
            }
        });

    }

    static PseudoPoint getClosestPseudoPoint(final Sample sample, final List<PseudoPoint> pseudoPoints) {

        if (pseudoPoints.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final HashMap<Sample, PseudoPoint> pseudoPointByCentroid = new HashMap<>();

        final List<Sample> centroids = pseudoPoints.stream()
                .map(pseudoPoint -> {
                    Sample centroid = pseudoPoint.getCentroid();
                    pseudoPointByCentroid.put(centroid, pseudoPoint);
                    return centroid;
                })
                .sorted(new SampleDistanceComparator(sample))
                .collect(Collectors.toList());

        final Sample closestCentroid = centroids.get(0);
        return pseudoPointByCentroid.get(closestCentroid);
    }

    double calculatePurity() {
        return (double) this.numberOfSampleForMostFrequentLabel / this.totalNumberOfSamples;
    }

    Sample getCentroid() {
        return centroid;
    }

    Integer getLabel() {
        return label;
    }

    double getRadius() {
        return radius;
    }

    int getTotalNumberOfSamples() {
        return totalNumberOfSamples;
    }

    int getNumberOfSampleForMostFrequentLabel() {
        return numberOfSampleForMostFrequentLabel;
    }


}
