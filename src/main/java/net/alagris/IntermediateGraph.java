package net.alagris;

import net.automatalib.commons.util.Pair;
import net.automatalib.graphs.concepts.GraphViewable;

import java.util.*;
import java.util.function.*;


/**
 * This is an intermediate automaton representation that can be used n Glushkov's algorithm.
 * Vertices <tt>N</tt> should not implement custom {@link Object#equals} nor {@link Object#hashCode()} methods!
 * Each object is treated as separate state. Similarly the same holds for vertices. Each object <tt>E</tt> determines
 * an edge and equality should be on per-object basis.
 *
 * @param <V> the set of states (meta-information attached to vertices),
 * @param <E> the set of "full" edges
 * @param <P> set of "partial" edges.
 * @param <N> the set of vertices
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
    P getEpsilon();

    /**
     * Setting it to null will erase the epsilon edge
     */
    void setEpsilon(P epsilon);

    /**
     * Initial vertices have "edges incoming from nowhere".
     * That is, each initial vertex is assigned some initial edge that can be seen as
     * incoming transition whose source vertex is not (yet) known and comes from "outside" of graph.
     */
    void addInitialEdge(N initialState, E edge);

    Iterator<Map.Entry<E, N>> iterateInitialEdges();

    /**
     * Final vertices have "edges outgoing to nowhere". That
     * is, each final vertex is assigned some final edge and that can be seen as outgoing
     * transition whose target vertex is not (yet) known and goes to "outside" of graph.
     * There can be at most one final edge per vertex.
     */
    void setFinalEdge(N finalState, P edge);

    /**
     * returns previously associated final edge or null if the state wasn't final at all
     */
    P removeFinalEdge(N finalState);

    /**
     * @return null if the state is not final
     */
    P getFinalEdge(N finalState);

    Iterator<Map.Entry<N, P>> iterateFinalEdges();


    boolean containsInitialEdge(N initialState, E edge);

    default boolean containsFinalEdge(N finalState, P edge) {
        return Objects.equals(edge, getFinalEdge(finalState));
    }

    boolean removeInitialEdge(N initialState, E edge);

    boolean removeFinalEdge(N finalState, P edge);

    Map<N, P> allFinalEdges();

    Map<E, N> allInitialEdges();


    default void replaceAllFinalEdges(BiFunction<P, N, P> f) {
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) this::iterateFinalEdges) {
            fin.setValue(f.apply(fin.getValue(), fin.getKey()));
        }
    }


    default void mutateAllFinalEdges(BiConsumer<P, N> consumer) {
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) this::iterateFinalEdges) {
            consumer.accept(fin.getValue(), fin.getKey());
        }
    }

    default void mutateAllInitialEdges(BiConsumer<E, N> consumer) {
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) this::iterateInitialEdges) {
            consumer.accept(init.getKey(), init.getValue());
        }
    }

    void clearInitial();

    void clearFinal();

    /**
     * Removes all initial vertices and adds of initial vertices of some other graph
     */
    void replaceInitial(IntermediateGraph<V, E, P, N> other);

    /**
     * Removes all final vertices and adds of final vertices of some other graph
     */
    void replaceFinal(IntermediateGraph<V, E, P, N> other);


    /**
     * This method creates a new dummy initial state that emulates all incoming partial edges. The
     * graph is singly-linked therefore addition of such vertex does not have any impact on overall structure of graph.
     * This initial state will be unreachable from any other vertex.
     */
    default N makeUniqueInitialState(V state) {
        N init = create(state);
        for (Map.Entry<E, N> i : (Iterable<Map.Entry<E, N>>) this::iterateInitialEdges) {
            add(init, i.getKey(), i.getValue());
        }
        return init;
    }


    /**
     * Prints graph in a human-readable format
     */
    default String serializeHumanReadable(Set<N> vertices,
                                          Function<E, String> edgeStringifier,
                                          Function<P, String> partialEdgeStringifier,
                                          Function<V, String> stateStringifier) {

        final StringBuilder sb = new StringBuilder();

        final HashMap<N, Integer> vertexToIndex = new HashMap<>();
        for (final N vertex : vertices) {
            vertexToIndex.put(vertex, vertexToIndex.size());
        }
        if (getEpsilon() != null) sb.append("eps ").append(partialEdgeStringifier.apply(getEpsilon())).append('\n');
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) this::iterateInitialEdges) {
            final Integer target = vertexToIndex.get(init.getValue());
            sb.append("init ")
                    .append(init.getKey() == null ? null : edgeStringifier.apply(init.getKey()))
                    .append(" ")
                    .append(target)
                    .append(" ")
                    .append(init.getValue() == null ? null : stateStringifier.apply(getState(init.getValue())))
                    .append("\n");
        }
        for (Map.Entry<N, Integer> entry : vertexToIndex.entrySet()) {
            final N vertex = entry.getKey();
            final int idx = entry.getValue();

            for (Map.Entry<E, N> outgoing : (Iterable<Map.Entry<E, N>>) () -> iterator(vertex)) {
                final Integer target = vertexToIndex.get(outgoing.getValue());
                sb.append(idx)
                        .append(" ")
                        .append(outgoing.getKey() == null ? null : edgeStringifier.apply(outgoing.getKey()))
                        .append(" ")
                        .append(target)
                        .append(" ")
                        .append(outgoing.getValue() == null ? null : stateStringifier.apply(getState(outgoing.getValue())))
                        .append("\n");
            }
        }
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) this::iterateFinalEdges) {
            final Integer target = vertexToIndex.get(fin.getKey());
            sb.append("fin ")
                    .append(target)
                    .append(" ")
                    .append(fin.getValue() == null ? null : partialEdgeStringifier.apply(fin.getValue()))
                    .append(" ")
                    .append(fin.getKey() == null ? null : stateStringifier.apply(getState(fin.getKey())))
                    .append("\n");
        }
        return sb.toString();
    }

    default <S extends Set<N>>  S collectVertices(S visited,Predicate<N> shouldContinue) {
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) this::iterateInitialEdges) {
            if (null == SinglyLinkedGraph.collect(this, init.getValue(), visited, shouldContinue)) {
                return null;
            }
        }
        return visited;
    }


    interface MergeFinalOutputs<P, N, Ex extends Throwable> {
        P merge(N finState1, P finEdge1, N finState2, P finEdge2) throws Ex;
    }

    /**
     * This is a pseudo minimization algorithm for nondeterministic machines that uses heuristics to reduce
     * the number of states but does not attempt find the smallest nondeterministic automaton possible
     * (as the problem is hard and would require too much resources). The heuristic approach is good enough in practice
     * and can be carried out efficiently. All the heuristics are inspired by (but are not direct implementations of)
     * Brzozowski's algorithm and Kameda & Weiner's nondeterministic minimization.
     * <p>
     * <br/><br/>
     * This implementation has one weak point. It's possible that two states have equivalent outgoing transitions
     * but are not merged despite it. For insatnce two edges like<br/>
     * <p>
     * q<sub>1</sub> [a-e]:"abc" &rarr; q<sub>3</sub> <br/>
     * q<sub>1</sub> [f-j]:"abc" &rarr; q<sub>3</sub> <br/>
     * are equivalent to a single edge <br/>
     * q<sub>2</sub> [a-j]:"abc" &rarr; q<sub>3</sub> <br/>
     * but states q<sub>1</sub> and q<sub>2</sub> won't be merged, because the number of transitions doesn't match.
     * However, such situations will rarely occur thanks to one special property of Glushkov's contruction. If
     * two transitions go to the same state q<sub>3</sub>, then those transitions will be identical. For instance in
     * regex ("a"|"b")"c" both transitions from "a" and "b" to "c" will have the same label [c-c]. A more pessimistic
     * example would be "b"([a-e]|[f-j]) | "a"[a-j]. Here the states corresponding to [a-e], [f-j] and [a-j] will
     * all be merged into one, however states for "b" and "a" won't be merged, even though in theory they could be.
     * However, in real life scenarios, such cases will be few and far between. It's a certain trade-off between
     * efficiency of algorithm and compactness of produced automaton (in order to discover equivalence between
     * "a" and "b" you would actually need to run this pseudo minimization algorithm several times and
     * merge transitions like [a-e] and [f-j] into one larger transition [a-j] bewteen each call).
     * It's worth noting that if you use regular expressions without ranges (that is, only singleton ranges [x-x]
     * are allowed), then this problem will never occur.
     *
     * @param mergeFinalOutputs if the two final edges are equivalent, then merge them into one. If
     *                          thet cannot be merged (are not equivalent) then return null. For instance, two final edges that have the same output
     *                          string but only differ in weight, might be merged by summing the weights or choosing the larger one.
     */
    default <Ex extends Throwable> void pseudoMinimize(BiFunction<N,Map<E, N>, Integer> hashOutgoing,
                                                       BiFunction<N,Map<E, N>, Integer> hashIncoming,
                                                       BiPredicate<Map<E, N>, Map<E, N>> areEquivalent,
                                                       BiPredicate<P, P> areFinalOutputsEquivalent,
                                                       MergeFinalOutputs<P, N, Ex> mergeFinalOutputs) throws Ex {
        final BiPredicate<P, P> areFinalOutputsEquivalentOrNull = (a, b) -> {
            if (a == null) return b == null;
            if (b == null) return false;
            return areFinalOutputsEquivalent.test(a, b);
        };
        final MergeFinalOutputs<P, N, Ex> mergeFinalOutputsOrNull = (aN, aP, bN, bP) -> {
            if (aP == null) return bP;
            if (bP == null) return aP;
            return mergeFinalOutputs.merge(aN, aP, bN, bP);
        };
        class HashVertex {
            int h;
            N vertex;

            public HashVertex(N vertex) {
                this.vertex = vertex;
            }
            void computeHash(){
                if(vertex==null)return;
                h = hashOutgoing.apply(vertex,outgoing(vertex));
            }
            void computeHashIncoming(){
                if(vertex==null)return;
                HashMap<E, N> incoming = (HashMap<E, N>) getColor(vertex);
                h = hashIncoming.apply(vertex,incoming);
            }
            void erase(){
                setColor(vertex, null);
                vertex = null;
                h = 0;
            }

        }
        final ArrayList<HashVertex> hashesAndVertices;
        {
            final HashSet<N> vertices = collectVertices(new HashSet<>(),n -> {
                setColor(n, new HashMap<E, N>());
                return true;
            });
            for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) this::iterateInitialEdges) {
                HashMap<E, N> incoming = (HashMap<E, N>) getColor(init.getValue());
                incoming.put(init.getKey(), null);//null vertex indicates initial edge
            }
            hashesAndVertices = new ArrayList<>(vertices.size());
            for (N vertex : vertices) {
                for (Map.Entry<E, N> outgoing : (Iterable<Map.Entry<E, N>>) () -> iterator(vertex)) {
                    HashMap<E, N> incoming = (HashMap<E, N>) getColor(outgoing.getValue());
                    incoming.put(outgoing.getKey(), vertex);
                }
                hashesAndVertices.add(new HashVertex(vertex));
            }
            //vertices can now be garbage collected
        }

        while (true) {
            hashesAndVertices.forEach(HashVertex::computeHash);
            hashesAndVertices.sort(Comparator.comparingInt(e->e.h));
            boolean anyChanged = false;
            for (int iPrev = 0; iPrev < hashesAndVertices.size(); ) {
                int nextIPrev = hashesAndVertices.size();
                final HashVertex prev = hashesAndVertices.get(iPrev);
                final N a = prev.vertex;
                for (int iCurr = iPrev + 1; iCurr < hashesAndVertices.size(); iCurr++) {
                    final HashVertex curr = hashesAndVertices.get(iCurr);
                    final N b = curr.vertex;
                    if (b == null) continue;
                    if (prev.h!=curr.h) {
                        if (nextIPrev == hashesAndVertices.size()) {
                            //this is just a tiny optimization
                            //but everything would work just fine without it
                            nextIPrev = iCurr;
                        }
                        break;
                    }
                    final P finalA = getFinalEdge(a);
                    final P finalB = getFinalEdge(b);
                    if (areFinalOutputsEquivalentOrNull.test(finalA, finalB) &&
                            areEquivalent.test(outgoing(a), outgoing(b))) {
                        final P mergedFinal = mergeFinalOutputsOrNull.merge(a, finalA, b, finalB);
                        if (mergedFinal == null) {
                            removeFinalEdge(a);
                        } else {
                            setFinalEdge(a, mergedFinal);
                        }
                        removeFinalEdge(b);
                        HashMap<E, N> incomingA = (HashMap<E, N>) getColor(a);
                        HashMap<E, N> incomingB = (HashMap<E, N>) getColor(b);
                        //redirect all edges incoming to B so that now the come to A
                        for (Map.Entry<E, N> incomingToB : incomingB.entrySet()) {
                            if (incomingToB.getValue() == null) {
                                addInitialEdge(a, incomingToB.getKey());
                                removeInitialEdge(b, incomingToB.getKey());
                            } else {
                                add(incomingToB.getValue(), incomingToB.getKey(), a);
                                remove(incomingToB.getValue(), incomingToB.getKey(), b);
                            }
                            incomingA.put(incomingToB.getKey(), incomingToB.getValue());
                        }

                        curr.erase();//B is now effectively lost
                        //and A took all the edges that were incident to B
                        anyChanged = true;
                    } else if (nextIPrev == hashesAndVertices.size()) {
                        //this is just a tiny optimization
                        //but everything would work just fine without it
                        nextIPrev = iCurr;
                    }
                }
                iPrev = nextIPrev;
            }
            if (!anyChanged) break;
        }
        /**Now the same process as above is repeated but this time for reversed transitions.
         * This corresponds exactly to the duality between observable and reachable states.
         * If two states have the exact same outgoing transitions, then you cannot observe any distinguishing
         * sequence for them (from Myhill-Nerode theorem). On the other hand if two states
         * have the exact same incoming transitions, then you cannot reach one of them without
         * also reaching the other (hence the reversed automaton has no distinguishing sequence for them).
         * Two states can be merged if there is either no observable or no reachable distinguishing sequence.
         * This also has many connections with factoring out common parts of regular expressions.
         * For instance observability corresponds to factoring out suffixes ("abc" | "dec") = ("ab"|"de") "c" and
         * reachability corresponds to factoring out prefixes ("cab" | "cde") = "c" ("ab"|"de").
         * See Brzozowski's algorithm for more detail.*/
        while (true) {
            hashesAndVertices.forEach(HashVertex::computeHashIncoming);
            hashesAndVertices.sort(Comparator.comparingInt(a -> a.h));
            boolean anyChanged = false;
            for (int iPrev = 0; iPrev < hashesAndVertices.size(); ) {
                int nextIPrev = hashesAndVertices.size();
                final HashVertex prev = hashesAndVertices.get(iPrev);
                final N a = prev.vertex;
                for (int iCurr = iPrev + 1; iCurr < hashesAndVertices.size(); iCurr++) {
                    final HashVertex curr = hashesAndVertices.get(iCurr);
                    final N b = curr.vertex;
                    if (b == null) continue;
                    if (prev.h!=curr.h) {
                        if (nextIPrev == hashesAndVertices.size()) {
                            //this is just a tiny optimization
                            //but everything would work just fine without it
                            nextIPrev = iCurr;
                        }
                        break;
                    }
                    final HashMap<E, N> incomingA = (HashMap<E, N>) getColor(a);
                    final HashMap<E, N> incomingB = (HashMap<E, N>) getColor(b);
                    if (areEquivalent.test(incomingA, incomingB)) {
                        final P mergedFinal = mergeFinalOutputsOrNull.merge(a, getFinalEdge(a), b, getFinalEdge(b));
                        if (mergedFinal != null) {
                            setFinalEdge(a, mergedFinal);
                        } else {
                            removeFinalEdge(a);
                        }
                        removeFinalEdge(b);
                        //copies all edges outgoing from B so that now the come out of A as well
                        for (Map.Entry<E, N> outgoingFromB : (Iterable<? extends Map.Entry<E, N>>) () -> iterator(b)) {
                            final N targetComingFromB = outgoingFromB.getValue();
                            add(a, outgoingFromB.getKey(), targetComingFromB);
                            final HashMap<E, N> outgoingFromBInverse = (HashMap<E, N>) getColor(targetComingFromB);
                            outgoingFromBInverse.put(outgoingFromB.getKey(), a);
                        }
                        for (Map.Entry<E, N> incomingToB : incomingB.entrySet()) {
                            final N sourceComingToB = incomingToB.getValue();
                            if (sourceComingToB == null) {
                                removeInitialEdge(b, incomingToB.getKey());
                            } else {
                                remove(sourceComingToB, incomingToB.getKey(), b);
                            }

                        }
                        curr.erase();//B is now effectively lost
                        //and A took all the edges that were incident to B
                        anyChanged = true;
                    } else if (nextIPrev == hashesAndVertices.size()) {
                        //this is just a tiny optimization
                        //but everything would work just fine without it
                        nextIPrev = iCurr;
                    }
                }
                iPrev = nextIPrev;
            }
            if (!anyChanged) break;
        }
        //clean-up
        for (HashVertex vertex : hashesAndVertices) {
            if (vertex.vertex != null) setColor(vertex.vertex, null);
        }
    }

}
