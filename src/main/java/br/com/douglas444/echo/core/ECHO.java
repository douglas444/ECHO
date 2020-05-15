package br.com.douglas444.echo.core;

import br.com.douglas444.echo.ClassificationResult;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.List;

public class ECHO {

    private List<Model> ensemble;

    public ClassificationResult process(final Sample sample) {
        return new ClassificationResult(null, false);
    }

}
