package net.alagris;

import java.util.*;
import java.util.function.Predicate;

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
    public Collection<Map.Entry<E, N<V, E>>> outgoing(N<V, E> from) {
        return from.outgoing.entrySet();
    }

    @Override
    public void add(N<V, E> from, E edge, N<V, E> to) {
        from.outgoing.put(edge, to);
    }

    @Override
    public boolean remove(N<V, E> from, E edge, N<V, E> to) {
        return from.outgoing.remove(edge, to);
    }

    @Override
    public void removeEdgeIf(N<V, E> from, Predicate<Map.Entry<E, N<V, E>>> filter) {
        from.outgoing.entrySet().removeIf(filter);
    }

    @Override
    public boolean contains(N<V, E> from, E edge, N<V, E> to) {
        return Objects.equals(from.outgoing.get(edge), to);
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
        private HashMap<E, N<V, E>> outgoing = new HashMap<>();
        V state;
        Object color;

        private N(N<V, E> other) {
            state = other.state;
            color = other.color;
        }

        private N(V state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return Objects.toString(state);
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
    public void removeFinalEdgeIf(Predicate<Map.Entry<N<V, E>, P>> filter) {
        finalEdges.entrySet().removeIf(filter);
    }

    @Override
    public void removeInitialEdgeIf(Predicate<Map.Entry<E, N<V, E>>> filter) {
        initialEdges.entrySet().removeIf(filter);
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

    @Override
    public void setFinalEdges(HashMap<N<V, E>, P> finalEdges) {
        this.finalEdges = finalEdges;
    }

    @Override
    public void useStateOutgoingEdgesAsInitial(N<V, E> initialState) {
        initialEdges = initialState.outgoing;
    }

    public static class LexUnicodeSpecification extends net.alagris.LexUnicodeSpecification<
            N<Pos, net.alagris.LexUnicodeSpecification.E>,
            HashMapIntermediateGraph<Pos, net.alagris.LexUnicodeSpecification.E, net.alagris.LexUnicodeSpecification.P>> {

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link LexUnicodeSpecification#introduceVariable})
         */
        public LexUnicodeSpecification(boolean eagerMinimisation,boolean eagerCopy,boolean eagerFunctionalityChecks,int minimalSymbol,int maximalSymbol, ExternalPipelineFunction externalPipelineFunction) {
            super(eagerMinimisation, minimalSymbol,maximalSymbol, eagerCopy,eagerFunctionalityChecks, externalPipelineFunction);
        }
        public LexUnicodeSpecification(boolean eagerCopy,boolean eagerFunctionalityChecks) {
            this(true,eagerCopy,eagerFunctionalityChecks,0,Integer.MAX_VALUE,null);
        }

        public LexUnicodeSpecification() {
            this(false,true);
        }

        @Override
        public HashMapIntermediateGraph<Pos, E, P> createEmptyGraph() {
            return new HashMapIntermediateGraph<>();
        }


    }

    @Override
    public String toString() {
        return serializeHumanReadable(collectVertexSet(new HashSet<>(),n -> null,(n,e)->null), E::toString, P::toString, V::toString);
    }
}
