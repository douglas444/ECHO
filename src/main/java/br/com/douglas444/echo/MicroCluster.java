package br.com.douglas444.echo;

import br.com.douglas444.ndc.datastructures.Cluster;
import br.com.douglas444.ndc.datastructures.Sample;
import br.com.douglas444.ndc.datastructures.SampleDistanceComparator;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MicroCluster {

    private final Integer label;
    private int n;
    private final double[] ls;
    private final double[] ss;

    public MicroCluster(Cluster cluster, Integer label) {

        if (cluster.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final int dimensions = cluster.getSamples().get(0).getX().length;

        this.n = 0;
        this.ls = new double[dimensions];
        this.ss = new double[dimensions];
        this.label = label;

        cluster.getSamples().forEach(this::update);
    }

    public void update(final Sample sample) {

        for (int i = 0; i < sample.getX().length; ++i) {
            this.ls[i] += sample.getX()[i];
            this.ss[i] += sample.getX()[i] * sample.getX()[i];
        }

        ++this.n;
    }

    public Sample calculateCentroid() {

        final double[] x = this.ls.clone();
        for (int i = 0; i < x.length; ++i) {
            x[i] /= this.n;
        }

        return new Sample(x, this.label);
    }

    public double calculateStandardDeviation() {

        double sum = 0;

        for (int i = 0; i < this.ss.length; ++i) {
            sum += (this.ss[i] / this.n) - Math.pow(this.ls[i] / this.n, 2);
        }

        return Math.sqrt(sum);

    }

    public static MicroCluster calculateClosestMicroCluster(final Sample sample,
                                                            final List<MicroCluster> microClusters) {

        if (microClusters.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final HashMap<Sample, MicroCluster> microClusterByCentroid = new HashMap<>();

        final List<Sample> decisionModelCentroids = microClusters.stream()
                .map(microCluster -> {
                    Sample microClusterCentroid = microCluster.calculateCentroid();
                    microClusterByCentroid.put(microClusterCentroid, microCluster);
                    return microClusterCentroid;
                })
                .sorted(new SampleDistanceComparator(sample))
                .collect(Collectors.toList());

        final Sample closestCentroid = decisionModelCentroids.get(0);
        return microClusterByCentroid.get(closestCentroid);
    }

    public Integer getLabel() {
        return label;
    }

}
