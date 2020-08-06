package br.com.douglas444.echo;

import br.com.douglas444.mltk.clustering.kmeans.KMeansPlusPlus;
import br.com.douglas444.mltk.datastructure.DynamicConfusionMatrix;
import br.com.douglas444.mltk.datastructure.Sample;
import br.com.douglas444.mltk.util.SampleDistanceComparator;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.*;
import java.util.stream.Collectors;

import static br.com.douglas444.echo.Classification.getConfidenceList;

public class ECHO {

    private int timestamp;
    private boolean warmed;
    private int labeledSamplesCount;
    private int conceptDriftsCount;
    private double confidenceSum;
    private int classificationsCount;
    private int noveltyCount;

    private final HashMap<Integer, StatElement> stat;
    private final List<Sample> filteredOutlierBuffer;
    private final List<Model> ensemble;
    private final List<MicroCluster> noveltyMicroClusters;
    private final List<Classification> window;
    private final Heater heater;
    private final int q;
    private final int k;
    private final double gamma;
    private final double sensitivity;
    private final double confidenceThreshold;
    private final double activeLearningThreshold;
    private final int filteredOutlierBufferMaxSize;
    private final int confidenceWindowMaxSize;
    private final int ensembleSize;
    private final int chunkSize;
    private final Random random;
    private final DynamicConfusionMatrix confusionMatrix;

    public ECHO(int q,
                int k,
                double gamma,
                double sensitivity,
                double confidenceThreshold,
                double activeLearningThreshold,
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
        this.activeLearningThreshold = activeLearningThreshold;
        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
        this.confidenceWindowMaxSize = confidenceWindowMaxSize;
        this.ensembleSize = ensembleSize;
        this.chunkSize = chunkSize;
        this.random = new Random(randomGeneratorSeed);

        this.timestamp = 1;
        this.warmed = false;
        this.labeledSamplesCount = 0;
        this.conceptDriftsCount = 0;
        this.confidenceSum = 0;
        this.noveltyCount = 0;
        this.classificationsCount = 0;

        this.stat = new HashMap<>();
        this.filteredOutlierBuffer = new ArrayList<>();
        this.ensemble = new ArrayList<>();
        this.noveltyMicroClusters = new ArrayList<>();
        this.window = new ArrayList<>();
        this.heater = new Heater(chunkSize, this.k, this.random);

        this.confusionMatrix = new DynamicConfusionMatrix();

    }

    public Classification process(final Sample sample) {

        if (!this.warmed) {
            this.warmUp(sample);
            return new Classification(null, sample, 0.0, false, false);
        }

        sample.setT(this.timestamp);
        final Classification classification = classify(sample);

        if (classification.isExplained()) {

            if (!classification.isNovelty()) {

                if (classification.getConfidence() < this.confidenceThreshold
                        || this.window.size() == this.confidenceWindowMaxSize) {

                    this.changeDetection().ifPresent(this::updateClassifier);
                }

                this.window.add(classification);
                this.confidenceSum += classification.getConfidence();
                ++this.classificationsCount;
            }
            this.confusionMatrix.addPrediction(sample.getY(), classification.getLabel(), classification.isNovelty());

        } else {

            this.filteredOutlierBuffer.add(sample);
            this.confusionMatrix.addUnknown(sample.getY());
            if (this.filteredOutlierBuffer.size() >= this.filteredOutlierBufferMaxSize) {
                this.novelClassDetection();
            }
        }

        ++this.timestamp;
        return classification;
    }



    private Classification classify(final Sample sample) {

        final List<Classification> classifications = this.ensemble.stream()
                .map(model -> model.classify(sample))
                .collect(Collectors.toList());

        final HashMap<Integer, Integer> votesByLabel = new HashMap<>();

        classifications
                .stream()
                .filter(Classification::isExplained)
                .forEach(classification -> {
                    if (classification.isExplained()) {

                        Integer label = classification.getLabel();

                        votesByLabel.putIfAbsent(label, 0);
                        Integer votes = votesByLabel.get(label);
                        votesByLabel.put(label, votes + 1);
                    }
                });

        final Integer votedLabel;
        final double ensembleConfidence;
        final boolean explained;
        final boolean novelty;

        if (votesByLabel.isEmpty()) {

            if (!this.noveltyMicroClusters.isEmpty()) {

                final MicroCluster closest = MicroCluster.calculateClosestMicroCluster(sample,
                        this.noveltyMicroClusters);

                final double distance = sample.distance(closest.calculateCentroid());

                if (distance <= 2 * closest.calculateStandardDeviation()) {
                    votedLabel = closest.getLabel();
                    ensembleConfidence = 1;
                    explained = true;
                    novelty = true;
                } else {
                    votedLabel = null;
                    ensembleConfidence = 0;
                    explained = false;
                    novelty = false;
                }

            } else {
                votedLabel = null;
                ensembleConfidence = 0;
                explained = false;
                novelty = false;
            }

        } else {
            votedLabel = Collections.max(votesByLabel.entrySet(), Map.Entry.comparingByValue()).getKey();
            ensembleConfidence = calculateConfidence(votedLabel, classifications);
            explained = true;
            novelty = false;
        }

        return new Classification(votedLabel, sample, ensembleConfidence, explained, novelty);


    }

    private static double calculateConfidence(final Integer votedLabel,
                                              final List<Classification> classifications) {

        final List<Classification> classificationsForVotedClass = classifications
                .stream()
                .filter(classification -> classification.getLabel().equals(votedLabel))
                .collect(Collectors.toList());

        final List<Double> confidenceValues = getConfidenceList(classificationsForVotedClass);

        return confidenceValues
                .stream()
                .reduce(0.0, Double::sum) / classifications.size();

    }

    private boolean meanShiftDetection(int i) {

        final List<Double> preConfidenceList = getConfidenceList(this.window).subList(0, i);
        final List<Double> postConfidenceList = getConfidenceList(this.window).subList(i, this.window.size());

        final double postMean = postConfidenceList.stream().reduce(0.0, Double::sum) / postConfidenceList.size();
        final double preMean;

        if (this.stat.containsKey(i)) {
            preMean = this.stat.get(i).getPreMean();
        } else {
            preMean = preConfidenceList.stream().reduce(0.0, Double::sum) / preConfidenceList.size();
            this.stat.put(i, new StatElement(preMean));
        }
        return (preMean - postMean) >= this.sensitivity;

    }

    private double calculateLogLikelihoodRatioSum(final int i, final int n, final BetaDistribution preBeta) {

        final BetaDistribution postBeta = estimateBetaDistribution(getConfidenceList(this.window).subList(i, n));

        return getConfidenceList(this.window).subList(i, n)
                .stream()
                .map(x -> {
                    if (x > 0.995) {
                        x = 0.995;
                    } else if (x < 0.005) {
                        x = 0.005;
                    }
                    return Math.log(postBeta.density(x) / preBeta.density(x));
                })
                .reduce(0.0, Double::sum);

    }

    private double recursiveLogLikelihoodRatioSum(final int i, final int n, final double cushion) {

        if (n <= 2 * cushion) {

            return 0;

        } else if (this.stat.get(i).getM() == -1) {

            final BetaDistribution preBeta = estimateBetaDistribution(
                    getConfidenceList(this.window).subList(0, i));

            final double logLikelihoodRatioSum = calculateLogLikelihoodRatioSum(i + 1, n, preBeta);

            this.stat.get(i).setM(n);
            this.stat.get(i).setLogLikelihoodRatioSum(logLikelihoodRatioSum);
            this.stat.get(i).setPreBeta(preBeta);

            return logLikelihoodRatioSum;

        } else {

            final StatElement statElement = this.stat.get(i);

            final double logLikelihoodRatioSum = calculateLogLikelihoodRatioSum(
                    statElement.getM() + 1, n, statElement.getPreBeta());

            return statElement.getLogLikelihoodRatioSum() + logLikelihoodRatioSum;

        }
    }

    private Optional<Integer> changeDetection() {

        final int n = this.window.size();
        final double meanConfidence = getConfidenceList(this.window).stream().reduce(0.0, Double::sum) / n;
        final int cushion = Math.max(100, (int) Math.floor(Math.pow(n, this.gamma)));

        if ((n > 2 * cushion && meanConfidence <= 0.3) || n >= this.confidenceWindowMaxSize) {
            return Optional.of(n);
        }

        double maxLogLikelihoodRatioSum = 0;
        int maxIndex = -1;

        for (int i = cushion; i <= n - cushion; ++i) {

            if (meanShiftDetection(i)) {

                final double logLikelihoodRatioSum = recursiveLogLikelihoodRatioSum(i, n, cushion);

                if (logLikelihoodRatioSum > maxLogLikelihoodRatioSum) {
                    maxLogLikelihoodRatioSum = logLikelihoodRatioSum;
                    maxIndex = i;
                }
            }
        }

        if (maxLogLikelihoodRatioSum != -1 && n >= 100 && meanConfidence < 0.3) {
            return Optional.of(n);
        }

        if (maxIndex != -1 && maxLogLikelihoodRatioSum >= -Math.log(this.sensitivity)) {
            ++this.conceptDriftsCount;
            return Optional.of(maxIndex);
        }

        return Optional.empty();
    }

    private static BetaDistribution estimateBetaDistribution(final List<Double> data) {

        final double mean = data.stream().reduce(0.0, Double::sum) / data.size();

        final double variance = data
                .stream()
                .map(value -> Math.abs(value - mean) * Math.abs(value - mean))
                .reduce(0.0, Double::sum) / data.size();

        final double alpha = ((Math.pow(mean, 2) - Math.pow(mean, 3)) / variance) - mean;
        final double beta = alpha * (((double) 1 / mean) - 1);

        return new BetaDistribution(alpha, beta);
    }

    private void novelClassDetection() { ;

        this.filteredOutlierBuffer.removeIf(p -> p.getT() < this.timestamp - this.chunkSize);

        final List<Sample> samples = new ArrayList<>(this.filteredOutlierBuffer);

        this.filteredOutlierBuffer.forEach(fOutlier -> {

            for (Model model : this.ensemble) {
                final double qNSC = calculateQNeighborhoodSilhouetteCoefficient(fOutlier, model, this.q);
                if (qNSC < 0) {
                    samples.remove(fOutlier);
                    break;
                }
            }

        });

        if (samples.size() >= this.q) {

            this.filteredOutlierBuffer.removeAll(samples);

            final List<MicroCluster> microClusters = KMeansPlusPlus
                    .execute(samples, this.k, this.random)
                    .stream()
                    .peek(cluster -> {
                        for (Sample sample : cluster.getSamples()) {
                            this.confusionMatrix.updatedDelayed(sample.getY(), this.noveltyCount, true);
                        }
                    })
                    .map(cluster -> new MicroCluster(cluster, this.noveltyCount))
                    .collect(Collectors.toList());

            this.noveltyMicroClusters.addAll(microClusters);

            ++this.noveltyCount;
        }

    }

    private double calculateQNeighborhoodSilhouetteCoefficient(final Sample sample, final Model model, final int q) {

        List<Sample> qNearestNeighbors = getQNearestNeighbors(sample, this.filteredOutlierBuffer, q);

        final double outMeanDistance = qNearestNeighbors.stream()
                .map(sample::distance)
                .reduce(0.0, Double::sum) / qNearestNeighbors.size();

        double minLabelMeanDistance = -1;

        for (Integer label : model.getKnownLabels()) {

            qNearestNeighbors = getQNearestNeighbors(sample, model.getPseudoPointsCentroid(), q, label);

            final double labelMeanDistance = qNearestNeighbors
                    .stream()
                    .map(sample::distance)
                    .reduce(0.0, Double::sum) / qNearestNeighbors.size();

            if (minLabelMeanDistance == -1 || labelMeanDistance < minLabelMeanDistance) {
                minLabelMeanDistance = labelMeanDistance;
            }

        }

        return (minLabelMeanDistance - outMeanDistance) / Math.max(minLabelMeanDistance, outMeanDistance);

    }

    private static List<Sample> getQNearestNeighbors(final Sample targetSample, final List<Sample> samples, final int q) {

        final List<Sample> sampleList = samples
                .stream()
                .filter(sample -> !sample.equals(targetSample))
                .sorted(new SampleDistanceComparator(targetSample))
                .collect(Collectors.toList());

        int n = q;

        if (n > sampleList.size()) {
            n = sampleList.size();
        }

        return sampleList.subList(0, n);
    }

    private static List<Sample> getQNearestNeighbors(final Sample targetSample, final List<Sample> samples, final int q,
                                                     final int label) {

        final List<Sample> sampleList = samples
                .stream()
                .filter(sample -> !sample.equals(targetSample))
                .filter(sample -> sample.getY().equals(label))
                .sorted(new SampleDistanceComparator(targetSample))
                .collect(Collectors.toList());

        int n = q;

        if (n > sampleList.size()) {
            n = sampleList.size();
        }

        return sampleList.subList(0, n);
    }

    private void updateClassifier(final int changePoint) {

        final List<Classification> classifications = new ArrayList<>();

        this.window.stream()
                .filter(classification -> classification.getConfidence() >= this.activeLearningThreshold)
                .forEach(classifications::add);

        final List<Sample> labeledSamples = new ArrayList<>();

        this.window.stream()
                .filter(classification -> classification.getConfidence() < this.activeLearningThreshold)
                .map(Classification::getSample)
                .forEach(labeledSamples::add);

        this.labeledSamplesCount += labeledSamples.size();

        final Model model = Model.fit(labeledSamples, classifications, this.k, this.random);

        model.getKnownLabels().forEach((label) -> {
            if (!confusionMatrix.isLabelKnown(label)) {
                confusionMatrix.addKnownLabel(label);
            }
        });

        this.ensemble.remove(0);
        this.ensemble.add(model);
        this.window.removeAll(this.window.subList(0, changePoint));
        this.stat.clear();

    }

    private void warmUp(final Sample sample) {

        if (!this.confusionMatrix.isLabelKnown(sample.getY())) {
            this.confusionMatrix.addKnownLabel(sample.getY());
        }

        this.heater.process(sample);

        if (this.heater.getEnsembleSize() == this.ensembleSize) {
            this.warmed = true;
            this.ensemble.addAll(this.heater.getResult());
        }

    }

    public int getLabeledSamplesCount() {
        return labeledSamplesCount;
    }

    public int getConceptDriftsCount() {
        return conceptDriftsCount;
    }

    public double getMeanConfidence() {
        return confidenceSum / classificationsCount;
    }

    public long getTimestamp() {
        return timestamp - 1;
    }

    public DynamicConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }

    public double calculateCER() {
        return this.confusionMatrix.cer();
    }

    public double calculateUnkR() {
        return this.confusionMatrix.unkR();
    }

    public int getNoveltyCount() {
        return noveltyCount;
    }
}
