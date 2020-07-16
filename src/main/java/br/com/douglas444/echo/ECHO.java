package br.com.douglas444.echo;

import br.com.douglas444.mltk.datastructure.DynamicConfusionMatrix;
import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.*;
import java.util.stream.Collectors;

import static br.com.douglas444.echo.ClassificationResult.getConfidenceList;

public class ECHO {

    private int timestamp;
    private boolean warmed;
    private int numberOfLabeledSamples;

    private final List<Sample> filteredOutlierBuffer;
    private final List<Model> ensemble;
    private final List<ClassificationResult> window;
    private final Heater heater;
    private final int q;
    private final int k;
    private final double gamma;
    private final double sensitivity;
    private final double confidenceThreshold;
    private final int filteredOutlierBufferMaxSize;
    private final int confidenceWindowMaxSize;
    private final int ensembleSize;
    private final Random random;
    private final DynamicConfusionMatrix confusionMatrix;

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
        this.random = new Random(randomGeneratorSeed);

        this.timestamp = 1;
        this.warmed = false;
        this.numberOfLabeledSamples = 0;

        this.filteredOutlierBuffer = new ArrayList<>();
        this.ensemble = new ArrayList<>();
        this.window = new ArrayList<>();
        this.heater = new Heater(chunkSize, this.k, this.random);

        this.confusionMatrix = new DynamicConfusionMatrix();

    }

    public ClassificationResult process(final Sample sample) {

        if (!this.warmed) {
            this.warmUp(sample);
            return new ClassificationResult(null, sample, 0.0, false);
        }

        sample.setT(this.timestamp);

        final ClassificationResult classificationResult = this.classify(sample);

        classificationResult.ifExplainedOrElse((label, confidence) -> {

            this.window.add(classificationResult);
            this.confusionMatrix.addPrediction(sample.getY(), label, false);

            if (confidence < this.confidenceThreshold) {
                this.changeDetection().ifPresent(this::updateClassifier);
            } else if (this.window.size() == this.confidenceWindowMaxSize) {
                this.window.remove(0);
            }

        }, () -> {

            this.filteredOutlierBuffer.add(sample);
            this.confusionMatrix.addUnknown(sample.getY());
            if (this.filteredOutlierBuffer.size() == this.filteredOutlierBufferMaxSize) {
                this.novelClassDetection();
            }

        });

        ++this.timestamp;

        return classificationResult;
    }

    private ClassificationResult classify(final Sample sample) {

        final List<ClassificationResult> classifications = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .collect(Collectors.toCollection(ArrayList::new));

        final HashMap<Integer, Integer> votesByLabel = new HashMap<>();

        boolean isOutlier = true;

        for (ClassificationResult classification : classifications) {

            if (classification.isExplained()) {
                isOutlier = false;
            }

            classification.ifExplained((label, confidence) -> {
                votesByLabel.putIfAbsent(label, 0);
                Integer votes = votesByLabel.get(label);
                votesByLabel.put(label, votes + 1);
            });

        }

        assert !votesByLabel.isEmpty();

        final Integer votedLabel;
        final double ensembleConfidence;

        if (isOutlier) {
            votedLabel = null;
            ensembleConfidence = 0;
        } else {
            votedLabel = Collections.max(votesByLabel.entrySet(), Map.Entry.comparingByValue()).getKey();
            ensembleConfidence = calculateConfidence(votedLabel, classifications);
        }

        return new ClassificationResult(votedLabel, sample, ensembleConfidence, !isOutlier);

    }

    private static double calculateConfidence(final Integer votedLabel,
                                              final List<ClassificationResult> classificationResults) {

        final Double maxConfidence = getConfidenceList(classificationResults)
                .stream()
                .max(Double::compareTo)
                .orElse(0.0);

        final Double minConfidence = getConfidenceList(classificationResults)
                .stream()
                .min(Double::compareTo)
                .orElse(0.0);

        final List<ClassificationResult> classificationResultsForVotedClass = classificationResults
                .stream()
                .filter(classificationResult ->
                    classificationResult
                            .getLabel()
                            .map(integer -> integer.equals(votedLabel))
                            .orElse(false))
                .collect(Collectors.toList());

        final List<Double> confidenceValues = getConfidenceList(classificationResultsForVotedClass);

        return confidenceValues
                .stream()
                .map(confidenceValue -> {
                    if (minConfidence.equals(maxConfidence)) {
                        return confidenceValue;
                    } else {
                        return (confidenceValue - minConfidence) / (maxConfidence - minConfidence);
                    }
                })
                .reduce(0.0, Double::sum) / classificationResults.size();


    }

    private boolean meanShiftDetection(final List<Double> preConfidence, final List<Double> postConfidence) {

        final double postMean = postConfidence.stream().reduce(0.0, Double::sum) / postConfidence.size();
        final double preMean = preConfidence.stream().reduce(0.0, Double::sum) / preConfidence.size();

        return (preMean - postMean) >= this.sensitivity;

    }

    private Optional<Integer> changeDetection() {

        final int n = this.window.size();
        final double meanConfidence = getConfidenceList(this.window).stream().reduce(0.0, Double::sum) / n;
        final int cushion = Math.max(100, (int) Math.floor(Math.pow(n, this.gamma)));

        if ((n > 2 * cushion && meanConfidence <= 0.3) || n == this.confidenceWindowMaxSize) {
            return Optional.of(n);
        }

        double maxLLRS = 0; //LLRS stands for Log Likelihood Ratio Sum
        int maxLLRSIndex = -1;

        for (int i = cushion; i <= n - cushion; ++i) {

            final List<Double> preConfidenceList = getConfidenceList(this.window).subList(0, i);
            final List<Double> postConfidenceList = getConfidenceList(this.window).subList(i, n);

            if (meanShiftDetection(preConfidenceList, postConfidenceList)) {

                final BetaDistribution preBeta = estimateBetaDistribution(preConfidenceList);
                final BetaDistribution postBeta = estimateBetaDistribution(postConfidenceList);

                final double lLRS = getConfidenceList(this.window).subList(i + 1, n)
                        .stream()
                        .map(x -> {
                            if (x > 0.995) {
                                x = 0.995;
                            } else if (x < 0.005) {
                                x = 0.005;
                            }
                            return preBeta.density(x) / postBeta.density(x);
                        })
                        .map(Math::log)
                        .reduce(0.0, Double::sum);

                if (lLRS > maxLLRS) {
                    maxLLRS = lLRS;
                    maxLLRSIndex = i;
                }

            }

        }

        if (maxLLRSIndex != -1 && n >= 100 && meanConfidence < 0.3) {
            return Optional.of(n);
        }

        if (maxLLRSIndex != -1 && maxLLRS >= -Math.log(this.sensitivity)) {
            System.out.println("Mudança de conceito detectada - ponto de mudança " + maxLLRSIndex + " - time " + this.timestamp + " - window size " + this.window.size());
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

        final List<Sample> sampleList = new ArrayList<>(samples);
        sampleList.remove(sample);
        sampleList.sort(new SampleDistanceComparator(sample));

        int n = q;

        if (n > sampleList.size()) {
            n = sampleList.size();
        }

        return sampleList.subList(0, n);
    }

    private void updateClassifier(final int changePoint) {

        final List<ClassificationResult> classificationResults = new ArrayList<>();

        this.window.stream()
                .filter(classificationResult -> classificationResult.getConfidence() > this.confidenceThreshold)
                .forEach(classificationResults::add);

        final List<Sample> labeledSamples = new ArrayList<>();

        this.window.stream()
                .filter(classificationResult -> classificationResult.getConfidence() <= this.confidenceThreshold)
                .map(ClassificationResult::getSample)
                .forEach(labeledSamples::add);

        this.numberOfLabeledSamples += labeledSamples.size();

        final Model model = Model.fit(labeledSamples, classificationResults, this.k, this.random);
        this.ensemble.remove(0);
        this.ensemble.add(model);
        this.window.removeAll(this.window.subList(0, changePoint));

    }

    private void warmUp(final Sample sample) {

        assert !warmed;

        if (!this.confusionMatrix.isLabelKnown(sample.getY())) {
            this.confusionMatrix.addKnownLabel(sample.getY());
        }

        this.heater.process(sample);

        if (this.heater.getEnsembleSize() == this.ensembleSize) {
            this.warmed = true;
            this.ensemble.addAll(this.heater.getResult());
        }

    }

    public int getNumberOfLabeledSamples() {
        return numberOfLabeledSamples;
    }

    public long getTimestamp() {
        return timestamp - 1;
    }

    public DynamicConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }
}
