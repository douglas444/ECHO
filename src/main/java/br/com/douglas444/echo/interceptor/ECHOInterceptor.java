package br.com.douglas444.echo.interceptor;

import br.com.douglas444.dsframework.interceptor.ConsumerOrRunnableInterceptor;
import br.com.douglas444.echo.interceptor.context.ClassifierUpdateContext;
import br.com.douglas444.echo.interceptor.context.NovelClassEmergenceContext;

public class ECHOInterceptor {

    public final ConsumerOrRunnableInterceptor<ClassifierUpdateContext> CLASSIFIER_UPDATE;
    public final ConsumerOrRunnableInterceptor<NovelClassEmergenceContext> NOVEL_CLASS_EMERGENCE;

    public ECHOInterceptor() {

        this.CLASSIFIER_UPDATE = new ConsumerOrRunnableInterceptor<>();
        this.NOVEL_CLASS_EMERGENCE = new ConsumerOrRunnableInterceptor<>();

    }

}
