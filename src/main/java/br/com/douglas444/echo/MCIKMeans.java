package br.com.douglas444.echo;

import br.com.douglas444.ndc.algorithms.KMeans;
import br.com.douglas444.ndc.datastructures.Sample;

import java.util.*;

public final class MCIKMeans {

    public static List<ImpurityBasedCluster> execute(List<Sample> labeledSamples,
                                                     List<Sample> unlabeledSamples,
                                                     final int k, final Random random) {

        if (k < 2) {
            throw new IllegalArgumentException();
        }

        if (labeledSamples.size() == 0) {
            throw new IllegalArgumentException();
        }

        labeledSamples = new ArrayList<>(labeledSamples);
        unlabeledSamples = new ArrayList<>(unlabeledSamples);

        final HashMap<Integer, List<Sample>> samplesByLabel = new HashMap<>();

        labeledSamples.forEach(labeledSample -> {
            samplesByLabel.putIfAbsent(labeledSample.getY(), new ArrayList<>());
            samplesByLabel.get(labeledSample.getY()).add(labeledSample);
        });

        final List<Sample> centroids = new ArrayList<>();


        List<List<Sample>> lists = new ArrayList<>(samplesByLabel.values());
        int[] numbersOfCentroids = new int[samplesByLabel.values().size()];
        double remaining = 0;

        for (int i = 0; i < lists.size(); ++i) {
            List<Sample> samples = lists.get(i);
            double rate = k * (double) samples.size() / labeledSamples.size();
            remaining += rate - (int) rate;
            numbersOfCentroids[i] = (int) rate;
        }

        while (remaining > 0) {

            --remaining;

            int minRate = -1;
            for (int i = 0; i < numbersOfCentroids.length; ++i) {
                if (minRate == -1 || numbersOfCentroids[i] < numbersOfCentroids[minRate]) {
                    minRate = i;
                }
            }

            ++numbersOfCentroids[minRate];

        }

        for (int i = 0; i < lists.size(); ++i) {

            List<Sample> samples = lists.get(i);
            centroids.addAll(chooseCentroids(samples, numbersOfCentroids[i]));

            if (centroids.size() < numbersOfCentroids[i] && !unlabeledSamples.isEmpty()) {
                final List<Sample> fillingSamples = new ArrayList<>(unlabeledSamples);
                Collections.shuffle(fillingSamples, random);
                while (centroids.size() < numbersOfCentroids[i] && !fillingSamples.isEmpty()) {
                    centroids.add(fillingSamples.remove(0));
                }

            }

        }

        return execute(labeledSamples, unlabeledSamples, centroids, random);

    }

    private static List<ImpurityBasedCluster> execute(final List<Sample> labeledSamples,
                                                      final List<Sample> unlabeledSamples,
                                                      final List<Sample> centroids, final Random random) {

        final List<ImpurityBasedCluster> clusters = new ArrayList<>();
        final HashMap<Integer, ImpurityBasedCluster> clusterById = new HashMap<>();

        for (int i = 0; i < centroids.size(); ++i) {
            clusters.add(new ImpurityBasedCluster(i, centroids.get(i)));
            clusterById.put(i, clusters.get(i));
        }

        boolean changing;

        do {
            changing = iterativeConditionalMode(labeledSamples, unlabeledSamples, clusters, clusterById, random);
            clusters.stream().filter(cluster -> cluster.size() > 0).forEach(ImpurityBasedCluster::updateCentroid);
        } while (changing);

        clusters.removeIf(cluster -> cluster.size() == 0);

        labeledSamples.forEach(sample -> sample.setId(null));
        unlabeledSamples.forEach(sample -> sample.setId(null));

        return clusters;

    }

    private static List<Sample> chooseCentroids(final List<Sample> samples, final int k) {

        if (samples.size() <= k) {
            return samples;
        }

        final List<Sample> centroids = new ArrayList<>();
        final List<Sample> candidates = new ArrayList<>(samples);

        for (int i = 0; i < k; ++i) {
            Sample centroid = farthestFirstTraversalHeuristic(candidates, centroids);
            candidates.remove(centroid);
            centroids.add(centroid);

        }

        return centroids;

    }

    private static Sample farthestFirstTraversalHeuristic(final List<Sample> samples, final List<Sample> centroids) {

        assert !samples.isEmpty();

        Sample farthest = samples.get(0);

        final Comparator<Sample> comparator = Comparator
                .comparing((sample) -> KMeans.distanceToTheClosestCentroid(sample, centroids));

        for (int i = 1; i < samples.size(); i++) {
            final Sample sample = samples.get(i);
            if (comparator.compare(sample, farthest) > 0) {
                farthest = sample;
            }
        }

        return farthest;
    }

    private static boolean iterativeConditionalMode(List<Sample> labeledSamples,
                                                    List<Sample> unlabeledSamples,
                                                    final List<ImpurityBasedCluster> clusters,
                                                    final HashMap<Integer, ImpurityBasedCluster> clusterById,
                                                    final Random random) {

        assert !labeledSamples.isEmpty();

        labeledSamples = new ArrayList<>(labeledSamples);
        unlabeledSamples = new ArrayList<>(unlabeledSamples);

        boolean changed;
        boolean noChanges = true;

        do {

            Collections.shuffle(labeledSamples, random);
            Collections.shuffle(unlabeledSamples, random);

            int numberOfSamples = labeledSamples.size() + unlabeledSamples.size();

            changed = false;

            for (int i = 0; i < numberOfSamples; ++i) {

                final boolean isLabeled;
                final Sample sample;

                if (unlabeledSamples.isEmpty() || random.nextBoolean()) {
                    isLabeled = true;
                    sample = labeledSamples.remove(0);
                } else {
                    isLabeled = false;
                    sample = unlabeledSamples.remove(0);
                }

                final Comparator<ImpurityBasedCluster> comparator = Comparator.comparing((cluster) -> {

                    if (isLabeled) {
                        return sample.distance(cluster.getCentroid()) *
                                (1 + cluster.getEntropy() * cluster.dissimilarityCount(sample));
                    } else {
                        return sample.distance(cluster.getCentroid());
                    }

                });

                Integer oldId = sample.getId();

                if (sample.getId() != null) {
                    if (isLabeled) {
                        clusterById.get(sample.getId()).removeLabeledSample(sample);
                    } else {
                        clusterById.get(sample.getId()).removeUnlabeledSample(sample);
                    }
                    sample.setId(null);
                }

                ImpurityBasedCluster chosenCluster = clusters.get(0);

                for (int j = 1; j < clusters.size(); j++) {
                    final ImpurityBasedCluster cluster = clusters.get(j);
                    if (comparator.compare(cluster, chosenCluster) < 0) {
                        chosenCluster = cluster;
                    }
                }

                if (isLabeled) {
                    chosenCluster.addLabeledSample(sample);
                } else {
                    chosenCluster.addUnlabeledSample(sample);
                }
                sample.setId(chosenCluster.getId());

                chosenCluster.updateEntropy();

                if (!chosenCluster.getId().equals(oldId)) {

                    changed = true;
                    noChanges = false;

                }

            }

        } while (changed);

        return !noChanges;

    }

}
