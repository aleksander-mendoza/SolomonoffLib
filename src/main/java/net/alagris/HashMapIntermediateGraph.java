package net.alagris;

import java.util.*;

/**
 * Perfect data structure for representing sparse graphs with only a few outgoing edges per state. Very fast inseritons
 * and deletions, at the cost larger memory usage per vertex (empty hash map buckets)
 */
public class HashMapIntermediateGraph<V, E, P> implements IntermediateGraph<V, E, P, HashMapIntermediateGraph.N<V, E>> {

    private P eps;
    private HashMap<E, N<V, E>> initialEdges = new HashMap<>();
    private HashMap<N<V, E>, P> finalEdges = new HashMap<>();

    @Override
    public int size(N<V, E> from) {
        return from.outgoing.size();
    }

    @Override
    public Object getColor(N<V, E> vertex) {
        return vertex.color;
    }

    @Override
    public void setColor(N<V, E> vertex, Object color) {
        vertex.color = color;
    }

    @Override
    public V getState(N<V, E> vertex) {
        return vertex.state;
    }

    @Override
    public void setState(N<V, E> vertex, V v) {
        vertex.state = v;
    }

    @Override
    public Iterator<Map.Entry<E,N<V, E>>> iterator(N<V, E> from) {
        return from.outgoing.entrySet().iterator();
    }

    @Override
    public Map<E, N<V, E>> outgoing(N<V, E> from) {
        return from.outgoing;
    }

    @Override
    public void add(N<V, E> from, E edge, N<V, E> to) {
        from.outgoing.put(edge,to);
    }

    @Override
    public boolean remove(N<V, E> from, E edge, N<V, E> to) {
        return from.outgoing.remove(edge, to);
    }

    @Override
    public boolean contains(N<V, E> from, E edge, N<V, E> to) {
        return Objects.equals(from.outgoing.get(edge),to);
    }

    @Override
    public N<V, E> create(V state) {
        return new N<>(state);
    }

    @Override
    public N<V, E> shallowCopy(N<V, E> other) {
        return new N<>(other);
    }

    public static class N<V, E> {
        final HashMap<E,N<V, E>> outgoing = new HashMap<>();
        V state;
        Object color;
        private N(N<V,E> other) {
            state = other.state;
            color = other.color;
        }
        private N(V state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return "N{" +
                    "outgoing=" + outgoing +
                    ", state=" + state +
                    '}';
        }
    }


    @Override
    public P getEpsilon() {
        return eps;
    }

    @Override
    public void setEpsilon(P epsilon) {
        eps = epsilon;
    }

    @Override
    public boolean containsInitialEdge(N<V, E> initialState, E edge) {
        return Objects.equals(initialEdges.get(edge), initialState);
    }

    @Override
    public void addInitialEdge(N<V, E> initialState, E edge) {
        initialEdges.put(edge, initialState);
    }

    @Override
    public Iterator<Map.Entry<E, N<V, E>>> iterateInitialEdges() {
        return initialEdges.entrySet().iterator();
    }

    @Override
    public void setFinalEdge(N<V, E> finalState, P edge) {
        finalEdges.put(finalState, edge);
    }

    @Override
    public P removeFinalEdge(N<V, E> finalState) {
        return finalEdges.remove(finalState);
    }

    @Override
    public P getFinalEdge(N<V, E> finalState) {
        return finalEdges.get(finalState);
    }

    @Override
    public Iterator<Map.Entry<N<V, E>, P>> iterateFinalEdges() {
        return finalEdges.entrySet().iterator();
    }

    @Override
    public boolean containsFinalEdge(N<V, E> finalState, P edge) {
        return Objects.equals(finalEdges.get(finalState), edge);
    }

    @Override
    public boolean removeInitialEdge(N<V, E> initialState, E edge) {
        return initialEdges.remove(edge, initialState);
    }

    @Override
    public boolean removeFinalEdge(N<V, E> finalState, P edge) {
        return finalEdges.remove(finalState, edge);
    }

    @Override
    public Map<N<V, E>, P> allFinalEdges() {
        return finalEdges;
    }

    @Override
    public Map<E, N<V, E>> allInitialEdges() {
        return initialEdges;
    }

    @Override
    public void clearInitial() {
        initialEdges.clear();
    }

    @Override
    public void clearFinal() {
        finalEdges.clear();
    }

    @Override
    public void replaceInitial(IntermediateGraph<V, E, P, N<V, E>> other) {
        initialEdges = ((HashMapIntermediateGraph<V, E, P>) other).initialEdges;
    }

    @Override
    public void replaceFinal(IntermediateGraph<V, E, P, N<V, E>> other) {
        finalEdges = ((HashMapIntermediateGraph<V, E, P>) other).finalEdges;
    }

    public static class LexUnicodeSpecification extends net.alagris.LexUnicodeSpecification<
            N<Pos, net.alagris.LexUnicodeSpecification.E>,
            HashMapIntermediateGraph<Pos, net.alagris.LexUnicodeSpecification.E, net.alagris.LexUnicodeSpecification.P>> {

        @Override
        public HashMapIntermediateGraph<Pos, E, P> createEmptyGraph() {
            return new HashMapIntermediateGraph<>();
        }


    }

    @Override
    public String toString() {
        return serializeHumanReadable(collectVertices(n->true), E::toString, P::toString, V::toString);
    }
}
