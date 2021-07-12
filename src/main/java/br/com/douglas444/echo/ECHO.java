package br.com.douglas444.echo;

import br.com.douglas444.streams.algorithms.KMeansPlusPlus;
import br.com.douglas444.streams.datastructures.Cluster;
import br.com.douglas444.streams.datastructures.DynamicConfusionMatrix;
import br.com.douglas444.streams.datastructures.Sample;
import br.com.douglas444.streams.datastructures.SampleDistanceComparator;
import br.ufu.facom.pcf.core.Category;
import br.ufu.facom.pcf.core.ClusterSummary;
import br.ufu.facom.pcf.core.Context;
import br.ufu.facom.pcf.core.Interceptor;
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
    private final double centroidPercentage;
    private final int mciKMeansMaxIterations;
    private final int conditionalModeMaxIterations;
    private final double gamma;
    private final double sensitivity;
    private final double confidenceThreshold;
    private final double activeLearningThreshold;
    private final int filteredOutlierBufferMaxSize;
    private final int confidenceWindowMaxSize;
    private final int ensembleSize;
    private final int chunkSize;
    private final boolean keepNoveltyDecisionModel;
    private final Random random;
    private final DynamicConfusionMatrix confusionMatrix;
    private final boolean multiClassNoveltyDetection;

    private final Interceptor interceptor;

    public ECHO(final int q,
                final int k,
                final double centroidPercentage,
                final int mciKMeansMaxIterations,
                final int conditionalModeMaxIterations,
                final double gamma,
                final double sensitivity,
                final double confidenceThreshold,
                final double activeLearningThreshold,
                final int filteredOutlierBufferMaxSize,
                final int confidenceWindowMaxSize,
                final int ensembleSize,
                final int randomGeneratorSeed,
                final int chunkSize,
                final boolean keepNoveltyDecisionModel,
                final boolean multiClassNoveltyDetection,
                final Interceptor interceptor) {

        this.q = q;
        this.k = k;
        this.mciKMeansMaxIterations = mciKMeansMaxIterations;
        this.conditionalModeMaxIterations = conditionalModeMaxIterations;
        this.centroidPercentage = centroidPercentage;
        this.gamma = gamma;
        this.sensitivity = sensitivity;
        this.confidenceThreshold = confidenceThreshold;
        this.activeLearningThreshold = activeLearningThreshold;
        this.filteredOutlierBufferMaxSize = filteredOutlierBufferMaxSize;
        this.confidenceWindowMaxSize = confidenceWindowMaxSize;
        this.ensembleSize = ensembleSize;
        this.keepNoveltyDecisionModel = keepNoveltyDecisionModel;
        this.chunkSize = chunkSize;
        this.random = new Random(randomGeneratorSeed);
        this.multiClassNoveltyDetection = multiClassNoveltyDetection;

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
        this.heater = new Heater(
                this.chunkSize,
                this.centroidPercentage,
                this.mciKMeansMaxIterations,
                this.conditionalModeMaxIterations,
                this.random);

        this.confusionMatrix = new DynamicConfusionMatrix();
        this.interceptor = interceptor;

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
                if (this.multiClassNoveltyDetection) {
                    this.novelClassDetectionMultiClass();
                } else {
                    this.novelClassDetectionSingleClass();
                }
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

        Integer label;
        final double ensembleConfidence;
        final boolean explained;
        final boolean novelty;

        if (votesByLabel.isEmpty()) {

            if (!this.noveltyMicroClusters.isEmpty()) {

                final MicroCluster closest = MicroCluster.calculateClosestMicroCluster(sample,
                        this.noveltyMicroClusters);

                final double distance = sample.distance(closest.calculateCentroid());

                if (distance <= 2 * closest.calculateStandardDeviation()) {
                    label = closest.getLabel();
                    ensembleConfidence = 1;
                    explained = true;
                    novelty = true;
                } else {
                    label = null;
                    ensembleConfidence = 0;
                    explained = false;
                    novelty = false;
                }

            } else {
                label = null;
                ensembleConfidence = 0;
                explained = false;
                novelty = false;
            }

        } else {
            label = Collections.max(votesByLabel.entrySet(), Map.Entry.comparingByValue()).getKey();
            ensembleConfidence = calculateConfidence(label, classifications);
            explained = true;
            novelty = false;
        }

        if (ensembleConfidence < this.activeLearningThreshold) {
            label = sample.getY();
            this.labeledSamplesCount++;
        }

        return new Classification(label, sample, ensembleConfidence, explained, novelty);


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

    private void novelClassDetectionSingleClass() {

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
            final List<Cluster> clusters = KMeansPlusPlus.execute(samples, this.k, this.random);

            for (Cluster cluster : clusters) {

                if (this.interceptor != null) {
                    final Context context = PCF.buildContext(cluster, Category.NOVELTY, ensemble);
                    if (context.getKnownLabels().size() > 1) {
                        this.interceptor.intercept(context);
                    }
                }

                this.addNovelty(cluster);
            }

            this.incrementNoveltyCount();

        }

    }

    private void novelClassDetectionMultiClass() {

        this.filteredOutlierBuffer.removeIf(p -> p.getT() < this.timestamp - this.chunkSize);

        final List<Cluster> clusters = KMeansPlusPlus
                .execute(this.filteredOutlierBuffer, this.k, this.random)
                .stream()
                .filter(cluster -> cluster.getSize() >= this.filteredOutlierBuffer.size() / this.k)
                .collect(Collectors.toList());


        final List<PseudoPoint> pseudoPoints = new ArrayList<>();
        ensemble.stream().map(Model::getPseudoPoints).forEach(pseudoPoints::addAll);

        for (Cluster cluster : clusters) {

            if (calculateSilhouette(cluster, pseudoPoints) <= 0) {
                continue;
            }

            this.filteredOutlierBuffer.removeAll(cluster.getSamples());
            if (this.interceptor != null) {
                final Context context = PCF.buildContext(cluster, Category.NOVELTY, ensemble);
                if (context.getKnownLabels().size() > 1) {
                    this.interceptor.intercept(context);
                }
            }

            this.addNovelty(cluster);
            this.incrementNoveltyCount();

        }


    }


    static double calculateSilhouette(final Cluster cluster, final List<PseudoPoint> pseudoPoints) {

        final Sample centroid = cluster.calculateCentroid();

        final List<Sample> decisionModelCentroids = pseudoPoints
                .stream()
                .map(PseudoPoint::getCentroid)
                .sorted(new SampleDistanceComparator(centroid))
                .collect(Collectors.toList());

        final double a = cluster.calculateStandardDeviation();

        final double b;
        if (decisionModelCentroids.size() > 0) {
            final Sample closestCentroid = decisionModelCentroids.get(0);
            b = centroid.distance(closestCentroid);
        } else {
            b = Double.MAX_VALUE;
        }

        return (b - a) / Math.max(b, a);

    }

    public void incrementNoveltyCount() {
        ++this.noveltyCount;
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

        final List<Sample> samples = new ArrayList<>();

        this.window.stream()
                .filter(classification -> classification.getConfidence() < this.activeLearningThreshold)
                .map(classification -> new Sample(classification.getSample().getX(), classification.getLabel()))
                .forEach(samples::add);

        final Set<Sample> labeledSamples = new HashSet<>(samples);

        this.window.stream()
                .filter(classification -> classification.getConfidence() >= this.activeLearningThreshold)
                .map(classification -> new Sample(classification.getSample().getX(), classification.getLabel()))
                .forEach(samples::add);

        final List<ImpurityBasedCluster> clusters = MCIKMeans
                .execute(
                        samples,
                        new ArrayList<>(),
                        this.mciKMeansMaxIterations,
                        this.conditionalModeMaxIterations,
                        this.centroidPercentage,
                        this.random)

                .stream()
                .filter(cluster -> cluster.size() > 1)
                .collect(Collectors.toList());

        if (this.interceptor != null) {
            for (ImpurityBasedCluster cluster : clusters) {
                final Context context = PCF.buildContext(cluster, labeledSamples, this.ensemble);
                if (context.getKnownLabels().size() > 1) {
                    this.interceptor.intercept(context);
                }
            }
        }

        final List<PseudoPoint> pseudoPoints = clusters
                .stream()
                .map(PseudoPoint::new)
                .collect(Collectors.toList());

        this.addModel(samples, pseudoPoints);

        this.window.removeAll(this.window.subList(0, changePoint));

    }

    public void addModel(final List<Sample> labeledSamples, List<PseudoPoint> pseudoPoints) {

        final Model model = Model.fit(labeledSamples, pseudoPoints);

        model.getKnownLabels().forEach((label) -> {
            if (!this.confusionMatrix.isLabelKnown(label)) {
                this.confusionMatrix.addKnownLabel(label);
            }
        });
        this.ensemble.remove(0);
        this.ensemble.add(model);
        this.stat.clear();

    }

    public void addNovelty(Cluster cluster) {

        if (this.keepNoveltyDecisionModel) {
            this.noveltyMicroClusters.add(new MicroCluster(cluster, this.noveltyCount));
        }

        cluster.getSamples().forEach(sample -> {
            this.confusionMatrix.updatedDelayed(sample.getY(), this.noveltyCount, true);
        });
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
        return this.confusionMatrix.measureCER();
    }

    public double calculateUnkR() {
        return this.confusionMatrix.measureUnkR();
    }

    public int getNoveltyCount() {
        return noveltyCount;
    }
}
