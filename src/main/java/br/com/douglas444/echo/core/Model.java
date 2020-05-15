package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;

public class Model {

    public ClassificationResult classify(final Sample sample) {
        return new ClassificationResult(null, false);
    }
}
