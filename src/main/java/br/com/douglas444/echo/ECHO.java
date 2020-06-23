package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class ECHO {

    private int timestamp;
    private boolean warmed;

    private final List<Sample> filteredOutlierBuffer;
    private final List<Model> ensemble;
    private final List<ClassifiedSample> window;
    private final Heater heater;

    private final int q;
    private final int k;
    private final double gamma;
    private final double sensitivity;
    private final double confidenceThreshold;
    private final int filteredOutlierBufferMaxSize;
    private final int confidenceWindowMaxSize;
    private final int ensembleSize;
    private final long randomGeneratorSeed;

    public ECHO(int q,
                int k,
                double gamma,
                double sensitivity,
                double confidenceThreshold,
                int filteredOutlierBufferMaxSize,
                int confidenceWindowMaxSize,
                int ensembleSize,
                int randomGeneratorSeed,
                int chunkSize) {

        this.q = q;
        this.k = k;
        this.gamma = gamma;
        this.sensitivity = sensitivity;
        this.confidenceThreshold = confidenceThreshold;
        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
        this.confidenceWindowMaxSize = confidenceWindowMaxSize;
        this.ensembleSize = ensembleSize;
        this.randomGeneratorSeed = randomGeneratorSeed;

        this.timestamp = 1;
        this.warmed = false;

        this.filteredOutlierBuffer = new ArrayList<>();
        this.ensemble = new ArrayList<>();
        this.window = new ArrayList<>();
        this.heater = new Heater(chunkSize, this.k, this.randomGeneratorSeed);
    }

    public Optional<Integer> process(final Sample sample) {

        sample.setT(this.timestamp++);

        if (!this.warmed) {
            this.warmUp(sample);
            return Optional.empty();
        }

        final Optional<ClassifiedSample> classifiedSample = this.classify(sample);

        if (classifiedSample.isPresent()) {

            this.window.add(classifiedSample.get());
            final Optional<Integer> changePoint = this.changeDetection();
            changePoint.ifPresent(this::updateClassifier);
            return Optional.of(classifiedSample.get().getLabel());

        } else {

            if (this.filteredOutlierBuffer.size() < this.filteredOutlierBufferMaxSize) {
                this.filteredOutlierBuffer.add(sample);
            } else {
                this.filteredOutlierBuffer.add(sample);
                this.novelClassDetection();
            }
            return  Optional.empty();

        }
    }

    private Optional<ClassifiedSample> classify(final Sample sample) {

        final List<ClassifiedSample> classifications = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(ArrayList::new));

        double ensembleConfidence = calculateConfidence(classifications);

        final HashMap<Integer, Integer> votesByLabel = new HashMap<>();
        classifications.forEach(classifiedSample -> {
            votesByLabel.putIfAbsent(classifiedSample.getLabel(), 0);
            Integer votes = votesByLabel.get(classifiedSample.getLabel());
            votesByLabel.put(classifiedSample.getLabel(), votes + 1);
        });

        return votesByLabel
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new ClassifiedSample(entry.getKey(), sample, ensembleConfidence));

    }

    private static double calculateConfidence(List<ClassifiedSample> classifiedSamples) {

        final List<Double> confidenceValues = getConfidenceList(classifiedSamples);

        final Double maxConfidence = confidenceValues.stream().max(Double::compareTo).orElse(1.0);

        return confidenceValues
                .stream()
                .map(confidenceValue -> confidenceValue / maxConfidence)
                .reduce(0.0, Double::sum) / confidenceValues.size();

    }

    private Optional<Integer> changeDetection() {

        final int n = this.window.size();
        final double meanConfidence = getConfidenceList(this.window).stream().reduce(0.0, Double::sum) / n;
        final int cushion = Math.max(100, (int) Math.floor(Math.pow(n, this.gamma)));

        if ((n > 2 * cushion && meanConfidence <= 0.3) || n > this.confidenceWindowMaxSize) {
            return Optional.of(n);
        }


        double maxLLRS = 0; //LLRS stands for Log Likelihood Ratio Sum
        int maxLLRSIndex = -1;

        for (int i = cushion; i <= n - cushion; ++i) {

            final BetaDistribution preBeta = estimateBetaDistribution(
                    getConfidenceList(this.window).subList(0, i));

            final BetaDistribution postBeta = estimateBetaDistribution(
                    getConfidenceList(this.window).subList(i, n));

            final double lLRS = getConfidenceList(this.window).subList(i + 1, n)
                    .stream()
                    .map(x -> preBeta.density(x) / postBeta.density(x))
                    .map(Math::log)
                    .reduce(0.0, Double::sum);

            if (lLRS > maxLLRS) {
                maxLLRS = lLRS;
                maxLLRSIndex = i;
            }

        }

        if (maxLLRSIndex != -1 && n >= 100 && meanConfidence < 0.3) {
            return Optional.of(n);
        }

        if (maxLLRSIndex != -1 && maxLLRS > -Math.log(this.sensitivity)) {
            return Optional.of(maxLLRSIndex);
        }

        return Optional.empty();
    }

    private static BetaDistribution estimateBetaDistribution(final List<Double> data) {

        final double mean = data.stream().reduce(0.0, Double::sum) / data.size();

        final double variance = data
                .stream()
                .map(value -> Math.abs(value - mean))
                .map(value -> value * value)
                .reduce(0.0, Double::sum) / data.size();

        final double alpha = ((Math.pow(mean, 2) - Math.pow(mean, 3)) / variance) - mean;
        final double beta = alpha * (((double) 1 / mean) - 1);

        return new BetaDistribution(alpha, beta);
    }

    private void novelClassDetection() {

        final HashSet<Sample> samples = new HashSet<>(this.filteredOutlierBuffer);

        this.filteredOutlierBuffer.forEach(fOutlier -> {

            for (Model model : this.ensemble) {
                double qNSC = calculateQNeighborhoodSilhouetteCoefficient(fOutlier, model, this.q);
                if (qNSC < 0) {
                    samples.remove(fOutlier);
                    break;
                }
            }

        });

        if (samples.size() > this.q) {
            System.out.println("Nova classe detectada");
        }

    }

    private double calculateQNeighborhoodSilhouetteCoefficient(final Sample sample, final Model model, final int q) {

        double outMeanDistance = getQNearestNeighbors(sample, this.filteredOutlierBuffer, q).stream()
                .map(sample::distance)
                .reduce(0.0, Double::sum);

        double minLabelMeanDistance = -1;

        for (Integer label : model.getKnownLabels()) {

            double labelMeanDistance = getQNearestNeighbors(sample, model.getPseudoPointsCentroid(), q).stream()
                    .filter(centroid -> centroid.getY().equals(label))
                    .map(sample::distance)
                    .reduce(0.0, Double::sum);

            if (minLabelMeanDistance == -1 || labelMeanDistance < minLabelMeanDistance) {
                minLabelMeanDistance = labelMeanDistance;
            }

        }

        return (minLabelMeanDistance - outMeanDistance) / Math.max(minLabelMeanDistance, outMeanDistance);

    }

    private static List<Sample> getQNearestNeighbors(final Sample sample, final List<Sample> samples, final int q) {

        List<Sample> sampleList = new ArrayList<>(samples);
        sampleList.remove(sample);
        sampleList.sort(new SampleDistanceComparator(sample));

        int n = q;

        if (n > sampleList.size()) {
            n = sampleList.size();
        }

        return sampleList.subList(0, n);
    }

    private void updateClassifier(int changePoint) {

        final List<Sample> labeledSamples = new ArrayList<>();
        final List<ClassifiedSample> classifiedSamples = new ArrayList<>();

        this.window.stream()
                .filter(classifiedSample -> classifiedSample.getConfidence() > this.confidenceThreshold)
                .forEach(classifiedSamples::add);

        this.window.stream()
                .filter(classifiedSample -> classifiedSample.getConfidence() <= this.confidenceThreshold)
                .map(ClassifiedSample::getSample)
                .forEach(labeledSamples::add);

        final Model model = Model.fit(labeledSamples, classifiedSamples, this.k, this.randomGeneratorSeed);
        this.ensemble.remove(0);
        this.ensemble.add(model);
        this.window.removeAll(this.window.subList(0, changePoint));

    }

    private void warmUp(final Sample sample) {

        assert !warmed;

        if (this.heater.getEnsembleSize() < this.ensembleSize) {
            this.heater.process(sample);
        } else {
            this.warmed = true;
            this.ensemble.addAll(this.heater.getResult());
        }

    }

    private static List<Double> getConfidenceList(List<ClassifiedSample> classifiedSamples) {
        return classifiedSamples.stream()
                .map(ClassifiedSample::getConfidence)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
