package br.com.douglas444.echo.interceptor.context;

import br.com.douglas444.dsframework.interceptor.Context;
import br.com.douglas444.echo.Model;
import br.com.douglas444.echo.PseudoPoint;
import br.com.douglas444.mltk.datastructure.Cluster;
import br.com.douglas444.mltk.datastructure.DynamicConfusionMatrix;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NovelClassEmergenceContext implements Context {

    private List<Cluster> clusters;
    private List<Model> ensemble;
    private BiConsumer<List<Sample>, List<PseudoPoint>> addModel;
    private Consumer<Cluster> addNovelty;
    private Runnable incrementNoveltyCount;
    private DynamicConfusionMatrix confusionMatrix;

    public NovelClassEmergenceContext NovelClassEmergenceContext() {
        return this;
    }

    public NovelClassEmergenceContext setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
        return this;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public NovelClassEmergenceContext setEnsemble(List<Model> ensemble) {
        this.ensemble = ensemble;
        return this;
    }

    public List<Model> getEnsemble() {
        return ensemble;
    }

    public NovelClassEmergenceContext setAddModel(BiConsumer<List<Sample>, List<PseudoPoint>> addModel) {
        this.addModel = addModel;
        return this;
    }

    public BiConsumer<List<Sample>, List<PseudoPoint>> getAddModel() {
        return addModel;
    }

    public NovelClassEmergenceContext setAddNovelty(Consumer<Cluster> addNovelty) {
        this.addNovelty = addNovelty;
        return this;
    }

    public Consumer<Cluster> getAddNovelty() {
        return addNovelty;
    }

    public NovelClassEmergenceContext setIncrementNoveltyCount(Runnable incrementNoveltyCount) {
        this.incrementNoveltyCount = incrementNoveltyCount;
        return this;
    }

    public Runnable getIncrementNoveltyCount() {
        return incrementNoveltyCount;
    }

    public DynamicConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }

    public NovelClassEmergenceContext setConfusionMatrix(DynamicConfusionMatrix confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
        return this;
    }
}
