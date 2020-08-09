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

    public void setImpurityBasedClusters(List<ImpurityBasedCluster> impurityBasedClusters) {
        this.impurityBasedClusters = impurityBasedClusters;
    }

    public List<ImpurityBasedCluster> getImpurityBasedClusters() {
        return impurityBasedClusters;
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
}
