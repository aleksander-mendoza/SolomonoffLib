package net.alagris;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * This is an intermediate automaton representation that can be used n Glushkov's algorithm.
 *
 * @param <V> the set of vertices,
 * @param <E> the set of "full" edges
 * @param <P> set of "partial" edges.
 * @param <N> the set of vertices
 *
 *            <tt>Eps</tt> are edges whose neither source nor target is know.
 *            <tt> Incoming</tt> are edges whose source is not known. <tt> Outgoing</tt>
 *            are edges whose target is not known. may not be known yet. Note that value of
 *            <tt>V</tt> does not uniquely determine the state, but rather it serves as a way of
 *            keeping some data attached to a state. It could be things like "colour",
 *            "output" or any meta-information of state. Similarly the same goes for
 *            <tt>E</tt>
 */
public interface IntermediateGraph<V, E, P, N> extends SinglyLinkedGraph<V, E, N> {

    /**
     * This is a special "free-floating" edge that is not adjoint to any vertex.
     * That is, this edge can be seen as one whose neither target nor source is not yet
     * known. It is used to represent epsilon transition of automata. Usually
     * this edge would have no input label, but it may have output label and weight.
     *
     * @return null if there is no such edge
     */
    public P getEpsilon();

    /**
     * Setting it to null will erase the epsilon edge
     */
    public void setEpsilon(P epsilon);

    /**
     * Initial vertices have "edges incoming from nowhere".
     * That is, each initial vertex is assigned some initial edge that can be seen as
     * incoming transition whose source vertex is not (yet) known and comes from "outside" of graph.
     * There can be at most one initial edge per vertex.
     */
    public void setInitialEdge(N initialState, E edge);

    /**
     * returns previously associated initial edge or null if the state wasn't initial at all
     */
    public E removeInitialEdge(N initialState);

    /**
     * @return null if the state is not initial
     */
    public E getInitialEdge(N initialState);

    public Iterator<EN<N, E>> iterateInitialEdges();

    /**
     * Final vertices have "edges outgoing to nowhere". That
     * is, each final vertex is assigned some final edge and that can be seen as outgoing
     * transition whose target vertex is not (yet) known and goes to "outside" of graph.
     * There can be at most one final edge per vertex.
     */
    public void setFinalEdge(N finalState, P edge);

    /**
     * returns previously associated final edge or null if the state wasn't final at all
     */
    public P removeFinalEdge(N finalState);

    /**
     * @return null if the state is not final
     */
    public P getFinalEdge(N finalState);

    public Iterator<EN<N, P>> iterateFinalEdges();


    public default boolean containsInitialEdge(N initialState, E edge) {
        return Objects.equals(edge, getInitialEdge(initialState));
    }

    public default boolean containsFinalEdge(N finalState, P edge) {
        return Objects.equals(edge, getFinalEdge(finalState));
    }

    public default boolean removeInitialEdge(N initialState, E edge) {
        if (containsInitialEdge(initialState, edge)) {
            removeFinalEdge(initialState);
            return true;
        }
        return false;
    }

    public default boolean removeFinalEdge(N finalState, P edge) {
        if (containsFinalEdge(finalState, edge)) {
            removeFinalEdge(finalState);
            return true;
        }
        return false;
    }

    public default List<EN<N, P>> allFinalEdges() {
        List<EN<N, P>> list = new ArrayList<>();
        iterateFinalEdges().forEachRemaining(list::add);
        return list;
    }

    public default List<EN<N, E>> allInitialEdges() {
        List<EN<N, E>> list = new ArrayList<>();
        iterateInitialEdges().forEachRemaining(list::add);
        return list;
    }

    public default E mapInitialEdge(N vertex, Function<E, E> map) {
        E e = map.apply(removeInitialEdge(vertex));
        setInitialEdge(vertex, e);
        return e;
    }

    public default P mapFinalEdge(N vertex, Function<P, P> map) {
        P e = map.apply(removeFinalEdge(vertex));
        setFinalEdge(vertex, e);
        return e;
    }

    public default void mapAllFinalEdges(BiFunction<P, N, P> map) {
        for (EN<N, P> fin : allFinalEdges()) {
            mapFinalEdge(fin.getVertex(), e -> map.apply(e, fin.getVertex()));
        }
    }

    public default void mapAllInitialEdges(BiFunction<E, N, E> map) {
        for (EN<N, E> init : allInitialEdges()) {
            mapInitialEdge(init.getVertex(), e -> map.apply(e, init.getVertex()));
        }
    }

    public default void clearInitial() {
        for (EN<N, E> init : allInitialEdges()) removeInitialEdge(init.getVertex(), init.getEdge());
    }

    public default void clearFinal() {
        for (EN<N, P> fin : allFinalEdges()) removeFinalEdge(fin.getVertex(), fin.getEdge());
    }

    public default void addAllInitial(Iterable<EN<N, E>> initialEdges) {
        for (EN<N, E> init : initialEdges) setInitialEdge(init.getVertex(), init.getEdge());
    }

    public default void addAllFinal(Iterable<EN<N, P>> finalEdges) {
        for (EN<N, P> fin : finalEdges) setFinalEdge(fin.getVertex(), fin.getEdge());
    }

    /**
     * Removes all initial vertices and adds of initial vertices of some other graph
     */
    public default void replaceInitial(IntermediateGraph<V, E, P, N> other) {
        Collection<EN<N, E>> otherInit = other.allInitialEdges();
        clearInitial();
        addAllInitial(otherInit);
    }

    /**
     * Removes all final vertices and adds of final vertices of some other graph
     */
    public default void replaceFinal(IntermediateGraph<V, E, P, N> other) {
        Collection<EN<N, P>> otherFinal = other.allFinalEdges();
        clearFinal();
        addAllFinal(otherFinal);
    }


    /**
     * This method creates a new dummy initial state that emulates all incoming partial edges. The
     * graph is singly-linked therefore addition of such vertex does not have any impact on overall structure of graph.
     * This initial state will be unreachable from any other vertex.
     */
    public default N makeUniqueInitialState(V state) {
        N init = create(state);
        for (EN<N, E> i : allInitialEdges()) {
            add(init, i.getEdge(), i.getVertex());
        }
        return init;
    }


    /**
     * Prints graph in a human-readable format
     */
    public default String serialize(Set<N> vertices,
                                    Function<E, String> edgeStringifier,
                                    Function<P, String> partialEdgeStringifier,
                                    Function<V, String> stateStringifier) {

        final StringBuilder sb = new StringBuilder();

        final HashMap<N, Integer> vertexToIndex = new HashMap<>();
        for (final N vertex : vertices) {
            vertexToIndex.put(vertex, vertexToIndex.size());
        }
        if(getEpsilon()!=null)sb.append("eps ").append(partialEdgeStringifier.apply(getEpsilon())).append('\n');
        for (EN<N, E> init : (Iterable<EN<N, E>>) () -> iterateInitialEdges()) {
            final Integer target = vertexToIndex.get(init.getVertex());
            sb.append("init ")
                    .append(edgeStringifier.apply(init.getEdge()))
                    .append(" ")
                    .append(target)
                    .append(" ")
                    .append(stateStringifier.apply(getState(init.getVertex())))
                    .append("\n");
        }
        for (Map.Entry<N, Integer> entry : vertexToIndex.entrySet()) {
            final N vertex = entry.getKey();
            final int idx = entry.getValue();

            for (EN<N, E> outgoing : (Iterable<EN<N, E>>) () -> iterator(vertex)) {
                final Integer target = vertexToIndex.get(outgoing.getVertex());
                sb.append(idx)
                        .append(" ")
                        .append(edgeStringifier.apply(outgoing.getEdge()))
                        .append(" ")
                        .append(target)
                        .append(" ")
                        .append(stateStringifier.apply(getState(outgoing.getVertex())))
                        .append("\n");
            }
        }
        for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> iterateFinalEdges()) {
            final Integer target = vertexToIndex.get(fin.getVertex());
            sb.append("fin ")
                    .append(target)
                    .append(" ")
                    .append(partialEdgeStringifier.apply(fin.getEdge()))
                    .append(" ")
                    .append(stateStringifier.apply(getState(fin.getVertex())))
                    .append("\n");
        }
        return sb.toString();
    }

}
