package net.alagris;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;

import net.alagris.LexUnicodeSpecification.*;
import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Specification for ranged transducer over integers (unicode) values and with arctic lexicographic semiring over integers
 */
public abstract class LexUnicodeSpecification<N, G extends IntermediateGraph<Pos, E, P, N>>
        implements Specification<Pos, E, P, Integer, IntSeq, Integer, N, G>,
        ParseSpecs<Pos, E, P, Integer, IntSeq, Integer, N, G> {


    private final boolean eagerMinimisation;
    /**
     * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link LexUnicodeSpecification#pseudoMinimize} will be automatically called from
     * {@link LexUnicodeSpecification#registerVar})
     */
    public LexUnicodeSpecification(boolean eagerMinimisation) {

        this.eagerMinimisation = eagerMinimisation;
    }

    public final HashMap<String, GMeta<Pos, E, P, N, G>> variableAssignments = new HashMap<>();


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
        return Epsilon;
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
    public final E leftAction(P edge, E edge2) {
        return new E(edge2.from, edge2.to, multiplyOutputs(edge.out, edge2.out), multiplyWeights(edge.weight, edge2.weight));
    }

    @Override
    public void leftActionInPlace(P edge, E edge2) {
        edge2.out = multiplyOutputs(edge.out, edge2.out);
        edge2.weight = multiplyWeights(edge.weight, edge2.weight);
    }

    @Override
    public final E rightAction(E edge, P edge2) {
        return new E(edge.from, edge.to, multiplyOutputs(edge.out, edge2.out), multiplyWeights(edge.weight, edge2.weight));
    }

    @Override
    public void rightActionInPlace(E edge, P edge2) {
        edge.out = multiplyOutputs(edge.out, edge2.out);
        edge.weight = multiplyWeights(edge.weight, edge2.weight);
    }

    @Override
    public final Integer from(E edge) {
        return edge.from;
    }

    @Override
    public final Integer to(E edge) {
        return edge.to;
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
    public boolean isSuccessor(Integer predecessor, Integer successor) {
        return predecessor + 1 == successor;
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
        return new E(from, to, Epsilon, 0);
    }


    @Override
    public void registerVar(GMeta<Pos, E, P, N, G> g) {
        GMeta<Pos, E, P, N, G> prev = variableAssignments.put(g.name, g);
        if (null != prev) {
            throw new IllegalArgumentException("Variable " + g.name + g.pos + " has already been defined " + prev.pos + "!");
        }
        if (eagerMinimisation) {
            pseudoMinimize(g.graph);
        }
    }

    @Override
    public GMeta<Pos, E, P, N, G> varAssignment(String varId) {
        return variableAssignments.get(varId);
    }

    @Override
    public final Specification<Pos, E, P, Integer, IntSeq, Integer, N, G> specification() {
        return this;
    }

    @Override
    public P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException {
        if (eps1.weight > eps2.weight) return eps1;
        if (eps1.weight < eps2.weight) return eps2;
        if (eps1.out.equals(eps2.out)) return eps1;
        throw new IllegalArgumentException("Both " + eps1 + " and " + eps2 + " have equal weights but different outputs");
    }

    @Override
    public P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException {
        if (eps.weight != 0)
            throw new IllegalArgumentException("Epsilon " + eps + " has non-zero weight under Kleene closure");
        if (!eps.out.isEmpty())
            throw new IllegalArgumentException("Epsilon " + eps + " has non-zero output under Kleene closure");
        return eps;
    }

    public static final IntSeq Epsilon = new IntSeq(new int[0]);

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

    public static IntSeq parseQuotedLiteral(TerminalNode literal) throws CompilationError {
        final String quotedLiteral = literal.getText();
        final String unquotedLiteral = quotedLiteral.substring(1, quotedLiteral.length() - 1);
        final int[] escaped = new int[unquotedLiteral.length()];
        int j = 0;
        boolean isAfterBackslash = false;
        for (int c : (Iterable<Integer>) unquotedLiteral.codePoints()::iterator) {
            if (isAfterBackslash) {
                switch (c) {
                    case '0':
                        throw new CompilationError.IllegalCharacter(new Pos(literal.getSymbol()), "\\0");
                    case 'b':
                        escaped[j++] = '\b';
                        break;
                    case 't':
                        escaped[j++] = '\t';
                        break;
                    case 'n':
                        escaped[j++] = '\n';
                        break;
                    case 'r':
                        escaped[j++] = '\r';
                        break;
                    case 'f':
                        escaped[j++] = '\f';
                        break;
                    case '#':
                        escaped[j++] = '#';
                        break;
                    default:
                        escaped[j++] = c;
                        break;
                }
                isAfterBackslash = false;
            } else {
                switch (c) {
                    case '\\':
                        isAfterBackslash = true;
                        break;
                    case '#':
                        escaped[j++] = 0;
                        break;
                    default:
                        escaped[j++] = c;
                        break;
                }

            }
        }
        return new IntSeq(escaped, j);
    }

    @Override
    public final IntSeq parseStr(TerminalNode parseNode) throws CompilationError {
        return parseQuotedLiteral(parseNode);
    }

    @Override
    public final Integer parseW(TerminalNode parseNode) {
        return Integer.parseInt(parseNode.getText());
    }

    @Override
    public final Pair<Integer, Integer> parseRange(int codepointFrom, int codepointTo) {
        return Pair.of(codepointFrom, codepointTo);
    }

    @Override
    public final Pair<Integer, Integer> dot() {
        return Pair.of(1, Integer.MAX_VALUE);
    }

    @Override
    public final Integer hashtag() {
        return 0;
    }

    /**
     * Sequence of integers implementation
     */
    public final static class IntSeq implements Seq<Integer>, Comparable<IntSeq> {
        public final int[] arr;
        public final int size;

        public IntSeq(String s) {
            this(s.codePoints().toArray());
        }

        private IntSeq(int[] arr) {
            this(arr, arr.length);
        }

        private IntSeq(int[] arr, int size) {
            this.arr = arr;
            this.size = size;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public Integer get(int i) {
            return arr[i];
        }

        public IntSeq concat(IntSeq rhs) {
            int[] n = new int[size() + rhs.size()];
            System.arraycopy(arr, 0, n, 0, size());
            System.arraycopy(rhs.arr, 0, n, size(), rhs.size());
            return new IntSeq(n);
        }

        @Override
        public boolean equals(Object obj) {
            IntSeq rhs = (IntSeq) obj;
            return Arrays.equals(arr, rhs.arr);
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < arr.length;
                }

                @Override
                public Integer next() {
                    return arr[i++];
                }
            };
        }

        @Override
        public String toString() {
            return Arrays.toString(arr);
        }


        @Override
        public int compareTo(IntSeq other) {
            int len1 = size();
            int len2 = other.size();
            int lim = Math.min(len1, len2);
            for (int k = 0; k < lim; k++) {
                int c1 = get(k);
                int c2 = other.get(k);
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
            return len1 - len2;
        }
    }

    /**
     * Full edge implementation
     */
    public final static class E implements Comparable<E> {
        private final int from, to;
        private IntSeq out;
        private int weight;

        public E(int from, int to, IntSeq out, int weight) {
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
            this.out = out;
            this.weight = weight;
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }

        public IntSeq getOut() {
            return out;
        }

        public int getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return "(" + +from +
                    "-" + to +
                    ":'" + out +
                    "' " + weight +
                    ')';
        }

        @Override
        public int compareTo(E other) {
            int c0 = Integer.compare(from, other.from);
            if (c0 != 0) return c0;
            int c1 = Integer.compare(to, other.to);
            if (c1 != 0) return c1;
            int c2 = Integer.compare(weight, other.weight);
            if (c2 != 0) return c2;
            return out.compareTo(other.out);
        }

        boolean equalTo(E other) {
            return from == other.from && to == other.to && weight == other.weight && out.equals(other.out);
        }


    }

    public static final P NeutralP = new P(Epsilon, 0);

    /**
     * Partial edge implementation
     */
    public final static class P {
        private final IntSeq out;
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
            return "(" + out +
                    ", " + weight +
                    ')';
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

        public FunctionalityCounterexampleToThirdState(N fromStateA, N fromStateB, E overEdgeA, E overEdgeB, N toStateC) {
            super(fromStateA, fromStateB);
            this.overEdgeA = overEdgeA;
            this.overEdgeB = overEdgeB;
            this.toStateC = toStateC;
        }
    }

    /**
     * Checks if automaton is strongly functional by searching for weight-conflicting transitions
     */
    public FunctionalityCounterexample<E, P, Pos> isStronglyFunctional(
            Specification.RangedGraph<Pos, Integer, E, P> g, int startpoint, Set<Pair<Integer, Integer>> collected) {
        return collectProduct(g, g, startpoint, startpoint, collected, (stateA, edgeA, stateB, edgeB) ->
                        !stateA.equals(stateB) && edgeA.getValue() != g.sinkState && edgeA.getValue().equals(edgeB.getValue())
                                && edgeA.getKey().weight == edgeB.getKey().weight && !edgeA.getKey().out.equals(edgeB.getKey().out) ?
                                new FunctionalityCounterexampleToThirdState<>(g.state(stateA), g.state(stateB), edgeA.getKey(),
                                        edgeB.getKey(), g.state(edgeA.getValue()))
                                : null
                , (a, b) -> {
                    if (!Objects.equals(a, b)) {
                        P finA = g.getFinalEdge(a);
                        P finB = g.getFinalEdge(b);
                        if (finA != null && finB != null && finA.weight == finB.weight && !finA.out.equals(finB.out)) {
                            return new FunctionalityCounterexampleFinal<>(g.state(a), g.state(b), finA, finB);
                        }
                    }
                    return null;
                });
    }

    /**
     * Carries information about all consecutively takes transitions in reverse order.
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
        P finalEdge;
        BacktrackingNode prev;

        BacktrackingHead(BacktrackingNode prev, P edge) {
            this.prev = prev;
            this.finalEdge = edge;
        }
    }

    /**
     * Performs evaluation and uses hashtags outputs as reflections of input
     */
    public String evaluate(Specification.RangedGraph<Pos, Integer, E, P> graph, String input) {
        final BacktrackingHead head = evaluate(graph, input.codePoints().iterator());
        if (head == null) return null;
        final StringBuilder sb = new StringBuilder();
        final int[] codepoints = input.codePoints().toArray();
        int inputIdx = codepoints.length;
        BacktrackingNode prev = head.prev;
        IntSeq out = head.finalEdge.out;
        while (true) {
            for (int outputIdx = out.size - 1; outputIdx >= 0; outputIdx--) {
                final int outputSymbol = out.get(outputIdx);
                if (hashtag() == outputSymbol) {
                    if (inputIdx != codepoints.length) sb.appendCodePoint(codepoints[inputIdx]);
                } else {
                    sb.appendCodePoint(outputSymbol);
                }
            }
            if (prev == null) return sb.reverse().toString();
            inputIdx--;
            out = prev.edge.out;
            prev = prev.prev;
        }


    }

    public BacktrackingHead evaluate(RangedGraph<Pos, Integer, E, P> graph, Iterable<Integer> input) {
        return evaluate(graph, input.iterator());
    }

    /**
     * Performs a very efficient evaluation algorithm for lexicographic ranged transducers.
     * It's O(n^2) for dense nondeterministic automata, O(n) for deterministic automata
     * and close to O(n) for sparse nondeterministic automata. The returned value is a singly linked
     * list of taken transitions (first element of list is the last taken transition).
     * <p>
     * The automaton must be strongly functional (have no weight-conflicting transitions)
     * in order for this algorithm to work. If automaton is not strongly functional, then the exact outcome is undefined
     * (could be any of the equally best paths).
     *
     * @return singly linked list of all transitions taken by the best (with highest weights) path.
     * May be null if automaton does not accept
     */
    public BacktrackingHead evaluate(RangedGraph<Pos, Integer, E, P> graph, Iterator<Integer> input) {

        HashMap<Integer, BacktrackingNode> thisList = new HashMap<>();
        HashMap<Integer, BacktrackingNode> nextList = new HashMap<>();
        thisList.put(graph.initial, null);
        while (input.hasNext() && !thisList.isEmpty()) {

            final int in = input.next();
            for (final Map.Entry<Integer, BacktrackingNode> stateAndNode : thisList.entrySet()) {
                final int state = stateAndNode.getKey();
                for (final RangedGraph.Trans<E> transition : binarySearch(graph, state, in)) {
                    if (transition.targetState != graph.sinkState) {
                        nextList.compute(transition.targetState, (key, prev) -> {
                            if (prev == null) return new BacktrackingNode(stateAndNode.getValue(), transition.edge);
                            if (prev.edge.weight < transition.edge.weight) {
                                prev.edge = transition.edge;
                                prev.prev = stateAndNode.getValue();
                            } else {
                                assert prev.edge.weight > transition.edge.weight ||
                                        prev.edge.out.equals(transition.edge.out) :
                                        prev + " " + transition;
                            }
                            return prev;
                        });
                    }
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

    public ParserListener<Pos, E, P, Integer, IntSeq, Integer, N, G> makeParser(Collection<ParserListener.Type<Pos, E, P, N, G>> types) {
        return new ParserListener<>(types, this);
    }

    /**
     * @throws net.alagris.CompilationError if typechcking fails
     */
    public void checkStrongFunctionality(RangedGraph<Pos, Integer, E, P> g) throws
            CompilationError.WeightConflictingFinal,
            CompilationError.WeightConflictingToThirdState {
        final FunctionalityCounterexample<E, P, Pos> weightConflictingTranitions =
                isStronglyFunctional(g, g.initial, new HashSet<>());
        if (weightConflictingTranitions != null) {
            if (weightConflictingTranitions instanceof FunctionalityCounterexampleFinal) {
                throw new CompilationError.WeightConflictingFinal(
                        (FunctionalityCounterexampleFinal<E, P, ?>)
                                weightConflictingTranitions);
            } else {
                throw new CompilationError.WeightConflictingToThirdState(
                        (FunctionalityCounterexampleToThirdState<E, P, ?>)
                                weightConflictingTranitions);
            }
        }
    }

    /**
     * @throws net.alagris.CompilationError.TypecheckException if typechcking fails
     */
    public void typecheck(String name, Pos graphPos, Pos typePos,
                          RangedGraph<Pos, Integer, E, P> graph,
                          RangedGraph<Pos, Integer, E, P> typeLhs,
                          RangedGraph<Pos, Integer, E, P> typeRhs) throws CompilationError {
        final RangedGraph<Pos, Integer, E, P> inOptimal = typeLhs;
        final List<RangedGraph.Trans<E>> nondeterminismCounterexampleIn = inOptimal.isDeterministic();
        if (nondeterminismCounterexampleIn != null) {
            throw new CompilationError.NondeterminismException(
                    inOptimal.state(nondeterminismCounterexampleIn.get(0).targetState),
                    inOptimal.state(nondeterminismCounterexampleIn.get(1).targetState), name);
        }
        final RangedGraph<Pos, Integer, E, P> outOptimal = typeRhs;
        final List<RangedGraph.Trans<E>> nondeterminismCounterexampleOut = outOptimal.isDeterministic();
        if (nondeterminismCounterexampleOut != null) {
            throw new CompilationError.NondeterminismException(
                    outOptimal.state(nondeterminismCounterexampleOut.get(0).targetState),
                    outOptimal.state(nondeterminismCounterexampleOut.get(1).targetState), name);
        }
        final Pair<Integer, Integer> counterexampleIn = isSubset(graph, inOptimal, graph.initial, inOptimal.initial, new HashSet<>());
        if (counterexampleIn != null) {
            throw new CompilationError.TypecheckException(graphPos, typePos, name);
        }
        final Pair<Integer, Integer> counterexampleOut = isOutputSubset(graph, outOptimal, new HashSet<>(), o -> o, e -> e.out);
        if (counterexampleOut != null) {
            throw new CompilationError.TypecheckException(graphPos, typePos, name);
        }

    }

    public RangedGraph<Pos, Integer, E, P> optimiseVar(String varId) {
        return optimiseGraph(variableAssignments.get(varId).graph);
    }


    public void pseudoMinimize(G graph) {
        graph.pseudoMinimize(transitions -> {
            ArrayList<E> edges = new ArrayList<>(transitions.size());
            for (E e : transitions.keySet()) {
                edges.add(e);
            }
            edges.sort(E::compareTo);
            int h = 0;
            for (E e : edges) {
                h = 31 * h + Objects.hash(e.from, e.to);
            }
            return h;
        }, (trA, trB) -> {
            if (trA.size() != trB.size()) return false;
            ArrayList<Map.Entry<E, N>> edgesA = new ArrayList<>(trA.size());
            edgesA.addAll(trA.entrySet());
            edgesA.sort(Map.Entry.comparingByKey());
            ArrayList<Map.Entry<E, N>> edgesB = new ArrayList<>(trB.size());
            edgesB.addAll(trB.entrySet());
            edgesB.sort(Map.Entry.comparingByKey());
            for (int i = 0; i < trA.size(); i++) {
                Map.Entry<E, N> a = edgesA.get(i);
                Map.Entry<E, N> b = edgesB.get(i);
                if (!a.getKey().equalTo(b.getKey()) || a.getValue() != b.getValue()) {
                    return false;
                }
            }
            return true;
        }, (finA, finB) -> finA.out.equals(finB.out), (finA, finB) -> finA.weight > finB.weight ? finA : finB);

    }

    /**
     * Any time there are two identical edges that only differ in weight, the highest one is chosen and all the remaining ones
     * are removed.
     */
    public void reduceEdges(RangedGraph<Pos, Integer, E, P> g) {
        for (ArrayList<Range<Integer, RangedGraph.Trans<E>>> state : g.graph) {
            for (Range<Integer, RangedGraph.Trans<E>> range : state) {
                reduceEdges(range.atThisInput);
                reduceEdges(range.betweenThisAndPreviousInput);
            }
        }
    }

    private void reduceEdges(List<RangedGraph.Trans<E>> tr) {
        if (tr.size() > 1) {
            tr.sort(Comparator.comparingInt((RangedGraph.Trans<E> x) -> x.targetState).thenComparingInt(x -> x.edge.weight));
            int j = 0;
            for (int i = 1; i < tr.size(); i++) {
                RangedGraph.Trans<E> prev = tr.get(i - 1);
                RangedGraph.Trans<E> curr = tr.get(i);
                if (prev.targetState != curr.targetState) {
                    tr.set(j++, prev);
                }
            }
            tr.set(j, tr.get(tr.size() - 1));
            Specification.removeTail(tr, j + 1);
        }
    }

}
