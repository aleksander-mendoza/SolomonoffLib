package net.alagris;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.alagris.Pair.IntPair;
import net.alagris.LexUnicodeSpecification.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Specification for ranged transducer over integers (unicode) values and with
 * arctic lexicographic semiring over integers
 */
public abstract class LexUnicodeSpecification<N, G extends IntermediateGraph<Pos, E, P, N>>
        implements Specification<Pos, E, P, Integer, IntSeq, Integer, N, G>,
        ParseSpecs<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> {

    private final boolean eagerMinimisation;
    private final HashMap<String, ExternalFunction<G>> externalFunc = new HashMap<>();
    private final HashMap<String, ExternalOperation<G>> externalOp = new HashMap<>();
    private final ExternalPipelineFunction externalPipelineFunction;
    public final HashMap<String, Var<N, G>> variableAssignments = new HashMap<>();
    private final HashMap<String, LexPipeline<N, G>> pipelines = new HashMap<>();


    public static class Var<N, G extends IntermediateGraph<Pos, E, P, N>> {
        public final G graph;
        public final String name;
        public final Pos pos;
        /**
         * If true, then exponential operation !! will always be implicitly assumed for this variable.
         */
        public final boolean alwaysCopy;
        private Specification.RangedGraph<Pos, Integer, E, P> optimal;

        public Specification.RangedGraph<Pos, Integer, E, P> getOptimal() {
            return optimal;
        }

        public Var(G graph, String name, Pos pos, boolean alwaysCopy) {
            this.graph = graph;
            this.name = name;
            this.pos = pos;
            this.alwaysCopy = alwaysCopy;
        }

        @Override
        public String toString() {
            return "Var{" +
                    "graph=" + graph +
                    ", name='" + name + '\'' +
                    ", pos=" + pos +
                    '}';
        }
    }


    @Override
    public Integer successor(Integer integer) {
        if (integer == Integer.MAX_VALUE)
            throw new IllegalArgumentException("No successor for max value");
        return integer + 1;
    }

    public Integer predecessor(Integer integer) {
        if (integer == 0)
            throw new IllegalArgumentException("No predecessor for min value");
        return integer - 1;
    }

    public interface ExternalPipelineFunction {
        Function<IntSeq, IntSeq> make(String funcName, List<Pair<IntSeq, IntSeq>> args);
    }

    /**
     * @param eagerMinimisation This will cause automata to be minimized as soon as
     *                          they are parsed/registered (that is, the
     *                          {@link LexUnicodeSpecification#pseudoMinimize} will
     *                          be automatically called from
     *                          {@link LexUnicodeSpecification#introduceVariable})
     */
    public LexUnicodeSpecification(boolean eagerMinimisation, ExternalPipelineFunction externalPipelineFunction) {

        this.eagerMinimisation = eagerMinimisation;
        this.externalPipelineFunction = externalPipelineFunction;
    }

    @Override
    public G externalFunction(Pos pos, String functionName, List<Pair<IntSeq, IntSeq>> args) throws CompilationError {
        final ExternalFunction<G> f = externalFunc.get(functionName);
        if (f == null)
            throw new CompilationError.UndefinedExternalFunc(functionName, pos);
        return f.call(pos, args);
    }

    @Override
    public G externalOperation(Pos pos, String functionName, List<G> args) throws CompilationError {
        final ExternalOperation<G> f = externalOp.get(functionName);
        if (f == null)
            throw new CompilationError.UndefinedExternalFunc(functionName, pos);
        return f.call(pos, args);
    }

    @Override
    public G getGraph(Var<N, G> variable) {
        return variable.graph;
    }

    @Override
    public Pos getDefinitionPos(Var<N, G> variable) {
        return variable.pos;
    }

    @Override
    public String getName(Var<N, G> variable) {
        return variable.name;
    }

    @Override
    public RangedGraph<Pos, Integer, E, P> getOptimised(Var<N, G> variable) throws CompilationError.WeightConflictingToThirdState {
        if (variable.optimal == null) {
            variable.optimal = optimiseGraph(variable.graph);
            reduceEdges(variable.optimal);
        }
        return variable.optimal;
    }

    @Override
    public void typecheckFunction(Pos typePos, String name, G in, G out) throws CompilationError {
        final Var<N, G> meta = borrowVariable(name);
        if (meta == null) throw new CompilationError.MissingFunction(typePos, name);
        final Pos graphPos = meta.pos;
        final RangedGraph<Pos, Integer, E, P> graph = getOptimised(meta);
        final RangedGraph<Pos, Integer, E, P> inOptimal = optimiseGraph(in);
        testDeterminism(name, inOptimal);
        final RangedGraph<Pos, Integer, E, P> outOptimal = optimiseGraph(out);
        testDeterminism(name, outOptimal);
        final Pair<Pos, Pos> counterexampleIn = isSubsetNondeterministic(inOptimal, graph, this::successor,
                this::predecessor);
        if (counterexampleIn != null) {
            throw new CompilationError.TypecheckException(counterexampleIn.r(), counterexampleIn.l(),
                    name);
        }
        final Pair<Integer, Integer> counterexampleOut = isOutputSubset(graph, outOptimal);
        if (counterexampleOut != null) {
            throw new CompilationError.TypecheckException(graphPos, typePos, name);
        }


    }

    @Override
    public void typecheckProduct(Pos typePos, String name, G in, G out) throws CompilationError {
        final Var<N, G> meta = borrowVariable(name);
        if (meta == null) throw new CompilationError.MissingFunction(typePos, name);
        final Pos graphPos = meta.pos;
        final RangedGraph<Pos, Integer, E, P> graph = getOptimised(meta);
        final RangedGraph<Pos, Integer, E, P> inOptimal = optimiseGraph(in);
        testDeterminism(name, inOptimal);
        final RangedGraph<Pos, Integer, E, P> outOptimal = optimiseGraph(out);
        testDeterminism(name, outOptimal);
        final IntPair counterexampleIn = isSubset(graph, inOptimal, graph.initial, inOptimal.initial,
                new HashSet<>());
        if (counterexampleIn != null) {
            throw new CompilationError.TypecheckException(graph.state(counterexampleIn.l),
                    inOptimal.state(counterexampleIn.r), name);
        }
        final Pair<Integer, Integer> counterexampleOut = isOutputSubset(graph, outOptimal);
        if (counterexampleOut != null) {
            throw new CompilationError.TypecheckException(graphPos, typePos, name);
        }
    }

    interface ExternalFunction<G> {
        G call(Pos pos, List<Pair<IntSeq, IntSeq>> text) throws CompilationError;
    }

    interface ExternalOperation<G> {
        G call(Pos pos, List<G> text) throws CompilationError;
    }

    /**
     * returns previously registered function
     */
    public ExternalFunction<G> registerExternalFunction(String name, ExternalFunction<G> f) {
        return externalFunc.put(name, f);
    }

    /**
     * returns previously registered function
     */
    public ExternalOperation<G> registerExternalOperation(String name, ExternalOperation<G> f) {
        return externalOp.put(name, f);
    }


    @Override
    public final Integer multiplyWeights(Integer lhs, Integer rhs) {
        return lhs + rhs;
    }

    @Override
    public final IntSeq multiplyOutputs(IntSeq lhs, IntSeq rhs) {
        return lhs == null ? null : (rhs == null ? null : lhs.concat(rhs));
    }

    @Override
    public Integer weightNeutralElement() {
        return 0;
    }

    @Override
    public final IntSeq outputNeutralElement() {
        return IntSeq.Epsilon;
    }

    @Override
    public final IntSeq output(E edge) {
        return edge.out;
    }

    @Override
    public final Integer weight(E edge) {
        return edge.weight;
    }

    @Override
    public final P multiplyPartialEdges(P edge, P edge2) {
        return new P(multiplyOutputs(edge.out, edge2.out), multiplyWeights(edge.weight, edge2.weight));
    }

    @Override
    public final P createPartialEdge(IntSeq s, Integer weight) {
        return new P(s, weight);
    }

    @Override
    public final E createFullEdge(Integer from, Integer to, P partialEdge) {
        return new E(from, to, partialEdge.out, partialEdge.weight);
    }

    @Override
    public E createFullEdgeOverSymbol(Integer symbol, P partialEdge) {
        assert symbol > 0;
        return new E(symbol - 1, symbol, partialEdge.out, partialEdge.weight);
    }

    @Override
    public final E leftAction(P edge, E edge2) {
        return new E(edge2.fromExclusive, edge2.toInclusive, multiplyOutputs(edge.out, edge2.out),
                multiplyWeights(edge.weight, edge2.weight));
    }

    @Override
    public void leftActionInPlace(P edge, E edge2) {
        edge2.out = multiplyOutputs(edge.out, edge2.out);
        edge2.weight = multiplyWeights(edge.weight, edge2.weight);
    }

    @Override
    public final E rightAction(E edge, P edge2) {
        return new E(edge.fromExclusive, edge.toInclusive, multiplyOutputs(edge.out, edge2.out),
                multiplyWeights(edge.weight, edge2.weight));
    }

    @Override
    public void rightActionInPlace(E edge, P edge2) {
        edge.out = multiplyOutputs(edge.out, edge2.out);
        edge.weight = multiplyWeights(edge.weight, edge2.weight);
    }

    @Override
    public final Integer fromExclusive(E edge) {
        return edge.fromExclusive;
    }

    @Override
    public final Integer toInclusive(E edge) {
        return edge.toInclusive;
    }

    @Override
    public Integer minimal() {
        return 0;
    }

    @Override
    public Integer maximal() {
        return Integer.MAX_VALUE;
    }

    @Override
    public final int compare(Integer point1, Integer point2) {
        return Integer.compare(point1, point2);
    }

    @Override
    public final P partialNeutralEdge() {
        return NeutralP;
    }

    @Override
    public final E fullNeutralEdge(Integer from, Integer to) {
        return new E(from, to, IntSeq.Epsilon, 0);
    }

    @Override
    public Var<N, G> introduceVariable(String name, Pos pos, G graph, boolean alwaysCopy) throws CompilationError {
        final Var<N, G> g = new Var<>(graph, name, pos, alwaysCopy);
        final Var<N, G> prev = variableAssignments.put(name, g);
        if (null != prev) {
            throw new CompilationError.DuplicateFunction(prev.pos, pos, name);
        }
        if (eagerMinimisation) {
            pseudoMinimize(graph);
        }
        return g;
    }

    @Override
    public Var<N, G> borrowVariable(String var) {
        return variableAssignments.get(var);
    }

    @Override
    public Var<N, G> consumeVariable(String varId) {
        class Ref {
            Var<N, G> meta;
        }
        final Ref ref = new Ref();
        variableAssignments.compute(varId, (k, meta) -> {
            ref.meta = meta;
            if (meta != null && meta.alwaysCopy) {
                return new Var<N, G>(deepClone(meta.graph), meta.name, meta.pos, true);
            } else {
                return null;
            }
        });
        return ref.meta;
    }

    @Override
    public Var<N, G> copyVariable(String var) {
        final Var<N, G> meta = variableAssignments.get(var);
        return meta == null ? null : new Var<>(deepClone(meta.graph), meta.name, meta.pos, meta.alwaysCopy);
    }

    @Override
    public final Specification<Pos, E, P, Integer, IntSeq, Integer, N, G> specification() {
        return this;
    }

    @Override
    public P epsilonUnion(@NonNull P eps1, @NonNull P eps2)
            throws IllegalArgumentException, UnsupportedOperationException {
        if (eps1.weight > eps2.weight)
            return eps1;
        if (eps1.weight < eps2.weight)
            return eps2;
        if (eps1.out.equals(eps2.out))
            return eps1;
        throw new IllegalArgumentException(
                "Both " + eps1 + " and " + eps2 + " have equal weights but different outputs");
    }

    @Override
    public P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException {
        if (eps.weight != 0)
            throw new IllegalArgumentException("Epsilon " + eps + " has non-zero weight under Kleene closure");
        if (!eps.out.isEmpty())
            throw new IllegalArgumentException("Epsilon " + eps + " has non-zero output under Kleene closure");
        return eps;
    }

    @Override
    public Pos metaInfoGenerator(TerminalNode parseNode) {
        return new Pos(parseNode.getSymbol());
    }

    @Override
    public Pos metaInfoGenerator(ParserRuleContext parseNode) {
        return new Pos(parseNode.start);
    }

    @Override
    public Pos metaInfoNone() {
        return Pos.NONE;
    }

    @Override
    public final IntSeq parseStr(IntSeq ints) throws CompilationError {
        return ints;
    }

    @Override
    public final Integer parseW(int weight) {
        return weight;
    }

    @Override
    public final Pair<Integer, Integer> parseRangeInclusive(int codepointFromInclusive, int codepointToInclusive) {
        assert codepointFromInclusive > 0;
        return Pair.of(codepointFromInclusive - 1, codepointToInclusive);
    }

    @Override
    public Pair<Integer, Integer> symbolAsRange(Integer symbol) {
        return parseRangeInclusive(symbol, symbol);
    }

    /**
     * Full edge implementation
     */
    public final static class E implements Comparable<E> {
        private final int fromExclusive, toInclusive;
        private IntSeq out;
        private int weight;

        public E(int from, int to, IntSeq out, int weight) {
            this.fromExclusive = Math.min(from, to);
            this.toInclusive = Math.max(from, to);
            assert fromExclusive < toInclusive : fromExclusive + " " + toInclusive;
            this.out = out;
            this.weight = weight;
        }

        public int getFromInclusive() {
            return fromExclusive;
        }

        public int getToExclsuive() {
            return toInclusive;
        }

        public IntSeq getOut() {
            return out;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "(" + +fromExclusive + "<" + toInclusive + ":" + out + " " + weight + ')';
        }

        @Override
        public int compareTo(E other) {
            int c0 = Integer.compare(fromExclusive, other.fromExclusive);
            if (c0 != 0)
                return c0;
            int c1 = Integer.compare(toInclusive, other.toInclusive);
            if (c1 != 0)
                return c1;
            int c2 = Integer.compare(weight, other.weight);
            if (c2 != 0)
                return c2;
            return out.compareTo(other.out);
        }

        boolean equalTo(E other) {
            return fromExclusive == other.fromExclusive && toInclusive == other.toInclusive && weight == other.weight && out.equals(other.out);
        }

    }

    public static final P NeutralP = new P(IntSeq.Epsilon, 0);

    /**
     * Partial edge implementation
     */
    public final static class P {
        private IntSeq out;
        private final int weight;

        public P(IntSeq out, Integer weight) {
            this.out = out;
            this.weight = weight;
        }

        public IntSeq getOut() {
            return out;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "(" + out + ", " + weight + ')';
        }
    }

    public static class FunctionalityCounterexample<E, P, N> {
        final N fromStateA;
        final N fromStateB;

        public FunctionalityCounterexample(N fromStateA, N fromStateB) {
            this.fromStateA = fromStateA;
            this.fromStateB = fromStateB;
        }
    }

    public static class FunctionalityCounterexampleFinal<E, P, N> extends FunctionalityCounterexample<E, P, N> {
        final P finalEdgeA;
        final P finalEdgeB;

        public FunctionalityCounterexampleFinal(N fromStateA, N fromStateB, P finalEdgeA, P finalEdgeB) {
            super(fromStateA, fromStateB);
            this.finalEdgeA = finalEdgeA;
            this.finalEdgeB = finalEdgeB;
        }
    }

    public static class FunctionalityCounterexampleToThirdState<E, P, N> extends FunctionalityCounterexample<E, P, N> {
        final E overEdgeA;
        final E overEdgeB;
        final N toStateC;

        public FunctionalityCounterexampleToThirdState(N fromStateA, N fromStateB, E overEdgeA, E overEdgeB,
                                                       N toStateC) {
            super(fromStateA, fromStateB);
            this.overEdgeA = overEdgeA;
            this.overEdgeB = overEdgeB;
            this.toStateC = toStateC;
        }
    }

    /**
     * Checks if automaton is strongly functional by searching for
     * weight-conflicting transitions
     */
    public FunctionalityCounterexample<E, P, Pos> isStronglyFunctional(Specification.RangedGraph<Pos, Integer, E, P> g,
                                                                       int startpoint, Set<IntPair> collected) {
        return collectProductSet(g, g, startpoint, startpoint, collected,
                (state, fromExclusive, toInclusive, edgeA, edgeB) -> {
                    //we can ignore sink state
                    if (edgeA == null || edgeB == null) return null;
                    //an edge cannot conflict with itself
                    if (edgeA.getKey() == edgeB.getKey()) return null;
                    //only consider cases when both transitions lead to the same target state
                    if (!edgeA.getValue().equals(edgeB.getValue())) return null;
                    //weights that are not equal cannot conflict
                    if (edgeA.getKey().weight != edgeB.getKey().weight) return null;
                    //Bingo!
                    return new FunctionalityCounterexampleToThirdState<>(g.state(state.l), g.state(state.r),
                            edgeA.getKey(), edgeB.getKey(), g.state(edgeA.getValue()));
                },
                (state) -> {
                    if (!Objects.equals(state.l, state.r)) {
                        P finA = g.getFinalEdge(state.l);
                        P finB = g.getFinalEdge(state.r);
                        if (finA != null && finB != null && finA.weight == finB.weight) {
                            return new FunctionalityCounterexampleFinal<>(g.state(state.l), g.state(state.r), finA, finB);
                        }
                    }
                    return null;
                });
    }

    /**
     * Carries information about all consecutively takes transitions in reverse
     * order.
     */
    public static class BacktrackingNode {
        E edge;
        /**
         * null its if the beginning of path and there is no previous transition
         */
        BacktrackingNode prev;

        BacktrackingNode(BacktrackingNode prev, E edge) {
            this.prev = prev;
            this.edge = edge;
        }
    }

    public static class BacktrackingHead {
        final P finalEdge;
        final BacktrackingNode prev;

        BacktrackingHead(BacktrackingNode prev, P edge) {
            this.prev = prev;
            this.finalEdge = edge;
        }

        int size() {
            int sum = 0;
            for (int outSymbol : finalEdge.out) {
                if (outSymbol != 0) {
                    sum++;
                }
            }
            BacktrackingNode curr = prev;
            while (curr != null) {
                sum += curr.edge.out.size();
                curr = curr.prev;
            }
            return sum;
        }

        IntSeq collect(Seq<Integer> input) {
            int[] output = new int[size()];
            collect(output, input);
            return new IntSeq(output);
        }

        void collect(int[] output, Seq<Integer> input) {
            assert output.length == size();
            int i = output.length - 1;
            for (int outSymbolIdx = finalEdge.out.size() - 1; outSymbolIdx >= 0; outSymbolIdx--) {
                final int outSymbol = finalEdge.out.get(outSymbolIdx);
                if (outSymbol != 0) {
                    output[i--] = outSymbol;
                }
            }
            BacktrackingNode curr = prev;
            int inputIdx = input.size() - 1;
            while (curr != null) {
                for (int outSymbolIdx = curr.edge.out.size() - 1; outSymbolIdx >= 0; outSymbolIdx--) {
                    final int outSymbol = curr.edge.out.get(outSymbolIdx);
                    if (outSymbol == 0) {
                        output[i--] = input.get(inputIdx);
                    } else {
                        output[i--] = outSymbol;
                    }
                }
                curr = curr.prev;
                inputIdx--;
            }
            assert i == -1;
            assert inputIdx == -1;
        }

    }

    public String evaluate(Specification.RangedGraph<Pos, Integer, E, P> graph, String input) {
        final IntSeq out = evaluate(graph, new IntSeq(input));
        return out == null ? null : out.toUnicodeString();
    }

    public IntSeq evaluate(Specification.RangedGraph<Pos, Integer, E, P> graph, IntSeq input) {
        return evaluate(graph, graph.initial, input);
    }

    /**
     * Performs evaluation and uses hashtags outputs as reflections of input
     */
    public IntSeq evaluate(Specification.RangedGraph<Pos, Integer, E, P> graph, int initial, IntSeq input) {
        final BacktrackingHead head = evaluate(graph, initial, input.iterator());
        return head == null ? null : head.collect(input);
    }

    public BacktrackingHead evaluate(RangedGraph<Pos, Integer, E, P> graph, Iterator<Integer> input) {
        return evaluate(graph, graph.initial, input);
    }

    /**
     * Performs a very efficient evaluation algorithm for lexicographic ranged
     * transducers. It's O(n^2) for dense nondeterministic automata, O(n) for
     * deterministic automata and close to O(n) for sparse nondeterministic
     * automata. The returned value is a singly linked list of taken transitions
     * (first element of list is the last taken transition).
     * <p>
     * The automaton must be strongly functional (have no weight-conflicting
     * transitions) in order for this algorithm to work. If automaton is not
     * strongly functional, then the exact outcome is undefined (could be any of the
     * equally best paths).
     *
     * @return singly linked list of all transitions taken by the best (with highest
     * weights) path. May be null if automaton does not accept
     */
    public BacktrackingHead evaluate(RangedGraph<Pos, Integer, E, P> graph, int initial, Iterator<Integer> input) {

        HashMap<Integer, BacktrackingNode> thisList = new HashMap<>();
        HashMap<Integer, BacktrackingNode> nextList = new HashMap<>();
        if (initial != -1) thisList.put(initial, null);
        while (input.hasNext() && !thisList.isEmpty()) {

            final int in = input.next();
            for (final Map.Entry<Integer, BacktrackingNode> stateAndNode : thisList.entrySet()) {
                final int state = stateAndNode.getKey();
                if (state == -1) continue;
                for (final RangedGraph.Trans<E> transition : binarySearch(graph, state, in)) {
                    nextList.compute(transition.targetState, (key, prev) -> {
                        if (prev == null)
                            return new BacktrackingNode(stateAndNode.getValue(), transition.edge);
                        if (prev.edge.weight < transition.edge.weight) {
                            prev.edge = transition.edge;
                            prev.prev = stateAndNode.getValue();
                        } else {
                            assert prev.edge.weight > transition.edge.weight
                                    || prev.edge.out.equals(transition.edge.out) : prev + " " + transition;
                        }
                        return prev;
                    });
                }
            }
            final HashMap<Integer, BacktrackingNode> tmp = thisList;
            thisList = nextList;
            nextList = tmp;
            nextList.clear();
        }
        final Iterator<Map.Entry<Integer, BacktrackingNode>> iter = thisList.entrySet().iterator();
        if (iter.hasNext()) {
            Map.Entry<Integer, BacktrackingNode> first = iter.next();
            P bestFinalEdge = graph.accepting.get(first.getKey());
            BacktrackingNode bestPreviousNode = first.getValue();
            while (iter.hasNext()) {
                Map.Entry<Integer, BacktrackingNode> next = iter.next();
                P otherFinalEdge = graph.accepting.get(next.getKey());
                if (otherFinalEdge != null && (bestFinalEdge == null || otherFinalEdge.weight > bestFinalEdge.weight)) {
                    bestFinalEdge = otherFinalEdge;
                    bestPreviousNode = next.getValue();
                }
            }
            return bestFinalEdge == null ? null : new BacktrackingHead(bestPreviousNode, bestFinalEdge);
        } else {
            return null;
        }

    }

    public ParserListener<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> makeParser() {
        return new ParserListener<>(this);
    }

    /**
     * @throws net.alagris.CompilationError if typechcking fails
     */
    public void checkStrongFunctionality(RangedGraph<Pos, Integer, E, P> g)
            throws CompilationError.WeightConflictingFinal, CompilationError.WeightConflictingToThirdState {
        final FunctionalityCounterexample<E, P, Pos> weightConflictingTranitions = isStronglyFunctional(g, g.initial,
                new HashSet<>());
        if (weightConflictingTranitions != null) {
            if (weightConflictingTranitions instanceof FunctionalityCounterexampleFinal) {
                throw new CompilationError.WeightConflictingFinal(
                        (FunctionalityCounterexampleFinal<E, P, ?>) weightConflictingTranitions);
            } else {
                throw new CompilationError.WeightConflictingToThirdState(
                        (FunctionalityCounterexampleToThirdState<E, P, ?>) weightConflictingTranitions);
            }
        }
    }

    public void testDeterminism(String name, RangedGraph<Pos, Integer, E, P> inOptimal)
            throws CompilationError.NondeterminismException {
        final List<RangedGraph.Trans<E>> nondeterminismCounterexampleIn = inOptimal.isDeterministic();
        if (nondeterminismCounterexampleIn != null) {
            throw new CompilationError.NondeterminismException(
                    inOptimal.state(nondeterminismCounterexampleIn.get(0).targetState),
                    inOptimal.state(nondeterminismCounterexampleIn.get(1).targetState), name);
        }
    }

    public Pair<Integer, Integer> isOutputSubset(RangedGraph<Pos, Integer, E, P> lhs,
                                                 RangedGraph<Pos, Integer, E, P> rhs) {
        return isOutputSubset(lhs, rhs, new HashSet<>(), IntSeq::iterator, e -> e.out.iterator());

    }

    public RangedGraph<Pos, Integer, E, P> optimiseVar(String varId) {
        return optimiseGraph(variableAssignments.get(varId).graph);
    }

    public void pseudoMinimize(G graph) throws CompilationError.WeightConflictingFinal {
        BiFunction<N, Map<E, N>, Integer> hash = (vertex, transitions) -> {
            ArrayList<Map.Entry<E, N>> edges = new ArrayList<>(transitions.size());
            edges.addAll(transitions.entrySet());
            edges.sort(Map.Entry.comparingByKey());
            int h = 0;
            for (Map.Entry<E, N> e : edges) {
                h = 31 * h + Objects.hash(e.getKey().fromExclusive, e.getKey().toInclusive, e.getValue());
            }
            return h;
        };
        graph.pseudoMinimize((vertex, transitions) -> {
            int h = hash.apply(vertex, transitions);
            P p = graph.getFinalEdge(vertex);
            return 31 * h + (p == null ? 0 : p.out.hashCode());
        }, hash, (trA, trB) -> {
            if (trA.size() != trB.size())
                return false;
            ArrayList<Map.Entry<E, N>> edgesA = new ArrayList<>(trA.size());
            edgesA.addAll(trA.entrySet());
            edgesA.sort(Map.Entry.comparingByKey());
            ArrayList<Map.Entry<E, N>> edgesB = new ArrayList<>(trB.size());
            edgesB.addAll(trB.entrySet());
            edgesB.sort(Map.Entry.comparingByKey());
            for (int i = 0; i < trA.size(); i++) {
                Map.Entry<E, N> a = edgesA.get(i);
                Map.Entry<E, N> b = edgesB.get(i);
                N targetStateA = a.getValue();
                N targetStateB = b.getValue();
                if (!a.getKey().equalTo(b.getKey()) || targetStateA != targetStateB) {
                    return false;
                }
            }
            return true;
        }, (finA, finB) -> finA.out.equals(finB.out), (stateA, finA, stateB, finB) -> {
            if (finA.weight > finB.weight) {
                return finA;
            } else if (finA.weight < finB.weight) {
                return finB;
            } else if (finA.out.equals(finB.out)) {
                return finA;// doesn't matter, both are the same
            } else {
                throw new CompilationError.WeightConflictingFinal(
                        new FunctionalityCounterexampleFinal<>(stateA, stateB, finA, finB));
            }
        });

    }

    /**
     * Normally it should not happen, but some operations may introduce edges that both start in the same source
     * state, end in th same target state, have overlapping input ranges and produce the same outputs. Such edges
     * violate strong functionality, but can be easily removed without affecting the transducer.
     * Any time there are two identical edges that only differ in weight, the
     * highest one is chosen and all the remaining ones are removed.
     */
    public void reduceEdges(RangedGraph<Pos, Integer, E, P> g) throws CompilationError.WeightConflictingToThirdState {
        for (int sourceState = 0; sourceState < g.graph.size(); sourceState++) {
            ArrayList<Range<Integer, List<RangedGraph.Trans<E>>>> state = g.graph.get(sourceState);
            for (Range<Integer, List<RangedGraph.Trans<E>>> range : state) {
                final List<RangedGraph.Trans<E>> tr = range.edges();
                /**
                 * All of the edges have the same input range and source state but may differ in
                 * outputs, weights and target states. The task is to find all those edges that
                 * also have the same target state and remove all except for the one with
                 * highest weight. However, if there are several edges with equal target state
                 * and equally highest weights, then make sure that their outputs are the same
                 * or otherwise throw a WeightConflictingException.
                 */
                if (tr.size() > 1) {
                    // First sort by target state so that this way all edges are grouped by target
                    // states.
                    // Additionally among all the edges with equal target state, sort them with
                    // respect to weight, so that
                    // the last edge has the highest weight.
                    tr.sort(Comparator.comparingInt((RangedGraph.Trans<E> x) -> x.targetState)
                            .thenComparingInt(x -> x.edge.weight));
                    int j = 0;
                    for (int i = 1; i < tr.size(); i++) {
                        RangedGraph.Trans<E> prev = tr.get(i - 1);
                        RangedGraph.Trans<E> curr = tr.get(i);
                        if (prev.targetState != curr.targetState) {
                            // prev edge is the last edge leading to prev.targetState, hence it also has the
                            // highest weight
                            tr.set(j++, prev);
                        } else if (prev.edge.weight == curr.edge.weight && !prev.edge.out.equals(curr.edge.out)) {
                            throw new CompilationError.WeightConflictingToThirdState(
                                    new FunctionalityCounterexampleToThirdState<>(sourceState, sourceState, prev.edge,
                                            curr.edge, prev.targetState));
                        }
                    }
                    tr.set(j, tr.get(tr.size() - 1));
                    Specification.removeTail(tr, j + 1);
                }
            }
        }
    }


    public static final class LexPipeline<N, G extends IntermediateGraph<Pos, E, P, N>> {
        public LexPipeline(LexUnicodeSpecification<N, G> spec) {
            this.spec = spec;
        }

        public LexPipeline<N, G> appendAll(LexPipeline<N, G> other) throws CompilationError {
            if (other.nodes.isEmpty()) {
                if (other.hoareAssertion != null)
                    appendLanguage(other.hoarePos, hoareAssertion);
            } else {
                for (Node node : other.nodes) {
                    if (node instanceof AutomatonNode) {
                        append(((AutomatonNode) node).g);
                    } else {
                        append(((ExternalNode) node).f);
                    }
                }
                this.hoarePos = other.hoarePos;
                this.hoareAssertion = other.hoareAssertion;
            }
            return this;
        }

        private interface Node {

            IntSeq evaluate(IntSeq input);

            void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> g)
                    throws CompilationError.CompositionTypecheckException;
        }

        private static final class AutomatonNode implements Node {
            final RangedGraph<Pos, Integer, E, P> g;
            private final LexUnicodeSpecification<?, ?> spec;

            private AutomatonNode(RangedGraph<Pos, Integer, E, P> g, LexUnicodeSpecification<?, ?> spec) {
                this.g = g;
                this.spec = spec;
            }

            @Override
            public IntSeq evaluate(IntSeq input) {
                return spec.evaluate(g, input);
            }

            @Override
            public void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> type)
                    throws CompilationError.CompositionTypecheckException {
                Pair<Integer, Integer> counterexample = spec.isOutputSubset(g, type);
                if (counterexample != null) {
                    Pos typePos = type.state(counterexample.r());
                    throw new CompilationError.CompositionTypecheckException(g.state(counterexample.l()),
                            typePos == null ? pos : typePos);
                }
            }
        }

        private static final class ExternalNode implements Node {
            final Function<IntSeq, IntSeq> f;

            private ExternalNode(Function<IntSeq, IntSeq> f) {
                this.f = f;
            }

            @Override
            public IntSeq evaluate(IntSeq input) {
                return f.apply(input);
            }

            @Override
            public void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> g)
                    throws CompilationError.CompositionTypecheckException {
                // nothing. Just assume it to be true. The user should be responsible.
            }
        }

        public Pos getPos() {
            return pos;
        }

        private Pos pos;
        private final LexUnicodeSpecification<N, G> spec;
        private final ArrayList<Node> nodes = new ArrayList<>();
        private Pos hoarePos;
        private RangedGraph<Pos, Integer, E, P> hoareAssertion;

        public LexPipeline<N, G> append(RangedGraph<Pos, Integer, E, P> g)
                throws CompilationError.CompositionTypecheckException {
            nodes.add(new AutomatonNode(g, spec));
            if (hoareAssertion != null) {
                assert hoarePos != null;
                Pair<Pos, Pos> counterexample = spec.isSubsetNondeterministic(hoareAssertion, g, spec::successor,
                        spec::predecessor);
                if (counterexample != null) {
                    throw new CompilationError.CompositionTypecheckException(counterexample.l(),
                            counterexample.r());
                }
            }
            hoarePos = null;
            hoareAssertion = null;
            return this;
        }

        public LexPipeline<N, G> append(Function<IntSeq, IntSeq> f) {
            nodes.add(new ExternalNode(f));
            hoarePos = null;
            hoareAssertion = null;
            return this;
        }

        public LexPipeline<N, G> appendLanguage(Pos pos, RangedGraph<Pos, Integer, E, P> g)
                throws CompilationError.CompositionTypecheckException {
            nodes.get(nodes.size() - 1).typecheckOutput(pos, g);
            hoareAssertion = g;
            hoarePos = pos;
            return this;
        }

        public String evaluate(String input) {
            final IntSeq out = evaluate(new IntSeq(input));
            return out == null ? null : out.toUnicodeString();
        }

        public IntSeq evaluate(IntSeq input) {
            for (Node node : nodes) {
                if (input == null)
                    break;
                input = node.evaluate(input);
            }
            return input;
        }
    }


    /**
     * @param name should not contain the @ sign as it is already implied by this
     *             methods
     */
    public LexPipeline<N, G> getPipeline(String name) {
        return pipelines.get(name);
    }

    @Override
    public LexPipeline<N, G> makeNewPipeline() {
        return new LexPipeline<N, G>(this);
    }

    @Override
    public void registerNewPipeline(Pos pos, LexPipeline<N, G> pipeline, String name)
            throws CompilationError.DuplicateFunction {
        final LexPipeline<N, G> prev = pipelines.put(name, pipeline);
        if (prev != null) {
            throw new CompilationError.DuplicateFunction(prev.pos, pipeline.pos, '@' + name);
        }
    }

    @Override
    public LexPipeline<N, G> appendAutomaton(Pos pos, LexPipeline<N, G> lexPipeline, G g)
            throws CompilationError.CompositionTypecheckException, CompilationError.WeightConflictingFinal {
        if (eagerMinimisation)
            pseudoMinimize(g);
        return lexPipeline.append(optimiseGraph(g));
    }

    @Override
    public LexPipeline<N, G> appendExternalFunction(Pos pos, LexPipeline<N, G> lexPipeline, String funcName,
                                                    List<Pair<IntSeq, IntSeq>> args) {
        return lexPipeline.append(externalPipelineFunction.make(funcName, args));
    }

    @Override
    public LexPipeline<N, G> appendLanguage(Pos pos, LexPipeline<N, G> lexPipeline, G g) throws CompilationError {
        RangedGraph<Pos, Integer, E, P> optimal = optimiseGraph(g);
        testDeterminism(pos.toString(), optimal);
        return lexPipeline.appendLanguage(pos, optimal);
    }

    @Override
    public LexPipeline<N, G> appendPipeline(Pos pos, LexPipeline<N, G> lexPipeline, String nameOfOtherPipeline)
            throws CompilationError {
        final LexPipeline<N, G> other = pipelines.get(nameOfOtherPipeline);
        if (other == null) {
            throw new CompilationError.MissingFunction(pos, '@' + nameOfOtherPipeline);
        }
        return lexPipeline.appendAll(other);
    }

    public G loadDict(Specification.NullTermIter<Pair<IntSeq, IntSeq>> dict, Pos state) throws CompilationError.AmbiguousDictionary {
        return loadDict(dict, state, (in, out1, out2) -> {
            throw new CompilationError.AmbiguousDictionary(in, out1, out2);
        });
    }

    /**
     * size
     * isEpsilon (weight out)?
     * (transtionNumber (from to target weight out )^transtionNumber)^size
     * initNumber (from to target weight out )^initNumber
     * (source outWeight outStr)*
     **/
    public void compressBinary(G g, DataOutputStream out) throws IOException {
        final LinkedHashMap<N, Integer> vertexToIndex = new LinkedHashMap<>();
        g.collectVertices((N n) -> {
                    class Ref {
                        boolean computed = false;
                    }
                    final Ref ref = new Ref();
                    vertexToIndex.computeIfAbsent(n, k -> {
                        ref.computed = true;
                        return vertexToIndex.size();
                    });
                    return ref.computed;
                }
                , v -> null, (n, e) -> null);
        out.writeInt(vertexToIndex.size());//size
        final P eps = g.getEpsilon();
        if (eps == null) {
            out.writeByte(0); //isEpsilon
        } else {
            out.writeByte(1); //isEpsilon
            out.writeInt(eps.weight);// weight
            out.writeUTF(eps.out.toUnicodeString());// out
        }
        for (Entry<N, Integer> vertexIdx : vertexToIndex.entrySet()) {
            final int transitionNumber = g.size(vertexIdx.getKey());
            out.writeInt(transitionNumber);//transitionNumber
            for (Entry<E, N> transition : (Iterable<Entry<E, N>>) () -> g.iterator(vertexIdx.getKey())) {
                final int targetIdx = vertexToIndex.get(transition.getValue());
                final int from = transition.getKey().fromExclusive;
                final int to = transition.getKey().toInclusive;
                final int weight = transition.getKey().weight;
                out.writeInt(from);//from
                out.writeInt(to);//to
                out.writeInt(targetIdx);//target
                out.writeInt(weight);//weight
                out.writeUTF(transition.getKey().out.toUnicodeString());//out
            }

        }
        final int initNumber = g.allInitialEdges().size();
        out.writeInt(initNumber);
        for (Entry<E, N> inVertex : (Iterable<Entry<E, N>>) () -> g.iterateInitialEdges()) {
            final N vertex = inVertex.getValue();
            final int idx = vertexToIndex.get(vertex);
            final E edge = inVertex.getKey();
            final int from = edge.fromExclusive;
            final int to = edge.toInclusive;
            final int initWeight = edge.weight;
            out.writeInt(from); // from
            out.writeInt(to); // to
            out.writeInt(idx); // target
            out.writeInt(initWeight);//weight
            out.writeUTF(edge.out.toUnicodeString());//out
        }
        for (Entry<N, Integer> vertexIdx : vertexToIndex.entrySet()) {
            final N vertex = vertexIdx.getKey();
            final int idx = vertexIdx.getValue();
            final P finEdge = g.getFinalEdge(vertex);
            if (finEdge != null) {
                final int finalWeight = finEdge.weight;
                out.writeInt(idx); // source
                out.writeInt(finalWeight);//weight
                out.writeUTF(finEdge.out.toUnicodeString());//out
            }
        }
    }

    public G decompressBinary(Pos meta, DataInputStream in) throws IOException {
        final G g = createEmptyGraph();
        final int size = in.readInt();//size
        final ArrayList<N> indexToVertex = Specification.filledArrayListFunc(size, i -> g.create(meta));
        final boolean isEpsilon = in.readBoolean();
        if (isEpsilon) {
            final int epsWeight = in.readInt();// weight
            final IntSeq epsOut = new IntSeq(in.readUTF());// out
            final P eps = createPartialEdge(epsOut, epsWeight);
            g.setEpsilon(eps);
        }
        for (N vertex : indexToVertex) {
            final int transitionNumber = in.readInt();
            for (int i = 0; i < transitionNumber; i++) {
                final int from = in.readInt();//from
                final int to = in.readInt();//to
                final int targetIdx = in.readInt();//target
                final int weight = in.readInt();//weight
                final IntSeq out = new IntSeq(in.readUTF());//out
                final E edge = createFullEdge(from, to, createPartialEdge(out, weight));
                g.add(vertex, edge, indexToVertex.get(targetIdx));
            }

        }
        final int initNumber = in.readInt();
        for (int i = 0; i < initNumber; i++) {
            final int from = in.readInt(); // from
            final int to = in.readInt(); // to
            final int idx = in.readInt(); // target
            final N initState = indexToVertex.get(idx);
            final int initWeight = in.readInt();//weight
            final IntSeq out = new IntSeq(in.readUTF());//out
            final E edge = createFullEdge(from, to, createPartialEdge(out, initWeight));
            g.addInitialEdge(initState, edge);
        }
        while (in.available() > 0) {
            final int idx = in.readInt(); // source
            final N vertex = indexToVertex.get(idx);
            final int finalWeight = in.readInt();//weight
            final IntSeq out = new IntSeq(in.readUTF());//out
            final P edge = createPartialEdge(out, finalWeight);
            g.setFinalEdge(vertex, edge);
        }
        return g;
    }

    public G subtract(G lhs, G rhs) {
        return subtract(optimiseGraph(lhs), optimiseGraph(rhs));
    }

    /**
     * Subtracts one transducer from another. It performs language difference on the input languages.
     * The output language stays the same as in the lhs automaton. In other words, if left automaton accepts
     * and right one doesn't, then the the output of left automaton is printed. If both automata accepts then
     * no output is printed. If left automaton doesn't accept then no output is printed.
     */
    public G subtract(RangedGraph<Pos, Integer, E, P> lhs, RangedGraph<Pos, Integer, E, P> rhs) {
        final Pair<G, HashMap<IntPair, ? extends StateProduct<N>>> g = product(lhs, rhs, (lv, rv) -> lv, (fromExclusive, toInclusive, le, re) ->
                        le == null ? null : new E(fromExclusive, toInclusive, le.out, le.weight)
                , (finL, finR) -> finR == null ? finL : null);
        trim(g.l(),()->g.r().values().stream().map(StateProduct::product).iterator());
        return g.l();
    }

    public G compose(G lhs, G rhs, Pos pos) {
        class Ref {
            int maxWeightRhs = Integer.MIN_VALUE;
        }
        final Ref ref = new Ref();
        final RangedGraph<Pos, Integer, E, P> rhsOptimal = optimiseGraph(rhs, n -> null, (e, n) -> {
            if (n.weight > ref.maxWeightRhs) ref.maxWeightRhs = n.weight;
            return null;
        });
        return compose(lhs, rhsOptimal, ref.maxWeightRhs, pos);
    }

    public G compose(G lhs, RangedGraph<Pos, Integer, E, P> rhs, int maxRhsWeight, Pos pos) {
        assert isStronglyFunctional(rhs, rhs.initial, new HashSet<>()) == null : isStronglyFunctional(rhs, rhs.initial, new HashSet<>()) + " " + rhs;
        return compose(lhs, rhs, pos, IntSeq::iterator, (l, r) -> l * maxRhsWeight + r,
                (prev, rhsTransTaken, lhsOutSymbol) -> {
                    assert rhsTransTaken != null;
                    if (rhsTransTaken.edge == null) {//sink
                        return Pair.of(weightNeutralElement(), prev.r());
                    }
                    final IntSeq outPrev = prev.r();
                    final IntSeq outNext = rhsTransTaken.edge.out;
                    final int[] outJoined = new int[outPrev.size() + outNext.size()];
                    for (int i = 0; i < outPrev.size(); i++) outJoined[i] = outPrev.get(i);
                    for (int i = 0; i < outNext.size(); i++)
                        outJoined[outPrev.size() + i] = outNext.get(i).equals(reflect()) ? lhsOutSymbol : outNext.get(i);
                    return Pair.of(rhsTransTaken.edge.weight, new IntSeq(outJoined));
                },
                (p, initial) -> {
                    final BacktrackingHead head = evaluate(rhs, initial, p.out.iterator());
                    return head == null ? null : Pair.of(head.finalEdge.weight, head.collect(p.out));
                },
                (a, b) -> {
                    if (a == null) return b;
                    if (b == null) return a;
                    return a.l() > b.l() ? a : b;//this assumes that both automata are strongly functional
                }
        );
    }

    public void inverse(G g) throws CompilationError {
        inverse(g, Pos.NONE, IntSeq::new, IntSeq::iteratorReversed, p -> p.out, p -> p.weight, (sourceState, conflictingEdges) -> {
            Pair<N, P> firstHighestWeightState = null;
            Pair<N, P> secondHighestWeightState = null;
            int firstHighestWeightValue = Integer.MIN_VALUE;
            int secondHighestWeightValue = Integer.MIN_VALUE;
            for (Pair<N, P> stateAndWeight : conflictingEdges) {
                final P fin = stateAndWeight.r();
                if (fin.weight >= firstHighestWeightValue) {
                    secondHighestWeightValue = firstHighestWeightValue;
                    secondHighestWeightState = firstHighestWeightState;
                    firstHighestWeightValue = fin.weight;
                    firstHighestWeightState = stateAndWeight;
                }
            }
            if (firstHighestWeightState != null && secondHighestWeightValue == firstHighestWeightValue) {
                assert secondHighestWeightState != null;
                throw new CompilationError.AmbiguousAcceptingState(g.getState(sourceState),
                        g.getState(firstHighestWeightState.l()),
                        g.getState(secondHighestWeightState.l()),
                        firstHighestWeightState.r(),
                        secondHighestWeightState.r());

            }
            return firstHighestWeightState.r();
        }, new InvertionErrorCallback<N, E, P, IntSeq>() {
            @Override
            public void doubleReflectionOnOutput(N vertex, E edge) throws CompilationError {
                throw new CompilationError.DoubleReflectionOnOutput(g.getState(vertex), edge);
            }

            @Override
            public void rangeWithoutReflection(N target, E edge) throws CompilationError {
                throw new CompilationError.RangeWithoutReflection(g.getState(target), edge);
            }

            @Override
            public void epsilonTransitionCycle(N state, IntSeq output, IntSeq output1) throws CompilationError {
                throw new CompilationError.EpsilonTransitionCycle(g.getState(state), output, output1);
            }
        });
    }

    /**
     * Makes all transitions return the exact same output as is their input.
     * If input is a single symbol, then the output is the same symbol. If input is a range,
     * then output is reflected (output is the {@link Specification#minimal()} element)
     */
    public void identity(G g) {
        final IntSeq REFLECT = new IntSeq(reflect());
        mutateEdges(g, edge -> {
            if (edge.fromExclusive + 1 == edge.toInclusive) {
                edge.out = new IntSeq(edge.toInclusive);
            } else {
                edge.out = REFLECT;
            }
        });
        g.mutateAllFinalEdges((fin,edge)->fin.out=IntSeq.Epsilon);
        g.getEpsilon().out=IntSeq.Epsilon;
    }

    /**Sets output of all edges to empty string*/
    public void clearOutput(G g) {
        mutateEdges(g, edge -> {
            edge.out = IntSeq.Epsilon;
        });
        g.mutateAllFinalEdges((fin,edge)->fin.out=IntSeq.Epsilon);
        g.getEpsilon().out=IntSeq.Epsilon;
    }


}
