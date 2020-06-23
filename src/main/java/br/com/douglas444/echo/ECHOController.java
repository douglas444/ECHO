package br.com.douglas444.echo;

import br.com.douglas444.dsframework.DSClassifierController;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.Optional;

public class ECHOController implements DSClassifierController {

    private final ECHO echo;

    public ECHOController(ECHO echo) {
        this.echo = echo;
    }

    @Override
    public Optional<Integer> process(final Sample sample) {
        return this.echo.process(sample);
    }

    @Override
    public String getLog() {
        return null;
    }
}
