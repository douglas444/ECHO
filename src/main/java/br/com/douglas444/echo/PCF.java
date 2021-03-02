package br.com.douglas444.echo;

import br.com.douglas444.ndc.datastructures.Cluster;
import br.com.douglas444.ndc.datastructures.Sample;
import br.ufu.facom.pcf.core.Category;
import br.ufu.facom.pcf.core.ClusterSummary;
import br.ufu.facom.pcf.core.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PCF {


    public static Context buildContext(final Cluster pattern,
                                       final Category category,
                                       final List<Model> ensemble) {

        return buildContext(
                pattern,
                null,
                category,
                ensemble);

    }

    public static Context buildContext(final ImpurityBasedCluster pattern,
                                       final List<Model> ensemble) {

        return buildContext(
                new Cluster(pattern.getSamples()),
                pattern.getMostFrequentLabel(),
                null,
                ensemble);

    }

    public static Context buildContext(final Cluster pattern,
                                       final Integer label,
                                       final Category category,
                                       final List<Model> ensemble) {

        final Set<Integer> knownLabels = new HashSet<>();

        ensemble.stream()
                .map(Model::getKnownLabels)
                .forEach(knownLabels::addAll);

        final Context context = new Context();
        context.setPatternClusterSummary(toClusterSummary(pattern, label));
        context.setKnownLabels(knownLabels);

        final List<Sample> samples = pattern.getSamples();

        final int sentence = samples.stream()
                .map(sample -> !knownLabels.contains(sample.getY()))
                .map(isNovel -> isNovel ? 1 : -1)
                .reduce(0, Integer::sum);

        if (sentence / (double) samples.size() >= 0) {
            context.setRealCategory(Category.NOVELTY);
        } else {
            context.setRealCategory(Category.KNOWN);
        }

        final List<PseudoPoint> pseudoPoints = new ArrayList<>();
        ensemble.stream().map(Model::getPseudoPoints).forEach(pseudoPoints::addAll);

        final List<ClusterSummary> knownClusterSummaries = pseudoPoints
                .stream()
                .map(PCF::toClusterSummary)
                .collect(Collectors.toList());

        context.setClusterSummaries(knownClusterSummaries);

        context.setSamplesAttributes(samples
                .stream()
                .map(Sample::getX)
                .map(double[]::clone)
                .collect(Collectors.toList()));

        context.setSamplesLabels(samples
                .stream()
                .map(Sample::getY)
                .collect(Collectors.toList()));

        if (category == null && knownLabels.contains(label)) {
            context.setPredictedCategory(Category.KNOWN);
        } else if (category == null) {
            context.setPredictedCategory(Category.NOVELTY);
        } else {
            context.setPredictedCategory(category);
        }

        return context;
    }

    static ClusterSummary toClusterSummary(final Cluster cluster, final Integer y) {

        return new ClusterSummary(){

            final double[] centroidAttributes = cluster.calculateCentroid().getX().clone();
            final double standardDeviation = cluster.calculateStandardDeviation();
            final Integer label = y;

            @Override
            public double[] getCentroidAttributes() {
                return centroidAttributes;
            }

            @Override
            public double getStandardDeviation() {
                return standardDeviation;
            }

            @Override
            public Integer getLabel() {
                return label;
            }
        };
    }

    static ClusterSummary toClusterSummary(final PseudoPoint pseudoPoint) {

        return new ClusterSummary(){

            final double[] centroidAttributes = pseudoPoint.getCentroid().getX().clone();
            final double standardDeviation = pseudoPoint.getRadius() / 2;
            final Integer label = pseudoPoint.getLabel();

            @Override
            public double[] getCentroidAttributes() {
                return centroidAttributes;
            }

            @Override
            public double getStandardDeviation() {
                return standardDeviation;
            }

            @Override
            public Integer getLabel() {
                return label;
            }
        };
    }

}
