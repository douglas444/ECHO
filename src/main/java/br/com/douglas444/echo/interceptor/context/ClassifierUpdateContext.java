package br.com.douglas444.echo.interceptor.context;

import br.com.douglas444.dsframework.interceptor.Context;
import br.com.douglas444.echo.Model;
import br.com.douglas444.echo.PseudoPoint;
import br.com.douglas444.mltk.datastructure.Cluster;
import br.com.douglas444.mltk.datastructure.ImpurityBasedCluster;
import br.com.douglas444.mltk.datastructure.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClassifierUpdateContext implements Context {

    private List<ImpurityBasedCluster> impurityBasedClusters;
    private List<Model> ensemble;
    private BiConsumer<List<Sample>, List<PseudoPoint>> addModel;
    private Consumer<Cluster> addNovelty;
    private Runnable incrementNoveltyCount;

    public ClassifierUpdateContext ClassifierUpdateContext() {
        return this;
    }

    public ClassifierUpdateContext setImpurityBasedClusters(List<ImpurityBasedCluster> impurityBasedClusters) {
        this.impurityBasedClusters = impurityBasedClusters;
        return this;
    }

    public List<ImpurityBasedCluster> getImpurityBasedClusters() {
        return impurityBasedClusters;
    }

    public ClassifierUpdateContext setEnsemble(List<Model> ensemble) {
        this.ensemble = ensemble;
        return this;
    }

    public List<Model> getEnsemble() {
        return ensemble;
    }

    public ClassifierUpdateContext setAddModel(BiConsumer<List<Sample>, List<PseudoPoint>> addModel) {
        this.addModel = addModel;
        return this;
    }

    public BiConsumer<List<Sample>, List<PseudoPoint>> getAddModel() {
        return addModel;
    }

    public ClassifierUpdateContext setAddNovelty(Consumer<Cluster> addNovelty) {
        this.addNovelty = addNovelty;
        return this;
    }

    public Consumer<Cluster> getAddNovelty() {
        return addNovelty;
    }

    public ClassifierUpdateContext setIncrementNoveltyCount(Runnable incrementNoveltyCount) {
        this.incrementNoveltyCount = incrementNoveltyCount;
        return this;
    }

    public Runnable getIncrementNoveltyCount() {
        return incrementNoveltyCount;
    }
}
