package net.alagris;

public class GMeta<V, E, P, N, G extends IntermediateGraph<V, E, P, N>> {
    public final G graph;
    public final String name;
    public final Pos pos;
    /**If true, then exponential operation !! will always be implicitly assumed for this variable.*/
    public final boolean alwaysCopy;

    public GMeta(G graph, String name, Pos pos, boolean alwaysCopy) {
        this.graph = graph;
        this.name = name;
        this.pos = pos;
        this.alwaysCopy = alwaysCopy;
    }

    @Override
    public String toString() {
        return "GMeta{" +
                "graph=" + graph +
                ", name='" + name + '\'' +
                ", pos=" + pos +
                '}';
    }
}