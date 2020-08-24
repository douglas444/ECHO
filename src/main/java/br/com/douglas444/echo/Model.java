package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Sample;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Model {

    private final List<PseudoPoint> pseudoPoints;
    private final HashSet<Integer> knownLabels;
    private final double accuracyAssociationCorrelation;
    private final double accuracyPurityCorrelation;

    private Model(List<PseudoPoint> pseudoPoints,
                  double accuracyAssociationCorrelation,
                  double accuracyPurityCorrelation,
                  HashSet<Integer> knownLabels) {

        this.pseudoPoints = pseudoPoints;
        this.accuracyAssociationCorrelation = accuracyAssociationCorrelation;
        this.accuracyPurityCorrelation = accuracyPurityCorrelation;
        this.knownLabels = knownLabels;
    }

    static Model fit(final List<Sample> labeledSamples, List<PseudoPoint> pseudoPoints) {

        final HashSet<Integer> knowLabels = new HashSet<>();

        pseudoPoints = pseudoPoints
                .stream()
                .peek(pseudoPoint -> knowLabels.add(pseudoPoint.getLabel()))
                .collect(Collectors.toCollection(ArrayList::new));

        final double[] hits = new double[labeledSamples.size()];
        final double[] associationValues = new double[labeledSamples.size()];
        final double[] purityValues = new double[labeledSamples.size()];

        for (int i = 0; i < labeledSamples.size(); i++) {

            final Sample labeledSample = labeledSamples.get(i);
            final PseudoPoint closestPseudoPoint = PseudoPoint.getClosestPseudoPoint(labeledSample, pseudoPoints);
            final double distance = closestPseudoPoint.getCentroid().distance(labeledSample);

            final boolean hit = closestPseudoPoint.getRadius() - distance >= 0 &&
                    labeledSample.getY().equals(closestPseudoPoint.getLabel());

            hits[i] = hit ? 1 : 0;
            associationValues[i] = calculateAssociation(labeledSample, closestPseudoPoint);
            purityValues[i] = closestPseudoPoint.calculatePurity();

        }

        double accuracyAssociationCorrelation = calculatePearsonCorrelationCoefficient(associationValues, hits);
        double accuracyPurityCorrelation = calculatePearsonCorrelationCoefficient(purityValues, hits);

        if (accuracyAssociationCorrelation == 0) {
            accuracyAssociationCorrelation = 1;
        }

        if (accuracyPurityCorrelation == 0) {
            accuracyPurityCorrelation = 1;
        }

        return new Model(pseudoPoints, accuracyAssociationCorrelation, accuracyPurityCorrelation, knowLabels);

    }

    Classification classify(final Sample sample) {

        final PseudoPoint closestPseudoPoint = PseudoPoint.getClosestPseudoPoint(sample, this.pseudoPoints);
        final double distance = closestPseudoPoint.getCentroid().distance(sample);

        return new Classification(
                closestPseudoPoint.getLabel(),
                sample,
                this.calculateConfidence(sample, closestPseudoPoint),
                distance <= closestPseudoPoint.getRadius(),
                false);

    }

    private double calculateConfidence(final Sample sample, final PseudoPoint closestPseudoPoint) {

        final double association = calculateAssociation(sample, closestPseudoPoint);
        final double purity = closestPseudoPoint.calculatePurity();

        return (this.accuracyAssociationCorrelation * association + this.accuracyPurityCorrelation * purity) /
                (this.accuracyAssociationCorrelation + this.accuracyPurityCorrelation) ;
    }

    private static double calculateAssociation(final Sample sample, final PseudoPoint closestPseudoPoint) {

        final double distance = sample.distance(closestPseudoPoint.getCentroid());
        final double difference = closestPseudoPoint.getRadius() - distance;

        return difference / Math.max(distance, closestPseudoPoint.getRadius());
    }

    private static double calculatePearsonCorrelationCoefficient(final double[] v1, final double[] v2) {

        if (v1.length != v2.length) {
            throw new IllegalStateException();
        }

        final int n = v1.length;

        final double v1Mean = Arrays.stream(v1).sum() / n;
        final double v2Mean = Arrays.stream(v2).sum() / n;

        final double[] v1Deviation = Arrays.stream(v1).map(x -> Math.abs(x - v1Mean)).toArray();
        final double[] v2Deviation = Arrays.stream(v2).map(x -> Math.abs(x - v2Mean)).toArray();

        final double covariance = IntStream.range(0, n).mapToDouble(i -> v1Deviation[i] * v2Deviation[i]).sum();

        if (covariance == 0) {
            return 0;
        }

        final double v1Variance = Arrays.stream(v1Deviation).map(x -> x * x).sum();
        final double v2Variance = Arrays.stream(v2Deviation).map(x -> x * x).sum();

        return covariance / (Math.sqrt(v1Variance) * Math.sqrt(v2Variance));

    }

    public HashSet<Integer> getKnownLabels() {
        return knownLabels;
    }

    public List<Sample> getPseudoPointsCentroid() {
        return pseudoPoints.stream()
                .map(PseudoPoint::getCentroid)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void merge(List<PseudoPoint> pseudoPoints) {
        this.pseudoPoints.addAll(pseudoPoints);
    }
}
