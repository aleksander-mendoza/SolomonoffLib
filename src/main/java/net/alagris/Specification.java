package net.alagris;

import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
 *              (methods {@link Specification#fromExclusive} and {@link Specification#toInclusive}).
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
     * Full edge constructor
     */
    E createFullEdgeOverSymbol(In symbol, P partialEdge);


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
     * It must always hold that compare(fromExclusive(r),toInclusive(r))<=0
     */
    In fromExclusive(E edge);

    /**
     * It must always hold that compare(fromExclusive(r),toInclusive(r))<=0
     */
    In toInclusive(E edge);

    In successor(In predecessor);

    /**
     * Returns the smallest input symbol. This symbol is excluded from symbols of alphabet.
     * It should not belong to &Sigma; (a.k.a the "dot wildcard")
     */
    In minimal();

    /**
     * Returns the largest input symbol. This symbol is included in the symbols of alphabet.
     * It does belong to &Sigma; (a.k.a the "dot wildcard")
     */
    In maximal();

    /**
     * It must always hold that compare(from(r),to(r))<=0
     */
    int compare(In point1, In point2);

    default In fromInclusive(E edge) {
        return successor(fromExclusive(edge));
    }

    /**
     * Lexicographic order on ranges
     */
    default int lexCompare(E edge, E edge2) {
        int c = compare(fromExclusive(edge), fromExclusive(edge2));
        return c == 0 ? compare(toInclusive(edge), toInclusive(edge2)) : c;
    }

    /**
     * checking if ranges share common interval
     */
    default boolean overlap(E edge, E edge2) {
        return compare(toInclusive(edge), fromInclusive(edge2)) >= 0 && compare(fromInclusive(edge), toInclusive(edge2)) <= 0;
    }

    /**
     * checking if range contains a point
     */
    default boolean contains(E edge, In point) {
        return compare(fromExclusive(edge), point) < 0 && compare(point, toInclusive(edge)) <= 0;
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
    Pair<In, In> parseRangeInclusive(int codepointFromInclusive, int codepointToInclusive);

    Pair<In, In> symbolAsRange(In symbol);

    /**
     * The largest range of values associated with the alphabet &Sigma; dot wildcard.
     * The left (lower) element is exclusive (does not belong to &Sigma;). The right (upper)
     * element is inclusive (belongs to &Sigma;).
     */
    default Pair<In, In> dot() {
        return Pair.of(minimal(), maximal());
    }

    /**
     * The special value associated with the # symbol on output side. This symbol will be used
     * to reflect input during automaton evaluation
     */
    default In reflect() {
        return minimal();
    }


    P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

    P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;


    //////////////////////////
    // Below are default functions added for convenience
    /////////////////////////

    default boolean isOutputNeutralElement(Out out) {
        return Objects.equals(out, outputNeutralElement());
    }

    default boolean isEpsilonOutput(E edge) {
        return isOutputNeutralElement(output(edge));
    }

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

    default E fullNeutralEdgeOverSymbol(In symbol) {
        return createFullEdgeOverSymbol(symbol, partialNeutralEdge());
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
    default G atomicRangeGraph(V state, Pair<In, In> range) {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        E e = fullNeutralEdge(range.getFirst(), range.getSecond());
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
     * @param startpoint             the vertex that marks beginning of graph. You may want
     *                               to use {@link IntermediateGraph#makeUniqueInitialState}
     *                               to generate the unique initial state.
     * @param shouldContinuePerState allows for early termination. Especially useful, when
     *                               you don't actually care about collecting the set but
     *                               only want to search something.
     * @return collected set if successfully explored entire graph. Otherwise null
     * if early termination occurred
     */
    default <S extends Set<N>> S collect(G graph, N startpoint, S set,
                                         Function<N, Object> shouldContinuePerState,
                                         BiFunction<N, E, Object> shouldContinuePerEdge) {
        return SinglyLinkedGraph.collectSet(graph, startpoint, set, shouldContinuePerState, shouldContinuePerEdge);
    }

    default HashSet<N> collect(G graph, N startpoint) {
        return collect(graph, startpoint, new HashSet<>(), x -> null, (n, e) -> null);
    }


    interface Range<In, M> {
        public In input();

        /**
         * Edges that should be taken in the reange between this (inclusive) and previous input.
         */
        public M edges();
    }

    public static class RangeImpl<In, M> implements Range<In, M> {
        public final In input;
        public M edges;

        public RangeImpl(In input, M edges) {
            this.input = input;
            this.edges = edges;
        }

        @Override
        public String toString() {
            return "Range{" +
                    "input=" + input +
                    ", edges=" + edges +
                    '}';
        }

        @Override
        public In input() {
            return input;
        }


        @Override
        public M edges() {
            return edges;
        }
    }

    static <X> void removeTail(List<X> list, int desiredLength) {
        while (list.size() > desiredLength) {
            list.remove(list.size() - 1);
        }
    }

    static <X> ArrayList<X> filledArrayList(int size, X defaultElement) {
        return filledArrayListFunc(size, i -> defaultElement);
    }

    static <X> ArrayList<X> filledArrayListFunc(int size, Function<Integer, X> defaultElement) {
        ArrayList<X> arr = new ArrayList<>(size);
        for (int i = 0; i < size; i++) arr.add(defaultElement.apply(i));
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

    /**removes duplicates in an array. First the array is sorted adn then all the equal elements
     * are merged into one. You can specify when elements are equal and how to merge them*/
    static <X> void removeDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        removeTail(sortedArray, shiftDuplicates(sortedArray, areEqual, mergeEqual));

    }

    /**
     * Every transition spans some range of symbols. Sometimes it's useful to know
     * how much of the alphabet is covered without getting into details of what edges exactly
     * cover each range. This way you can later easily detect holes that are not covered by any range.
     * In essence it works just like {@link Specification#optimise}, but it doesn't keep track of all the
     * individual edges. Instead it only holds boolean value that tells us whether there is any edge or not.
     * It's like running {@link Specification#optimise} and then performing {@link List#isEmpty()} on the list
     * stored in each {@link Range}.
     */
    default ArrayList<Range<In, Boolean>> detectSigmaCoverage(G graph, N vertex) {
        return mergeRangedEdges(graph.iterator(vertex), r -> fromExclusive(r.getKey()), r -> toInclusive(r.getKey()),
                edges -> !edges.isEmpty(), false);
    }


    /**
     * The {@link SinglyLinkedGraph} is a perfect data structure for performing mutatable operations,
     * but it doesn't let us look-up transitions by symbol efficiently. When you need to perform heavy operations
     * by looking-up transitions per symbol, then the optimal solution would be to first sort all transitions
     * by their symbols. However, in Solomonoff it's not so straight forward, because the transitions don't have
     * symbols as labels. Instead they span entire ranges. This method efficiently "squashes" all those ranges into
     * a sorted array of symbols, and ot each symbol associates list of edges that cover the range between the current symbol
     * and the previous one. This way you can perform binary search to easily lookup list of all transitions that
     * match any symbol.
     */
    default <M> ArrayList<Range<In, M>> optimise(G graph, N vertex,
                                                 Function<List<Map.Entry<E, N>>, M> map,
                                                 M nullEdge) {
        return mergeRangedEdges(graph.iterator(vertex), r -> fromExclusive(r.getKey()), r -> toInclusive(r.getKey()),
                map, nullEdge);
    }

    default <M, R> ArrayList<Range<In, M>> mergeRangedEdges(Iterator<R> edges,
                                                            Function<R, In> fromExclusive,
                                                            Function<R, In> toInclusive,
                                                            Function<List<R>, M> squashEdges,
                                                            M nullEdge) {
        class IBE {
            /**input symbol*/
            final In i;
            /**edges beginning (exclusive) at input symbol*/
            final ArrayList<R> b = new ArrayList<>();
            /**edges ending (inclusive) at input symbol*/
            final ArrayList<R> e = new ArrayList<>();

            IBE(R b, R e) {
                this.i = b == null ? toInclusive.apply(e) : fromExclusive.apply(b);
                if (b != null) this.b.add(b);
                if (e != null) this.e.add(e);
            }

            int compare(IBE other) {
                return Specification.this.compare(i, other.i);
            }
        }
        final ArrayList<IBE> points = new ArrayList<>();
        edges.forEachRemaining(edge -> {
            assert compare(fromExclusive.apply(edge), toInclusive.apply(edge)) < 0 : edge;
            points.add(new IBE(edge, null));
            points.add(new IBE(null, edge));
        });
        points.sort(IBE::compare);
        removeDuplicates(points, (prev, curr) -> prev.i.equals(curr.i), (prev, curr) -> {
            prev.b.addAll(curr.b);
            prev.e.addAll(curr.e);
        });
        final ArrayList<Range<In, M>> transitions = new ArrayList<>(
                checkLast(points, l -> Objects.equals(l.i, maximal())) ? points.size() : points.size() + 1);
        final ArrayList<R> accumulated = new ArrayList<>();
        for (IBE ibe : points) {
            final In endInclusive = ibe.i;
            final M edgesInRange = squashEdges.apply(accumulated);
            if (compare(minimal(), endInclusive) < 0) transitions.add(new RangeImpl<>(endInclusive, edgesInRange));
            accumulated.removeAll(ibe.e);
            accumulated.addAll(ibe.b);
        }
        if (!checkLast(transitions, last -> Objects.equals(last.input(), maximal()))) {
            transitions.add(new RangeImpl<>(maximal(), nullEdge));
        }
        assert accumulated.isEmpty() : "Unexpected leftovers! " + accumulated.toString();
        assert isFullSigmaCovered(transitions) : transitions;
        return transitions;
    }

    public static <T> boolean checkLast(List<T> list, Predicate<T> expected) {
        return !list.isEmpty() && expected.test(list.get(list.size() - 1));
    }

    public class RangedGraph<V, In, E, P> {

        public boolean isAccepting(int state) {
            return state != -1 && accepting.get(state) != null;
        }

        public V state(int stateIdx) {
            return stateIdx == -1 ? null : indexToState.get(stateIdx);
        }

        public P getFinalEdge(int state) {
            return state == -1 ? null : accepting.get(state);
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

        public ArrayList<ArrayList<Range<In, List<Trans<E>>>>> graph;
        public ArrayList<P> accepting;
        public ArrayList<V> indexToState;
        public int initial;

        RangedGraph(ArrayList<ArrayList<Range<In, List<Trans<E>>>>> graph, ArrayList<P> accepting,
                    ArrayList<V> indexToState, int initial) {
            assert graph.size() == accepting.size() : graph.size() + " " + accepting.size();
            assert graph.size() == indexToState.size() : graph.size() + " " + indexToState.size();
            assert initial < graph.size();
            this.graph = graph;
            this.accepting = accepting;
            this.indexToState = indexToState;
            this.initial = initial;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("init ").append(initial).append('\n');
            for (int i = 0; i < graph.size(); i++) {
                for (Range<In, List<Trans<E>>> r : graph.get(i)) {
                    sb.append(i)
                            .append(" ")
                            .append(r.input())
                            .append(" ")
                            .append(r.edges())
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
            for (ArrayList<Range<In, List<Trans<E>>>> state : graph) {
                for (Range<In, List<Trans<E>>> trans : state) {
                    if (trans.edges().size() > 1)
                        return trans.edges();
                }
            }
            return null;
        }


        public List<Range<In, List<Trans<E>>>> getTransOrSink(int index, List<Range<In, List<Trans<E>>>> sinkTrans) {
            if (index == -1) return sinkTrans;
            return graph.get(index);
        }

        public int size() {
            return graph.size();
        }


    }

    default List<Range<In, List<RangedGraph.Trans<E>>>> getTransOrSink(RangedGraph<V, In, E, P> g, int index) {
        if (index == -1) return sinkTrans();
        return g.graph.get(index);
    }

    default <M> Range<In, M> sinkRange(M nullEdge) {
        return new RangeImpl<>(maximal(), nullEdge);
    }

    default <M> List<Range<In, M>> sinkTrans(M nullEdge) {
        return Collections.singletonList(sinkRange(nullEdge));
    }

    default <M> ArrayList<Range<In, M>> sinkMutTrans(M nullEdge) {
        return singeltonArrayList(sinkRange(nullEdge));
    }

    default <M> ArrayList<Range<In, List<RangedGraph.Trans<E>>>> sinkMutTrans() {
        return sinkMutTrans(Collections.emptyList());
    }

    default List<Range<In, List<RangedGraph.Trans<E>>>> sinkTrans() {
        return sinkTrans(Collections.emptyList());
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
            ArrayList<Range<In, List<RangedGraph.Trans<E>>>> dfaTrans;

            IdxAndTrans(int index) {
                this.index = index;
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
        while (!toVisit.isEmpty()) {
            final PS powersetState = toVisit.pop();
            final IdxAndTrans source = powersetStateToIndex.get(powersetState);
            if (source.dfaTrans != null) continue;//already visited
            final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> powersetTrans =
                    powersetTransitions(g, powersetState.states);

            source.dfaTrans = new ArrayList<>(powersetTrans.size());
            assert isFullSigmaCovered(powersetTrans);
            In beginExclusive = minimal();
            for (Range<In, List<RangedGraph.Trans<E>>> range : powersetTrans) {
                final PS target = new PS(range.edges());
                final In endInclusive = range.input();
                final E edge = fullNeutralEdge(beginExclusive, endInclusive);
                final List<RangedGraph.Trans<E>> trans;
                if (target.states.length == 0) {
                    trans = Collections.emptyList();
                } else {
                    final IdxAndTrans targetIndex = powersetStateToIndex.computeIfAbsent(target, k -> new IdxAndTrans(powersetStateToIndex.size()));
                    if (targetIndex.dfaTrans == null)
                        toVisit.add(target);
                    trans = singeltonArrayList(new RangedGraph.Trans<>(edge, targetIndex.index));
                }
                source.dfaTrans.add(new RangeImpl<>(range.input(), trans));
                beginExclusive = endInclusive;
            }
        }

        final ArrayList<ArrayList<Range<In, List<RangedGraph.Trans<E>>>>> graph = filledArrayList(powersetStateToIndex.size(), null);
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
                0);
    }

    public static <X> int indexOf(Iterable<X> list, Predicate<X> f) {
        int i = 0;
        for (X x : list)
            if (f.test(x)) return i;
            else i++;
        return -1;
    }

    default ArrayList<Range<In, List<RangedGraph.Trans<E>>>> powersetTransitions(RangedGraph<V, In, E, P> g, int[] states) {
        assert isStrictlyIncreasing(states) : Arrays.toString(states);
        int stateIdx = 0;
        assert states.length > 0 : g.toString();
        NullTermIter<Range<In, List<RangedGraph.Trans<E>>>> transitions = fromIterable(g.graph.get(states[stateIdx]));
        for (stateIdx++; stateIdx < states.length; stateIdx++) {
            transitions = zipTransitionRanges(transitions, fromIterable(g.graph.get(states[stateIdx])),
                    (fromExclusive,toInclusive, l, r) -> new RangeImpl<>(toInclusive, lazyConcatImmutableLists(l, r)));
        }
        final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> collected = new ArrayList<>();
        Range<In, List<RangedGraph.Trans<E>>> range;
        while ((range = transitions.next()) != null) {
            collected.add(new RangeImpl<>(range.input(), new ArrayList<>(range.edges())));
        }
        return collected;
    }

    public static <X> ArrayList<X> singeltonArrayList(X x) {
        ArrayList<X> a = new ArrayList<>(1);
        a.add(x);
        return a;
    }

    default RangedGraph<V, In, E, P> optimiseGraph(G graph) {
        return optimiseGraph(graph, n -> null, (n, e) -> null);
    }

    default RangedGraph<V, In, E, P> optimiseGraph(G graph,
                                                   Function<N, Object> shouldContinuePerState,
                                                   BiFunction<N, E, Object> shouldContinuePerEdge) {
        final N initial = graph.makeUniqueInitialState(null);
        final HashSet<N> states = collect(graph, initial, new HashSet<>(), shouldContinuePerState, shouldContinuePerEdge);
        final ArrayList<ArrayList<Range<In, List<RangedGraph.Trans<E>>>>> graphTransitions = filledArrayList(states.size(), null);
        final HashMap<N, Integer> stateToIndex = new HashMap<>(states.size());
        final ArrayList<V> indexToState = new ArrayList<>(states.size());
        final ArrayList<P> accepting = filledArrayList(states.size(), null);
        for (N state : states) {
            int idx = indexToState.size();
            indexToState.add(graph.getState(state));
            stateToIndex.put(state, idx);
        }
        for (final Map.Entry<N, Integer> state : stateToIndex.entrySet()) {
            final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> transitions = optimise(graph, state.getKey(),
                    accumulated -> {
                        final ArrayList<RangedGraph.Trans<E>> edgesInRange = new ArrayList<>(accumulated.size());
                        accumulated.forEach(acc -> edgesInRange.add(new RangedGraph.Trans<E>(acc.getKey(), stateToIndex.get(acc.getValue()))));
                        return edgesInRange;
                    }, Collections.emptyList());
            graphTransitions.set(state.getValue(), transitions);
            accepting.set(state.getValue(), graph.getFinalEdge(state.getKey()));
        }
        accepting.set(stateToIndex.get(initial), graph.getEpsilon());
        return new RangedGraph<>(graphTransitions, accepting, indexToState, stateToIndex.get(initial));
    }

    default <M> boolean isFullSigmaCovered(List<Range<In, M>> transitions) {
        if (transitions.isEmpty()) return false;
        if (!isStrictlyIncreasing(transitions, (a, b) -> compare(a.input(), b.input()))) return false;
        if (Objects.equals(transitions.get(0).input(), minimal())) return false;
        return Objects.equals(transitions.get(transitions.size() - 1).input(), maximal());
    }

    /**
     * @return the outgoing transition containing given input. Otherwise transition to sink state is returned.
     */
    default List<RangedGraph.Trans<E>> binarySearch(RangedGraph<V, In, E, P> graph, int state, In input) {
        final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> transitions = graph.graph.get(state);
        assert isFullSigmaCovered(transitions) : transitions;
        return transitions.get(binarySearchIndex(transitions, input)).edges();
    }

    /**
     * @return index to a transition containing given input. This is a special index that must be interpreted using
     * {@link RangedGraph#getTransOrSink}. Those indices are useful when you need to iterate over transitions.
     */
    default <M> int binarySearchIndex(ArrayList<Range<In, M>> transitions, In input) {
        assert isFullSigmaCovered(transitions) : transitions;
        int low = 0;
        int high = transitions.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Range<In, M> midVal = transitions.get(mid);
            int c = compare(midVal.input(), input);
            if (c < 0)
                low = mid + 1;
            else if (c > 0)
                high = mid - 1;
            else
                return mid; // key found at transitions.get(mid)
        }
        assert low < transitions.size() : low + " " + transitions;
        return low;//key is somewhere between transitions.get(low) and transitions.get(low-1)
    }

    /**
     * This method assumes that graph is deterministic
     *
     * @return null if transition leads to sink state
     */
    default RangedGraph.Trans<E> deltaBinarySearchDeterministic(RangedGraph<V, In, E, P> graph, int state,
                                                                In input) {
        final List<RangedGraph.Trans<E>> transitions = binarySearch(graph, state, input);
        return transitions.isEmpty() ? null : transitions.get(0);
    }

    /**
     * This method assumes that graph is deterministic. Returns -1 if rejected. The index -1 represents the sink state.
     */
    default int deltaBinarySearchTransitiveDeterministic(RangedGraph<V, In, E, P> graph, int state,
                                                         Iterator<In> input) {
        while (input.hasNext()) {
            final RangedGraph.Trans<E> tran = deltaBinarySearchDeterministic(graph, state, input.next());
            if (tran == null) return -1;
            state = tran.targetState;
        }
        return state;
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

    static <X> ArrayList<X> collect(NullTermIter<X> iter, int capacity) {
        final ArrayList<X> arr = new ArrayList<>(capacity);
        X x;
        while ((x = iter.next()) != null) {
            arr.add(x);
        }
        return arr;
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

    /**
     * This is a utility function that can take some list of transitions. If the list is null,
     * then a sink transition is implied. Sink transition is represented by null
     */
    public static <Y> List<Y> listOrSingletonWithNull(List<Y> list) {
        return list.isEmpty() ? Collections.singletonList(null) : list;
    }

    /**
     * @param crossProduct called for every pair of transitions that have some overlapping range.
     *                     If a certain transition on one side does not have any overlapping transition on the other, then one of the arguments
     *                     will become a null value. Null transitions symbolise transitions to sink state. Sink state has index -1.
     */
    default <L, R, Y> Y crossProductOfTransitions(
            NullTermIter<Range<In, List<L>>> lhs,
            NullTermIter<Range<In, List<R>>> rhs,
            RangeCrossProduct<In, L, R, Y> crossProduct) {
        class Wrapper {
            final Y y;

            Wrapper(Y y) {
                this.y = y;
            }
        }
        final NullTermIter<Wrapper> iter = zipTransitionRanges(lhs, rhs,
                (fromExclusive, toInclusive, l, r) -> {
                    for (L prevLhs : listOrSingletonWithNull(l)) {
                        for (R prevRhs : listOrSingletonWithNull(r)) {
                            final Y y = crossProduct.times(fromExclusive,toInclusive, prevLhs, prevRhs);
                            if (y != null){
                                return new Wrapper(y);
                            }
                        }
                    }
                    return new Wrapper(null);
                });
        Wrapper next;
        while ((next = iter.next()) != null) {
            if (next.y != null) return next.y;
        }
        return null;
    }

    interface RangeCrossProduct<In, L, R, Z> {
        Z times(In fromExclusive,In toInclusive, L lhsTran, R rhsTran);
    }

    default <L, R, Z> NullTermIter<Z> zipTransitionRanges(
            NullTermIter<? extends Range<In, L>> lhs,
            NullTermIter<? extends Range<In, R>> rhs,
            RangeCrossProduct<In, L, R, Z> crossProduct) {
        return new NullTermIter<Z>() {
            Range<In, L> lhsRange = lhs.next();
            Range<In, R> rhsRange = rhs.next();
            In prev = minimal();
            @Override
            public Z next() {
                assert (lhsRange == null) == (rhsRange == null) : lhsRange + " " + rhsRange;
                if (lhsRange != null && rhsRange != null) {
                    final int c = compare(lhsRange.input(), rhsRange.input());
                    final L l = lhsRange.edges();
                    final R r = rhsRange.edges();
                    final In in;
                    if (c < 0) {
                        in = lhsRange.input();
                        lhsRange = lhs.next();
                    } else if (c > 0) {
                        in = rhsRange.input();
                        rhsRange = rhs.next();
                    } else {
                        in = rhsRange.input();
                        lhsRange = lhs.next();
                        rhsRange = rhs.next();
                    }

                    final Z z = crossProduct.times(prev, in, l, r);
                    prev = in;
                    return z;
                } else {
                    return null;
                }
            }
        };
    }

    class IntPair {
        final int l, r;

        public IntPair(int l, int r) {
            this.l = l;
            this.r = r;
        }

        public int getL() {
            return l;
        }

        public int getR() {
            return r;
        }

        @Override
        public boolean equals(Object o) {
            IntPair intPair = (IntPair) o;
            return l == intPair.l &&
                    r == intPair.r;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l, r);
        }

        @Override
        public String toString() {
            return "(" + l +
                    "," + r +
                    ")";
        }
    }

    interface StateCollector<N> {
        /**
         * Return product of states that should be visited, or null if this product has already been visited before
         */
        N visit(int lState, int rState);
    }

    interface ShouldContinuePerEdge<In, N, E, Y> {
        Y shouldContinue(N source,In fromExclusive,In toInclusive, Map.Entry<E, Integer> lEdge, Map.Entry<E, Integer> rEdge);
    }

    interface ShouldContinuePerState<N, E, Y> {
        Y shouldContinue(N stateProduct);
    }

    default <Y> Y collectProductSet(RangedGraph<V, In, E, P> lhs,
                                    RangedGraph<V, In, E, P> rhs,
                                    int startpointLhs,
                                    int startpointRhs,
                                    Set<IntPair> collect,
                                    ShouldContinuePerEdge<In, IntPair,E, Y> shouldContinuePerEdge,
                                    ShouldContinuePerState<IntPair,E, Y> shouldContinuePerState) {
        return collectProduct(lhs, rhs, startpointLhs, startpointRhs, (l,r)->{
                    final IntPair p = new IntPair(l,r);
                    if(collect.add(p))return p;
                    return null;
                },
                IntPair::getL,IntPair::getR,
                shouldContinuePerEdge, shouldContinuePerState);
    }

    /**
     * Performs product of automata. Collects all reachable pairs of states.
     * A pair of transitions between pairs of states is taken only when their input ranges overlap with one another.
     *
     * @param collect                this function is callback that will receive all reached pairs of states.
     *                               It should remember states that were already seen and return new meta M when the pair is seen for the first time.
     *                               If a pair has already been registered, then null must be returned.
     * @param shouldContinuePerEdge  is invoked for every pair of traversed transitions. If non-null value is returned then
     *                               breath-first search terminates early and the function as a whole returns the obtained value.
     *                               This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of edges.
     * @param shouldContinuePerState is invoked for every pair of visited states. If non-null value is returned then
     *                               breath-first search terminates early and the function as a whole returns the
     *                               obtained value. This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of states.
     */
    default <Y, N> Y collectProduct(RangedGraph<V, In, E, P> lhs,
                                    RangedGraph<V, In, E, P> rhs,
                                    int startpointLhs,
                                    int startpointRhs,
                                    StateCollector<N> collect,
                                    Function<N, Integer> leftState,
                                    Function<N, Integer> rightState,
                                    ShouldContinuePerEdge<In, N, E, Y> shouldContinuePerEdge,
                                    ShouldContinuePerState<N, E, Y> shouldContinuePerState) {

        final Stack<N> toVisit = new Stack<>();
        final N init = collect.visit(startpointLhs, startpointRhs);
        if (init != null) {
            final Y out = shouldContinuePerState.shouldContinue(init);
            if (out != null) return out;
            toVisit.push(init);
        }

        while (!toVisit.isEmpty()) {
            final N pair = toVisit.pop();
            final int l = leftState.apply(pair);
            final int r = rightState.apply(pair);
            Y y = crossProductOfTransitions(fromIterable(getTransOrSink(lhs, l)), fromIterable(getTransOrSink(rhs, r)),
                    (fromExclusive,toInclusive, prevLhs, prevRhs) -> {
                        final Y y2 = shouldContinuePerEdge.shouldContinue(pair, fromExclusive,toInclusive,prevLhs, prevRhs);
                        if (y2 != null) return y2;
                        final int targetLhs = prevLhs == null ? -1 : prevLhs.targetState;
                        final int targetRhs = prevRhs == null ? -1 : prevRhs.targetState;
                        final N targetPair = collect.visit(targetLhs, targetRhs);
                        if (targetPair!=null) {
                            final Y out = shouldContinuePerState.shouldContinue(targetPair);
                            if (out != null) return out;
                            toVisit.push(targetPair);
                        }
                        return null;
                    });
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
    default IntPair isSubset(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                             int startpointLhs, int startpointRhs, Set<IntPair> collected) {
        return collectProductSet(lhs, rhs, startpointLhs, startpointRhs, collected, (state, fromExclusive,toInclusive,a, b) -> null, (state) ->
                lhs.isAccepting(state.l) && !rhs.isAccepting(state.r) ? state : null
        );
    }

    /**
     * This function, unlike {@link Specification#isSubset} allows both arguments to be nondeterministic. However, it comes
     * at the cost of exponential time complexity (because powerset construction needs to be performed first)
     */
    default Pair<V, V> isSubsetNondeterministic(RangedGraph<V, In, E, P> lhs,
                                                RangedGraph<V, In, E, P> rhs,
                                                Function<In, In> successor,
                                                Function<In, In> predecessor) {
        final RangedGraph<V, In, E, P> dfa = powerset(rhs, successor, predecessor);
        final IntPair counterexample = isSubset(lhs, dfa, lhs.initial, dfa.initial, new HashSet<>());
        return counterexample == null ? null : Pair.of(lhs.state(counterexample.l), dfa.state(counterexample.r));
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
            Function<Out, ? extends Iterator<In>> outputAsString,
            Function<P, ? extends Iterator<In>> finalStateOutputAsString,
            BiFunction<Integer, Integer, Y> shouldContinuePerState,
            BiFunction<Integer, Integer, Y> shouldContinueAfterSubsequentialOutput) {

        assert find(rhs.graph, t -> !isFullSigmaCovered(t)) == null : rhs + " " + find(rhs.graph, t -> !isFullSigmaCovered(t));
        final Stack<Pair<Integer, Integer>> pairsToVisit = new Stack<>();

        final Pair<Integer, Integer> pairStartpoint = Pair.of(startpointLhs, startpointRhs);
        if (visited.add(pairStartpoint)) {
            final Y y = shouldContinuePerState.apply(startpointLhs, startpointRhs);
            if (y != null) return y;
            pairsToVisit.push(pairStartpoint);
        }
        while (!pairsToVisit.isEmpty()) {
            final Pair<Integer, Integer> pair = pairsToVisit.pop();
            final int l = pair.getFirst();
            final int r = pair.getSecond();
            final P partialFinalEdgeL = lhs.getFinalEdge(l);
            if (partialFinalEdgeL != null) {
                int rhsTargetState = deltaBinarySearchTransitiveDeterministic(rhs, r, finalStateOutputAsString.apply(partialFinalEdgeL));
                final Y y = shouldContinueAfterSubsequentialOutput.apply(l, rhsTargetState);
                if (y != null) return y;
            }

            In fromExclusive = minimal();
            final List<Range<In, List<RangedGraph.Trans<E>>>> transLhs = getTransOrSink(lhs, l);
            assert isFullSigmaCovered(transLhs) : transLhs + " (" + l + ") " + lhs;
            for (Range<In, List<RangedGraph.Trans<E>>> entryLhs : transLhs) {
                final In toInclusive = entryLhs.input();
                for (RangedGraph.Trans<E> tran : listOrSingletonWithNull(entryLhs.edges())) {
                    final HashMap<Integer, ArrayList<Range<In, Object>>> rhsTargetStates =
                            composedMirroredOutputDeltaConfiguration(rhs, r, fromExclusive, toInclusive,
                                    outputAsString.apply(outputOrSink(tran)));
                    for (int targetStateRhs : rhsTargetStates.keySet()) {
                        final int targetStateLhs = tran == null ? -1 : tran.targetState;
                        final Pair<Integer, Integer> targetState = Pair.of(targetStateLhs, targetStateRhs);
                        if (visited.add(targetState)) {
                            final Y y = shouldContinuePerState.apply(targetStateLhs, targetStateRhs);
                            if (y != null) return y;
                            pairsToVisit.push(targetState);
                        }
                    }
                }
                fromExclusive = toInclusive;
                assert isFullSigmaCovered(transLhs) : transLhs + " (" + l + ") " + lhs;
            }
        }
        return null;
    }

    default Out outputOrSink(RangedGraph.Trans<E> tran) {
        return tran == null ? outputNeutralElement() : output(tran.edge);
    }

    default HashMap<Integer, ArrayList<Range<In, Object>>> composedMirroredOutputDeltaConfiguration(RangedGraph<V, In, E, P> rhs, int startpointRhs,
                                                                                                    In lhsTranRangeFrom,
                                                                                                    In lhsTranRangeTo,
                                                                                                    Iterator<In> lhsOutput) {
        final Object DUMMY = new Object();
        return composedMirroredOutputDelta(rhs, makeRangesSuperposition(startpointRhs, DUMMY, null, lhsTranRangeFrom, lhsTranRangeTo), (in, a, b) -> {
            assert a != null;
            assert b != null;
            return DUMMY;
        }, null, (a, b) -> a == null ? b : a, lhsOutput);
    }


    default HashMap<Integer, ArrayList<Range<In, Pair<W, Out>>>> composedMirroredOutputDeltaSuperposition(RangedGraph<V, In, E, P> rhs,
                                                                                                          int startpointRhs,
                                                                                                          In lhsTranRangeFrom,
                                                                                                          In lhsTranRangeTo,
                                                                                                          ComposeSuperposition<In, E, Pair<W, Out>> compose,
                                                                                                          BiFunction<Pair<W, Out>, Pair<W, Out>, Pair<W, Out>> union,
                                                                                                          Iterator<In> lhsOutput) {
        final Pair<W, Out> neutralPair = Pair.of(weightNeutralElement(), outputNeutralElement());
        return composedMirroredOutputDelta(rhs, makeRangesSuperposition(startpointRhs, neutralPair, null, lhsTranRangeFrom, lhsTranRangeTo), compose, null, union, lhsOutput);
    }


    default <P> ArrayList<Range<In, P>> makeSingletonRanges(P startpointRhsEdge,
                                                            P nullEdge,
                                                            In lhsTranRangeFromExclusive,
                                                            In lhsTranRangeToInclusive) {

        assert compare(lhsTranRangeFromExclusive, lhsTranRangeToInclusive) <= 0 : lhsTranRangeFromExclusive + " " + lhsTranRangeToInclusive;
        final ArrayList<Range<In, P>> ranges = new ArrayList<>(Objects.equals(lhsTranRangeToInclusive, maximal()) ? 2 : 3);
        if (compare(minimal(), lhsTranRangeFromExclusive) < 0)
            ranges.add(new RangeImpl<>(lhsTranRangeFromExclusive, nullEdge));
        assert compare(lhsTranRangeFromExclusive, lhsTranRangeToInclusive) < 0 : lhsTranRangeFromExclusive + " " + lhsTranRangeToInclusive;
        ranges.add(new RangeImpl<>(lhsTranRangeToInclusive, startpointRhsEdge));
        if (!Objects.equals(lhsTranRangeToInclusive, maximal())) {
            ranges.add(new RangeImpl<>(maximal(), nullEdge));
        }
        assert isFullSigmaCovered(ranges) : ranges;
        return ranges;
    }

    default <P> ArrayList<Range<In, P>> makeEmptyRanges(P nullEdge) {
        final ArrayList<Range<In, P>> ranges = new ArrayList<>();
        ranges.add(new RangeImpl<>(maximal(), nullEdge));
        assert isFullSigmaCovered(ranges) : ranges;
        return ranges;
    }


    default <P> HashMap<Integer, ArrayList<Range<In, P>>> makeRangesSuperposition(int startpointRhs,
                                                                                  P startpointRhsEdge,
                                                                                  P nullEdge,
                                                                                  In lhsTranRangeFrom,
                                                                                  In lhsTranRangeTo) {
        assert compare(lhsTranRangeFrom, lhsTranRangeTo) <= 0 : lhsTranRangeFrom + " " + lhsTranRangeTo;
        final HashMap<Integer, ArrayList<Range<In, P>>> superposition = new HashMap<>();
        superposition.put(startpointRhs, makeSingletonRanges(startpointRhsEdge, nullEdge, lhsTranRangeFrom, lhsTranRangeTo));
        return superposition;
    }

    interface ComposeSuperposition<In, E, P2> {
        P2 compose(P2 prev, RangedGraph.Trans<E> rhsTransTaken, In lhsOutSymbol);

        default Range<In, P2> composeRanges(Range<In, P2> prev, P2 nullEdge, RangedGraph.Trans<E> rhsTransTaken, In lhsOutSymbol) {
            return Objects.equals(prev.edges(), nullEdge) ? prev : new RangeImpl<>(prev.input(), compose(prev.edges(), rhsTransTaken, lhsOutSymbol));
        }

        default ArrayList<Range<In, P2>> composeSuperpositions(ArrayList<Range<In, P2>> prev, P2 nullEdge, RangedGraph.Trans<E> rhsTransTaken, In lhsOutSymbol) {
            final ArrayList<Range<In, P2>> arr = new ArrayList<>(prev.size());
            for (Range<In, P2> range : prev) {
                arr.add(composeRanges(range, nullEdge, rhsTransTaken, lhsOutSymbol));
            }
            return arr;
        }
    }


    default <P> void insertRange(ArrayList<Range<In, P>> ranges, In fromExclusive, In toInclusive, P nullEdge, P edge,
                                 BiFunction<P, P, P> union) {
        assert !Objects.equals(edge, nullEdge);
        assert isFullSigmaCovered(ranges);
        int fromIdx = binarySearchIndex(ranges, fromExclusive);
        int toIdx = binarySearchIndex(ranges, toInclusive);
        final Range<In, P> lowerRange = ranges.get(fromIdx);
        final In lowerBound = lowerRange.input();
        if (!Objects.equals(lowerBound, fromExclusive) && compare(minimal(), fromExclusive) < 0) {
            assert compare(fromExclusive, lowerBound) < 0;
            ranges.add(fromIdx, new RangeImpl<>(fromExclusive, lowerRange.edges()));
            toIdx++;
            assert isStrictlyIncreasing(ranges, (a, b) -> compare(a.input(), b.input()));
            assert compare(lowerBound, ranges.get(fromIdx + 1).input()) == 0;
        }
        assert Objects.equals(ranges.get(fromIdx).input(), maximal()) == (fromIdx + 1 == ranges.size()) : fromIdx + " " + ranges;
        assert Objects.equals(ranges.get(fromIdx).input(), maximal()) || compare(fromExclusive, ranges.get(fromIdx + 1).input()) < 0;
        final Range<In, P> upperRange = ranges.get(toIdx);
        final In upperBound = upperRange.input();
        assert compare(minimal(), toInclusive) < 0;
        if (!Objects.equals(upperBound, toInclusive)) {
            assert compare(toInclusive, upperBound) < 0;
            ranges.add(toIdx, new RangeImpl<>(toInclusive, union.apply(upperRange.edges(), edge)));
            assert isStrictlyIncreasing(ranges, (a, b) -> compare(a.input(), b.input()));
            assert compare(upperBound, ranges.get(toIdx + 1).input()) == 0;
        }
        while (++fromIdx < toIdx) {
            final Range<In, P> range = ranges.get(fromIdx);
            final In input = range.input();
            assert compare(minimal(), fromExclusive) < 0;
            assert compare(fromExclusive, input) < 0;
            assert compare(input, toInclusive) < 0;
            ranges.set(fromIdx, new RangeImpl<>(input, union.apply(range.edges(), edge)));
        }
    }

    /**Searches a collection for an element that satisfies some predicate*/
    static <T> T find(Collection<T> c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return t;
        return null;
    }

    /**Composes the output of some edge of left transducer with the input of right transducer */
    default <P2> HashMap<Integer, ArrayList<Range<In, P2>>> composedMirroredOutputDelta(RangedGraph<V, In, E, P> rhs,
                                                                                        HashMap<Integer, ArrayList<Range<In, P2>>> rhsStartpointStates,
                                                                                        ComposeSuperposition<In, E, P2> compose,
                                                                                        P2 nullEdge,
                                                                                        BiFunction<P2, P2, P2> union,
                                                                                        Iterator<In> lhsOutput) {
        HashMap<Integer, ArrayList<Range<In, P2>>> swap = new HashMap<>();
        assert find(rhsStartpointStates.values(), t -> !isFullSigmaCovered(t)) == null : rhsStartpointStates + " " + find(rhsStartpointStates.values(), t -> !isFullSigmaCovered(t));
        for (In in : (Iterable<In>) () -> lhsOutput) {
            final HashMap<Integer, ArrayList<Range<In, P2>>> tmp = swap;

            for (Map.Entry<Integer, ArrayList<Range<In, P2>>> stateAndSuperposition : rhsStartpointStates.entrySet()) {
                final int sourceState = stateAndSuperposition.getKey();
                if (sourceState == -1) {
                    tmp.put(-1, null);
                } else {
                    final ArrayList<Range<In, P2>> superposition = stateAndSuperposition.getValue();
                    if (Objects.equals(in, reflect())) {
                        final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> outgoing = rhs.graph.get(sourceState);
                        assert isFullSigmaCovered(superposition) : superposition;
                        assert isFullSigmaCovered(outgoing) : outgoing;
                        final NullTermIter<Object> zipped = zipTransitionRanges(fromIterable(superposition), fromIterable(outgoing),
                                (fromExclusive, toInclusive, l, trans) -> {
                                    if (!Objects.equals(l, nullEdge)) {
                                        if (trans.isEmpty()) {
                                            tmp.put(-1, null);
                                        } else {
                                            for (RangedGraph.Trans<E> tran : trans) {
                                                final ArrayList<Range<In, P2>> targetSuperposition = tmp.computeIfAbsent(tran.targetState, k -> Specification.this.sinkMutTrans(nullEdge));
                                                final P2 composed = compose.compose(l, tran, in);
                                                Specification.this.insertRange(targetSuperposition, fromExclusive, toInclusive, nullEdge, composed, union);
                                            }
                                        }
                                    }
                                    return 1;//some dummy value
                                });
                        while (zipped.next() != null) ;
                    } else {
                        final List<RangedGraph.Trans<E>> trans = binarySearch(rhs, sourceState, in);
                        if (trans.isEmpty()) {
                            tmp.put(-1, null);
                        } else {
                            for (RangedGraph.Trans<E> tran : trans) {
                                tmp.compute(tran.targetState, (k, prevSuperposition) -> {
                                    final ArrayList<Range<In, P2>> composedSuperposition =
                                            compose.composeSuperpositions(superposition, nullEdge, tran, in);
                                    if (prevSuperposition == null) return composedSuperposition;
                                    return collect(zipTransitionRanges(fromIterable(prevSuperposition), fromIterable(composedSuperposition),
                                            (fromExclusive,toInclusive, l, r) -> new RangeImpl<>(toInclusive, union.apply(l, r))),
                                            prevSuperposition.size() + composedSuperposition.size());
                                });
                            }
                        }
                    }
                }
            }

            swap = rhsStartpointStates;
            rhsStartpointStates = tmp;
            swap.clear();
        }
        return rhsStartpointStates;
    }

    interface EdgeProduct<In,E>{
        E product(In fromExclusive,In toInclusive,E lEdge,E rEdge);
    }
    interface StateProduct<N>{
        int left();
        int right();
        N product();
    }
    /**
     * Performs product of two automata. Take note that it's a quite heavyweight operation.
     * The resulting transducer might be of quadratic size in pessimistic case.
     */
    default Pair<G,HashMap<IntPair, ? extends StateProduct<N>>> product(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                      BiFunction<V, V, V> metaProduct,
                      EdgeProduct<In,E> edgeProduct,
                      BiFunction<P, P, P> outputProduct) {
        final G product = createEmptyGraph();
        final boolean omitSinkState = null==outputProduct.apply(lhs.getFinalEdge(-1),rhs.getFinalEdge(-1));
        class LRProduct implements StateProduct<N>{
            final int l,r;
            final N p;
            boolean visited=false;
            LRProduct(int l, int r) {
                this.l = l;
                this.r = r;
                this.p = product.create(metaProduct.apply(lhs.state(l),rhs.state(r)));
                assert !(omitSinkState && l==-1 && r==-1);
                final P fin = outputProduct.apply(lhs.getFinalEdge(l),rhs.getFinalEdge(r));
                if(fin!=null)product.setFinalEdge(p,fin);
            }

            @Override
            public int left() {
                return l;
            }

            @Override
            public int right() {
                return r;
            }

            @Override
            public N product() {
                return p;
            }
        }
        final HashMap<IntPair, LRProduct> crossProductToNew = new HashMap<>();

        collectProduct(lhs, rhs, lhs.initial, rhs.initial, (int lState, int rState) -> {
            if(omitSinkState && lState==-1 && rState==-1)return null;
            final LRProduct productState = crossProductToNew.computeIfAbsent(new IntPair(lState,rState),k-> new LRProduct(lState,rState));
            if(productState.visited)return null;
            productState.visited = true;
            return productState;
        },(LRProduct o) -> o.l,(LRProduct o)->o.r,( source, fromExclusive, toInclusive, lEdge,rEdge) -> {
            final E edgeP = edgeProduct.product(fromExclusive,toInclusive,lEdge==null?null:lEdge.getKey(), rEdge==null?null:rEdge.getKey());
            if (edgeP != null) {
                final int tL = lEdge==null?-1:lEdge.getValue();
                final int tR = rEdge==null?-1:rEdge.getValue();
                if(omitSinkState && tL==-1 && tR==-1)return null;
                final LRProduct target = crossProductToNew.computeIfAbsent(new IntPair(tL, tR), k -> new LRProduct(tL,tR));
                product.add(source.p, edgeP, target.p);
            }
            return null;
        },stateProduct -> null);
        final N init = crossProductToNew.get(new IntPair(lhs.initial, rhs.initial)).p;
        product.useStateOutgoingEdgesAsInitial(init);
        product.setEpsilon(product.removeFinalEdge(init));
        return Pair.of(product,crossProductToNew);
    }

    /**
     * Composes two transducers. If x is some string then lhs(x) is the output that lhs produces for input x.
     * The composition compose(lhs,rhs) yields a new automaton that works like rhs(lhs(x))=compose(lhs,rhs)(x).
     */
    default G compose(G lhs, RangedGraph<V, In, E, P> rhs,
                      V initialState,
                      Function<Out, ? extends Iterator<In>> outputAsString,
                      BiFunction<W, W, W> composeLhsAndRhsEdgeWeights,
                      ComposeSuperposition<In, E, Pair<W, Out>> compose,
                      BiFunction<P, Integer, Pair<W, Out>> evaluateRhsFromGivenStartpoint,
                      BiFunction<Pair<W, Out>, Pair<W, Out>, Pair<W, Out>> union) {
        final N initL = lhs.makeUniqueInitialState(initialState);
        lhs.setFinalEdge(initL, lhs.getEpsilon());
        final int initR = rhs.initial;
        final HashMap<Pair<N, Integer>, N> crossProductToNew = new HashMap<>();
        class LRComposed {
            final N l;
            final int r;
            final N composed;

            LRComposed(N l, int r, N composed) {
                this.l = l;
                this.r = r;
                this.composed = composed;
            }
        }
        final Stack<LRComposed> toVisit = new Stack<>();
        final G composed = createEmptyGraph();
        final N initComposed = composed.create(initialState);
        toVisit.push(new LRComposed(initL, initR, initComposed));
        while (!toVisit.isEmpty()) {
            final LRComposed lrc = toVisit.pop();
            final P fin = lhs.getFinalEdge(lrc.l);
            if (fin != null) {
                final Pair<W, Out> outputR = evaluateRhsFromGivenStartpoint.apply(fin, lrc.r);
                if (outputR != null) {
                    composed.setFinalEdge(lrc.composed, createPartialEdge(outputR.getSecond(), outputR.getFirst()));
                }
            }
            for (Map.Entry<E, N> edgeTargetL : (Iterable<Map.Entry<E, N>>) () -> lhs.iterator(lrc.l)) {
                final N targetL = edgeTargetL.getValue();
                final E inEdgeL = edgeTargetL.getKey();
                final In from = fromExclusive(inEdgeL);
                final In to = toInclusive(inEdgeL);
                final W weightL = weight(inEdgeL);
                final HashMap<Integer, ArrayList<Range<In, Pair<W, Out>>>> reachableR = composedMirroredOutputDeltaSuperposition(rhs, lrc.r, from, to, compose, union, outputAsString.apply(output(inEdgeL)));
                for (Map.Entry<Integer, ArrayList<Range<In, Pair<W, Out>>>> reachableStateAndOutputR : reachableR.entrySet()) {
                    final int targetR = reachableStateAndOutputR.getKey();
                    if (targetR == -1) continue;
                    final Pair<N, Integer> target = Pair.of(targetL, targetR);
                    final N composedTarget = crossProductToNew.computeIfAbsent(target, k -> {
                        final N newComposed = composed.create(lhs.getState(targetL));
                        toVisit.push(new LRComposed(targetL, targetR, newComposed));
                        return newComposed;
                    });
                    In fromExclusive = minimal();
                    for (Range<In, Pair<W, Out>> range : reachableStateAndOutputR.getValue()) {
                        final In toExclusive = range.input();
                        if (range.edges() != null) {
                            final E composedE = createFullEdge(fromExclusive, toExclusive,
                                    createPartialEdge(range.edges().getSecond(),
                                            composeLhsAndRhsEdgeWeights.apply(weightL, range.edges().getFirst())));
                            composed.add(lrc.composed, composedE, composedTarget);
                        }
                        fromExclusive = toExclusive;
                    }
                }
            }
        }
        composed.useStateOutgoingEdgesAsInitial(initComposed);
        composed.setEpsilon(composed.removeFinalEdge(initComposed));
        return composed;
    }

    /**Normally all transducers are guaranteed to be trim by most of the operations.
     * However, a few operations may leave the transducer in a non-trim state (some states
     * are dead-ends). This procedure will trim the transducer.*/
    default void trim(G g, Iterable<N> reachableStates){
        for(N state : reachableStates){
            g.setColor(state,new HashSet<N>());//reverse
        }
        for(N state : reachableStates){
            for (Map.Entry<E, N> edge : (Iterable<Map.Entry<E, N>>) () -> g.iterator(state)) {
                final HashSet<N> reversed = (HashSet<N>) g.getColor(edge.getValue());
                reversed.add(state);
            }
        }
        /**observable states are those that are reachable in the reversed transducer*/
        final HashSet<N> observable = new HashSet<>();
        final Stack<N> toVisit = new Stack<>();
        for(Map.Entry<N, P> fin:(Iterable<Map.Entry<N, P>>) () ->g.iterateFinalEdges()){
            toVisit.push(fin.getKey());
            observable.add(fin.getKey());
        }
        while(!toVisit.isEmpty()){
            final N state = toVisit.pop();
            final HashSet<N> reversed = (HashSet<N>) g.getColor(state);
            for(N incoming:reversed){
                if(observable.add(incoming)){
                    toVisit.push(incoming);
                }
            }
        }
        for(N state : reachableStates) {
            g.removeEdgeIf(state,e->!observable.contains(e.getValue()));
        }
        g.removeInitialEdgeIf(e->!observable.contains(e.getValue()));
    }


    default void mutateEdges(G g, Consumer<E> mutate){
        final N init = g.makeUniqueInitialState(null);
        collect(g,init,new HashSet<>(),s-> null,(source, edge)->{
            mutate.accept(edge);
            return null;
        });
        g.useStateOutgoingEdgesAsInitial(init);
    }

    interface InvertionErrorCallback<N, E, P, Out> {
        void doubleReflectionOnOutput(N vertex, E edge) throws CompilationError;

        void rangeWithoutReflection(N target, E edge) throws CompilationError;

        void epsilonTransitionCycle(N state, Out output, Out output1) throws CompilationError;

    }

    interface ConflictingAcceptingEdgesResolver<P, N> {
        P resolve(N sourceState, List<Pair<N, P>> conflictingFinalEdges) throws CompilationError;
    }



    /**Inverts transducer. Inputs become outputs and outputs become inputs. The transduction should be
     * a bijection or otherwise, inverse becomes nondeterministic and will fail {@link LexUnicodeSpecification#testDeterminism}*/
    default void inverse(G g, V initialStateMeta,
                         Function<In, Out> singletonOutput,
                         Function<Out, Iterator<In>> outputAsReversedInputSequence,
                         Function<P, Out> partialOutput,
                         Function<P, W> partialWeight,
                         ConflictingAcceptingEdgesResolver<P, N> aggregateConflictingFinalEdges,
                         InvertionErrorCallback<N, E, P, Out> error) throws CompilationError {
        class EpsilonEdge {

            final Out output;
            final W weight;

            public EpsilonEdge(Out output, W weight) {
                this.output = output;
                this.weight = weight;
            }

            public EpsilonEdge multiply(Out out, W weight) {
                return new EpsilonEdge(multiplyOutputs(output, out), multiplyWeights(this.weight, weight));
            }

            public E multiply(E edge) {
                return leftAction(createPartialEdge(output, weight), edge);
            }

            public P multiplyPartial(P edge) {
                return multiplyPartialEdges(createPartialEdge(output, weight), edge);
            }
        }
        final EpsilonEdge NEUTRAL = new EpsilonEdge(outputNeutralElement(), weightNeutralElement());
        final HashMap<N, P> reachableFinalEdges = new HashMap<>();
        class InvertedEdge {
            /**Every non-empty output string will be converted into path that accepts the same string.*/
            final N beginningOfInvertedPath;
            /**If null then it means that the path was created by inverting subsequential output, which naturally did
             * not have any target state in the original graph*/
            final N endOfInvertedPath;
            final E edgeIncomingToBeginningOfPath;

            InvertedEdge(N beginningOfInvertedPath, N endOfInvertedPath, E edgeIncomingToBeginningOfPath) {
                this.beginningOfInvertedPath = beginningOfInvertedPath;
                this.endOfInvertedPath = endOfInvertedPath;
                this.edgeIncomingToBeginningOfPath = edgeIncomingToBeginningOfPath;
            }
        }
        class TmpMeta {
            final N sourceState;

            final HashMap<N, EpsilonEdge> epsilonClosure = new HashMap<>();
            final ArrayList<InvertedEdge> invertedEdges = new ArrayList<>();

            /**If source state becomes accepting after invertion, then this is its final edge*/
            P invertedFinalEdge;

            TmpMeta(N sourceState) {
                this.sourceState = sourceState;
            }

            public void putEpsilon(Pair<N, EpsilonEdge> epsilonTransition) throws CompilationError {
                final N state = epsilonTransition.getFirst();
                final EpsilonEdge epsilonEdge = epsilonTransition.getSecond();
                final EpsilonEdge prev = epsilonClosure.put(state, epsilonEdge);
                if (prev != null) {
                    error.epsilonTransitionCycle(state, prev.output, epsilonEdge.output);
                    throw new IllegalStateException("epsilonTransitionCycle " + state);
                }
            }

            void putInvertedEdge(E edge, N target) throws CompilationError {
                final In from = fromExclusive(edge);
                final In to = toInclusive(edge);
                final Out out = output(edge);
                final V meta = g.getState(sourceState);
                final W w = weight(edge);
                final Iterator<In> symbols = outputAsReversedInputSequence.apply(out);
                if (symbols.hasNext()) {
                    N current = target;
                    E invertedEdge = null;
                    boolean hadMirrorOutput = false;
                    do {
                        if (invertedEdge != null) {
                            final N prev = g.create(meta);
                            g.add(prev, invertedEdge, current);
                            invertedEdge = null;//setting to null just for sanity
                            //but will most likely be optimised out by compiler
                            current = prev;
                        }
                        final In symbol = symbols.next();

                        if (Objects.equals(reflect(), symbol)) {
                            if (hadMirrorOutput) {
                                error.doubleReflectionOnOutput(sourceState, edge);// This should throw.
                                //The below exception should never normally fire.
                                throw new IllegalStateException("doubleReflectionOnOutput " + sourceState + " " + edge);
                            } else {
                                hadMirrorOutput = true;
                                assert Objects.equals(reflect(), symbol);
                                final Out symbolAsString = singletonOutput.apply(symbol);
                                invertedEdge = createFullEdge(from, to, partialOutputEdge(symbolAsString));
                            }
                        } else {
                            invertedEdge = createFullEdgeOverSymbol(symbol, partialNeutralEdge());
                        }
                        assert invertedEdge != null;
                        if (current == target) {
                            rightActionInPlace(invertedEdge, partialWeightedEdge(w));
                            assert Objects.equals(weight(invertedEdge), w) : invertedEdge + " " + edge;
                        }
                    } while (symbols.hasNext());
                    assert invertedEdge != null;
                    if (!hadMirrorOutput) {
                        assert Objects.equals(successor(from), to) : from + " " + to;
                        rightActionInPlace(invertedEdge, partialOutputEdge(singletonOutput.apply(to)));
                    }
                    invertedEdges.add(new InvertedEdge(current, target, invertedEdge));
                }
            }

            void putInvertedSubsequentialOutput(P edge) throws CompilationError {
                final Out out = partialOutput.apply(edge);
                final W w = partialWeight.apply(edge);
                final V meta = g.getState(sourceState);
                final Iterator<In> symbols = outputAsReversedInputSequence.apply(out);
                final P fin = partialWeightedEdge(w);
                N current = null;
                E invertedEdge = null;//is incoming to current state
                while (symbols.hasNext()) {
                    final In symbol = symbols.next();
                    if (Objects.equals(reflect(), symbol)) continue;
                    assert (current == null) == (invertedEdge == null) : current + " " + invertedEdge;
                    final N prev = g.create(meta);
                    if (current == null) {
                        reachableFinalEdges.put(prev, fin);
                    } else {
                        assert invertedEdge != null;
                        g.add(prev, invertedEdge, current);
                    }
                    invertedEdge = createFullEdgeOverSymbol(symbol, partialNeutralEdge());
                    current = prev;
                }
                if (current == null) {
                    invertedFinalEdge = fin;
                } else {
                    invertedEdges.add(new InvertedEdge(current, null, invertedEdge));
                }
            }
        }
        //collect all vertices. By the end of inversion, some of those vertices will no longer be reachable (which is ok)
        //and some new vertices will be added (whenever there is output string of length greater than 1)
        final HashMap<N, TmpMeta> vertices = new HashMap<>();
        g.collectVertices(v -> vertices.put(v, new TmpMeta(v)) == null, n -> null, (e, n) -> null);

        final N init = g.makeUniqueInitialState(initialStateMeta);
        final P epsilon = g.getEpsilon();
        if (epsilon != null) {
            g.setFinalEdge(init, epsilon);
        }
        vertices.put(init, new TmpMeta(init));
        //First we collect the epsilon closures
        for (Map.Entry<N, TmpMeta> vertexAndEpsilonClosure : vertices.entrySet()) {
            final Stack<Pair<N, EpsilonEdge>> epsilonClosure = new Stack<>();
            epsilonClosure.push(Pair.of(vertexAndEpsilonClosure.getKey(), NEUTRAL));//neutral loop epsilon transition is always assumed
            while (!epsilonClosure.isEmpty()) {
                final Pair<N, EpsilonEdge> epsilonTransition = epsilonClosure.pop();
                vertexAndEpsilonClosure.getValue().putEpsilon(epsilonTransition);
                for (Map.Entry<E, N> edgeTarget : (Iterable<Map.Entry<E, N>>) () -> g.iterator(epsilonTransition.getFirst())) {
                    final E edge = edgeTarget.getKey();
                    final N target = edgeTarget.getValue();
                    if (isEpsilonOutput(edge)) {
                        final In from = fromInclusive(edge);
                        final In to = toInclusive(edge);
                        if (Objects.equals(from, to)) {
                            epsilonClosure.push(Pair.of(target, epsilonTransition.getSecond().multiply(singletonOutput.apply(from), weight(edge))));
                        } else {
                            error.rangeWithoutReflection(target, edge);
                            throw new IllegalStateException("rangeWithoutReflection " + target + " " + edge);
                        }
                    }
                }
            }
        }
        //Second we invert all those transitions that have non-empty output and remove all those with empty output
        for (Map.Entry<N, TmpMeta> vertexAndMeta : vertices.entrySet()) {
            final N vertex = vertexAndMeta.getKey();
            final TmpMeta meta = vertexAndMeta.getValue();
            final Map<E, N> outgoing = g.outgoing(vertex);
            for (Map.Entry<E, N> edgeTarget : outgoing.entrySet()) {
                final E edge = edgeTarget.getKey();
                final N target = edgeTarget.getValue();
                meta.putInvertedEdge(edge, target);
            }
            outgoing.clear();
            final P fin = g.getFinalEdge(vertex);
            if (fin != null) {
                meta.putInvertedSubsequentialOutput(fin);
            }
        }
        //Lastly we add missing transitions with help of previously collected epsilon closures
        final Stack<N> toVisit = new Stack<>();
        toVisit.push(init);
        final HashSet<N> reachable = new HashSet<>();
        reachable.add(init);

        //Now it's time to do the actual inversion
        while (!toVisit.isEmpty()) {
            final N vertex = toVisit.pop();
            assert vertex != null;
            final TmpMeta meta = vertices.get(vertex);
            assert meta != null;
            assert g.outgoing(vertex).isEmpty();
            final ArrayList<Pair<N, P>> reachableFinalEdgesFromThisVertex = new ArrayList<>();
            for (Map.Entry<N, EpsilonEdge> intermediateStateAndEpsilonEdge : meta.epsilonClosure.entrySet()) {
                final N intermediateState = intermediateStateAndEpsilonEdge.getKey();
                final EpsilonEdge epsilonEdge = intermediateStateAndEpsilonEdge.getValue();
                final TmpMeta intermediateMeta = vertices.get(intermediateState);
                for (InvertedEdge edgeTarget : intermediateMeta.invertedEdges) {
                    g.add(vertex, epsilonEdge.multiply(edgeTarget.edgeIncomingToBeginningOfPath), edgeTarget.beginningOfInvertedPath);
                    if (edgeTarget.endOfInvertedPath != null && reachable.add(edgeTarget.endOfInvertedPath))
                        toVisit.push(edgeTarget.endOfInvertedPath);
                }
                assert intermediateState != vertex || Objects.equals(epsilonEdge.weight, weightNeutralElement()) && Objects.equals(epsilonEdge.output, outputNeutralElement()) : intermediateStateAndEpsilonEdge;
                if (intermediateMeta.invertedFinalEdge != null) {
                    reachableFinalEdgesFromThisVertex.add(Pair.of(intermediateState, epsilonEdge.multiplyPartial(intermediateMeta.invertedFinalEdge)));
                }
            }
            switch (reachableFinalEdgesFromThisVertex.size()) {
                case 0:
                    break;
                case 1: {
                    final P prev = reachableFinalEdges.put(vertex, reachableFinalEdgesFromThisVertex.get(0).getSecond());
                    assert prev == null : prev;
                    break;
                }
                default: {
                    final P prev = reachableFinalEdges.put(vertex, aggregateConflictingFinalEdges.resolve(vertex, reachableFinalEdgesFromThisVertex));
                    assert prev == null : prev;
                    break;
                }
            }
        }

        g.setEpsilon(reachableFinalEdges.remove(init));
        g.setFinalEdges(reachableFinalEdges);
        g.useStateOutgoingEdgesAsInitial(init);
        assert g.collectVertexSet(new HashSet<>(), x -> null, (e, n) -> null).containsAll(reachableFinalEdges.keySet()) : g.collectVertexSet(new HashSet<>(), x -> null, (e, n) -> null) + " " + reachableFinalEdges;
        assert !g.collectVertexSet(new HashSet<>(), x -> null, (e, n) -> null).contains(init) : g.collectVertexSet(new HashSet<>(), x -> null, (e, n) -> null) + " " + init;
        assert !reachableFinalEdges.containsKey(init) : reachableFinalEdges + " " + init;
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
                                                  Function<Out, ? extends Iterator<In>> outputAsString,
                                                  Function<P, ? extends Iterator<In>> finalStateOutputAsString) {
        return collectOutputProductDeterministic(lhs, rhs, lhs.initial, rhs.initial, collected, outputAsString,
                finalStateOutputAsString, (a, b) -> null, (a, b) -> !rhs.isAccepting(b) ? Pair.of(a, b) : null);
    }

    interface AmbiguityHandler<I, O, E extends Throwable> {
        O handleAmbiguity(I input, O firstOutput, O secondOutput) throws E;
    }

    /**
     * Loads dictionary of input and output pairs
     */
    default <E extends Throwable, Str extends Iterable<In>> G loadDict(
            Specification.NullTermIter<Pair<Str, Out>> dict,
            V state,
            AmbiguityHandler<Str, Out, E> ambiguityHandler) throws E {
        class Trie {
            final HashMap<In, Trie> children = new HashMap<>(1);
            Out value;

            void toGraph(G g, N n) {
                if (value != null) {
                    g.setFinalEdge(n, partialOutputEdge(value));
                }
                for (Map.Entry<In, Trie> entry : children.entrySet()) {
                    final N nextNode = g.create(state);
                    g.add(n, fullNeutralEdgeOverSymbol(entry.getKey()), nextNode);
                    entry.getValue().toGraph(g, nextNode);
                }
            }
        }
        final Trie root = new Trie();
        Pair<Str, Out> entry;
        while ((entry = dict.next()) != null) {
            if (entry.getSecond() == null) continue;
            Trie node = root;
            for (In symbol : entry.getFirst()) {
                final Trie parent = node;
                node = node.children.computeIfAbsent(symbol, k -> new Trie());
            }
            if (node.value == null) {
                node.value = entry.getSecond();
            } else if (!node.value.equals(entry.getSecond())) {
                ambiguityHandler.handleAmbiguity(entry.getFirst(), entry.getSecond(), node.value);
            }
        }

        final G g = createEmptyGraph();
        for (Map.Entry<In, Trie> initEntry : root.children.entrySet()) {
            final N init = g.create(state);
            initEntry.getValue().toGraph(g, init);
            g.addInitialEdge(init, fullNeutralEdgeOverSymbol(initEntry.getKey()));
        }
        return g;
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
