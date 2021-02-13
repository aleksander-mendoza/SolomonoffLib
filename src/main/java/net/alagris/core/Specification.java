package net.alagris.core;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.Collectors;

import static net.alagris.core.Pair.IntPair;

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
     * Partial edge constructor
     */
    P clonePartialEdge(P p);

    /**
     * Full edge constructor
     */
    E cloneFullEdge(E e);


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
     * The special value associated with the \0 symbol on output side. This symbol will be used
     * to reflect input during automaton evaluation
     */
    default In reflect() {
        return minimal();
    }


    P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

    P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;


    Seq<In> evaluate(RangedGraph<?, In, E, P> graph, Seq<In> input);

    void reduceEdges(V meta,RangedGraph<V, In, E, P> g) throws CompilationError;

    //////////////////////////
    // Below are default functions added for convenience
    /////////////////////////

    void testDeterminism(String name, RangedGraph<V, In, E, P> inOptimal) throws CompilationError;

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
        E e = fullNeutralEdge(range.l(), range.r());
        N n = empty.create(state);
        empty.addInitialEdge(n, e);
        empty.setFinalEdge(n, p);
        return empty;
    }

    /**
     * Singleton graph built from a single state that accepts a specified range of inputs
     */
    default G atomicRangesGraph(V state, NullTermIter<Pair<In, In>> range) {
        G empty = createEmptyGraph();
        P p = partialNeutralEdge();
        N n = empty.create(state);
        empty.setFinalEdge(n, p);
        Pair<In, In> r;
        while ((r = range.next()) != null) {
            E e = fullNeutralEdge(r.l(), r.r());
            empty.addInitialEdge(n, e);
        }
        return empty;
    }

    default G atomicEpsilonGraph() {
        return atomicEpsilonGraph(partialNeutralEdge());
    }

    /**
     * Empty graph with a single epsilon edge
     */
    default G atomicEpsilonGraph(P epsilonEdge) {
        G empty = createEmptyGraph();
        empty.setEpsilon(epsilonEdge);
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
    default <S extends Set<N>> S collect(boolean depthFirstSearch,
                                         G graph, N startpoint, S set,
                                         Function<N, Object> shouldContinuePerState,
                                         BiFunction<N, E, Object> shouldContinuePerEdge) {
        return SinglyLinkedGraph.collectSet(depthFirstSearch, graph, startpoint, set, shouldContinuePerState, shouldContinuePerEdge);
    }

    default HashSet<N> collect(boolean depthFirstSearch, G graph, N startpoint) {
        return collect(depthFirstSearch, graph, startpoint, new HashSet<>(), x -> null, (n, e) -> null);
    }

    public static class FunctionalityCounterexample<E, P, N> {
        public final LexUnicodeSpecification.BiBacktrackingNode trace;
        public final Specification.RangedGraph<Pos, Integer, E, P> g;
        @Override
        public String toString() {
            return strTrace();
        }

        public FunctionalityCounterexample(Specification.RangedGraph<Pos, Integer, E, P> g, LexUnicodeSpecification.BiBacktrackingNode trace) {
            this.g = g;
            this.trace = trace;
        }

        public String strTrace() {
            LexUnicodeSpecification.BiBacktrackingNode t = trace;
            if (t == null) return "";
            final StringBuilder sb = new StringBuilder();
            while (t.source != null) {
                final StringBuilder tmp = new StringBuilder();
                IntSeq.appendRange(tmp, t.fromExclusive + 1, t.toInclusive);
                t = t.source;
                sb.insert(0, tmp);
            }
            return sb.toString();
        }

        public String posTraceLeft() {
            LexUnicodeSpecification.BiBacktrackingNode t = trace;
            if (t == null) return "";
            final StringBuilder sb = new StringBuilder(g.state(t.lhsTargetState).toString());
            while (t.source != null) {
                t = t.source;
                sb.insert(0, g.state(t.lhsTargetState)+" -> ");
            }
            return sb.toString();
        }
        public String posTraceRight() {
            LexUnicodeSpecification.BiBacktrackingNode t = trace;
            if (t == null) return "";
            final StringBuilder sb = new StringBuilder(g.state(t.rhsTargetState).toString());
            while (t.source != null) {
                t = t.source;
                sb.insert(0, g.state(t.rhsTargetState)+" -> ");
            }
            return sb.toString();
        }
    }

    public static class FunctionalityCounterexampleFinal<E, P, N> extends FunctionalityCounterexample<E, P, N> {
        public final P finalEdgeA;
        public final P finalEdgeB;

        public FunctionalityCounterexampleFinal(Specification.RangedGraph<Pos, Integer, E, P> g, P finalEdgeA, P finalEdgeB, LexUnicodeSpecification.BiBacktrackingNode trace) {
            super(g, trace);
            this.finalEdgeA = finalEdgeA;
            this.finalEdgeB = finalEdgeB;
        }

        public String getMessage(Pos automatonPos) {
            return getMessage(automatonPos.toString());
        }
        public String getMessage(String automatonPos) {
            return "Automaton "+automatonPos+" contains weight conflicting final states "+finalEdgeA+" and "+finalEdgeB
                    +". Path "+posTraceLeft()+" conflicts with "+posTraceRight()+". Example of input "+strTrace();
        }
    }

    public static class FunctionalityCounterexampleToThirdState<E, P, N> extends FunctionalityCounterexample<E, P, N> {
        public final E overEdgeA;
        public final E overEdgeB;


        public FunctionalityCounterexampleToThirdState(Specification.RangedGraph<Pos, Integer, E, P> g, E overEdgeA, E overEdgeB, LexUnicodeSpecification.BiBacktrackingNode trace) {
            super(g, trace);
            this.overEdgeA = overEdgeA;
            this.overEdgeB = overEdgeB;
        }

        public String getMessage(Pos automatonPos) {
            return getMessage(automatonPos.toString());
        }
        public String getMessage(String automatonPos) {
            return "Automaton "+automatonPos+" contains weight conflicting transitions "+overEdgeA+" and "+overEdgeB+". Path "+posTraceLeft()+" conflicts with "+posTraceRight()
                    +". Example of input "+strTrace();
        }
    }

    FunctionalityCounterexample<E, P, V> isFunctional(RangedGraph<V, In, E, P> optimised, int startpoint);

    /**
     * @throws CompilationError if typechcking fails
     */
    public default void checkFunctionality(RangedGraph<V, In, E, P> g, Pos pos)
            throws CompilationError.WeightConflictingFinal, CompilationError.WeightConflictingToThirdState {
        final FunctionalityCounterexample<E, P, V> weightConflictingTranitions = isFunctional(g, g.initial);
        if (weightConflictingTranitions != null) {
            if (weightConflictingTranitions instanceof FunctionalityCounterexampleFinal) {
                FunctionalityCounterexampleFinal<E, P, ?> c = (FunctionalityCounterexampleFinal<E, P, ?>) weightConflictingTranitions;
                throw new CompilationError.WeightConflictingFinal(pos, c);
            } else {
                FunctionalityCounterexampleToThirdState<E, P, ?> c = (FunctionalityCounterexampleToThirdState<E, P, ?>) weightConflictingTranitions;
                throw new CompilationError.WeightConflictingToThirdState(pos, c);
            }
        }
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
        return mergeRangedEdges(graph.outgoing(vertex).iterator(), r -> fromExclusive(r.getKey()), r -> toInclusive(r.getKey()),
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
        return mergeRangedEdges(graph.outgoing(vertex).iterator(), r -> fromExclusive(r.getKey()), r -> toInclusive(r.getKey()),
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
        Util.removeDuplicates(points, (prev, curr) -> prev.i.equals(curr.i), (prev, curr) -> {
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
            public final E edge;
            public int targetState;

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

        public static class BiTrans<E> extends Trans<E> {
            int sourceState;

            BiTrans(int sourceState, Trans<E> tr) {
                this(sourceState, tr.edge, tr.targetState);
            }

            BiTrans(int sourceState, E edge, int targetState) {
                super(edge, targetState);
                this.sourceState = sourceState;
            }

            @Override
            public String toString() {
                return sourceState + "->" + edge + "->" + targetState;
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


    default List<Range<In, List<RangedGraph.Trans<E>>>> getTransOrSink(RangedGraph<?, In, E, P> g, int index) {
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
        return Util.singeltonArrayList(sinkRange(nullEdge));
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

    static class PowersetState {//powerset state
        /**
         * Sparse bitset can be represented as array of indices at which
         * true bits reside.
         */
        final int[] states;
        final int hash;

        public PowersetState(int... states) {
            this.states = states;
            hash = Arrays.hashCode(states);
            assert isStrictlyIncreasing(states) : Arrays.toString(states);
        }

        public <E> PowersetState(List<? extends RangedGraph.Trans<E>> transitions) {
            /**Sparse bitset is array of indices  of true bits. All permutation of those indices
             * still encode the same bitset. Hence we need to sort them in order to
             * fix one representative permutation. This will allow us to use PowersetState as
             * key in hashmap.*/
            transitions.sort(Comparator.comparingInt(a -> a.targetState));
            states = new int[Util.shiftDuplicates(transitions, (a, b) -> a.targetState == b.targetState, (a, b) -> {
            })];
            for (int i = 0; i < states.length; i++) {
                states[i] = transitions.get(i).targetState;
            }
            hash = Arrays.hashCode(states);
            assert isStrictlyIncreasing(states) : Arrays.toString(states);
        }

        @Override
        public boolean equals(Object o) {
            return Arrays.equals(states, ((PowersetState) o).states);
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

    static class IdxAndTrans<In, E, T> {
        /**
         * Index of the powerset state
         */
        final int index;
        /**
         * The transitions outgoing from powerset state
         */
        ArrayList<Range<In, List<RangedGraph.Trans<E>>>> dfaTrans;
        /**
         * All the original transitions outgoing from any of the original states
         * that make up the powerset state
         */
        ArrayList<Range<In, List<T>>> nfaTrans;

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

    /**
     * This is only used in assertions
     */
    public static <In, V, E, P, T extends RangedGraph.BiTrans<E>> boolean validate(IdxAndTrans<In, E, T> i, PowersetState ps,
                                                                                   RangedGraph<V, In, E, P> powersetGraph,
                                                                                   RangedGraph<V, In, E, P> original) {
        for (Range<In, List<T>> range : i.nfaTrans) {
            for (T tr : range.edges()) {
                boolean contains = false;
                for (int sourceState : ps.states) {
                    if (sourceState == tr.sourceState) {
                        contains = true;
                        break;
                    }
                }
                assert contains : tr + " " + ps + " " + i.nfaTrans + "\nORIGINAL=\n" + original + "\nPOWERSET=\n" + powersetGraph;
                if (!contains) return false;
                boolean containsEdge = false;
                for (Range<In, List<RangedGraph.Trans<E>>> originalRange : original.graph.get(tr.sourceState)) {
                    for (RangedGraph.Trans<E> originalTr : originalRange.edges()) {
                        if (originalTr.edge == tr.edge) {
                            containsEdge = true;
                        }
                    }
                }
                assert containsEdge : tr + " " + ps + " " + i.nfaTrans;
                if (!containsEdge) return false;
            }
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
    default RangedGraph<V, In, E, P> powerset(RangedGraph<V, In, E, P> g) {
        return powersetWithSuperstates(g, (i, e) -> e).r();
    }

    default <T extends RangedGraph.Trans<E>> Pair<HashMap<PowersetState, IdxAndTrans<In, E, T>>, RangedGraph<V, In, E, P>> powersetWithSuperstates(RangedGraph<V, In, E, P> g,
                                                                                                                                                   BiFunction<Integer, List<RangedGraph.Trans<E>>, List<T>> stateAndTransitionsToT) {


        final Stack<PowersetState> toVisit = new Stack<>();
        final HashMap<PowersetState, IdxAndTrans<In, E, T>> powersetStateToIndex = new HashMap<>();
        final PowersetState initPS = new PowersetState(g.initial);
        toVisit.push(initPS);
        powersetStateToIndex.put(initPS, new IdxAndTrans<In, E, T>(0));
        while (!toVisit.isEmpty()) {
            final PowersetState powersetState = toVisit.pop();
            final IdxAndTrans<In, E, T> source = powersetStateToIndex.get(powersetState);
            assert (source.nfaTrans == null) == (source.dfaTrans == null);
            if (source.dfaTrans != null) continue;//already visited
            final ArrayList<Range<In, List<T>>> powersetTrans =
                    powersetTransitions(g, powersetState.states, stateAndTransitionsToT);
            assert source.nfaTrans == null;
            source.nfaTrans = powersetTrans;
            source.dfaTrans = new ArrayList<>(powersetTrans.size());
            assert isFullSigmaCovered(powersetTrans);
            In beginExclusive = minimal();
            for (Range<In, List<T>> range : powersetTrans) {
                //Combine targets of all the edges into one powerset state target
                final PowersetState target = new PowersetState(range.edges());
                final In endInclusive = range.input();
                final E edge = fullNeutralEdge(beginExclusive, endInclusive);
                //Combine all the edges into one new edge. The output and weight is lost
                final List<RangedGraph.Trans<E>> trans;
                if (target.states.length == 0) {
                    trans = Collections.emptyList();
                } else {
                    final IdxAndTrans<In, E, T> targetIndex = powersetStateToIndex.computeIfAbsent(target, k -> new IdxAndTrans<>(powersetStateToIndex.size()));
                    if (targetIndex.dfaTrans == null)
                        toVisit.add(target);
                    trans = Util.singeltonArrayList(new RangedGraph.Trans<>(edge, targetIndex.index));
                }
                source.dfaTrans.add(new RangeImpl<>(range.input(), trans));
                beginExclusive = endInclusive;
            }
        }

        final ArrayList<ArrayList<Range<In, List<RangedGraph.Trans<E>>>>> graph = Util.filledArrayList(powersetStateToIndex.size(), null);
        final ArrayList<P> accepting = Util.filledArrayList(powersetStateToIndex.size(), null);
        for (Map.Entry<PowersetState, IdxAndTrans<In, E, T>> state : powersetStateToIndex.entrySet()) {
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
        final RangedGraph<V, In, E, P> out = new RangedGraph<>(graph, accepting,
                Util.filledArrayList(powersetStateToIndex.size(), null),
                0);
        assert out.isDeterministic() == null;
        return Pair.of(powersetStateToIndex, out);
    }

    public static <X> int indexOf(Iterable<X> list, Predicate<X> f) {
        int i = 0;
        for (X x : list)
            if (f.test(x)) return i;
            else i++;
        return -1;
    }

    default <T extends RangedGraph.Trans<E>> ArrayList<Range<In, List<T>>> powersetTransitions(RangedGraph<V, In, E, P> g, int[] states,
                                                                                               BiFunction<Integer, List<RangedGraph.Trans<E>>, List<T>> stateAndTransitionsToT) {
        assert isStrictlyIncreasing(states) : Arrays.toString(states);
        int stateIdx = 0;
        assert states.length > 0 : g.toString();
        final int firstState = states[stateIdx];
        NullTermIter<Range<In, List<T>>> transitions = NullTermIter.fromIterableMapped(g.graph.get(firstState), r -> new RangeImpl<>(r.input(), stateAndTransitionsToT.apply(firstState, r.edges())));
        for (stateIdx++; stateIdx < states.length; stateIdx++) {
            final int sourceState = states[stateIdx];
            transitions = zipTransitionRanges(transitions, NullTermIter.fromIterable(g.graph.get(sourceState)),
                    (fromExclusive, toInclusive, l, r) -> new RangeImpl<>(toInclusive, Util.lazyConcatImmutableLists(l, stateAndTransitionsToT.apply(sourceState, r))));
        }
        final ArrayList<Range<In, List<T>>> collected = new ArrayList<>();
        Range<In, List<T>> range;
        while ((range = transitions.next()) != null) {
            collected.add(new RangeImpl<>(range.input(), new ArrayList<>(range.edges())));
        }
        return collected;
    }

    default RangedGraph<V, In, E, P> optimiseGraph(G graph) {
        return optimiseGraph(graph, n -> null, (n, e) -> null);
    }

    default RangedGraph<V, In, E, P> optimiseGraph(G graph,
                                                   Function<N, Object> shouldContinuePerState,
                                                   BiFunction<N, E, Object> shouldContinuePerEdge) {
        final N initial = graph.makeUniqueInitialState(null);
        final HashSet<N> states = collect(true, graph, initial, new HashSet<>(), shouldContinuePerState, shouldContinuePerEdge);
        final ArrayList<ArrayList<Range<In, List<RangedGraph.Trans<E>>>>> graphTransitions = Util.filledArrayList(states.size(), null);
        final HashMap<N, Integer> stateToIndex = new HashMap<>(states.size());
        final ArrayList<V> indexToState = new ArrayList<>(states.size());
        final ArrayList<P> accepting = Util.filledArrayList(states.size(), null);
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
        return isFullSigmaCovered(transitions, Range::input);
    }

    default <M, R> boolean isFullSigmaCovered(List<R> transitions, Function<R, In> input) {
        assert !transitions.isEmpty();
        assert isStrictlyIncreasing(transitions, (a, b) -> compare(input.apply(a), input.apply(b))):transitions;
        assert !Objects.equals(input.apply(transitions.get(0)), minimal());
        assert Objects.equals(input.apply(transitions.get(transitions.size() - 1)), maximal());
        return true;
    }

    /**
     * @return the outgoing transition containing given input. Otherwise transition to sink state is returned.
     */
    default List<RangedGraph.Trans<E>> binarySearch(RangedGraph<?, In, E, P> graph, int state, In input) {
        final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> transitions = graph.graph.get(state);
        assert isFullSigmaCovered(transitions) : transitions + "\nstate=" + state + "\n" + graph;
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
                    for (L prevLhs : Util.listOrSingletonWithNull(l)) {
                        for (R prevRhs : Util.listOrSingletonWithNull(r)) {
                            final Y y = crossProduct.times(fromExclusive, toInclusive, prevLhs, prevRhs);
                            if (y != null) {
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
        Z times(In fromExclusive, In toInclusive, L lhsTran, R rhsTran);
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


    interface StateCollector<In, E, N> {
        /**
         * Return product of states that should be visited, or null if this product has already been visited before
         */
        N visit(In fromExclusive, In toInclusive, int targetLhs,
                RangedGraph.Trans<E> edgeLhs, int targetRhs, RangedGraph.Trans<E> edgeRhs,
                N source);
    }

    interface ShouldContinuePerEdge<In, N, E, Y> {
        Y shouldContinue(N source, In fromExclusive, In toInclusive, Map.Entry<E, Integer> lEdge, Map.Entry<E, Integer> rEdge);
    }

    interface ShouldContinuePerState<N, E, Y> {
        Y shouldContinue(N stateProduct);
    }

    default <Y> Y collectProductSet(boolean depthFirstSearch,
                                    RangedGraph<V, In, E, P> lhs,
                                    RangedGraph<V, In, E, P> rhs,
                                    int startpointLhs,
                                    int startpointRhs,
                                    Set<IntPair> collect,
                                    ShouldContinuePerEdge<In, IntPair, E, Y> shouldContinuePerEdge,
                                    ShouldContinuePerState<IntPair, E, Y> shouldContinuePerState) {
        return collectProduct(depthFirstSearch, lhs, rhs, startpointLhs, startpointRhs, (f, t, l, le, r, re, src) -> {
                    final IntPair p = new IntPair(l, r);
                    if (collect.add(p)) return p;
                    return null;
                },
                IntPair::getL, IntPair::getR,
                shouldContinuePerEdge, shouldContinuePerState);
    }

    default <Y, N> Y collectProduct(boolean depthFirstSearch,
                                    RangedGraph<V, In, E, P> lhs,
                                    RangedGraph<V, In, E, P> rhs,
                                    int startpointL,
                                    int startpointR,
                                    StateCollector<In, E, N> collect,
                                    Function<N, Integer> leftState,
                                    Function<N, Integer> rightState,
                                    ShouldContinuePerEdge<In, N, E, Y> shouldContinuePerEdge,
                                    ShouldContinuePerState<N, E, Y> shouldContinuePerState) {
        return collectProduct(depthFirstSearch, lhs, rhs, collect.visit(minimal(), minimal(), startpointL, null, startpointR, null, null),
                collect, leftState, rightState, shouldContinuePerEdge, shouldContinuePerState);
    }

    public interface InOut<N> {
        void in(N n);

        N out();

        boolean isEmpty();
    }

    public static class FIFO<N> extends LinkedList<N> implements InOut<N> {
        @Override
        public void in(N n) {
            add(n);
        }

        @Override
        public N out() {
            return poll();
        }
    }

    public static class FILO<N> extends Stack<N> implements InOut<N> {
        @Override
        public void in(N n) {
            push(n);
        }

        @Override
        public N out() {
            return pop();
        }
    }

    /**
     * Performs product of automata. Collects all reachable pairs of states.
     * A pair of transitions between pairs of states is taken only when their input ranges overlap with one another.
     *
     * @param collect                this function is callback that will receive all reached pairs of states.
     *                               It should remember states that were already seen and return new meta M when the pair is seen for the first time.
     *                               If a pair has already been registered, then null must be returned.
     * @param shouldContinuePerEdge  is invoked for every pair of traversed transitions. If non-null value is returned then
     *                               depth-first search terminates early and the function as a whole returns the obtained value.
     *                               This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of edges.
     * @param shouldContinuePerState is invoked for every pair of visited states. If non-null value is returned then
     *                               depth-first search terminates early and the function as a whole returns the
     *                               obtained value. This should be used when you don't really need to collect all reachable
     *                               states but only care about finding some particular pair of states.
     */
    default <Y, N> Y collectProduct(boolean depthFirstSearch,
                                    RangedGraph<?, In, E, P> lhs,
                                    RangedGraph<?, In, E, P> rhs,
                                    final N init,
                                    StateCollector<In, E, N> collect,
                                    Function<N, Integer> leftState,
                                    Function<N, Integer> rightState,
                                    ShouldContinuePerEdge<In, N, E, Y> shouldContinuePerEdge,
                                    ShouldContinuePerState<N, E, Y> shouldContinuePerState) {

        final InOut<N> toVisit = depthFirstSearch ? new FILO<>() : new FIFO<>();
        if (init != null) {
            final Y out = shouldContinuePerState.shouldContinue(init);
            if (out != null) return out;
            toVisit.in(init);
        }

        while (!toVisit.isEmpty()) {
            final N sourcePair = toVisit.out();
            final int l = leftState.apply(sourcePair);
            final int r = rightState.apply(sourcePair);
            Y y = crossProductOfTransitions(NullTermIter.fromIterable(getTransOrSink(lhs, l)),NullTermIter.fromIterable(getTransOrSink(rhs, r)),
                    (fromExclusive, toInclusive, prevLhs, prevRhs) -> {
                        final Y y2 = shouldContinuePerEdge.shouldContinue(sourcePair, fromExclusive, toInclusive, prevLhs, prevRhs);
                        if (y2 != null) return y2;
                        final int targetLhs = prevLhs == null ? -1 : prevLhs.targetState;
                        final int targetRhs = prevRhs == null ? -1 : prevRhs.targetState;
                        final N targetPair = collect.visit(fromExclusive, toInclusive, targetLhs, prevLhs, targetRhs, prevRhs, sourcePair);
                        if (targetPair != null) {
                            final Y out = shouldContinuePerState.shouldContinue(targetPair);
                            if (out != null) return out;
                            toVisit.in(targetPair);
                        }
                        return null;
                    });
            if (y != null) return y;
        }
        return null;
    }

    class AdvAndDelState<O, N extends Queue<O, N>> {
        final int leftState, rightState;
        final AdvAndDelState<O, N> prev;
        N outLeft, outRight;


        @Override
        public String toString() {
            return "AdvAndDelState{" +
                    "leftState=" + leftState +
                    ", rightState=" + rightState +
                    ", outLeft=" + outLeft +
                    ", outRight=" + outRight +
                    '}';
        }

        boolean isBalanceable() {
            return outLeft == null || outRight == null;
        }

        public AdvAndDelState(int leftState, int rightState) {
            this.leftState = leftState;
            this.rightState = rightState;
            prev = null;
        }

        public AdvAndDelState(int lState, int rState, N outputLeft, N outputRight, AdvAndDelState<O, N> prev, Supplier<N> make) {
            leftState = lState;
            rightState = rState;
            this.prev = prev;
            outLeft = Queue.copyAndConcat(prev.outLeft, outputLeft, make);
            outRight = Queue.copyAndConcat(prev.outRight, outputRight, make);
            while (outLeft != null && outRight != null && Objects.equals(outLeft.val(), outRight.val())) {
                outLeft = outLeft.next();
                outRight = outRight.next();
            }
        }
    }

    interface AdvAndDelAlreadyVisited<O, N extends Queue<O, N>> {
        boolean alreadyVisited(AdvAndDelState<O, N> srcState, AdvAndDelState<O, N> newState);
    }

    /**
     * Performs product of automata while also keeps track of advanced and delayed output.
     */
    default <Y, O, N extends Queue<O, N>> Y advanceAndDelay(RangedGraph<?, In, E, P> lhs,
                                                            RangedGraph<?, In, E, P> rhs,
                                                            int startpointLhs,
                                                            int startpointRhs,
                                                            AdvAndDelAlreadyVisited<O, N> alreadyVisited,
                                                            Function<E, N> edgeOutputQueue,
                                                            ShouldContinuePerEdge<In, AdvAndDelState<O, N>, E, Y> shouldContinuePerEdge,
                                                            ShouldContinuePerState<AdvAndDelState<O, N>, E, Y> shouldContinuePerState,
                                                            Supplier<N> make) {
        return collectProduct(true, lhs, rhs, new AdvAndDelState<O, N>(startpointLhs, startpointRhs), (f, t, lState, lEdge, rState, rEdge, srcState) -> {
                    if (srcState == null) return null;
                    final AdvAndDelState<O, N> next = new AdvAndDelState<>(lState, rState,
                            lEdge == null ? null : edgeOutputQueue.apply(lEdge.edge),
                            rEdge == null ? null : edgeOutputQueue.apply(rEdge.edge),
                            srcState, make);
                    return alreadyVisited.alreadyVisited(srcState, next) ? null : next;
                },
                s -> s.leftState,
                s -> s.rightState,
                shouldContinuePerEdge,
                shouldContinuePerState
        );
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
    default IntPair isSubset(boolean depthFirstSearch, RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                             int startpointLhs, int startpointRhs, Set<IntPair> collected) {
        return collectProductSet(depthFirstSearch, lhs, rhs, startpointLhs, startpointRhs, collected, (state, fromExclusive, toInclusive, a, b) -> null, (state) ->
                lhs.isAccepting(state.l) && !rhs.isAccepting(state.r) ? state : null
        );
    }

    /**
     * This function, unlike {@link Specification#isSubset} allows both arguments to be nondeterministic. However, it comes
     * at the cost of exponential time complexity (because powerset construction needs to be performed first)
     */
    default Pair<V, V> isSubsetNondeterministic(RangedGraph<V, In, E, P> lhs,
                                                RangedGraph<V, In, E, P> rhs) {
        final RangedGraph<V, In, E, P> dfa = powerset(rhs);
        final IntPair counterexample = isSubset(true, lhs, dfa, lhs.initial, dfa.initial, new HashSet<>());
        return counterexample == null ? null : Pair.of(lhs.state(counterexample.l), dfa.state(counterexample.r));
    }

    /**
     * Implements delta function with the assumption that the underlying graph is deterministic.
     * The automaton may be partial and hence this function may return null.
     */
    default N deterministicDelta(G graph, N startpoint, In input) {
        for (Map.Entry<E, N> init : graph.outgoing(startpoint)) {
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

        assert Util.find(rhs.graph, t -> !isFullSigmaCovered(t)) == null : rhs + " " + Util.find(rhs.graph, t -> !isFullSigmaCovered(t));
        final Stack<Pair<Integer, Integer>> pairsToVisit = new Stack<>();

        final Pair<Integer, Integer> pairStartpoint = Pair.of(startpointLhs, startpointRhs);
        if (visited.add(pairStartpoint)) {
            final Y y = shouldContinuePerState.apply(startpointLhs, startpointRhs);
            if (y != null) return y;
            pairsToVisit.push(pairStartpoint);
        }
        while (!pairsToVisit.isEmpty()) {
            final Pair<Integer, Integer> pair = pairsToVisit.pop();
            final int l = pair.l();
            final int r = pair.r();
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
                for (RangedGraph.Trans<E> tran : Util.listOrSingletonWithNull(entryLhs.edges())) {
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

    /**
     * Composes the output of some edge of left transducer with the input of right transducer
     */
    default <P2> HashMap<Integer, ArrayList<Range<In, P2>>> composedMirroredOutputDelta(RangedGraph<V, In, E, P> rhs,
                                                                                        HashMap<Integer, ArrayList<Range<In, P2>>> rhsStartpointStates,
                                                                                        ComposeSuperposition<In, E, P2> compose,
                                                                                        P2 nullEdge,
                                                                                        BiFunction<P2, P2, P2> union,
                                                                                        Iterator<In> lhsOutput) {
        HashMap<Integer, ArrayList<Range<In, P2>>> swap = new HashMap<>();
        assert Util.find(rhsStartpointStates.values(), t -> !isFullSigmaCovered(t)) == null : rhsStartpointStates + " " + Util.find(rhsStartpointStates.values(), t -> !isFullSigmaCovered(t));
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
                        final NullTermIter<Object> zipped = zipTransitionRanges(NullTermIter.fromIterable(superposition), NullTermIter.fromIterable(outgoing),
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
                                    return NullTermIter.collect(zipTransitionRanges(NullTermIter.fromIterable(prevSuperposition), NullTermIter.fromIterable(composedSuperposition),
                                            (fromExclusive, toInclusive, l, r) -> new RangeImpl<>(toInclusive, union.apply(l, r))),
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

    interface EdgeProduct<In, E> {
        E product(In fromExclusive, In toInclusive, E lEdge, E rEdge);
    }

    interface StateProduct<N> {
        int left();

        int right();

        N product();
    }

    /**
     * Performs product of two automata. Take note that it's a quite heavyweight operation.
     * The resulting transducer might be of quadratic size in pessimistic case.
     */
    default Pair<G, HashMap<IntPair, ? extends StateProduct<N>>> product(RangedGraph<V, In, E, P> lhs, RangedGraph<V, In, E, P> rhs,
                                                                         BiFunction<V, V, V> metaProduct,
                                                                         EdgeProduct<In, E> edgeProduct,
                                                                         BiFunction<P, P, P> outputProduct) {
        final G product = createEmptyGraph();
        final boolean omitSinkState = null == outputProduct.apply(lhs.getFinalEdge(-1), rhs.getFinalEdge(-1));
        class LRProduct implements StateProduct<N> {
            final int l, r;
            final N p;
            boolean visited = false;

            LRProduct(int l, int r) {
                this.l = l;
                this.r = r;
                this.p = product.create(metaProduct.apply(lhs.state(l), rhs.state(r)));
                assert !(omitSinkState && l == -1 && r == -1);
                final P fin = outputProduct.apply(lhs.getFinalEdge(l), rhs.getFinalEdge(r));
                if (fin != null) product.setFinalEdge(p, fin);
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

        collectProduct(true, lhs, rhs, lhs.initial, rhs.initial, (In from, In to, int lState, RangedGraph.Trans<E> lEdge,
                                                                  int rState, RangedGraph.Trans<E> rEdge, LRProduct source) -> {
            if (omitSinkState && lState == -1 && rState == -1) return null;
            final LRProduct productState = crossProductToNew.computeIfAbsent(new IntPair(lState, rState), k -> new LRProduct(lState, rState));
            if (productState.visited) return null;
            productState.visited = true;
            return productState;
        }, (LRProduct o) -> o.l, (LRProduct o) -> o.r, (source, fromExclusive, toInclusive, lEdge, rEdge) -> {
            final E edgeP = edgeProduct.product(fromExclusive, toInclusive, lEdge == null ? null : lEdge.getKey(), rEdge == null ? null : rEdge.getKey());
            if (edgeP != null) {
                final int tL = lEdge == null ? -1 : lEdge.getValue();
                final int tR = rEdge == null ? -1 : rEdge.getValue();
                if (omitSinkState && tL == -1 && tR == -1) return null;
                final LRProduct target = crossProductToNew.computeIfAbsent(new IntPair(tL, tR), k -> new LRProduct(tL, tR));
                product.add(source.p, edgeP, target.p);
            }
            return null;
        }, stateProduct -> null);
        final N init = crossProductToNew.get(new IntPair(lhs.initial, rhs.initial)).p;
        product.useStateOutgoingEdgesAsInitial(init);
        product.setEpsilon(product.removeFinalEdge(init));
        return Pair.of(product, crossProductToNew);
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
                    composed.setFinalEdge(lrc.composed, createPartialEdge(outputR.r(), outputR.l()));
                }
            }
            for (Map.Entry<E, N> edgeTargetL : lhs.outgoing(lrc.l)) {
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
                                    createPartialEdge(range.edges().r(),
                                            composeLhsAndRhsEdgeWeights.apply(weightL, range.edges().l())));
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

    default void trim(G g, N initial) {
        final HashSet<N> states = collect(true, g, initial);
        trim(g, states, states::contains);
    }

    /**
     * Normally all transducers are guaranteed to be trim by most of the operations.
     * However, a few operations may leave the transducer in a non-trim state (some states
     * are dead-ends). This procedure will trim the transducer.
     */
    default void trim(G g, Iterable<N> reachableStates, Predicate<N> isReachable) {
        g.removeFinalEdgeIf(e -> !isReachable.test(e.getKey()));
        for (N state : reachableStates) {
            g.setColor(state, new HashSet<N>());//reverse
        }
        for (N state : reachableStates) {
            for (Map.Entry<E, N> edge : g.outgoing(state)) {
                final HashSet<N> reversed = (HashSet<N>) g.getColor(edge.getValue());
                reversed.add(state);
            }
        }
        /**observable states are those that are reachable in the reversed transducer*/
        final HashSet<N> observable = new HashSet<>();
        final Stack<N> toVisit = new Stack<>();
        for (Map.Entry<N, P> fin : (Iterable<Map.Entry<N, P>>) () -> g.iterateFinalEdges()) {
            toVisit.push(fin.getKey());
            observable.add(fin.getKey());
        }
        while (!toVisit.isEmpty()) {
            final N state = toVisit.pop();
            final HashSet<N> reversed = (HashSet<N>) g.getColor(state);
            for (N incoming : reversed) {
                if (observable.add(incoming)) {
                    toVisit.push(incoming);
                }
            }
        }
        for (N state : reachableStates) {
            g.setColor(state, null);
            g.removeEdgeIf(state, e -> !observable.contains(e.getValue()));
        }
        g.removeInitialEdgeIf(e -> !observable.contains(e.getValue()));
    }


    default void mutateEdges(G g, Consumer<E> mutate) {
        final N init = g.makeUniqueInitialState(null);
        collect(true, g, init, new HashSet<>(), s -> null, (source, edge) -> {
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


    /**
     * Inverts transducer. Inputs become outputs and outputs become inputs. The transduction should be
     * a bijection or otherwise, inverse becomes nondeterministic and will fail {@link LexUnicodeSpecification#testDeterminism}
     */
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

            /**If source state becomes accepting after inversion, then this is its final edge*/
            P invertedFinalEdge;

            TmpMeta(N sourceState) {
                this.sourceState = sourceState;
            }

            public void putEpsilon(Pair<N, EpsilonEdge> epsilonTransition) throws CompilationError {
                final N state = epsilonTransition.l();
                final EpsilonEdge epsilonEdge = epsilonTransition.r();
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
        g.collectVertices(true, v -> vertices.put(v, new TmpMeta(v)) == null, n -> null, (e, n) -> null);

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
                for (Map.Entry<E, N> edgeTarget : g.outgoing(epsilonTransition.l())) {
                    final E edge = edgeTarget.getKey();
                    final N target = edgeTarget.getValue();
                    if (isEpsilonOutput(edge)) {
                        final In from = fromInclusive(edge);
                        final In to = toInclusive(edge);
                        if (Objects.equals(from, to)) {
                            epsilonClosure.push(Pair.of(target, epsilonTransition.r().multiply(singletonOutput.apply(from), weight(edge))));
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
            final Collection<Entry<E, N>> outgoing = g.outgoing(vertex);
            for (Map.Entry<E, N> edgeTarget : outgoing) {
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
                    final P prev = reachableFinalEdges.put(vertex, reachableFinalEdgesFromThisVertex.get(0).r());
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
            NullTermIter<Pair<Str, Out>> dict,
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
            if (entry.r() == null) continue;
            Trie node = root;
            for (In symbol : entry.l()) {
                final Trie parent = node;
                node = node.children.computeIfAbsent(symbol, k -> new Trie());
            }
            if (node.value == null) {
                node.value = entry.r();
            } else if (!node.value.equals(entry.r())) {
                ambiguityHandler.handleAmbiguity(entry.l(), entry.r(), node.value);
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

    interface SymbolGenerator<In> {
        In gen();
    }

    interface EdgeGenerator<E, In> {
        E genEdge(In fromExclusive, In toInclusive);
    }

    interface StateGenerator<P, V> {
        /**
         * Generates pair of state meta-data V and state finite edge P. Final edge may be
         * null to represent non-accepting states
         */
        Pair<P, V> genState();
    }

    default G randomDeterministic(int stateCount, int maxRangesCount, double partialityFactor,
                                  SymbolGenerator<In> symbolGen,
                                  EdgeGenerator<E, In> edgeGen,
                                  StateGenerator<P, V> stateGen,
                                  Random rnd) {
        return randomDeterministic(stateCount, maxRangesCount, partialityFactor, () -> {
            final int rangeCount = rnd.nextInt(maxRangesCount);
            final ArrayList<In> ranges = new ArrayList<>(rangeCount);
            for (int j = 0; j < rangeCount; j++) {
                ranges.add(symbolGen.gen());
            }
            ranges.sort(this::compare);
            Util.removeDuplicates(ranges, (a, b) -> compare(a, b) == 0, (a, b) -> {
            });
            if (ranges.isEmpty() || !Objects.equals(ranges.get(ranges.size() - 1), maximal())) {
                ranges.add(maximal());
            }
            return ranges;
        }, edgeGen, stateGen, rnd);
    }

    /**
     * Generates random automaton that ensures determinism. It does not ensure that the graph is trim.
     * Some of the edges might be unreachable and some might be dead-ends. After performing trimming operation,
     * the graph might have less states than requested.
     *
     * @param stateCount       - generated transducer will have less or equal number of states than the value specified in this parameter.
     * @param maxRangesCount   - the higher the value, the smaller ranges will be used as edge input labels.
     * @param partialityFactor - the higher value the more likely it will be that some edge is missing
     *                         (a.k.a leads to sink state). Set to zero if you wish to generate total automata.
     *                         If you set it too large, the automata might have barely any edges. It's ok to keep
     *                         it around 0.1 or 0.2
     */
    default G randomDeterministic(int stateCount, int maxRangesCount, double partialityFactor,
                                  Supplier<List<In>> rangesGen,
                                  EdgeGenerator<E, In> edgeGen,
                                  StateGenerator<P, V> stateGen,
                                  Random rnd) {
        final int statesPlusInitial = stateCount + 1;
        assert statesPlusInitial > 0;
        final G g = createEmptyGraph();
        final ArrayList<N> states = new ArrayList<>(statesPlusInitial);
        for (int i = 0; i < statesPlusInitial; i++) {
            final Pair<P, V> pv = stateGen.genState();
            final N n = g.create(pv.r());
            final P fin = pv.l();
            if (fin != null) g.setFinalEdge(n, fin);
            states.add(n);
        }
        for (N sourceState : states) {
            final List<In> ranges = rangesGen.get();
            assert isFullSigmaCovered(ranges, i -> i) : ranges + " " + minimal() + " " + maximal();
            In fromExclusive = minimal();
            for (In toInclusive : ranges) {
                assert compare(fromExclusive, toInclusive) < 0 : fromExclusive + " " + toInclusive + " " + ranges;
                final int marginThatLeadsToSink = (int) (stateCount * partialityFactor);
                final int targetStateIdx = 1 + rnd.nextInt(stateCount + marginThatLeadsToSink);
                assert targetStateIdx > 0;
                if (targetStateIdx < statesPlusInitial) {
                    final N targetState = states.get(targetStateIdx);
                    g.add(sourceState, edgeGen.genEdge(fromExclusive, toInclusive), targetState);
                }
                fromExclusive = toInclusive;
            }
        }
        final N init = states.get(0);
        trim(g, init);
        g.useStateOutgoingEdgesAsInitial(init);
        g.setEpsilon(g.removeFinalEdge(init));
        return g;
    }

    /**
     * Compares two unweighted deterministic trim transducers
     * and checks if they are equivalent.
     * Returns a counterexample of two states that violate equivalence.
     * A pair of states violates equivalence if 1. their advanced and delayed outputs
     * are not balanceable or 2. one state from pair is accepting while the other one is not
     * or 3. both are accepting but have unbalanced output or 4. tha same pair of states
     * could be reached twice with different advanced-and-delayed outputs. Determinism is necessary to ensure that
     * second case indeed violates equivalence.
     */
    default <O, N extends Queue<O, N>> AdvAndDelState<O, N> areEquivalent(RangedGraph<?, In, E, P> lhs,
                                                                          RangedGraph<?, In, E, P> rhs,
                                                                          Function<E, N> edgeOutputQueue,
                                                                          Function<P, N> finEdgeOutputQueue,
                                                                          Supplier<N> make) {
        assert lhs.isDeterministic() == null : lhs;
        assert rhs.isDeterministic() == null : rhs;
        final HashMap<IntPair, AdvAndDelState<O, N>> visited = new HashMap<>();
        return advanceAndDelay(lhs, rhs, lhs.initial, rhs.initial, (srcState, newState) -> {
                    assert srcState.isBalanceable() : srcState + " " + newState;
                    if (newState.rightState == -1 || newState.leftState == -1) return true;
                    final AdvAndDelState<O, N> prev = visited.putIfAbsent(Pair.of(newState.leftState, newState.rightState), newState);
                    if (prev == null) return false;
                    assert newState.rightState == prev.rightState;
                    assert newState.leftState == prev.leftState;
                    return Queue.equals(prev.outRight, newState.outRight) && Queue.equals(prev.outLeft, newState.outLeft);
                }, edgeOutputQueue, (source, fromExclusive, toInclusive, lEdge, rEdge) -> null,
                s -> {
                    if (s.leftState == -1 || s.rightState == -1) return null;//automaton is trim,
                    //so sink state is the only state that will never accept
                    if (!s.isBalanceable()) return s;
                    final P finL = lhs.getFinalEdge(s.leftState);
                    final P finR = rhs.getFinalEdge(s.rightState);
                    if (finL == null) {
                        if (finR == null) {
                            //pass
                        } else {
                            return s;
                        }
                    } else {
                        if (finR == null) {
                            return s;
                        } else {
                            if (!new AdvAndDelState<>(-1, -1, finEdgeOutputQueue.apply(finL), finEdgeOutputQueue.apply(finR), s, make).isBalanceable()) {
                                return s;
                            }
                        }
                    }
                    final AdvAndDelState<O, N> prev = visited.get(Pair.of(s.leftState, s.rightState));
                    if (prev != null) {
                        if (Queue.equals(prev.outRight, s.outRight) && Queue.equals(prev.outLeft, s.outLeft)) {
                            return null;
                        } else {
                            return s;
                        }
                    } else {
                        return null;
                    }
                }, make);
    }

    default <Q,T,V> RangedGraph<V, In, E, P> convertCustomGraphToRanged(CustomGraph<Q,T,E,P,V> c,Function<E,In> everyEdgeCoversSingleSymbol) {
        final Stack<Q> toVisit = new Stack<>();
        toVisit.push(c.init());
        final LinkedHashMap<Q, Integer> visited = new LinkedHashMap<>();
        visited.put(c.init(), 0);
        while (!toVisit.isEmpty()) {
            final Q state = toVisit.pop();
            final Iterator<T> transitions = c.outgoing(state);
            while (transitions.hasNext()) {
                final T transition = transitions.next();
                if (transition != null) {
                    final Q targetQ = c.target(state,transition);
                    if(!visited.containsKey(targetQ)){
                        visited.put(targetQ, visited.size());
                        toVisit.push(targetQ);
                    }
                }
            }
        }
        final ArrayList<ArrayList<Range<In, List<RangedGraph.Trans<E>>>>> graph = new ArrayList<>(visited.size());
        final ArrayList<P> accepting = new ArrayList<>(visited.size());
        final ArrayList<V> metaVars = new ArrayList<>(visited.size());
        for (Map.Entry<Q, Integer> p : visited.entrySet()) {
            final Q state = p.getKey();
            final int source = p.getValue();
            assert source == accepting.size();
            assert source == graph.size();
            metaVars.add(c.meta(state));
            final P fin = c.stateOutput(state);
            if (fin!=null) {
                accepting.add(fin);
            } else {
                accepting.add(null);
            }

            final ArrayList<Range<In, List<RangedGraph.Trans<E>>>> ranges = new ArrayList<>();
            final Iterator<T> transitions = c.outgoing(state);
            while (transitions.hasNext()) {
                final T transition = transitions.next();
                if (transition != null) {
                    final E e = c.edge(state,transition);
                    final Q targetQ = c.target(state,transition);
                    final int target = visited.get(targetQ);
                    final In in = everyEdgeCoversSingleSymbol.apply(e);
                    final RangedGraph.Trans<E> tr = new RangedGraph.Trans<>(e, target);
                    final Range<In, List<RangedGraph.Trans<E>>> range = new RangeImpl<>(in,
                            Util.singeltonArrayList(tr));
                    ranges.add(range);
                }
            }
            if (ranges.isEmpty() || !ranges.get(ranges.size() - 1).input().equals(maximal())) {
                ranges.add(new RangeImpl<>(maximal(), Util.filledArrayList(0, null)));
            }
            assert isFullSigmaCovered(ranges) : ranges;
            graph.add(ranges);

        }
        return new Specification.RangedGraph<>(graph, accepting, metaVars, 0);
    }


    default <Q,T> G convertCustomGraphToIntermediate(CustomGraph<Q,T,E,P,V> c) {
        final G g = createEmptyGraph();
        final Stack<Q> toVisit = new Stack<>();
        toVisit.push(c.init());
        final LinkedHashMap<Q, N> visited = new LinkedHashMap<>();
        final N initN = g.create(c.meta(c.init()));
        visited.put(c.init(), initN);
        while (!toVisit.isEmpty()) {
            final Q state = toVisit.pop();
            final N n = visited.get(state);
            final P fin = c.stateOutput(state);
            if (fin!=null) {
                g.setFinalEdge(n, fin);
            }
            final Iterator<T> transitions = c.outgoing(state);
            while(transitions.hasNext()) {
                final T transition = transitions.next();
                if (transition != null) {
                    final Q targetQ = c.target(state,transition);
                    final N targetN;
                    if (visited.containsKey(targetQ)) {
                        targetN = visited.get(targetQ);
                    } else {
                        targetN = g.create(c.meta(targetQ));
                        visited.put(targetQ, targetN);
                        toVisit.push(targetQ);
                    }
                    g.add(n, c.edge(state,transition), targetN);
                }
            }
        }
        g.useStateOutgoingEdgesAsInitial(initN);
        return g;
    }

    interface CustomGraph<Q,T,E,P,V>{
        Q init();
        P stateOutput(Q state);
        Iterator<T> outgoing(Q state);
        Q target(Q state, T transition);
        E edge(Q state, T transition);
        V meta(Q state);
    }


}
