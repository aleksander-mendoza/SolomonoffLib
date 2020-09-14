package net.alagris;

import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.function.*;

/**
 * Specification of edges of ranged weighted transducers.
 *
 * @param <E>   set of (full) edges that have associated output
 *              and weight. Each edge spans over range of input symbols
 *              (methods {@link Specification#from} and {@link Specification#to}).
 *              <b>The edges should be immutable as they are often used as keys in various maps!</b>
 * @param <P>   set of partial edges that have weight and output but have no input.
 *              <b>The edges should be immutable as they are often used as keys in various maps!</b>
 * @param <In>  set of initial symbols. It must be totally ordered using {@link Specification#compare}.
 *              Moreover this gives raise to lexicographic ordering {@link Specification#lexCompare} on edges E.
 * @param <Out> set of output symbols. Forms monoid under multiplication
 * @param <W>   set of weights. Forms monoid under multiplication
 */
public interface Specification<V, E, P, In, Out, W, N, G extends IntermediateGraph<V, E, P, N>> {
    /**
     * Multiplicative operation of monoid on set of weights
     */
    W multiplyWeights(W lhs, W rhs);

    /**
     * Multiplicative operation of monoid on set of outputs
     */
    Out multiplyOutputs(Out lhs, Out rhs);

    /**
     * Neutral element of monoid on set of weights
     */
    W weightNeutralElement();

    /**
     * Neutral element of monoid on set of output elements
     */
    Out outputNeutralElement();

    Out output(E edge);

    W weight(E edge);

    /**
     * Group multiplication in set of partial edges (the multiplication itself is not partial!)
     */
    P multiplyPartialEdges(P edge, P edge2);

    /**
     * Partial edge constructor
     */
    P createPartialEdge(Out out, W weight);

    /**
     * Full edge constructor
     */
    E createFullEdge(In from, In to, P partialEdge);

    /**
     * Intermediate graph constructor. The empty graph has no edges nor vertices
     */
    G createEmptyGraph();

    /**
     * Group of partial edges acting on set of full edges. It essentially prepends partial edge to the full edge.
     */
    E leftAction(P edge, E edge2);

    /**
     * Group of partial edges acting on set of full edges. It essentially appends partial edge to the full edge.
     */
    E rightAction(E edge, P edge2);


    /**
     * It must always hold that compare(from(r),to(r))<=0
     */
    In from(E edge);

    /**
     * It must always hold that compare(from(r),to(r))<=0
     */
    In to(E edge);

    /**
     * Should return true if there is no In symbol that would reside between predecessor and successor.
     * For instance, if input alphabet are real number (floats) then this function will always return false.
     * If the alphabet consists of letters a,b,c,d then isSuccessor(a,b)==true and isSuccessor(a,c)==false.
     * You can assume that predecessor&lt;successor hence calls like isSuccessor(a,a)
     * or isSuccessor(b,a) should not happen.
     */
    boolean isSuccessor(In predecessor, In successor);

    /**
     * Returns the smallest input symbol
     */
    In minimal();

    /**
     * Returns the largest input symbol
     */
    In maximal();

    /**
     * It must always hold that compare(from(r),to(r))<=0
     */
    int compare(In point1, In point2);

    /**
     * Lexicographic order on ranges
     */
    default int lexCompare(E edge, E edge2) {
        int c = compare(from(edge), from(edge2));
        return c == 0 ? compare(to(edge), to(edge2)) : c;
    }

    /**
     * checking if ranges share common interval
     */
    default boolean overlap(E edge, E edge2) {
        return compare(to(edge), from(edge2)) >= 0 && compare(from(edge), to(edge2)) <= 0;
    }

    /**
     * checking if range contains a point
     */
    default boolean contains(E edge, In point) {
        return compare(from(edge), point) <= 0 && compare(point, to(edge)) <= 0;
    }

    /**
     * this function takes parsing context and produced meta-information that should
     * be associated with given AST node. It can be used to obtain line number which
     * would later be useful for debugging and printing meaningful error messages.
     */
    public V metaInfoGenerator(TerminalNode parseNode);

    public V metaInfoGenerator(ParserRuleContext parseNode);

    public V metaInfoNone();

    /**
     * it takes terminal node associated with particular string literal that will be
     * used to build Product node in AST.
     */
    public Out parseStr(TerminalNode parseNode) throws CompilationError;

    /**
     * Parses weights. In the source code weights are denoted with individual
     * integers. You may parse them to something else than numbers if you want.
     */
    public W parseW(TerminalNode parseNode) throws CompilationError;

    /**
     * Parses ranges. In the source code ranges are denoted with pairs of unicode
     * codepoints. You may parse them to something else if you want.
     */
    public Pair<In, In> parseRange(int codepointFrom, int codepointTo);

    /**
     * The largest range of values associated with the . dot
     */
    Pair<In, In> dot();

    /**
     * The special value associated with the # symbol
     */
    In hashtag();


    P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

    P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;


    //////////////////////////
    // Below are default functions added for convenience
    /////////////////////////

    public default P partialNeutralEdge() {
        return createPartialEdge(outputNeutralElement(), weightNeutralElement());
    }

    public default P partialOutputEdge(Out out) {
        return createPartialEdge(out, weightNeutralElement());
    }

    public default P partialWeightedEdge(W w) {
        return createPartialEdge(outputNeutralElement(), w);
    }

    public default E fullNeutralEdge(In from, In to) {
        return createFullEdge(from, to, partialNeutralEdge());
    }

    /**
     * Singleton graph (single vertex with one incoming and one outgoing transition)
     */
    public default G singletonGraph(E initialEdge, N singletonState, P finalEdge) {
        G empty = createEmptyGraph();
        empty.setInitialEdge(singletonState, initialEdge);
        empty.setFinalEdge(singletonState, finalEdge);
        return empty;
    }

    /**
     * Singleton graph built from a single state that accepts a specified range of inputs
     */
    public default G atomicRangeGraph(In from, V state, In to) {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        E e = fullNeutralEdge(from, to);
        N n = empty.create(state);
        empty.setInitialEdge(n, e);
        empty.setFinalEdge(n, p);
        return empty;
    }

    /**
     * Empty graph with a single epsilon edge
     */
    public default G atomicEpsilonGraph() {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        empty.setEpsilon(p);
        return empty;
    }

    /**
     * This method is largely based on {@link SinglyLinkedGraph#deepClone}
     */
    public default G deepClone(G original) {
        G clone = createEmptyGraph();
        HashMap<N, N> clonedVertices = new HashMap<>();
        for (EN<N, E> init : (Iterable<EN<N, E>>) () -> original.iterateInitialEdges()) {
            N clonedVertex = SinglyLinkedGraph.deepClone(original, init.getVertex(), clonedVertices);
            clone.setInitialEdge(clonedVertex, init.getEdge());
        }
        for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> original.iterateFinalEdges()) {
            N clonedVertex = SinglyLinkedGraph.deepClone(original, fin.getVertex(), clonedVertices);
            clone.setFinalEdge(clonedVertex, fin.getEdge());
        }
        clone.setEpsilon(original.getEpsilon());
        return clone;
    }

    /**
     * The two graphs should have no vertices in common. After running this method
     * the left graph will contain joined contents of both graphs and the right
     * graph should not be reused!
     * You have to dispose of the right graph because mutations to it might
     * accidentally change contents of left graph.
     *
     * @param mergeEpsilons custom procedure for merging two epsilon transitions in cases when both
     *                      graphs already had their own epsilon edges. This function may throw if
     *                      such operation is not supported.
     */
    public default G union(G lhs, G rhs, BiFunction<P, P, P> mergeEpsilons) {
        for (EN<N, E> init : (Iterable<EN<N, E>>) () -> rhs.iterateInitialEdges()) {
            lhs.setInitialEdge(init.getVertex(), init.getEdge());
        }
        for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> rhs.iterateFinalEdges()) {
            lhs.setFinalEdge(fin.getVertex(), fin.getEdge());
        }
        P lhsEps = lhs.getEpsilon();
        P rhsEps = rhs.getEpsilon();
        lhs.setEpsilon(lhsEps == null ? rhsEps : (rhsEps == null ? lhsEps : mergeEpsilons.apply(lhsEps, rhsEps)));
        return lhs;
    }

    /**
     * The two graphs should have no vertices in common. After running this method
     * the left graph will contain concatenated contents of both graphs and the right
     * graph should not be reused!
     * You have to dispose of the right graph because mutations to it might
     * accidentally change contents of left graph.
     */
    public default G concat(G lhs, G rhs) {
        for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> lhs.iterateFinalEdges()) {
            for (EN<N, E> init : (Iterable<EN<N, E>>) () -> rhs.iterateInitialEdges()) {
                lhs.add(fin.getVertex(), leftAction(fin.getEdge(), init.getEdge()), init.getVertex());
            }
        }
        P lhsEps = lhs.getEpsilon();
        if (lhsEps != null) {
            for (EN<N, E> init : (Iterable<EN<N, E>>) () -> rhs.iterateInitialEdges()) {
                lhs.setInitialEdge(init.getVertex(), leftAction(lhsEps, init.getEdge()));
            }
        }
        P rhsEps = rhs.getEpsilon();
        if (rhsEps != null) {
            for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> lhs.iterateFinalEdges()) {
                rhs.setFinalEdge(fin.getVertex(), multiplyPartialEdges(fin.getEdge(), rhsEps));
            }
        }
        lhs.replaceFinal(rhs);
        if (lhsEps != null && rhsEps != null) {
            lhs.setEpsilon(multiplyPartialEdges(lhsEps, rhsEps));
        } else {
            lhs.setEpsilon(null);
        }
        return lhs;
    }

    /**
     * Perform operation of Kleene closure on graph.
     */
    public default G kleene(G graph, Function<P, P> kleeneEpsilon) {
        for (EN<N, P> fin : (Iterable<EN<N, P>>) () -> graph.iterateFinalEdges()) {
            for (EN<N, E> init : (Iterable<EN<N, E>>) () -> graph.iterateInitialEdges()) {
                graph.add(fin.getVertex(), leftAction(fin.getEdge(), init.getEdge()), init.getVertex());
            }
        }
        P eps = graph.getEpsilon();
        if (eps == null) {
            graph.setEpsilon(partialNeutralEdge());
        } else {
            graph.setEpsilon(kleeneEpsilon.apply(eps));
        }
        return graph;
    }

    /**
     * Performs left action on all initial edges and epsilon.
     */
    public default G leftActionOnGraph(P edge, G graph) {
        graph.mapAllInitialEdges((e, vertex) -> leftAction(edge, e));
        P eps = graph.getEpsilon();
        if (eps != null) graph.setEpsilon(multiplyPartialEdges(edge, eps));
        return graph;
    }

    /**
     * Performs right action on all final edges and epsilon.
     */
    public default G rightActionOnGraph(G graph, P edge) {
        graph.mapAllFinalEdges((e, vertex) -> multiplyPartialEdges(e, edge));
        P eps = graph.getEpsilon();
        if (eps != null) graph.setEpsilon(multiplyPartialEdges(eps, edge));
        return graph;
    }

    /**
     * Collects all vertices in this graph into a specified set. This methods also
     * implements depth-first search. Because the graph may contain loops, the
     * search cannot be done without collecting the visited vertices alongside,
     * hence this algorithm is called "collect" rather than "search".
     *
     * @param startpoint     the vertex that marks beginning of graph. You may want
     *                       to use {@link IntermediateGraph#makeUniqueInitialState}
     *                       to generate the unique initial state.
     * @param shouldContinue allows for early termination. Especially useful, when
     *                       you don't actually care about collecting the set but
     *                       only want to search something.
     * @return collected set if successfully explored entire graph. Otherwise null
     * if early termination occurred
     */
    public default <S extends Set<N>> S collect(G graph, N startpoint, S set, Predicate<N> shouldContinue) {
        return SinglyLinkedGraph.collect(graph, startpoint, set, shouldContinue);
    }

    public default HashSet<N> collect(G graph, N startpoint) {
        return SinglyLinkedGraph.collect(graph, startpoint, new HashSet<>(), x -> true);
    }


    public static class Range<In, M> {
        private final In input;
        private  List<M> atThisInput;
        private  List<M> betweenThisAndPreviousInput;

        public Range(In input, List<M> atThisInput, List<M> betweenThisAndPreviousInput) {
            this.input = input;
            this.atThisInput = atThisInput;
            this.betweenThisAndPreviousInput = betweenThisAndPreviousInput;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "input=" + input +
                    ", atThisInput=" + atThisInput +
                    ", betweenThisAndPreviousInput=" + betweenThisAndPreviousInput +
                    '}';
        }
    }

    public static <X> void removeTail(List<X> list, int desiredLength) {
        while (list.size() > desiredLength) {
            list.remove(list.size() - 1);
        }
    }

    public static <X> ArrayList<X> filledArrayList(int size, X defaultElement) {
        ArrayList<X> arr = new ArrayList<>(size);
        while (size-- > 0) arr.add(defaultElement);
        return arr;
    }

    public static <X> ArrayList<X> concatArrayLists(List<X> a, List<X> b) {
        ArrayList<X> arr = new ArrayList<>(a.size() + b.size());
        arr.addAll(a);
        arr.addAll(b);
        return arr;
    }

    public static <X> void removeDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        if (sortedArray.size() > 1) {
            int i = 0;
            for (int j = 1; j < sortedArray.size(); j++) {
                X prev = sortedArray.get(i);
                X curr = sortedArray.get(j);
                if (areEqual.test(prev, curr)) {
                    mergeEqual.accept(prev, curr);
                } else {
                    i++;
                    sortedArray.set(i, curr);
                }
            }
            removeTail(sortedArray, i + 1);
        }
    }

    public default <M> ArrayList<Range<In, M>> optimise(G graph, N vertex, List<M> sinkTransition, Function<EN<N, E>, M> map) {
        class IBE {
            /**input symbol*/
            final In i;
            /**edges beginning at input symbol*/
            final ArrayList<EN<N, E>> b = new ArrayList<>();
            /**edges ending at input symbol*/
            final ArrayList<EN<N, E>> e = new ArrayList<>();

            IBE(EN<N, E> b, EN<N, E> e) {
                this.i = b == null ? to(e.getEdge()) : from(b.getEdge());
                if (b != null) this.b.add(b);
                if (e != null) this.e.add(e);
            }

            int compare(IBE other) {
                return Specification.this.compare(i, other.i);
            }
        }
        final ArrayList<IBE> points = new ArrayList<>();
        graph.iterator(vertex).forEachRemaining(edge -> {
            points.add(new IBE(edge, null));
            points.add(new IBE(null, edge));
        });
        points.sort(IBE::compare);
        removeDuplicates(points, (prev, curr) -> prev.i.equals(curr.i), (prev, curr) -> {
            prev.b.addAll(curr.b);
            prev.e.addAll(curr.e);
        });

        ArrayList<Range<In, M>> transitions = new ArrayList<>(points.size());
        ArrayList<EN<N, E>> accumulated = new ArrayList<>();
        In prev = minimal();
        for (IBE ibe : points) {
            final List<M> betweenThisAndPreviousInput;
            if (accumulated.size() == 0 && !Objects.equals(ibe.i, prev) && !isSuccessor(prev, ibe.i)) {
                betweenThisAndPreviousInput = sinkTransition;
            } else {
                betweenThisAndPreviousInput = new ArrayList<>(accumulated.size());
                for (final EN<N, E> acc : accumulated) betweenThisAndPreviousInput.add(map.apply(acc));
            }
            accumulated.addAll(ibe.b);
            final List<M> atThisInput;
            if (accumulated.size() == 0 && !Objects.equals(ibe.i, prev) && !isSuccessor(prev, ibe.i)) {
                atThisInput = sinkTransition;
            } else {
                atThisInput = new ArrayList<>(accumulated.size());
                for (final EN<N, E> acc : accumulated) atThisInput.add(map.apply(acc));
            }
            transitions.add(new Range<>(ibe.i, atThisInput, betweenThisAndPreviousInput));
            accumulated.removeAll(ibe.e);
            prev = ibe.i;
        }
        assert accumulated.isEmpty() : "Unexpected leftovers! " + accumulated.toString();
        return transitions;
    }

    public static class RangedGraph<V, In, E, P> {


        public boolean isAccepting(int state) {
            return accepting.get(state) != null;
        }

        public V state(int stateIdx) {
            return indexToState.get(stateIdx);
        }

        public P getFinalEdge(int state) {
            return accepting.get(state);
        }

        static class Trans<E> implements EN<Integer, E> {
            final E edge;
            int targetState;

            public Trans(E edge, int targetState) {
                this.edge = edge;
                this.targetState = targetState;
            }

            @Override
            public E getEdge() {
                return edge;
            }

            @Override
            public Integer getVertex() {
                return targetState;
            }

            @Override
            public String toString() {
                return edge + "->" + targetState;
            }
        }

        ArrayList<ArrayList<Range<In, Trans<E>>>> graph;
        ArrayList<P> accepting;
        ArrayList<V> indexToState;
        int initial;
        int sinkState;
        /**
         * Normally all transitions should be distinct instances of arrays, so that mutations in one of them doesn't
         * accidentally affect other places. However, due to how frequent and special the transition to sink state it,
         * it's the only array whose instance is shared among all edges. When designing algorithms that mutate transitions,
         * make sure to check object equality with == to make sure it's not the sink transition. It needs to be treated
         * separately.
         */
        private List<Trans<E>> sinkTrans;

        public RangedGraph(ArrayList<ArrayList<Range<In, Trans<E>>>> graph, ArrayList<P> accepting,
                           ArrayList<V> indexToState, int initial, int sinkState, List<RangedGraph.Trans<E>> sinkTrans) {
            this.graph = graph;
            this.accepting = accepting;
            this.indexToState = indexToState;
            this.initial = initial;
            this.sinkState = sinkState;
            this.sinkTrans = sinkTrans;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("init ").append(initial).append('\n');
            for (int i = 0; i < graph.size(); i++) {
                for (Range<In, Trans<E>> r : graph.get(i)) {
                    sb.append(i)
                            .append(" ")
                            .append(r.input)
                            .append(" ")
                            .append(r.betweenThisAndPreviousInput)
                            .append(r.atThisInput)
                            .append('\n');

                }
            }
            for (int i = 0; i < accepting.size(); i++) {
                if (accepting.get(i) != null) {
                    sb.append("fin ").append(i)
                            .append(" ")
                            .append(accepting.get(i))
                            .append('\n');

                }
            }
            return sb.toString();
        }

        /**
         * Attempts to find a state with two transitions outgoing to different states
         * but having overlapping (and hence ambiguous) ranges. If successfully found, then the transitions are
         * returned and automaton is nondeterministic. Otherwise returns null and the automaton is deterministic.
         * Note that the functioning of this method is based on the assumption that
         * the automaton is always trim (has no unreachable/unobservable states)
         */
        public List<Trans<E>> isDeterministic() {
            for (ArrayList<Range<In, Trans<E>>> state : graph) {
                for (Range<In, Trans<E>> trans : state) {
                    if (trans.atThisInput.size() > 1)
                        return trans.atThisInput;
                    if (trans.betweenThisAndPreviousInput.size() > 1)
                        return trans.betweenThisAndPreviousInput;
                }
            }
            return null;
        }

        /**
         * Implements a special indexing for outgoing transitions. Even indices represent transitions over
         * ranges (betweenThisAndPreviousInput), while odd indices represent transitions at particular points
         * (atThisInput)
         */
        public List<Trans<E>> getTrans(ArrayList<Range<In, Trans<E>>> transitions, int index) {
            if (index >= transitions.size() * 2) return sinkTrans;
            if (index % 2 == 0) {
                return transitions.get(index / 2).betweenThisAndPreviousInput;
            } else {
                return transitions.get(index / 2).atThisInput;
            }
        }


        private boolean areEquivalentFromReverse(BiPredicate<E, E> areEquivalent,
                                                 BiPredicate<P, P> arePartialEquivalent,
                                                 TranWithHash prev, TranWithHash curr, int[] originalIndexToNewIndex) {

            if (prev.transitions.size() != curr.transitions.size())
                return false;
            if (!arePartialEquivalent.test(accepting.get(prev.originalIndex), accepting.get(curr.originalIndex)))
                return false;
            for (int j = 0; j < prev.transitions.size(); j++) {
                final Range<In, Trans<E>> prevTran = prev.transitions.get(j);
                final Range<In, Trans<E>> currTran = curr.transitions.get(j);
                if (!areEquivalentFromReverse(areEquivalent, originalIndexToNewIndex,
                        prevTran.atThisInput, currTran.atThisInput)) {
                    return false;
                }
                if (!areEquivalentFromReverse(areEquivalent, originalIndexToNewIndex,
                        prevTran.betweenThisAndPreviousInput, currTran.betweenThisAndPreviousInput)) {
                    return false;
                }
            }
            return true;
        }

        private boolean areEquivalentFromReverse(BiPredicate<E, E> areEquivalent, int[] originalIndexToNewIndex,
                                                 List<Trans<E>> prevTran, List<Trans<E>> currTran) {
            if (prevTran.size() != currTran.size()) {
                return false;
            }
            for (int k = 0; k < prevTran.size(); k++) {
                final Trans<E> prevEdge = prevTran.get(k);
                final Trans<E> currEdge = currTran.get(k);
                if (originalIndexToNewIndex[prevEdge.targetState] !=
                        originalIndexToNewIndex[currEdge.targetState] ||
                        !areEquivalent.test(prevEdge.edge, currEdge.edge)) {
                    return false;
                }
            }
            return true;
        }

        class TranRevWithHash {
            int hash;
            final int originalIndex;

            public void add(int i, In from, In to, E edge) {
                transitions.add(new Edge(i, from, to, edge));
            }

            class Edge {
                final int source;
                final In from, to;
                final E edge;

                Edge(int source, In from, In to, E edge) {
                    this.source = source;
                    this.from = from;
                    this.to = to;
                    this.edge = edge;
                }
            }

            /**
             * Array of all incoming edges. Pairs of edges and source states.
             */
            final ArrayList<Edge> transitions = new ArrayList<>();

            public TranRevWithHash(int originalIndex) {
                this.originalIndex = originalIndex;
            }

        }

        private final class TranWithHash {
            /**
             * This hash carries information about number and labels of transitions.
             * The idea behind this pseudo minimisation algorithm is to merge states that have equivalent transitions.
             * Instead of implementing quadratic algorithm comparing all vertices with each other, a certain
             * hashing function should be designed in such a way that all potentially equivalent states get equal
             * hashes. Then it's enough to sort the array of all states according to their hashes and only compare
             * those of them that share equal hash. Each time an equivalent pair is found and merged, the array should
             * not need to be updated. Therefore, the value of hash should not depend on indices of any states.
             * It should depend solely on inputs,outputs and weights of edges.
             */
            final int hash;
            final int originalIndex;
            final ArrayList<Range<In, Trans<E>>> transitions;

            public TranWithHash(int originalIndex, ArrayList<Range<In, Trans<E>>> transitions) {
                int h = 0;
                for (Range<In, Trans<E>> r : transitions) {
                    h = 31 * h + r.input.hashCode();
                }
                this.hash = h;
                this.originalIndex = originalIndex;
                this.transitions = transitions;
            }

            @Override
            public String toString() {
                return "Tran{" +
                        "hash=" + hash +
                        ", originalIndex=" + originalIndex +
                        ", transitions=" + transitions +
                        '}';
            }
        }

        private boolean areEquivalentFromReverse(BiPredicate<E, E> areEquivalent,
                                                 int[] originalIndexToNewIndex,
                                                 TranRevWithHash prev,
                                                 TranRevWithHash curr) {
            if (prev.transitions.size() != curr.transitions.size())
                return false;
            for (int j = 0; j < prev.transitions.size(); j++) {
                final TranRevWithHash.Edge prevTran = prev.transitions.get(j);
                final TranRevWithHash.Edge currTran = curr.transitions.get(j);
                if (originalIndexToNewIndex[prevTran.source] !=
                        originalIndexToNewIndex[currTran.source] ||
                        !areEquivalent.test(prevTran.edge, currTran.edge)) {
                    return false;
                }
            }
            return true;
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
         * @param areEquivalent  normally you should compare whether weight and output of two edges are the same, but
         *                       it's possible to implement custom logic for transition equivalence
         *                       (like ignoring weights or storing custom additional attributes in each edge)
         * @param comp
         * @param fixTransitions All of the transitions start in the same source vertex,
         *                       and have the same input range. The only thing that's different are outputs, weights and target vertices.
         *                       After minimization, many vertices are merged and their indices are changed. This may leads to conflicts and
         *                       duplication. You need to implement here some way of  removing duplicates and disambiguating
         *                       different outputs based on their weights etc.
         * @param finalUnion     merging of states might lead to merging several accepting states together. In such cases,
         *                       the conflicts need to be somehow resolved (probably by choosing the one with higher weight)
         */
        public void pseudoMinimize(In minimal,
                                   BiPredicate<E, E> areEquivalent,
                                   BiPredicate<P, P> arePartialEquivalent,
                                   Consumer<ArrayList<TranRevWithHash.Edge>> sortAndRemoveDuplicateEdges,
                                   Comparator<In> comp,
                                   Function<List<Trans<E>>, List<Trans<E>>> fixTransitions,
                                   BiFunction<P, P, P> finalUnion) {
            if (graph.size() <= 3) return;//sink states, initial state and one more state

            int[] originalIndexToNewIndex = new int[graph.size()];
            {
                ArrayList<TranWithHash> states = new ArrayList<>(graph.size());
                for (int i = 0; i < graph.size(); i++) {
                    final ArrayList<Range<In, Trans<E>>> state = graph.get(i);
                    states.add(new TranWithHash(i, state));
                }
                states.sort(Comparator.comparingInt(a -> a.hash));

                /** At the end of running this algorithm, several states might be assigned new
                 * identical indices. Such states will effectively be merged. So the merging process
                 * is all about reassigning indices with some becoming duplicate and some being lost completely*/
                for (int i = 0; i < states.size(); i++) {
                    originalIndexToNewIndex[states.get(i).originalIndex] = i;
                }
                while (true) {
                    boolean anyChanged = false;
                    for (int iPrev = 0; iPrev < states.size(); ) {
                        int nextIPrev = states.size();
                        //all transitions that have the same hash must be compared with one another,
                        //which results in quadratic algorithm (with respect to number of elements with
                        //equal hashes).
                        for (int iCurr = iPrev + 1; iCurr < states.size(); iCurr++) {
                            final TranWithHash prev = states.get(iPrev);
                            final TranWithHash curr = states.get(iCurr);
                            if (prev.hash != curr.hash) {
                                if (nextIPrev == states.size()) {
                                    //this is just a tiny optimization
                                    //but everything would work just fine without it
                                    nextIPrev = iCurr;
                                }
                                break;
                            }
                            if (originalIndexToNewIndex[prev.originalIndex] == originalIndexToNewIndex[curr.originalIndex]) {
                                continue;
                            }
                            if (areEquivalentFromReverse(areEquivalent, arePartialEquivalent, prev, curr, originalIndexToNewIndex)) {
                                originalIndexToNewIndex[curr.originalIndex] = originalIndexToNewIndex[prev.originalIndex];
                                anyChanged = true;
                            } else if (nextIPrev == states.size()) {
                                //this is just a tiny optimization
                                //but everything would work just fine without it
                                nextIPrev = iCurr;
                            }
                        }
                        iPrev = nextIPrev;
                    }
                    if (!anyChanged) break;
                }
                // Now states array can be garbage collected. It might be a problem for
                // large automata
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
            {
                ArrayList<TranRevWithHash> states = new ArrayList<>(graph.size());
                //initialize array of reversed transitions
                for (int i = 0; i < graph.size(); i++) {
                    states.add(new TranRevWithHash(i));
                }
                //populate edges
                for (int i = 0; i < graph.size(); i++) {
                    In prevIn = minimal;
                    for (Range<In, Trans<E>> r : graph.get(i)) {
                        for (Trans<E> tr : r.atThisInput) {
                            states.get(tr.targetState).add(i, r.input, r.input, tr.edge);
                        }
                        for (Trans<E> tr : r.betweenThisAndPreviousInput) {
                            states.get(tr.targetState).add(i, prevIn, r.input, tr.edge);
                        }
                        prevIn = r.input;
                    }
                }
                //compute hashes
                for (TranRevWithHash state : states) {
                    sortAndRemoveDuplicateEdges.accept(state.transitions);
                    int h = 0;
                    for (TranRevWithHash.Edge tr : state.transitions) {
                        h = 31 * h + Objects.hash(tr.from, tr.to);
                    }
                    state.hash = h;
                }
                //find states available for merging
                states.sort(Comparator.comparingInt(a -> a.hash));
                while (true) {
                    boolean anyChanged = false;
                    for (int iPrev = 0; iPrev < states.size(); iPrev++) {
                        for (int iCurr = iPrev + 1; iCurr < states.size(); iCurr++) {

                            final TranRevWithHash prev = states.get(iPrev);
                            final TranRevWithHash curr = states.get(iCurr);
                            if (prev.hash != curr.hash) {
                                break;
                            }

                            if (originalIndexToNewIndex[prev.originalIndex] == originalIndexToNewIndex[curr.originalIndex])
                                continue;

                            if (areEquivalentFromReverse(areEquivalent, originalIndexToNewIndex, prev, curr)) {
                                originalIndexToNewIndex[curr.originalIndex] = originalIndexToNewIndex[prev.originalIndex];
                                anyChanged = true;
                            }
                        }
                    }
                    if (!anyChanged) break;
                }
                // Now states array can be garbage collected. It might be a problem for
                // large automata
            }
            shift(originalIndexToNewIndex);
            mergeStates(originalIndexToNewIndex, comp, fixTransitions, finalUnion);
        }


        /**
         * Reassigns new indices in such a way that no indices are left unused. For example
         * [2,7,1,3] would become [1,3,0,2] because the indices 0,4,5 and 6 were unused.
         */
        public void shift(int[] originalIndexToNewIndex) {
            int[] newIndexToNewNewIndex = new int[originalIndexToNewIndex.length];
            for (int originalIdx = 0; originalIdx < originalIndexToNewIndex.length; originalIdx++) {
                newIndexToNewNewIndex[originalIndexToNewIndex[originalIdx]] = 1;
            }
            for (int newIdx = 0, newNewIdx = 0; newIdx < newIndexToNewNewIndex.length; newIdx++) {
                if (newIndexToNewNewIndex[newIdx] == 1) {
                    newIndexToNewNewIndex[newIdx] = newNewIdx++;
                }
            }
            for (int originalIdx = 0; originalIdx < originalIndexToNewIndex.length; originalIdx++) {
                originalIndexToNewIndex[originalIdx] = newIndexToNewNewIndex[originalIndexToNewIndex[originalIdx]];
            }
        }

        private List<Trans<E>> transUnionOrSink(List<Trans<E>> lhs, List<Trans<E>> rhs) {
            if (lhs == sinkTrans) {
                if (rhs == sinkTrans) {
                    return sinkTrans;
                } else {
                    return new ArrayList<>(rhs);
                }
            }
            if (rhs == sinkTrans) return new ArrayList<>(lhs);
            return concatArrayLists(lhs, rhs);
        }

        public ArrayList<Range<In, Trans<E>>> mergeTransitions(ArrayList<Range<In, Trans<E>>> lhs,
                                                               ArrayList<Range<In, Trans<E>>> rhs,
                                                               Comparator<In> comp) {
            ArrayList<Range<In, Trans<E>>> merged = new ArrayList<>(lhs.size() + rhs.size());
            int i = 0;
            int j = 0;
            while (i < lhs.size() && j < rhs.size()) {
                final Range<In, Trans<E>> rLhs = lhs.get(i);
                final Range<In, Trans<E>> rRhs = rhs.get(j);
                final int c = comp.compare(rLhs.input, rRhs.input);
                if (c < 0) {//lhs<rhs
                    final List<Trans<E>> atThisInput =
                            transUnionOrSink(rLhs.atThisInput, rRhs.betweenThisAndPreviousInput);
                    final List<Trans<E>> betweenThisAndPreviousInput =
                            transUnionOrSink(rLhs.betweenThisAndPreviousInput, rRhs.betweenThisAndPreviousInput);
                    merged.add(new Range<>(rLhs.input, atThisInput, betweenThisAndPreviousInput));
                    i++;
                } else if (c > 0) {//lhs>rhs
                    final List<Trans<E>> atThisInput =
                            transUnionOrSink(rLhs.betweenThisAndPreviousInput, rRhs.atThisInput);
                    final List<Trans<E>> betweenThisAndPreviousInput =
                            transUnionOrSink(rLhs.betweenThisAndPreviousInput, rRhs.betweenThisAndPreviousInput);
                    merged.add(new Range<>(rRhs.input, atThisInput, betweenThisAndPreviousInput));
                    j++;
                } else {//lhs==rhs
                    final List<Trans<E>> atThisInput =
                            transUnionOrSink(rLhs.atThisInput, rRhs.atThisInput);
                    final List<Trans<E>> betweenThisAndPreviousInput =
                            transUnionOrSink(rLhs.betweenThisAndPreviousInput, rRhs.betweenThisAndPreviousInput);
                    merged.add(new Range<>(rRhs.input, atThisInput, betweenThisAndPreviousInput));
                    i++;
                    j++;
                }
            }
            while(i < lhs.size()){
                final Range<In, Trans<E>> rLhs = lhs.get(i);
                merged.add(new Range<>(rLhs.input,
                        new ArrayList<>(rLhs.atThisInput),
                        new ArrayList<>(rLhs.betweenThisAndPreviousInput)));
                i++;
            }
            while(j < rhs.size()){
                final Range<In, Trans<E>> rRhs = rhs.get(j);
                merged.add(new Range<>(rRhs.input,
                        new ArrayList<>(rRhs.atThisInput),
                        new ArrayList<>(rRhs.betweenThisAndPreviousInput)));
                j++;
            }
            return merged;
        }

        /**
         * Performs merging of all states that are mapped to the same new index. It's usually a good idea to
         * run {@link RangedGraph#shift} before running this method.
         *
         * @param fixTransitions All of the transitions start in the same source vertex,
         *                       and have the same input range. The only thing that's different are outputs, weights and target vertices.
         *                       After minimization, many vertices are merged and their indices are changed. This may leads to conflicts and
         *                       duplication. You need to implement here some way of  removing duplicates and disambiguating
         *                       different outputs based on their weights etc.
         * @param finalUnion     merging of states might lead to merging several accepting states together. In such cases,
         *                       the conflicts need to be somehow resolved (probably by choosing the one with higher weight)
         */
        public void mergeStates(int[] originalIndexToNewIndex, Comparator<In> comp,
                                Function<List<Trans<E>>, List<Trans<E>>> fixTransitions,
                                BiFunction<P, P, P> finalUnion) {

            //update target vertices in all transitions
            for (ArrayList<Range<In, Trans<E>>> ranges : graph) {

                for (Range<In, Trans<E>> transition : ranges) {
                    if (transition.betweenThisAndPreviousInput != sinkTrans) {
                        for (Trans<E> outgoing : transition.betweenThisAndPreviousInput) {
                            outgoing.targetState = originalIndexToNewIndex[outgoing.targetState];
                        }
                    }
                    if (transition.atThisInput != sinkTrans) {
                        for (Trans<E> outgoing : transition.atThisInput) {
                            outgoing.targetState = originalIndexToNewIndex[outgoing.targetState];
                        }
                    }
                }
            }
            for (Trans<E> outgoing : sinkTrans) {
                outgoing.targetState = originalIndexToNewIndex[outgoing.targetState];
            }
            //merge transitions for all states that have the same new index
            int maxNewIdx = 0;
            for (int newIdx : originalIndexToNewIndex) if (newIdx > maxNewIdx) maxNewIdx = newIdx;
            final ArrayList<ArrayList<Range<In, Trans<E>>>> mergedGraph = filledArrayList(maxNewIdx + 1, null);

            for (int originalIdx = 0; originalIdx < graph.size(); originalIdx++) {
                final int newIdx = originalIndexToNewIndex[originalIdx];
                final ArrayList<Range<In, Trans<E>>> prev = mergedGraph.get(newIdx);
                if (prev == null) {
                    mergedGraph.set(newIdx, graph.get(originalIdx));
                } else {
                    mergedGraph.set(newIdx, mergeTransitions(prev, graph.get(originalIdx), comp));
                }
            }
            graph = mergedGraph;
            //previous graph is free to be garbage collected now. It may be a problem for
            //large automata

            //fix transitions to remove duplicate edges and resolve output ambiguities
            for (ArrayList<Range<In, Trans<E>>> ranges : graph) {

                for (Range<In, Trans<E>> transition : ranges) {
                    if (transition.betweenThisAndPreviousInput != sinkTrans) {
                        transition.betweenThisAndPreviousInput = fixTransitions.apply(transition.betweenThisAndPreviousInput);
                    }
                    if (transition.atThisInput != sinkTrans) {
                        transition.atThisInput = fixTransitions.apply(transition.atThisInput);
                    }
                }
            }

            initial = originalIndexToNewIndex[initial];
            sinkState = originalIndexToNewIndex[sinkState];// this is ok,
            //because sink state cannot ever be merged with any other state
            //(if that happens, then it's fault of caller of this method for
            // providing incorrect originalIndexToNewIndex array).
            //That's because merging sink state with any other state effectively leads to
            //losing sink state altogether.
            final ArrayList<P> mergedAccepting = filledArrayList(maxNewIdx + 1, null);
            for (int originalIdx = 0; originalIdx < accepting.size(); originalIdx++) {
                final P orig = accepting.get(originalIdx);
                if (orig != null) {
                    final int newIdx = originalIndexToNewIndex[originalIdx];
                    final P prev = mergedAccepting.get(newIdx);
                    if (prev == null) {
                        mergedAccepting.set(newIdx, orig);
                    } else {
                        mergedAccepting.set(newIdx, finalUnion.apply(prev, orig));
                    }
                }
            }
            accepting = mergedAccepting;
            //previous array of accepting states is free to be garbage collected now. It may be a problem for
            //large automata
            final ArrayList<V> mergedIndexToState = filledArrayList(maxNewIdx + 1, null);
            for (int originalIdx = 0; originalIdx < indexToState.size(); originalIdx++) {
                final int newIdx = originalIndexToNewIndex[originalIdx];
                mergedIndexToState.set(newIdx, indexToState.get(originalIdx));//Overwrites are possible.
                //In case of conflicts, the outcome is arbitrary.
            }
            indexToState = mergedIndexToState;

        }
    }

    static <X> ArrayList<X> singeltonArrayList(X x) {
        ArrayList<X> a = new ArrayList<>(1);
        a.add(x);
        return a;
    }

    public default RangedGraph<V, In, E, P> optimiseGraph(G graph) {
        final N initial = graph.makeUniqueInitialState(null);
        final HashSet<N> states = collect(graph, initial);
        final int statesNum = states.size() + 1;//extra one for sink state
        final ArrayList<ArrayList<Range<In, RangedGraph.Trans<E>>>> graphTransitions = new ArrayList<>(statesNum);
        final HashMap<N, Integer> stateToIndex = new HashMap<>(statesNum);
        final ArrayList<V> indexToState = new ArrayList<>(statesNum);
        final ArrayList<P> accepting = new ArrayList<>(statesNum);
        final int sinkState = states.size();
        for (N state : states) {
            int idx = indexToState.size();
            indexToState.add(graph.getState(state));
            stateToIndex.put(state, idx);
            graphTransitions.add(null);
            accepting.add(null);
        }
        final List<RangedGraph.Trans<E>> sinkTrans = Collections.singletonList(
                new RangedGraph.Trans<>(null, sinkState));
        graphTransitions.add(singeltonArrayList(new Range<>(maximal(), sinkTrans, sinkTrans)));//sink state transitions
        accepting.add(null);//sink state doesn't accept
        //stateToIndex returns null for sink state
        for (final Map.Entry<N, Integer> state : stateToIndex.entrySet()) {
            final ArrayList<Range<In, RangedGraph.Trans<E>>> transitions = optimise(graph, state.getKey(), sinkTrans,
                    en -> new RangedGraph.Trans<>(en.getEdge(), stateToIndex.get(en.getVertex())));
            graphTransitions.set(state.getValue(), transitions);
            accepting.set(state.getValue(), graph.getFinalEdge(state.getKey()));
        }
        accepting.set(stateToIndex.get(initial), graph.getEpsilon());
        return new RangedGraph<>(graphTransitions, accepting, indexToState, stateToIndex.get(initial), sinkState, sinkTrans);
    }


    /**
     * @return the outgoing transition containing given input. Otherwise transition to sink state is returned.
     */
    public default List<RangedGraph.Trans<E>> binarySearch(RangedGraph<V, In, E, P> graph, int state, In input) {
        ArrayList<Range<In, RangedGraph.Trans<E>>> transitions = graph.graph.get(state);
        return graph.getTrans(transitions, binarySearchIndex(transitions, input));
    }

    /**
     * @return index to a transition containing given input. This is a special index that must be interpreted using
     * {@link RangedGraph#getTrans}. Those indices are useful when you need to iterate over transitions. It's
     * used for implementing {@link Specification#deltaRanged}
     */
    public default int binarySearchIndex(ArrayList<Range<In, RangedGraph.Trans<E>>> transitions, In input) {
        int low = 0;
        int high = transitions.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Range<In, RangedGraph.Trans<E>> midVal = transitions.get(mid);
            int c = compare(midVal.input, input);
            if (c < 0)
                low = mid + 1;
            else if (c > 0)
                high = mid - 1;
            else
                return mid * 2 + 1; // key found
        }
        return low * 2;
    }

    /**
     * This method assumes that graph is deterministic
     */
    public default int deltaBinarySearchDeterministic(RangedGraph<V, In, E, P> graph, int state,
                                                      In input) {
        return binarySearch(graph, state, input).get(0).targetState;
    }

    /**
     * This method assumes that graph is deterministic
     */
    public default int deltaBinarySearchTransitiveDeterministic(RangedGraph<V, In, E, P> graph, int state,
                                                                Iterator<In> input) {
        while (input.hasNext() && state != graph.sinkState) {
            state = deltaBinarySearchDeterministic(graph, state, input.next());
        }
        return state;
    }

    /**
     * For a given range of input symbols, returns joint list of all states that can be reached by taking any of the
     * inputs in the range. All the reached states added to reachedTargetStates.
     * This method assumes that inputFrom &le; inputTo.
     */
    public default void deltaRanged(RangedGraph<V, In, E, P> graph, int state, In inputFrom, In inputTo,
                                    Collection<Integer> reachedTargetStates) {
        final ArrayList<Range<In, RangedGraph.Trans<E>>> transitions = graph.graph.get(state);
        int from = binarySearchIndex(transitions, inputFrom);
        int to = binarySearchIndex(transitions, inputTo);
        for (int i = from; i <= to; i++) {
            for (RangedGraph.Trans<E> transition : graph.getTrans(transitions, i)) {
                reachedTargetStates.add(transition.targetState);
            }
        }
    }

    /**
     * This is n-ary version of {@link Specification#deltaRanged} which takes as input entire list of start-point states.
     */
    public default void deltaRangedNary(RangedGraph<V, In, E, P> graph, Collection<Integer> startpointStates,
                                        In inputFrom, In inputTo,
                                        Collection<Integer> reachedTargetStates) {
        for (int state : startpointStates) {
            deltaRanged(graph, state, inputFrom, inputTo, reachedTargetStates);
        }
    }


    /**
     * This is n-ary version of {@link Specification#deltaBinarySearchDeterministic} which takes as input entire list of start-point states.
     */
    public default void deltaBinarySearchDeterministicNary(RangedGraph<V, In, E, P> graph, Collection<Integer> startpointStates,
                                                           In input,
                                                           Collection<Integer> reachedTargetStates) {
        for (int state : startpointStates) {
            reachedTargetStates.add(deltaBinarySearchDeterministic(graph, state, input));
        }
    }

    public static interface QuadFunction<A, B, C, D, E> {
        E apply(A a, B b, C c, D d);

    }

    /**
     * Performs product of automata. Collects all reachable pairs of states.
     * A pair of transitions between pairs of states is taken only when their input ranges overlap with one another.
     *
     * @param shouldContinuePerEdge  is invoked for every pair of traversed transitions. If non-null value is returned then
     *                               depth-first search terminates early and the function as a whole returns the obtained value.
     *                               This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of edges.
     * @param shouldContinuePerState is invoked for every pair of visited states. If non-null value is returned then
     *                               depth-first search terminates early and the function as a whole returns the
     *                               obtained value. This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of states.
     */
    public default <Y> Y collectProduct(RangedGraph<V, In, E, P> lhs,
                                        RangedGraph<V, In, E, P> rhs,
                                        int startpointLhs,
                                        int startpointRhs,
                                        Set<Pair<Integer, Integer>> collected,
                                        QuadFunction<Integer, EN<Integer, E>, Integer, EN<Integer, E>, Y> shouldContinuePerEdge,
                                        BiFunction<Integer, Integer, Y> shouldContinuePerState) {

        if (collected.add(Pair.of(startpointLhs, startpointRhs))) {
            final Y out = shouldContinuePerState.apply(startpointLhs, startpointRhs);
            if (out != null) return out;
            final ArrayList<Range<In, RangedGraph.Trans<E>>> lhsEdges = lhs.graph.get(startpointLhs);
            final ArrayList<Range<In, RangedGraph.Trans<E>>> rhsEdges = rhs.graph.get(startpointRhs);
            int lhsIdx = 0;
            int rhsIdx = 0;
            while (lhsIdx < lhsEdges.size() && rhsIdx < rhsEdges.size()) {
                final Range<In, RangedGraph.Trans<E>> lhsRange = lhsEdges.get(lhsIdx);
                final Range<In, RangedGraph.Trans<E>> rhsRange = rhsEdges.get(rhsIdx);
                final int c = compare(lhsRange.input, rhsRange.input);
                if (c < 0) {
                    for (RangedGraph.Trans<E> prevRhs : rhsRange.betweenThisAndPreviousInput) {
                        for (RangedGraph.Trans<E> prevLhs : lhsRange.betweenThisAndPreviousInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                        for (RangedGraph.Trans<E> prevLhs : lhsRange.atThisInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                    }
                    lhsIdx++;
                } else if (c > 0) {
                    for (RangedGraph.Trans<E> prevLhs : lhsRange.betweenThisAndPreviousInput) {
                        for (RangedGraph.Trans<E> prevRhs : rhsRange.betweenThisAndPreviousInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                        for (RangedGraph.Trans<E> prevRhs : rhsRange.atThisInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                    }
                    rhsIdx++;
                } else {
                    for (RangedGraph.Trans<E> prevRhs : rhsRange.betweenThisAndPreviousInput) {
                        for (RangedGraph.Trans<E> prevLhs : lhsRange.betweenThisAndPreviousInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                    }
                    for (RangedGraph.Trans<E> prevRhs : rhsRange.atThisInput) {
                        for (RangedGraph.Trans<E> prevLhs : lhsRange.atThisInput) {
                            final Y out2 = shouldContinuePerEdge.apply(startpointLhs, prevLhs, startpointRhs, prevRhs);
                            if (out2 != null) return out2;
                            final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, prevRhs.targetState,
                                    collected, shouldContinuePerEdge, shouldContinuePerState);
                            if (out3 != null) return out3;
                        }
                    }
                    lhsIdx++;
                    rhsIdx++;
                }
            }
            while (lhsIdx < lhsEdges.size()) {
                final Range<In, RangedGraph.Trans<E>> lhsRange = lhsEdges.get(lhsIdx);
                for (RangedGraph.Trans<E> prevLhs : lhsRange.betweenThisAndPreviousInput) {
                    final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, rhs.sinkState,
                            collected, shouldContinuePerEdge, shouldContinuePerState);
                    if (out3 != null) return out3;
                }
                for (RangedGraph.Trans<E> prevLhs : lhsRange.atThisInput) {
                    final Y out3 = collectProduct(lhs, rhs, prevLhs.targetState, rhs.sinkState,
                            collected, shouldContinuePerEdge, shouldContinuePerState);
                    if (out3 != null) return out3;
                }
                lhsIdx++;
            }
            while (rhsIdx < rhsEdges.size()) {
                final Range<In, RangedGraph.Trans<E>> rhsRange = rhsEdges.get(rhsIdx);
                for (RangedGraph.Trans<E> prevRhs : rhsRange.betweenThisAndPreviousInput) {
                    final Y out3 = collectProduct(lhs, rhs, lhs.sinkState, prevRhs.targetState,
                            collected, shouldContinuePerEdge, shouldContinuePerState);
                    if (out3 != null) return out3;
                }
                for (RangedGraph.Trans<E> prevRhs : rhsRange.atThisInput) {
                    final Y out3 = collectProduct(lhs, rhs, lhs.sinkState, prevRhs.targetState,
                            collected, shouldContinuePerEdge, shouldContinuePerState);
                    if (out3 != null) return out3;
                }
                rhsIdx++;
            }
        }
        return null;
    }


    /**
     * Checks if language of left automaton is a subset of the language of right automaton.
     * The check is performed by computing product of automata and searching for a reachable pair
     * of states such that the left state accepts while right state doesn't. If such a pair is found then
     * the left automaton cannot be subset of right one. It's a very efficient (quadratic) algorithm which
     * works only when the right automaton is deterministic (use {@link RangedGraph#isDeterministic} to test it).
     * The left automaton may or may not be deterministic. Note that if both automata are nondeterministic then the problem
     * becomes PSPACE-complete and cannot be answered by performing such simple product of automata.
     * <br/>
     * <b>IMPORTANT:</b> note that this algorithm completely ignores weights and outputs. It does not check whether
     * regular relation generated by the one automaton is a subrelation of the other. Moreover, the input set <tt>In</tt>
     * is assumed to form a free monoid under concatenation.
     *
     * @param startpointLhs the state that marks beginning of graph. You may want to use
     *                      {@link IntermediateGraph#makeUniqueInitialState} to generate one unique initial state
     * @param startpointRhs similar to startpointLhs but for the right-hand-side graph
     * @return a reachable pair of states that could be a counter-example to the hypothesis of left automaton
     * being subset of the right one. If no such pair is found then null is returned and the left automaton is
     * a subset of the right one.
     */
    public default Pair<Integer, Integer> isSubset(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                                                   int startpointLhs, int startpointRhs, Set<Pair<Integer, Integer>> collected) {
        return collectProduct(lhs, rhs, startpointLhs, startpointRhs, collected, (a, a2, b, b2) -> null, (a, b) ->
                lhs.isAccepting(a) && !rhs.isAccepting(b) ? Pair.of(a, b) : null
        );
    }


    /**
     * Implements delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null.
     */
    public default N deterministicDelta(G graph, N startpoint, In input) {
        for (EN<N, E> init : (Iterable<EN<N, E>>) () -> graph.iterator(startpoint)) {
            if (contains(init.getEdge(), input)) {
                return init.getVertex();
            }
        }
        return null;
    }

    /**
     * Similar to {@link Specification#deterministicDelta(IntermediateGraph, Object)} but starts in
     * initial state and traverses initial edges
     */
    public default N deterministicDelta(G graph, In input) {
        for (EN<N, E> init : (Iterable<EN<N, E>>) () -> graph.iterateInitialEdges()) {
            if (contains(init.getEdge(), input)) {
                return init.getVertex();
            }
        }
        return null;
    }

    public default N deterministicTransitiveDelta(G graph, N startpoint, Iterable<In> input) {
        return deterministicTransitiveDelta(graph, startpoint, input.iterator());
    }

    /**
     * Implements recursive delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null. Starts in initial state and
     * requires the iterator to contain at least one element. Always check {@link Iterator#hasNext()}
     * before calling this method.
     */
    public default N deterministicTransitiveDelta(G graph, Iterator<In> input) {
        return deterministicTransitiveDelta(graph, deterministicDelta(graph, input.next()), input);
    }

    /**
     * Implements recursive delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null.
     */
    public default N deterministicTransitiveDelta(G graph, N startpoint, Iterator<In> input) {
        while (input.hasNext() && startpoint != null)
            startpoint = deterministicDelta(graph, startpoint, input.next());
        return startpoint;
    }

    /**
     * Similar to {@link Specification#collectProduct} but traverses outputs of left automaton edges,
     * rather than its input labels. It also assumes that right automaton is deterministic.
     *
     * @param visited        this is different from <tt>collected</tt> parameter of {@link Specification#collectProduct}.
     *                       Here the visited and collected states are two different things, due to the fact that each
     *                       accepting state may have some subsequential output, that needs to be printed before accepting.
     *                       The actual product of automata states is collected in <tt>shouldContinuePerState</tt> callback.
     * @param rhs            right automaton must be deterministic
     * @param outputAsString the outputs of left automaton must be strings of input symbols.
     */
    public default <Y> Y collectOutputProductDeterministic(
            RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
            int startpointLhs, int startpointRhs,
            Set<Pair<Integer, Integer>> visited,
            Function<Out, Seq<In>> outputAsString,
            Function<P, Seq<In>> finalStateOutputAsString,
            BiFunction<Integer, Integer, Y> shouldContinuePerState) {

        if (visited.add(Pair.of(startpointLhs, startpointRhs))) {
            final P partialFinalEdge = lhs.accepting.get(startpointLhs);
            if (lhs.accepting.get(startpointLhs) != null) {
                final Seq<In> lhsStateOutput = finalStateOutputAsString.apply(partialFinalEdge);
                int rhsTargetState = deltaBinarySearchTransitiveDeterministic(rhs, startpointRhs, lhsStateOutput.iterator());
                final Y y = shouldContinuePerState.apply(startpointLhs, rhsTargetState);
                if (y != null) return y;
            }

            for (Range<In, RangedGraph.Trans<E>> entryLhs : lhs.graph.get(startpointLhs)) {
                for (RangedGraph.Trans<E> tran : entryLhs.atThisInput) {
                    final Seq<In> lhsOutput = outputAsString.apply(output(tran.getEdge()));
                    HashSet<Integer> rhsTargetStates = composedMirroredOutputDelta(rhs, startpointRhs, tran, lhsOutput);
                    for (int targetStateRhs : rhsTargetStates) {
                        final Y y2 = collectOutputProductDeterministic(lhs, rhs, tran.targetState, targetStateRhs, visited,
                                outputAsString, finalStateOutputAsString, shouldContinuePerState);
                        if (y2 != null) return y2;
                    }
                }
            }
        }
        return null;
    }

    default HashSet<Integer> composedMirroredOutputDelta(RangedGraph<V, In, E, P> rhs, int startpointRhs,
                                                         RangedGraph.Trans<E> tran, Seq<In> lhsOutput) {
        HashSet<Integer> rhsStartpointStates = new HashSet<>();
        HashSet<Integer> rhsTargetStates = new HashSet<>();
        rhsTargetStates.add(startpointRhs);
        for (In in : lhsOutput) {
            final HashSet<Integer> tmp = rhsStartpointStates;
            rhsStartpointStates = rhsTargetStates;
            rhsTargetStates = tmp;
            rhsTargetStates.clear();
            if (Objects.equals(in, hashtag())) {
                deltaRangedNary(rhs, rhsStartpointStates, from(tran.edge), to(tran.edge), rhsTargetStates);
            } else {
                deltaBinarySearchDeterministicNary(rhs, rhsStartpointStates, in, rhsTargetStates);
            }
        }
        return rhsTargetStates;
    }


    /**
     * Works similarly to {@link Specification#isSubset} but checks if the output language of
     * left automaton is a subset of input language of right automaton.
     *
     * @param rhs            right automaton must be deterministic
     * @param outputAsString the output of left automaton must be in the form of strings on input symbols
     */
    public default Pair<Integer, Integer> isOutputSubset(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                                                         Set<Pair<Integer, Integer>> collected,
                                                         Function<Out, Seq<In>> outputAsString,
                                                         Function<P, Seq<In>> finalStateOutputAsString) {
        return collectOutputProductDeterministic(lhs, rhs, lhs.initial, rhs.initial, collected, outputAsString,
                finalStateOutputAsString, (a, b) -> lhs.isAccepting(a) && !rhs.isAccepting(b) ? Pair.of(a, b) : null);
    }


}
