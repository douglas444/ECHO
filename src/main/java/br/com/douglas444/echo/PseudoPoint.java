package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Cluster;
import br.com.douglas444.mltk.datastructure.ImpurityBasedCluster;
import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

class PseudoPoint {

    private final Sample centroid;
    private final double radius;
    private final int totalNumberOfLabeledSamples;
    private final int numberOfSampleForMostFrequentLabel;
    private final Integer label;

    PseudoPoint(ImpurityBasedCluster cluster) {

        cluster.getSamples().forEach(sample -> sample.setClusterId(null));

        this.centroid = cluster.getCentroid();
        this.radius = 2 * cluster.calculateStandardDeviation();
        this.totalNumberOfLabeledSamples = cluster.getNumberOfLabeledSamples();
        this.label = cluster.getMostFrequentLabel();
        this.numberOfSampleForMostFrequentLabel = cluster.getSamplesByLabel().get(this.label).size();

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
        return (double) this.numberOfSampleForMostFrequentLabel / this.totalNumberOfLabeledSamples;
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

}
