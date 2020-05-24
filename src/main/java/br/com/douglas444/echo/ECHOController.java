package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierController;
import br.com.douglas444.echo.core.ECHO;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.Optional;

public class ECHOController implements DSClassifierController {

    private ECHO echo;

    public ECHOController(ECHO echo) {
        this.echo = echo;
    }

    public Optional<Integer> predictAndUpdate(Sample sample) {
        return this.echo.process(sample);
    }

    public String getLog() {
        return null;
    }
}
