package net.alagris;

public class GMeta<V, E, P, N, G extends IntermediateGraph<V, E, P, N>> {
    public final G graph;
    public final String name;
    public final Pos pos;

    public GMeta(G graph,  String name, Pos pos) {
        this.graph = graph;
        this.name = name;
        this.pos = pos;
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