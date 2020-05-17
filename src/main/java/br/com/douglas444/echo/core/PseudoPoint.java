package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PseudoPoint {

    private Sample centroid;
    private double radius;
    private HashMap<Integer, Integer> numberOfSamplesByLabel;
    private int totalNumberOfSamples;
    private int numberOfSampleForMostFrequentLabel;
    private Integer label;


    public static PseudoPoint getClosestPseudoPoint(Sample sample, List<PseudoPoint> pseudoPoints) {

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

    public double calculatePurity() {
        return (double) this.numberOfSampleForMostFrequentLabel / this.totalNumberOfSamples;
    }

    public Sample getCentroid() {
        return centroid;
    }

    public Integer getLabel() {
        return label;
    }

    public double getRadius() {
        return radius;
    }

    public int getTotalNumberOfSamples() {
        return totalNumberOfSamples;
    }

    public int getNumberOfSampleForMostFrequentLabel() {
        return numberOfSampleForMostFrequentLabel;
    }


}
