package net.alagris;

import com.google.common.collect.Streams;
import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

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
     * Group multiplication in set of partial edges (the multiplication itself is not partial!).
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
     * Group of partial edges acting on set of full edges. It essentially prepends partial edge to the full edge.
     */
    void leftActionInPlace(P edge, E edge2);

    /**
     * Group of partial edges acting on set of full edges. It essentially appends partial edge to the full edge.
     */
    E rightAction(E edge, P edge2);


    /**
     * Group of partial edges acting on set of full edges. It essentially appends partial edge to the full edge.
     */
    void rightActionInPlace(E edge, P edge2);

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
    V metaInfoGenerator(TerminalNode parseNode);

    V metaInfoGenerator(ParserRuleContext parseNode);

    V metaInfoNone();

    /**
     * it takes terminal node associated with particular string literal that will be
     * used to build Product node in AST.
     */
    Out parseStr(IntSeq ints) throws CompilationError;

    /**
     * Parses weights. In the source code weights are denoted with individual
     * integers. You may parse them to something else than numbers if you want.
     */
    W parseW(int integer) throws CompilationError;

    /**
     * Parses ranges. In the source code ranges are denoted with pairs of unicode
     * codepoints. You may parse them to something else if you want.
     */
    Pair<In, In> parseRange(int codepointFrom, int codepointTo);

    /**
     * The largest range of values associated with the . dot
     */
    Pair<In, In> dot();

    /**
     * The special value associated with the # symbol on output side. This symbol will be used
     * to reflect input during automaton evaluation
     */
    In reflect();


    P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

    P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;


    //////////////////////////
    // Below are default functions added for convenience
    /////////////////////////

    default P partialNeutralEdge() {
        return createPartialEdge(outputNeutralElement(), weightNeutralElement());
    }

    default P partialOutputEdge(Out out) {
        return createPartialEdge(out, weightNeutralElement());
    }

    default P partialWeightedEdge(W w) {
        return createPartialEdge(outputNeutralElement(), w);
    }

    default E fullNeutralEdge(In from, In to) {
        return createFullEdge(from, to, partialNeutralEdge());
    }

    /**
     * Singleton graph (single vertex with one incoming and one outgoing transition)
     */
    default G singletonGraph(E initialEdge, N singletonState, P finalEdge) {
        G empty = createEmptyGraph();
        empty.addInitialEdge(singletonState, initialEdge);
        empty.setFinalEdge(singletonState, finalEdge);
        return empty;
    }

    /**
     * Singleton graph built from a single state that accepts a specified range of inputs
     */
    default G atomicRangeGraph(In from, V state, In to) {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        E e = fullNeutralEdge(from, to);
        N n = empty.create(state);
        empty.addInitialEdge(n, e);
        empty.setFinalEdge(n, p);
        return empty;
    }

    /**
     * Empty graph with a single epsilon edge
     */
    default G atomicEpsilonGraph() {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        empty.setEpsilon(p);
        return empty;
    }

    /**
     * This method is largely based on {@link SinglyLinkedGraph#deepClone}
     */
    default G deepClone(G original) {
        G clone = createEmptyGraph();
        HashMap<N, N> clonedVertices = new HashMap<>();
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) original::iterateInitialEdges) {
            N clonedVertex = SinglyLinkedGraph.deepClone(original, init.getValue(), clonedVertices);
            clone.addInitialEdge(clonedVertex, init.getKey());
        }
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) original::iterateFinalEdges) {
            N clonedVertex = SinglyLinkedGraph.deepClone(original, fin.getKey(), clonedVertices);
            clone.setFinalEdge(clonedVertex, fin.getValue());
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
    default G union(G lhs, G rhs, BiFunction<P, P, P> mergeEpsilons) {
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) rhs::iterateInitialEdges) {
            lhs.addInitialEdge(init.getValue(), init.getKey());
        }
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) rhs::iterateFinalEdges) {
            lhs.setFinalEdge(fin.getKey(), fin.getValue());
        }
        P lhsEps = lhs.getEpsilon();
        P rhsEps = rhs.getEpsilon();
        lhs.setEpsilon(lhsEps == null ? rhsEps : (rhsEps == null ? lhsEps : mergeEpsilons.apply(lhsEps, rhsEps)));
        return lhs;
    }

    default boolean isEmpty(G g) {
        return g.getEpsilon() == null && (g.allInitialEdges().isEmpty() || g.allFinalEdges().isEmpty());
    }

    /**
     * The two graphs should have no vertices in common. After running this method
     * the left graph will contain concatenated contents of both graphs and the right
     * graph should not be reused!
     * You have to dispose of the right graph because mutations to it might
     * accidentally change contents of left graph.
     */
    default G concat(G lhs, G rhs) {
        if (isEmpty(lhs)) return lhs;
        if (isEmpty(rhs)) return rhs;
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) () -> lhs.iterateFinalEdges()) {
            for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> rhs.iterateInitialEdges()) {
                lhs.add(fin.getKey(), leftAction(fin.getValue(), init.getKey()), init.getValue());
            }
        }
        P lhsEps = lhs.getEpsilon();
        if (lhsEps != null) {
            for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> rhs.iterateInitialEdges()) {
                lhs.addInitialEdge(init.getValue(), leftAction(lhsEps, init.getKey()));
            }
        }
        P rhsEps = rhs.getEpsilon();
        if (rhsEps != null) {
            for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) () -> lhs.iterateFinalEdges()) {
                rhs.setFinalEdge(fin.getKey(), multiplyPartialEdges(fin.getValue(), rhsEps));
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
     * Perform operation of Kleene closure (0 or more repetition) on graph.
     */
    default G kleene(G graph, Function<P, P> kleeneEpsilon) {
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) () -> graph.iterateFinalEdges()) {
            for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> graph.iterateInitialEdges()) {
                graph.add(fin.getKey(), leftAction(fin.getValue(), init.getKey()), init.getValue());
            }
        }
        kleeneOptional(graph, kleeneEpsilon);
        return graph;
    }

    /**
     * Perform operation of optional Kleene closure  (0 or 1 repetition) on graph.
     */
    default G kleeneOptional(G graph, Function<P, P> kleeneEpsilon) {
        P eps = graph.getEpsilon();
        if (eps == null) {
            graph.setEpsilon(partialNeutralEdge());
        } else {
            graph.setEpsilon(kleeneEpsilon.apply(eps));
        }
        return graph;
    }

    /**
     * Perform operation of semigroup Kleene closure (1 or more repetition) on graph.
     */
    default G kleeneSemigroup(G graph, Function<P, P> kleeneEpsilon) {
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) () -> graph.iterateFinalEdges()) {
            for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> graph.iterateInitialEdges()) {
                graph.add(fin.getKey(), leftAction(fin.getValue(), init.getKey()), init.getValue());
            }
        }
        P eps = graph.getEpsilon();
        if (eps != null) {
            graph.setEpsilon(kleeneEpsilon.apply(eps));
        }
        return graph;
    }

    /**
     * Performs left action on all initial edges and epsilon.
     */
    default G leftActionOnGraph(P edge, G graph) {
        graph.mutateAllInitialEdges((e, vertex) -> leftAction(edge, e));
        P eps = graph.getEpsilon();
        if (eps != null) graph.setEpsilon(multiplyPartialEdges(edge, eps));
        return graph;
    }

    /**
     * Performs right action on all final edges and epsilon.
     */
    default G rightActionOnGraph(G graph, P edge) {
        graph.replaceAllFinalEdges((e, vertex) -> multiplyPartialEdges(e, edge));
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
    default <S extends Set<N>> S collect(G graph, N startpoint, S set, Predicate<N> shouldContinue) {
        return SinglyLinkedGraph.collect(graph, startpoint, set, shouldContinue);
    }

    default HashSet<N> collect(G graph, N startpoint) {
        return SinglyLinkedGraph.collect(graph, startpoint, new HashSet<>(), x -> true);
    }


    interface Range<In, M> {
        public In input();

        public List<M> atThisInput();

        public List<M> betweenThisAndPreviousInput();
    }

    class RangeImpl<In, M> implements Range<In, M> {
        public final In input;
        public List<M> atThisInput;
        public List<M> betweenThisAndPreviousInput;

        private RangeImpl(Range<In, M> other, boolean isSuccessor, List<M> sinkTransition) {
            this.betweenThisAndPreviousInput = isSuccessor ? new ArrayList<M>(0) : copyTransitions(other.betweenThisAndPreviousInput(), sinkTransition);
            this.atThisInput = copyTransitions(other.atThisInput(), sinkTransition);
            this.input = other.input();
        }

        private List<M> copyTransitions(List<M> other, List<M> sinkTransition) {
            final int size = other.size();
            if (size == 0) return new ArrayList<M>(0);
            final ArrayList<M> arr = new ArrayList<M>(size);
            for (M transition : other) {
                if (transition != sinkTransition.get(0)) arr.add(transition);
            }
            if (arr.isEmpty()) {
                return sinkTransition;
            }
            return arr;
        }

        public RangeImpl(In input, List<M> atThisInput, List<M> betweenThisAndPreviousInput) {
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

        @Override
        public In input() {
            return input;
        }

        @Override
        public List<M> atThisInput() {
            return atThisInput;
        }

        @Override
        public List<M> betweenThisAndPreviousInput() {
            return betweenThisAndPreviousInput;
        }
    }

    static <X> void removeTail(List<X> list, int desiredLength) {
        while (list.size() > desiredLength) {
            list.remove(list.size() - 1);
        }
    }
    static <X> ArrayList<X> filledArrayList(int size, X defaultElement) {
        return filledArrayListFunc(size,i->defaultElement);
    }
    static <X> ArrayList<X> filledArrayListFunc(int size, Function<Integer,X> defaultElement) {
        ArrayList<X> arr = new ArrayList<>(size);
        for (int i=0;i<size;i++) arr.add(defaultElement.apply(i));
        return arr;
    }

    static <X> ArrayList<X> concatArrayLists(List<X> a, List<X> b) {
        ArrayList<X> arr = new ArrayList<>(a.size() + b.size());
        arr.addAll(a);
        arr.addAll(b);
        return arr;
    }

    /**
     * returns new size of list
     */
    static <X> int shiftDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
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
            return i + 1;
        }
        return sortedArray.size();
    }

    static <X> void removeDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        removeTail(sortedArray, shiftDuplicates(sortedArray, areEqual, mergeEqual));

    }

    default <M> ArrayList<Range<In, M>> optimise(G graph, N vertex,
                                                 List<M> sinkTransition,
                                                 Function<Map.Entry<E, N>, M> map) {
        return mergeRangedEdges(graph.iterator(vertex), r -> from(r.getKey()), r -> to(r.getKey()),
                sinkTransition, map);
    }

    default <M, R> ArrayList<Range<In, M>> mergeRangedEdges(Iterator<R> edges,
                                                            Function<R, In> from,
                                                            Function<R, In> to,
                                                            List<M> sinkTransition,
                                                            Function<R, M> map) {
        class IBE {
            /**input symbol*/
            final In i;
            /**edges beginning at input symbol*/
            final ArrayList<R> b = new ArrayList<>();
            /**edges ending at input symbol*/
            final ArrayList<R> e = new ArrayList<>();

            IBE(R b, R e) {
                this.i = b == null ? to.apply(e) : from.apply(b);
                if (b != null) this.b.add(b);
                if (e != null) this.e.add(e);
            }

            int compare(IBE other) {
                return Specification.this.compare(i, other.i);
            }
        }
        final ArrayList<IBE> points = new ArrayList<>();
        edges.forEachRemaining(edge -> {
            points.add(new IBE(edge, null));
            points.add(new IBE(null, edge));
        });
        points.sort(IBE::compare);
        removeDuplicates(points, (prev, curr) -> prev.i.equals(curr.i), (prev, curr) -> {
            prev.b.addAll(curr.b);
            prev.e.addAll(curr.e);
        });

        final ArrayList<Range<In, M>> transitions = new ArrayList<>(points.size());
        final ArrayList<R> accumulated = new ArrayList<>();
        In prev = null;
        for (IBE ibe : points) {
            final List<M> betweenThisAndPreviousInput;
            assert !Objects.equals(ibe.i, prev):ibe+" "+prev;
            if (accumulated.size() == 0 && (prev==null || !isSuccessor(prev, ibe.i))) {
                betweenThisAndPreviousInput = sinkTransition;
            } else {
                betweenThisAndPreviousInput = new ArrayList<>(accumulated.size());
                for (final R acc : accumulated) betweenThisAndPreviousInput.add(map.apply(acc));
            }
            accumulated.addAll(ibe.b);
            final List<M> atThisInput;
            if (accumulated.size() == 0 && (prev==null || !isSuccessor(prev, ibe.i))) {
                atThisInput = sinkTransition;
            } else {
                atThisInput = new ArrayList<>(accumulated.size());
                for (final R acc : accumulated) atThisInput.add(map.apply(acc));
            }
            transitions.add(new RangeImpl<>(ibe.i, atThisInput, betweenThisAndPreviousInput));
            accumulated.removeAll(ibe.e);
            prev = ibe.i;
        }
        assert accumulated.isEmpty() : "Unexpected leftovers! " + accumulated.toString();
        return transitions;
    }

    public class RangedGraph<V, In, E, P> {


        public boolean isAccepting(int state) {
            return accepting.get(state) != null;
        }

        public V state(int stateIdx) {
            return indexToState.get(stateIdx);
        }

        public P getFinalEdge(int state) {
            return accepting.get(state);
        }

        public static class Trans<E> implements Map.Entry<E, Integer> {
            final E edge;
            int targetState;

            Trans(E edge, int targetState) {
                this.edge = edge;
                this.targetState = targetState;
            }

            @Override
            public E getKey() {
                return edge;
            }

            @Override
            public Integer getValue() {
                return targetState;
            }

            @Override
            public Integer setValue(Integer targetState) {
                int prev = this.targetState;
                this.targetState = targetState;
                return prev;
            }

            @Override
            public String toString() {
                return edge + "->" + targetState;
            }
        }

        public ArrayList<ArrayList<Range<In, Trans<E>>>> graph;
        public ArrayList<P> accepting;
        public ArrayList<V> indexToState;
        public int initial;
        public int sinkState;
        /**
         * Normally all transitions should be distinct instances of arrays, so that mutations in one of them doesn't
         * accidentally affect other places. However, due to how frequent and special the transition to sink state it,
         * it's the only array whose instance is shared among all edges. When designing algorithms that mutate transitions,
         * make sure to check object equality with == to make sure it's not the sink transition. It needs to be treated
         * separately.
         */
        private List<Trans<E>> sinkTrans;

        RangedGraph(ArrayList<ArrayList<Range<In, Trans<E>>>> graph, ArrayList<P> accepting,
                    ArrayList<V> indexToState, int initial, int sinkState, List<RangedGraph.Trans<E>> sinkTrans) {
            assert graph.size()==accepting.size();
            assert graph.size()==indexToState.size();
            assert initial<graph.size();
            assert sinkState<graph.size();
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
                            .append(r.input())
                            .append(" ")
                            .append(r.betweenThisAndPreviousInput())
                            .append(r.atThisInput())
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
                    if (trans.atThisInput().size() > 1)
                        return trans.atThisInput();
                    if (trans.betweenThisAndPreviousInput().size() > 1)
                        return trans.betweenThisAndPreviousInput();
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
                return transitions.get(index / 2).betweenThisAndPreviousInput();
            } else {
                return transitions.get(index / 2).atThisInput();
            }
        }

		public int size() {
			return graph.size();
		}


    }

    /**
     * Returns
     */
    default boolean accepts(RangedGraph<V, In, E, P> g, Iterator<In> input) {
        return g.isAccepting(deltaBinarySearchTransitiveDeterministic(g, g.initial, input));
    }

    static <X> boolean isStrictlyIncreasing(int[] arr) {
        return isStrictlyIncreasing(Arrays.stream(arr).boxed().collect(Collectors.toList()), Integer::compare);
    }

    static <X> boolean isStrictlyIncreasing(List<X> list, Comparator<X> comp) {
        for (int i = 1; i < list.size(); i++) {
            if (comp.compare(list.get(i - 1), list.get(i)) > 0) return false;
        }
        return true;
    }

    /**
     * Takes arbitrary automaton (could be transducer with weights etc) and returns the deterministic
     * underlying acceptor (DFA). The transducer outputs and weights are stripped and ignored.
     * Notice that in general case it's impossible to build deterministic transducer equivalent to
     * nondeterministic one. Hence it's necessary to ignore the outputs in order to perform powerset
     * construction. This operation does not modify original automaton. A completely new
     * automaton is built and returned at each call.
     */
    default RangedGraph<V, In, E, P> powerset(RangedGraph<V, In, E, P> g,
                                              Function<In, In> successor,
                                              Function<In, In> predecessor) {
        class PS {//powerset state
            final int[] states;
            final int hash;

            public PS(int... states) {
                this.states = states;
                hash = Arrays.hashCode(states);
                assert isStrictlyIncreasing(states) : Arrays.toString(states);
            }

            public PS(List<RangedGraph.Trans<E>> transitions) {
                transitions.sort(Comparator.comparingInt(a -> a.targetState));
                states = new int[shiftDuplicates(transitions, (a, b) -> a.targetState == b.targetState, (a, b) -> {
                })];
                for (int i = 0; i < states.length; i++) {
                    states[i] = transitions.get(i).targetState;
                }
                hash = Arrays.hashCode(states);
                assert isStrictlyIncreasing(states) : Arrays.toString(states);
            }

            @Override
            public boolean equals(Object o) {
                return Arrays.equals(states, ((PS) o).states);
            }


            @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public String toString() {
                return Arrays.toString(states);
            }
        }
        class IdxAndTrans {
            final int index;
            ArrayList<Range<In, RangedGraph.Trans<E>>> dfaTrans;

            IdxAndTrans(int index) {
                this.index = index;
            }

            IdxAndTrans(int index, ArrayList<Range<In, RangedGraph.Trans<E>>> dfaTrans) {
                this.index = index;
                this.dfaTrans = dfaTrans;
            }

            @Override
            public String toString() {
                return "IdxAndTrans{" +
                        "index=" + index +
                        ", dfaTrans=" + dfaTrans +
                        '}';
            }
        }
        final Stack<PS> toVisit = new Stack<>();
        final HashMap<PS, IdxAndTrans> powersetStateToIndex = new HashMap<>();
        final PS initPS = new PS(g.initial);
        toVisit.push(initPS);
        powersetStateToIndex.put(initPS, new IdxAndTrans(0));
        final PS sinkPS = new PS(g.sinkState);
        final int SINK_STATE = 1;
        final List<RangedGraph.Trans<E>> sinkTrans = Collections.singletonList(new RangedGraph.Trans<>(null, SINK_STATE));
        final ArrayList<Range<In, RangedGraph.Trans<E>>> sinkTransitions = singeltonArrayList(new RangeImpl<>(maximal(), sinkTrans, sinkTrans));
        powersetStateToIndex.put(sinkPS, new IdxAndTrans(SINK_STATE, sinkTransitions));
        assert powersetStateToIndex.size() == SINK_STATE + 1;
        assert powersetStateToIndex.get(sinkPS).index == SINK_STATE;
        while (!toVisit.isEmpty()) {
            final PS powersetState = toVisit.pop();
            final IdxAndTrans source = powersetStateToIndex.get(powersetState);
            if (source.dfaTrans != null) continue;//already visitxed
            final ArrayList<Range<In, RangedGraph.Trans<E>>> powersetTrans =
                    powersetTransitions(g,powersetState.states);

            source.dfaTrans = new ArrayList<>(powersetTrans.size());
            In prev = null;
            for (Range<In, RangedGraph.Trans<E>> range : powersetTrans) {
                final PS targetAtThisInput = new PS(range.atThisInput());
                assert targetAtThisInput.states.length > 0;//Because of the sink state, there should always
                //be at least one state in each reachable powerset state
                final PS targetBetweenThisAndPreviousInput = new PS(range.betweenThisAndPreviousInput());
                assert targetBetweenThisAndPreviousInput.states.length > 0 ^ range.betweenThisAndPreviousInput().isEmpty() : g.toString();
                assert range.betweenThisAndPreviousInput().isEmpty() == (prev!=null && isSuccessor(prev, range.input())) : prev + " " + range + "\n" + g;
                final IdxAndTrans targetAtThisInputIndex = powersetStateToIndex.computeIfAbsent(targetAtThisInput, k -> new IdxAndTrans(powersetStateToIndex.size()));
                if (targetAtThisInputIndex.dfaTrans == null)
                    toVisit.add(targetAtThisInput);
                final IdxAndTrans targetBetweenThisAndPreviousInputIndex;
                if (targetBetweenThisAndPreviousInput.states.length == 0) {
                    targetBetweenThisAndPreviousInputIndex = null;
                } else {
                    targetBetweenThisAndPreviousInputIndex = powersetStateToIndex.computeIfAbsent(targetBetweenThisAndPreviousInput, k -> new IdxAndTrans(powersetStateToIndex.size()));
                    if (targetBetweenThisAndPreviousInputIndex.dfaTrans == null)
                        toVisit.add(targetBetweenThisAndPreviousInput);
                }
                final E edgeAtThisInput = fullNeutralEdge(range.input(), range.input());
                final E edgeBetweenThisAndPreviousInput = fullNeutralEdge(prev==null?minimal():successor.apply(prev), predecessor.apply(range.input()));
                assert (prev !=null && isSuccessor(prev, range.input())) == range.betweenThisAndPreviousInput().isEmpty() : "powerState=" + powersetState +
                        "\nrange=" + range +
                        "\nedge=" + edgeBetweenThisAndPreviousInput +
                        "\n" + g;
                final List<RangedGraph.Trans<E>> transAtThisInput = targetAtThisInputIndex.index == SINK_STATE ? sinkTrans :
                        singeltonArrayList(new RangedGraph.Trans<>(edgeAtThisInput, targetAtThisInputIndex.index));
                final List<RangedGraph.Trans<E>> transBetweenThisAndPreviousInput =
                        targetBetweenThisAndPreviousInputIndex == null ? filledArrayList(0, null)
                                : (targetBetweenThisAndPreviousInputIndex.index == SINK_STATE ?
                                sinkTrans
                                : singeltonArrayList(new RangedGraph.Trans<>(edgeBetweenThisAndPreviousInput, targetBetweenThisAndPreviousInputIndex.index)));
                assert (prev!=null&&isSuccessor(prev, range.input()))==
                        transBetweenThisAndPreviousInput.isEmpty() : transBetweenThisAndPreviousInput + " " + edgeBetweenThisAndPreviousInput + "\n" + g;
                source.dfaTrans.add(new RangeImpl<>(
                        range.input(),
                        transAtThisInput,
                        transBetweenThisAndPreviousInput));
                prev = range.input();
            }
        }

        final ArrayList<ArrayList<Range<In, RangedGraph.Trans<E>>>> graph = filledArrayList(powersetStateToIndex.size(), null);
        final ArrayList<P> accepting = filledArrayList(powersetStateToIndex.size(), null);
        for (Map.Entry<PS, IdxAndTrans> state : powersetStateToIndex.entrySet()) {
            assert isStrictlyIncreasing(state.getValue().dfaTrans, (a, b) -> compare(a.input(), b.input())) : state;
            assert graph.get(state.getValue().index) == null;
            graph.set(state.getValue().index, state.getValue().dfaTrans);
            for (int originalState : state.getKey().states) {
                if (g.isAccepting(originalState)) {
                    accepting.set(state.getValue().index, partialNeutralEdge());
                    break;
                }
            }
        }
        assert indexOf(graph, Objects::isNull) == -1 : g;
        return new RangedGraph<>(graph, accepting,
                filledArrayList(powersetStateToIndex.size(), null),
                0, SINK_STATE, sinkTrans);
    }

    public static <X> int indexOf(Iterable<X> list, Predicate<X> f) {
        int i = 0;
        for (X x : list)
            if (f.test(x)) return i;
            else i++;
        return -1;
    }

    default ArrayList<Range<In, RangedGraph.Trans<E>>> powersetTransitions(RangedGraph<V, In, E, P> g,int[] states) {
        assert isStrictlyIncreasing(states) : Arrays.toString(states);
        int stateIdx = 0;
        assert states.length > 0 : g.toString();
        NullTermIter<? extends Range<In, RangedGraph.Trans<E>>> transitions = fromIterable(g.graph.get(states[stateIdx]));
        for (stateIdx++; stateIdx < states.length; stateIdx++) {
            transitions = zipTransitionRanges(transitions, fromIterable(g.graph.get(states[stateIdx])), g.sinkTrans, g.sinkTrans);
        }
        final ArrayList<Range<In, RangedGraph.Trans<E>>> collected = new ArrayList<>();
        assert g.sinkTrans.size() == 1;
        Range<In, RangedGraph.Trans<E>> range = transitions.next();
        boolean isSuccessor = false;
        if(range!=null){
            while(true) {
                collected.add(new RangeImpl<>(range, isSuccessor, g.sinkTrans));
                Range<In, RangedGraph.Trans<E>> nextRange = transitions.next();
                if(nextRange==null)break;
                isSuccessor = isSuccessor(range.input(), nextRange.input());
                range = nextRange;
            }
        }
        return collected;
    }

    static <X> ArrayList<X> singeltonArrayList(X x) {
        ArrayList<X> a = new ArrayList<>(1);
        a.add(x);
        return a;
    }

    default RangedGraph<V, In, E, P> optimiseGraph(G graph) {
        return optimiseGraph(graph,metaInfoNone());
    }
    default RangedGraph<V, In, E, P> optimiseGraph(G graph, V sinkStateMeta) {
        final N initial = graph.makeUniqueInitialState(null);
        final HashSet<N> states = collect(graph, initial);
        final int statesNum = states.size() + 1;//extra one for sink state
        final ArrayList<ArrayList<Range<In, RangedGraph.Trans<E>>>> graphTransitions = filledArrayList(statesNum,null);
        final HashMap<N, Integer> stateToIndex = new HashMap<>(statesNum);
        final ArrayList<V> indexToState = new ArrayList<>(statesNum);
        final ArrayList<P> accepting = filledArrayList(statesNum,null);
        final int sinkState = states.size();
        for (N state : states) {
            int idx = indexToState.size();
            indexToState.add(graph.getState(state));
            stateToIndex.put(state, idx);
        }
        final List<RangedGraph.Trans<E>> sinkTrans = Collections.singletonList(
                new RangedGraph.Trans<>(null, sinkState));
        graphTransitions.set(sinkState,singeltonArrayList(new RangeImpl<>(maximal(), sinkTrans, sinkTrans)));//sink state transitions
        indexToState.add(sinkStateMeta);
        assert indexToState.get(sinkState)==sinkStateMeta;
        //sink state doesn't accept hence stateToIndex returns null for sink state
        for (final Map.Entry<N, Integer> state : stateToIndex.entrySet()) {
            final ArrayList<Range<In, RangedGraph.Trans<E>>> transitions = optimise(graph, state.getKey(), sinkTrans,
                    en -> new RangedGraph.Trans<>(en.getKey(), stateToIndex.get(en.getValue())));
            graphTransitions.set(state.getValue(), transitions);
            accepting.set(state.getValue(), graph.getFinalEdge(state.getKey()));
        }
        accepting.set(stateToIndex.get(initial), graph.getEpsilon());
        return new RangedGraph<>(graphTransitions, accepting, indexToState, stateToIndex.get(initial), sinkState, sinkTrans);
    }


    /**
     * @return the outgoing transition containing given input. Otherwise transition to sink state is returned.
     */
    default List<RangedGraph.Trans<E>> binarySearch(RangedGraph<V, In, E, P> graph, int state, In input) {
        ArrayList<Range<In, RangedGraph.Trans<E>>> transitions = graph.graph.get(state);
        return graph.getTrans(transitions, binarySearchIndex(transitions, input));
    }

    /**
     * @return index to a transition containing given input. This is a special index that must be interpreted using
     * {@link RangedGraph#getTrans}. Those indices are useful when you need to iterate over transitions. It's
     * used for implementing {@link Specification#deltaRanged}
     */
    default int binarySearchIndex(ArrayList<Range<In, RangedGraph.Trans<E>>> transitions, In input) {
        int low = 0;
        int high = transitions.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Range<In, RangedGraph.Trans<E>> midVal = transitions.get(mid);
            int c = compare(midVal.input(), input);
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
    default int deltaBinarySearchDeterministic(RangedGraph<V, In, E, P> graph, int state,
                                               In input) {
        return binarySearch(graph, state, input).get(0).targetState;
    }

    /**
     * This method assumes that graph is deterministic
     */
    default int deltaBinarySearchTransitiveDeterministic(RangedGraph<V, In, E, P> graph, int state,
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
    default void deltaRanged(RangedGraph<V, In, E, P> graph, int state, In inputFrom, In inputTo,
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
    default void deltaRangedNary(RangedGraph<V, In, E, P> graph, Collection<Integer> startpointStates,
                                 In inputFrom, In inputTo,
                                 Collection<Integer> reachedTargetStates) {
        for (int state : startpointStates) {
            deltaRanged(graph, state, inputFrom, inputTo, reachedTargetStates);
        }
    }


    /**
     * This is n-ary version of {@link Specification#deltaBinarySearchDeterministic} which takes as input entire list of start-point states.
     */
    default void deltaBinarySearchDeterministicNary(RangedGraph<V, In, E, P> graph, Collection<Integer> startpointStates,
                                                    In input,
                                                    Collection<Integer> reachedTargetStates) {
        for (int state : startpointStates) {
            reachedTargetStates.add(deltaBinarySearchDeterministic(graph, state, input));
        }
    }

    static interface QuadFunction<A, B, C, D, E> {
        E apply(A a, B b, C c, D d);

    }

    /**
     * Iterator that returns null when end is reached
     */
    interface NullTermIter<X> {
        X next();
    }

    static <X> NullTermIter<X> fromIterable(Iterable<X> iter) {
        return fromIter(iter.iterator());
    }

    static <X> NullTermIter<X> fromIter(Iterator<X> iter) {
        return () -> iter.hasNext() ? iter.next() : null;
    }

    static <X, Y> void collect(NullTermIter<X> iter, Function<X, Y> convert, List<Y> output) {
        X x;
        while ((x = iter.next()) != null) {
            output.add(convert.apply(x));
        }
    }

    class ZippedRange<In, M> implements Range<In, M> {
        public final In input;
        /**
         * This should not be mutated
         */
        public final List<M> atThisInputLhs;
        /**
         * This should not be mutated
         */
        public final List<M> betweenThisAndPreviousInputLhs;
        /**
         * This should not be mutated
         */
        public final List<M> atThisInputRhs;
        /**
         * This should not be mutated
         */
        public final List<M> betweenThisAndPreviousInputRhs;

        public ZippedRange(In input, List<M> atThisInputLhs, List<M> atThisInputRhs,
                           List<M> betweenThisAndPreviousInputLhs, List<M> betweenThisAndPreviousInputRhs) {
            this.input = input;
            this.atThisInputLhs = atThisInputLhs;
            this.betweenThisAndPreviousInputLhs = betweenThisAndPreviousInputLhs;
            this.atThisInputRhs = atThisInputRhs;
            this.betweenThisAndPreviousInputRhs = betweenThisAndPreviousInputRhs;
        }

        @Override
        public In input() {
            return input;
        }

        /**
         * Yields undefined behaviour if underlying structure is mutated. If you need to
         * edit the structure of transitions, then to it in a new separate copy
         */
        @Override
        public List<M> atThisInput() {
            return lazyConcatImmutableLists(atThisInputLhs, atThisInputRhs);
        }

        /**
         * Yields undefined behaviour if underlying structure is mutated. If you need to
         * edit the structure of transitions, then to it in a new separate copy
         */
        @Override
        public List<M> betweenThisAndPreviousInput() {
            return lazyConcatImmutableLists(betweenThisAndPreviousInputLhs, betweenThisAndPreviousInputRhs);
        }
    }

    /**
     * Yields undefined behaviour if underlying structure is mutated. If you need to
     * edit the structure of transitions, then to it in a new separate copy
     */
    static <X> List<X> lazyConcatImmutableLists(List<X> lhs, List<X> rhs) {
        return new AbstractList<X>() {
            /**this should not change*/
            final int offset = lhs.size();

            @Override
            public Iterator<X> iterator() {
                return new Iterator<X>() {
                    boolean finishedLhs = false;
                    Iterator<X> lhsIter = lhs.iterator();
                    Iterator<X> rhsIter = rhs.iterator();

                    @Override
                    public boolean hasNext() {
                        if (finishedLhs) return rhsIter.hasNext();
                        return lhsIter.hasNext() || rhsIter.hasNext();
                    }

                    @Override
                    public X next() {
                        if (finishedLhs) return rhsIter.next();
                        if (lhsIter.hasNext()) return lhsIter.next();
                        finishedLhs = true;
                        return rhsIter.next();
                    }
                };
            }

            @Override
            public X get(int index) {
                return index < offset ? lhs.get(index) : rhs.get(index - offset);
            }

            @Override
            public int size() {
                return offset + lhs.size();
            }
        };
    }

    default <M, Y> Y zipTransitions(
            NullTermIter<Range<In, M>> lhs,
            NullTermIter<Range<In, M>> rhs,
            BiFunction<M, M, Y> callback,
            List<M> lhsSinkTransition,
            List<M> rhsSinkTransition) {
        final NullTermIter<ZippedRange<In, M>> iter = zipTransitionRanges(lhs, rhs, lhsSinkTransition, rhsSinkTransition);
        ZippedRange<In, M> next;
        while ((next = iter.next()) != null) {
            for (M prevRhs : next.atThisInputRhs) {
                for (M prevLhs : next.atThisInputLhs) {
                    final Y y = callback.apply(prevLhs, prevRhs);
                    if (y != null) return y;
                }
            }
            for (M prevRhs : next.betweenThisAndPreviousInputRhs) {
                for (M prevLhs : next.betweenThisAndPreviousInputLhs) {
                    final Y y = callback.apply(prevLhs, prevRhs);
                    if (y != null) return y;
                }
            }
        }
        return null;
    }

    default <M> NullTermIter<ZippedRange<In, M>> zipTransitionRanges(
            NullTermIter<? extends Range<In, M>> lhs,
            NullTermIter<? extends Range<In, M>> rhs,
            List<M> lhsSinkTransition,
            List<M> rhsSinkTransition) {
        return new NullTermIter<ZippedRange<In, M>>() {
            Range<In, M> lhsRange = lhs.next();
            Range<In, M> rhsRange = rhs.next();

            @Override
            public ZippedRange<In, M> next() {
                final ZippedRange<In, M> out;
                if (lhsRange != null && rhsRange != null) {
                    final int c = compare(lhsRange.input(), rhsRange.input());
                    if (c < 0) {
                        out = new ZippedRange<>(lhsRange.input(),
                                lhsRange.atThisInput(), rhsRange.betweenThisAndPreviousInput(),
                                lhsRange.betweenThisAndPreviousInput(), rhsRange.betweenThisAndPreviousInput());
                        lhsRange = lhs.next();
                    } else if (c > 0) {
                        out = new ZippedRange<>(rhsRange.input(),
                                lhsRange.betweenThisAndPreviousInput(), rhsRange.atThisInput(),
                                lhsRange.betweenThisAndPreviousInput(), rhsRange.betweenThisAndPreviousInput());
                        rhsRange = rhs.next();
                    } else {
                        out = new ZippedRange<>(lhsRange.input(),
                                lhsRange.atThisInput(), rhsRange.atThisInput(),
                                lhsRange.betweenThisAndPreviousInput(), rhsRange.betweenThisAndPreviousInput());
                        lhsRange = lhs.next();
                        rhsRange = rhs.next();
                    }
                } else if (lhsRange != null) {
                    out = new ZippedRange<>(lhsRange.input(),
                            lhsRange.atThisInput(), rhsSinkTransition,
                            lhsRange.betweenThisAndPreviousInput(), rhsSinkTransition);
                    lhsRange = lhs.next();
                } else if (rhsRange != null) {
                    out = new ZippedRange<>(rhsRange.input(),
                            lhsSinkTransition, rhsRange.atThisInput(),
                            lhsSinkTransition, rhsRange.betweenThisAndPreviousInput());
                    rhsRange = rhs.next();
                } else {
                    out = null;
                }
                return out;
            }
        };
    }

    /**
     * Performs product of automata. Collects all reachable pairs of states.
     * A pair of transitions between pairs of states is taken only when their input ranges overlap with one another.
     *
     * @param shouldContinuePerEdge  is invoked for every pair of traversed transitions. If non-null value is returned then
     *                               breath-first search terminates early and the function as a whole returns the obtained value.
     *                               This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of edges.
     * @param shouldContinuePerState is invoked for every pair of visited states. If non-null value is returned then
     *                               breath-first search terminates early and the function as a whole returns the
     *                               obtained value. This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of states.
     */
    default <Y> Y collectProduct(RangedGraph<V, In, E, P> lhs,
                                 RangedGraph<V, In, E, P> rhs,
                                 int startpointLhs,
                                 int startpointRhs,
                                 Set<Pair<Integer, Integer>> collected,
                                 QuadFunction<Integer, Map.Entry<E, Integer>, Integer, Map.Entry<E, Integer>, Y> shouldContinuePerEdge,
                                 BiFunction<Integer, Integer, Y> shouldContinuePerState) {
        final Stack<Pair<Integer, Integer>> toVisit = new Stack<>();
        final Pair<Integer, Integer> startpoint = Pair.of(startpointLhs, startpointRhs);
        if (collected.add(startpoint)) toVisit.push(startpoint);

        while (!toVisit.isEmpty()) {
            final Pair<Integer, Integer> pair = toVisit.pop();
            final int stateLhs = pair.getFirst();
            final int stateRhs = pair.getSecond();
            final Y out = shouldContinuePerState.apply(stateLhs, stateRhs);
            if (out != null) return out;
            assert lhs.sinkTrans.size() == 1 : lhs.sinkTrans;
            assert rhs.sinkTrans.size() == 1 : rhs.sinkTrans;
            Y y = zipTransitions(fromIterable(lhs.graph.get(stateLhs)), fromIterable(rhs.graph.get(stateRhs)),
                    (prevLhs, prevRhs) -> {
                        final Y y2 = shouldContinuePerEdge.apply(stateLhs, prevLhs, stateRhs, prevRhs);
                        if (y2 != null) return y2;
                        final Pair<Integer, Integer> newPair = Pair.of(prevLhs.targetState, prevRhs.targetState);
                        if (collected.add(newPair)) toVisit.push(newPair);
                        return null;
                    }, lhs.sinkTrans, rhs.sinkTrans);
            if (y != null) return y;
        }
        return null;
    }


    /**
     * Checks if language of left automaton is a subset of the language of right automaton.
     * The check is performed by computing product of automata and searching for a reachable pair
     * of states such that the left state accepts while right state doesn't. If such a pair is found then
     * the left automaton cannot be subset of right one. It's a very efficient (quadratic) algorithm which
     * works only when the right automaton is deterministic (use {@link RangedGraph#isDeterministic} to test it
     * or use {@link Specification#powerset} to ensure it. Beware as powerset construction is an exponential algorithm).
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
    default Pair<Integer, Integer> isSubset(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                                            int startpointLhs, int startpointRhs, Set<Pair<Integer, Integer>> collected) {
        return collectProduct(lhs, rhs, startpointLhs, startpointRhs, collected, (a, a2, b, b2) -> null, (a, b) ->
                lhs.isAccepting(a) && !rhs.isAccepting(b) ? Pair.of(a, b) : null
        );
    }

    /**This function, unlike {@link Specification#isSubset} allows both arguments to be nondeterministic. However, it comes
     * at the cost of exponential time complexity (because powerset construction needs to be performed first)*/
    default Pair<V, V> isSubsetNondeterministic(RangedGraph<V, In, E, P> lhs,
                                                              RangedGraph<V, In, E, P> rhs,
                                                              Function<In, In> successor,
                                                              Function<In, In> predecessor){
        final RangedGraph<V, In, E, P> dfa = powerset(rhs,successor,predecessor);
        final Pair<Integer,Integer> counterexample = isSubset(lhs, dfa, lhs.initial, dfa.initial, new HashSet<>());
        return counterexample==null?null:Pair.of(lhs.state(counterexample.getFirst()),dfa.state(counterexample.getSecond()));
    }

    /**
     * Implements delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null.
     */
    default N deterministicDelta(G graph, N startpoint, In input) {
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> graph.iterator(startpoint)) {
            if (contains(init.getKey(), input)) {
                return init.getValue();
            }
        }
        return null;
    }

    /**
     * Similar to {@link Specification#deterministicDelta(IntermediateGraph, Object)} but starts in
     * initial state and traverses initial edges
     */
    default N deterministicDelta(G graph, In input) {
        for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) () -> graph.iterateInitialEdges()) {
            if (contains(init.getKey(), input)) {
                return init.getValue();
            }
        }
        return null;
    }

    default N deterministicTransitiveDelta(G graph, N startpoint, Iterable<In> input) {
        return deterministicTransitiveDelta(graph, startpoint, input.iterator());
    }

    /**
     * Implements recursive delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null. Starts in initial state and
     * requires the iterator to contain at least one element. Always check {@link Iterator#hasNext()}
     * before calling this method.
     */
    default N deterministicTransitiveDelta(G graph, Iterator<In> input) {
        return deterministicTransitiveDelta(graph, deterministicDelta(graph, input.next()), input);
    }

    /**
     * Implements recursive delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null.
     */
    default N deterministicTransitiveDelta(G graph, N startpoint, Iterator<In> input) {
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
    default <Y> Y collectOutputProductDeterministic(
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
                for (RangedGraph.Trans<E> tran : entryLhs.atThisInput()) {
                    final Seq<In> lhsOutput = outputAsString.apply(output(tran.getKey()));
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
            if (Objects.equals(in, reflect())) {
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
    default Pair<Integer, Integer> isOutputSubset(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                                                  Set<Pair<Integer, Integer>> collected,
                                                  Function<Out, Seq<In>> outputAsString,
                                                  Function<P, Seq<In>> finalStateOutputAsString) {
        return collectOutputProductDeterministic(lhs, rhs, lhs.initial, rhs.initial, collected, outputAsString,
                finalStateOutputAsString, (a, b) -> lhs.isAccepting(a) && !rhs.isAccepting(b) ? Pair.of(a, b) : null);
    }


    default G importATT(File file) throws FileNotFoundException {

        try (Scanner sc = new Scanner(file)) {
            final HashMap<String, N> stringToState = new HashMap<>();
            while (sc.hasNextLine()) {
                String fields = sc.nextLine();
            }
        }
        return null;
    }

}
