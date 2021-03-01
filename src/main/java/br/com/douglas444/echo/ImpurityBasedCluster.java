package br.com.douglas444.echo;

import br.com.douglas444.ndc.datastructures.Sample;

import java.util.*;

public class ImpurityBasedCluster {

    private final Integer id;
    private double entropy;
    private int numberOfLabeledSamples;
    private Sample centroid;
    private final HashMap<Integer, List<Sample>> samplesByLabel;
    private final List<Sample> unlabeledSamples;

    public ImpurityBasedCluster(Integer id, Sample centroid) {

        this.id = id;
        this.centroid = centroid;

        this.numberOfLabeledSamples = 0;
        this.samplesByLabel = new HashMap<>();
        this.unlabeledSamples = new ArrayList<>();
        this.entropy = 0;
    }

    public int size() {
        return this.numberOfLabeledSamples + unlabeledSamples.size();
    }

    public ImpurityBasedCluster(Integer id, List<Sample> labeledSamples, List<Sample> unlabeledSamples) {

        if ((labeledSamples == null || labeledSamples.isEmpty()) &&
                (unlabeledSamples == null || unlabeledSamples.isEmpty())) {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.numberOfLabeledSamples = 0;
        this.samplesByLabel = new HashMap<>();

        if (labeledSamples != null) {
            labeledSamples.forEach(labeledSample -> {
                this.samplesByLabel.putIfAbsent(labeledSample.getY(), new ArrayList<>());
                this.samplesByLabel.get(labeledSample.getY()).add(labeledSample);
                this.numberOfLabeledSamples++;
            });
        }

        this.unlabeledSamples = new ArrayList<>(unlabeledSamples);
        this.updateEntropy();
    }

    public void addUnlabeledSample(Sample sample) {
        this.unlabeledSamples.add(sample);
    }

    public void addLabeledSample(Sample sample) {
        this.samplesByLabel.putIfAbsent(sample.getY(), new ArrayList<>());
        this.samplesByLabel.get(sample.getY()).add(sample);
        this.numberOfLabeledSamples++;
    }

    public void removeUnlabeledSample(Sample sample) {
        this.unlabeledSamples.remove(sample);
    }

    public void removeLabeledSample(Sample sample) {

        if (this.samplesByLabel.containsKey(sample.getY())) {
            this.samplesByLabel.get(sample.getY()).remove(sample);
        }

        this.numberOfLabeledSamples--;
    }

    public void updateEntropy() {

        this.entropy = this.samplesByLabel.keySet()
                .stream()
                .map(this::calculateLabelProbability)
                .map(p -> -p * Math.log(p))
                .reduce(0.0, Double::sum);

    }

    public Sample getCentroid() {
        return this.centroid;
    }

    public double getEntropy() {
        return this.entropy;
    }

    public double calculateLabelProbability(Integer label) {

        return (double) this.samplesByLabel.get(label).size() / this.numberOfLabeledSamples;

    }

    public void updateCentroid() {

        final List<Sample> samples = new ArrayList<>();
        this.samplesByLabel.values().forEach(samples::addAll);
        samples.addAll(this.unlabeledSamples);

        final Sample centroid = samples.get(0).copy();

        if (samples.size() > 1) {
            samples.subList(1, samples.size()).forEach(centroid::sum);
        }

        centroid.divide(samples.size());
        this.centroid = centroid;

    }

    public double calculateStandardDeviation() {

        final List<Sample> samples = new ArrayList<>();
        this.samplesByLabel.values().forEach(samples::addAll);
        samples.addAll(this.unlabeledSamples);

        final Sample centroid = this.getCentroid();

        final double sum = samples
                .stream()
                .mapToDouble(sample -> Math.pow(sample.distance(centroid), 2))
                .sum();

        return Math.sqrt(sum / samples.size());
    }

    public double calculateRadius() {

        final List<Sample> samples = new ArrayList<>();
        this.samplesByLabel.values().forEach(samples::addAll);
        samples.addAll(this.unlabeledSamples);

        return Collections.max(samples, Comparator.comparing(sample -> centroid.distance(sample)))
                .distance(centroid);

    }

    public List<Sample> getSamples() {
        final List<Sample> samples = new ArrayList<>();
        this.samplesByLabel.values().forEach(samples::addAll);
        samples.addAll(this.unlabeledSamples);
        return samples;
    }

    public Integer getMostFrequentLabel() {

        return Collections.max(this.samplesByLabel.keySet(),
                Comparator.comparing(label -> this.samplesByLabel.get(label).size()));
    }

    public int dissimilarityCount(final Sample labeledSample) {

        if (!this.samplesByLabel.containsKey(labeledSample.getY())) {
            return this.numberOfLabeledSamples;
        }

        return this.numberOfLabeledSamples - this.samplesByLabel.get(labeledSample.getY()).size();

    }

    public int getNumberOfLabeledSamples() {
        return numberOfLabeledSamples;
    }

    public HashMap<Integer, List<Sample>> getSamplesByLabel() {
        return samplesByLabel;
    }

    public List<Sample> getUnlabeledSamples() {
        return unlabeledSamples;
    }

    public Integer getId() {
        return id;
    }

}
