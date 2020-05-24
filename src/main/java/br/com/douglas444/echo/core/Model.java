package br.com.douglas444.echo.core;

import br.com.douglas444.mltk.clustering.kmeans.MCIKMeans;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Model {

    private final List<PseudoPoint> pseudoPoints;
    private final double[] correlationVector;

    private Model(List<PseudoPoint> pseudoPoints, double[] correlationVector) {
        this.pseudoPoints = pseudoPoints;
        this.correlationVector = correlationVector;
    }


    static Model fit(final List<Sample> samples, final List<ClassifiedSample> classifiedSamples, int k,
                            long seed) {

        final List<Sample> labeledSamples = new ArrayList<>(samples);

        classifiedSamples.stream()
                .map(classifiedSample -> new Sample(classifiedSample.getSample().getX(), classifiedSample.getLabel()))
                .forEach(labeledSamples::add);

        return fit(labeledSamples, k, seed);

    }

    static Model fit(final List<Sample> labeledSamples, int k, long seed) {

        final List<PseudoPoint> pseudoPoints = MCIKMeans
                .execute(labeledSamples, new ArrayList<>(), k, seed)
                .stream()
                .map(PseudoPoint::new)
                .collect(Collectors.toCollection(ArrayList::new));

        final double[] hits = new double[labeledSamples.size()];
        final double[] associationValues = new double[labeledSamples.size()];
        final double[] purityValues = new double[labeledSamples.size()];

        for (int i = 0; i < labeledSamples.size(); i++) {

            final Sample labeledSample = labeledSamples.get(i);
            final PseudoPoint closestPseudoPoint = PseudoPoint.getClosestPseudoPoint(labeledSample, pseudoPoints);
            final double distance = closestPseudoPoint.getCentroid().distance(labeledSample);

            final boolean hit = distance <= closestPseudoPoint.getRadius()
                    && labeledSample.getY() == closestPseudoPoint.getLabel();

            hits[i] = hit ? 1 : 0;
            associationValues[i] = calculateAssociation(labeledSample, closestPseudoPoint);
            purityValues[i] = closestPseudoPoint.calculatePurity();

        }

        final double[] correlationVector = new double[]{
                calculatePearsonCorrelationCoefficient(associationValues, hits),
                calculatePearsonCorrelationCoefficient(purityValues, hits)
        };


        return new Model(pseudoPoints, correlationVector);

    }

    Optional<ClassifiedSample> classify(final Sample sample) {

        final PseudoPoint closestPseudoPoint = PseudoPoint.getClosestPseudoPoint(sample, this.pseudoPoints);
        final double distance = closestPseudoPoint.getCentroid().distance(sample);

        if (distance > closestPseudoPoint.getRadius()) {
            return Optional.empty();
        } else {

            ClassifiedSample classifiedSample = new ClassifiedSample(closestPseudoPoint.getLabel(), sample,
                    this.calculateConfidence(sample, closestPseudoPoint));

            return Optional.of(classifiedSample);
        }

    }

    private double calculateConfidence(final Sample sample, final PseudoPoint closestPseudoPoint) {

        final double association = calculateAssociation(sample, closestPseudoPoint);
        final double purity = closestPseudoPoint.calculatePurity();

        final double[] heuristicVector = {association, purity};

        return this.correlationVector[0] * heuristicVector[0]
                + this.correlationVector[1] * heuristicVector[1];

    }

    private static double calculateAssociation(final Sample sample, final PseudoPoint closestPseudoPoint) {
        return closestPseudoPoint.getRadius() - sample.distance(closestPseudoPoint.getCentroid());
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
        final double v1Variance = Arrays.stream(v1).map(x -> x * x).sum();
        final double v2Variance = Arrays.stream(v1).map(x -> x * x).sum();

        return covariance / (Math.sqrt(v1Variance) * Math.sqrt(v2Variance));

    }
}
