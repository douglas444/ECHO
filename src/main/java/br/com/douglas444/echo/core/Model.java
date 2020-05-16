package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.List;

public class Model {

    private List<PseudoPoint> pseudoPoints;

    public Model(List<PseudoPoint> pseudoPoints) {
        this.pseudoPoints = pseudoPoints;
    }

    public ClassificationResult classify(final Sample sample) {

        final PseudoPoint closestPseudoPoint = PseudoPoint.getClosestPseudoPoint(sample, this.pseudoPoints);
        final double distance = closestPseudoPoint.getCentroid().distance(sample);

        return new ClassificationResult(closestPseudoPoint.getLabel(),
                distance <= closestPseudoPoint.getRadius());

    }
}
