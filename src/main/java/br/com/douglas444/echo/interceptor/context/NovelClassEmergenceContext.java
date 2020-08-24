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

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setEnsemble(List<Model> ensemble) {
        this.ensemble = ensemble;
    }

    public List<Model> getEnsemble() {
        return ensemble;
    }

    public void setAddModel(BiConsumer<List<Sample>, List<PseudoPoint>> addModel) {
        this.addModel = addModel;
    }

    public BiConsumer<List<Sample>, List<PseudoPoint>> getAddModel() {
        return addModel;
    }

    public void setAddNovelty(Consumer<Cluster> addNovelty) {
        this.addNovelty = addNovelty;
    }

    public Consumer<Cluster> getAddNovelty() {
        return addNovelty;
    }

    public void setIncrementNoveltyCount(Runnable incrementNoveltyCount) {
        this.incrementNoveltyCount = incrementNoveltyCount;
    }

    public Runnable getIncrementNoveltyCount() {
        return incrementNoveltyCount;
    }

    public DynamicConfusionMatrix getConfusionMatrix() {
        return confusionMatrix;
    }

    public void setConfusionMatrix(DynamicConfusionMatrix confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
    }
}
