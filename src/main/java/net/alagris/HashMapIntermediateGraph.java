package net.alagris;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Perfect data structure for representing sparse graphs with only a few outgoing edges per state. Very fast inseritons
 * and deletions, at the cost larger memory usage per vertex (empty hash map buckets)
 */
public class HashMapIntermediateGraph<V, E, P> implements IntermediateGraph<V, E, P, HashMapIntermediateGraph.N<V, E>> {

    private P eps;
    private HashMap<N<V, E>, E> initialEdges = new HashMap<>();
    private HashMap<N<V, E>, P> finalEdges = new HashMap<>();

    @Override
    public int size(N<V, E> from) {
        return from.outgoing.size();
    }

    @Override
    public V getState(N<V, E> vertex) {
        return vertex.state;
    }

    @Override
    public void setState(N<V, E> vertex, V v) {
        vertex.state = v;
    }


    public static <X,Y> Iterator<Y> map(Iterator<X> i,Function<X,Y> map){
        return new Iterator<Y>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public Y next() {
                return map.apply(i.next());
            }
        };
    }

    @Override
    public Iterator<EN<N<V, E>, E>> iterator(N<V, E> from) {
        return from.outgoing.iterator();
    }

    @Override
    public void add(N<V, E> from, E edge, N<V, E> to) {
        from.outgoing.add(EN.of(to, edge));
    }

    @Override
    public boolean remove(N<V, E> from, E edge, N<V, E> to) {
        return from.outgoing.remove(EN.of(to, edge));
    }

    @Override
    public boolean contains(N<V, E> from, E edge, N<V, E> to) {
        return from.outgoing.contains(EN.of(to, edge));
    }

    @Override
    public N<V, E> create(V state) {
        return new N<>(state);
    }

    @Override
    public N<V, E> shallowCopy(N<V, E> other) {
        return new N<>(other.state);
    }

    public static class N<V, E> {
        final HashSet<EN<N<V, E>, E>> outgoing = new HashSet<>();
        V state;

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
        return Objects.equals(initialEdges.get(initialState), edge);
    }

    @Override
    public void setInitialEdge(N<V, E> initialState, E edge) {
        initialEdges.put(initialState, edge);
    }

    @Override
    public E removeInitialEdge(N<V, E> initialState) {
        return initialEdges.remove(initialState);
    }

    @Override
    public E getInitialEdge(N<V, E> initialState) {
        return initialEdges.get(initialState);
    }

    @Override
    public Iterator<EN<N<V, E>, E>> iterateInitialEdges() {
        return map(initialEdges.entrySet().iterator(), EN::ofMap);
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
    public Iterator<EN<N<V, E>, P>> iterateFinalEdges() {
        return map(finalEdges.entrySet().iterator(), EN::ofMap);
    }

    @Override
    public boolean containsFinalEdge(N<V, E> finalState, P edge) {
        return Objects.equals(finalEdges.get(finalState), edge);
    }

    @Override
    public boolean removeInitialEdge(N<V, E> initialState, E edge) {
        return initialEdges.remove(initialState, edge);
    }

    @Override
    public boolean removeFinalEdge(N<V, E> finalState, P edge) {
        return finalEdges.remove(finalState, edge);
    }

    @Override
    public List<EN<N<V, E>, P>> allFinalEdges() {
        ArrayList<EN<N<V, E>, P>> arr = new ArrayList<>(finalEdges.size());
        finalEdges.entrySet().forEach(e -> arr.add(EN.of(e.getKey(), e.getValue())));
        return arr;
    }

    @Override
    public List<EN<N<V, E>, E>> allInitialEdges() {
        ArrayList<EN<N<V, E>, E>> arr = new ArrayList<>(initialEdges.size());
        initialEdges.entrySet().forEach(e -> arr.add(EN.of(e.getKey(), e.getValue())));
        return arr;
    }

    @Override
    public E mapInitialEdge(N<V, E> vertex, Function<E, E> map) {
        return initialEdges.compute(vertex, (k, v) -> map.apply(v));
    }

    @Override
    public P mapFinalEdge(N<V, E> vertex, Function<P, P> map) {
        return finalEdges.compute(vertex, (k, v) -> map.apply(v));
    }

    @Override
    public void mapAllFinalEdges(BiFunction<P, N<V, E>, P> map) {
        finalEdges.entrySet().forEach(e -> e.setValue(map.apply(e.getValue(), e.getKey())));
    }

    @Override
    public void mapAllInitialEdges(BiFunction<E, N<V, E>, E> map) {
        initialEdges.entrySet().forEach(e -> e.setValue(map.apply(e.getValue(), e.getKey())));
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

    public Iterator<EN<N<V, E>, E>> lexSortedIterator(N<V, E> from) {
        return iterator(from);
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
        N<V, E> init = makeUniqueInitialState(null);
        HashSet<N<V, E>> set = SinglyLinkedGraph.collect(this, init);
        set.remove(init);
        return serializeHumanReadable(set, E::toString, P::toString, V::toString);
    }
}
