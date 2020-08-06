package br.com.douglas444.echo;

import org.apache.commons.math3.distribution.BetaDistribution;

public class StatElement {

    private int m;
    private final double preMean;
    private BetaDistribution preBeta;
    private double logLikelihoodRatioSum;

    public StatElement(double preMean) {
        this.preMean = preMean;
        this.m = -1;
    }

    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }

    public BetaDistribution getPreBeta() {
        return preBeta;
    }

    public void setPreBeta(BetaDistribution preBeta) {
        this.preBeta = preBeta;
    }

    public double getLogLikelihoodRatioSum() {
        return logLikelihoodRatioSum;
    }

    public void setLogLikelihoodRatioSum(double logLikelihoodRatioSum) {
        this.logLikelihoodRatioSum = logLikelihoodRatioSum;
    }

    public double getPreMean() {
        return preMean;
    }
}
