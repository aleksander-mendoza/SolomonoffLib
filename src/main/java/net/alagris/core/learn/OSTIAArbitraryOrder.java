package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.core.Pair.IntPair;
import net.alagris.core.Queue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class OSTIAArbitraryOrder {


    public static <C> State<C> buildPtt(IntEmbedding alph, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State<C> root = new State<>(alph.size(), IntSeq.Epsilon);
        root.isInitial = true;
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            OSTIAState.buildPttOnward(root, alph, inout.l(), inout.r());
        }

        return root;
    }


    private static class Index {
        final int stateIndices;
        int score;

        private Index(int stateIndices) {
            this.stateIndices = stateIndices;
        }

        <C> Pair<State<C>, State<C>> state(ArrayList<State<C>> states) {
            IntPair i = MatrixIndexing.lowerTriagleCell(stateIndices);
            State<C> row = states.get(i.l);
            State<C> col = states.get(i.r);
            assert row.index == i.l;
            assert col.index == i.r;
            return Pair.of(row, col);
        }
    }

    public static boolean isNotMergeable(int rowState, int colState, boolean[] notMergeable) {
        return notMergeable[MatrixIndexing.lowerTriangleIndex(rowState, colState)];
    }

    public static void setNotMergeable(int rowState, int colState, boolean[] notMergeable) {
        notMergeable[MatrixIndexing.lowerTriangleIndex(rowState, colState)] = true;
    }

    public interface ScoringFunction<C> {
        int score(State<C> a, State<C> b, ArrayList<State<C>> states);
    }

    public interface MergingPolicy<C> {
        boolean shouldMerge(Index index, ArrayList<State<C>> states);
    }


    private static <C> int pairing(int score, ArrayList<State<C>> states, State<C> a, State<C> b) {
        if (score == -1) return -1;
        return (score + 1) * 2 * states.size() - a.index - b.index - 1;
    }

    public static ScoringFunction<StatePTT> SCORING_MAX_COMPATIBLE_INPUTS_AND_OUTPUTS = (a, b, states) -> pairing(compatibleInputsAndOutputs(a, b), states, a, b);
    public static ScoringFunction<StatePTT> SCORING_MAX_COMPATIBLE_INPUTS = (a, b, states) -> pairing(compatibleInputs(a, b), states, a, b);
    public static ScoringFunction<StatePTT> SCORING_MAX_COMPATIBLE = (a, b, states) -> pairing(compatibleOutputs(a, b), states, a, b);
    public static ScoringFunction<StatePTT> SCORING_MAX_OVERLAP = (a, b, states) -> pairing(a.ptt.overlappingOutputs(b.ptt), states, a, b);
    public static ScoringFunction<StatePTT> SCORING_MAX_OVERLAP_INPUTS_AND_OUTPUTS = (a, b, states) -> pairing(a.ptt.overlappingInputsAndOutputs(b.ptt), states, a, b);

    public static <C> ScoringFunction<C> SCORING_MAX_DEEP_OVERLAP() {
        return (a, b, states) -> pairing(deepOverlap(a, b, states), states, a, b);
    }

    public static <C> MergingPolicy<C> POLICY_GREEDY() {
        return (idx, states) -> true;
    }

    public static <C> MergingPolicy<C> POLICY_THRESHOLD(int minScoreIncl) {
        return (idx, states) -> idx.score >= minScoreIncl;
    }

    public static <C> State<C> ostia(State<C> transducer, ScoringFunction<C> scoring, MergingPolicy<C> policy, BiFunction<C, C, C> merge) {
        final ArrayList<State<C>> states = new ArrayList<>();
        int stackHeight = 0;
        states.add(transducer);
        while (stackHeight < states.size()) {
            final State<C> s = states.get(stackHeight);
            s.index = stackHeight;
            stackHeight++;
            for (Edge<C> e : s.transitions) {
                if (e != null) {
                    assert !states.contains(e.target);//initially transducer is a tree
                    states.add(e.target);
                }
            }
        }
        final int triangle = MatrixIndexing.lowerTriangleSize(states.size());
        final ArrayList<Index> score = new ArrayList<>(triangle);
        for (int i = 0; i < triangle; i++) {
            score.add(new Index(i));
        }
        final FoldContext<C> ctx = new FoldContext<>(score, states);
        outer:
        while (true) {

            assert validateGraph(transducer, ctx);
            score.removeIf(i -> {
                final Pair<State<C>, State<C>> s = i.state(states);
                final State<C> a = s.l();
                final State<C> b = s.r();
                assert a != b;
                if (a.changeIteration == ctx.iter || b.changeIteration == ctx.iter) {
                    i.score = scoring.score(a, b, states);
                }
                assert i.score >= -1 : i.score;
                return i.score == -1;
            });
            ctx.iter++;
            score.sort(Comparator.comparingInt(a -> -a.score));
            for (int j = 0; j < score.size(); j++) {
                final Index i = score.get(j);
                if (policy.shouldMerge(i, states)) {
                    final Pair<State<C>, State<C>> s = i.state(states);
                    ctx.mergedWith.clear();
                    ctx.mutated.clear();
                    assert validateGraph(transducer, ctx);
                    if (ostiaMerge(s.l(), s.r(), ctx, merge)) {
                        transducer = ctx.mergeDestination(transducer);
                        assert transducer.isInitial;
                        assert transducer.incoming != null;
                        assert transducer.transitions != null;
                        continue outer;
//                    } else {
//                        notMergeable[j] = true;
//                        assert notMergeable[j];
                    }
                }
            }
            break;
        }
//        LearnLibCompatibility.visualize(tr.specs.convertCustomGraphToRanged(asGraph(tr.specs, transducer, i -> i + 1, i -> i), LexUnicodeSpecification.E::getToExclsuive));
        assert validateGraph(transducer, ctx);
        return transducer;
    }


    public static <C> boolean validateGraph(State<C> a, FoldContext<C> ctx) {
        final HashMap<State<C>, HashMap<State<C>, ArrayList<Integer>>> visited = new HashMap<>();
        final Stack<State<C>> stack = new Stack<>();
        stack.add(a);
        assert a.isInitial : a.index + "\n" + ctx;
        visited.put(a, new HashMap<>());
        while (!stack.isEmpty()) {
            final State<C> s = stack.pop();
            assert s.transitions != null;
            assert s.incoming != null;
            for (int symbol = 0; symbol < s.transitions.length; symbol++) {
                final Edge<C> e = s.transitions[symbol];
                if (e != null) {
                    HashMap<State<C>, ArrayList<Integer>> incoming = visited.get(e.target);
                    if (incoming == null) {
                        incoming = new HashMap<>();
                        visited.put(e.target, incoming);
                        assert !e.target.isInitial : e.target.index + "\n" + ctx;
                        stack.add(e.target);
                    }
                    incoming.computeIfAbsent(s, i -> new ArrayList<>()).add(symbol);
                }
            }
        }
        for (Map.Entry<State<C>, HashMap<State<C>, ArrayList<Integer>>> e : visited.entrySet()) {
            final State<C> s = e.getKey();
            final HashMap<State<C>, ArrayList<Integer>> incoming = e.getValue();
            for (Map.Entry<State<C>, ArrayList<Integer>> fromStateOverSymbol : incoming.entrySet()) {
                final State<C> fromState = fromStateOverSymbol.getKey();
                assert visited.containsKey(fromState);
                final ArrayList<Integer> overSymbol1 = fromStateOverSymbol.getValue();
                final ArrayList<Integer> overSymbol2 = s.incoming.get(fromState);
                final HashSet<Integer> s1 = new HashSet<>(overSymbol1);
                final HashSet<Integer> s2 = new HashSet<>(overSymbol2);
                assert s1.equals(s2);
            }
            assert incoming.size() == e.getKey().incoming.size();
            if (s.ptt instanceof StatePTT) {
                ((StatePTT) s.ptt).forEach((in, out) -> {
                    IntSeq actual = run(s, in);
                    assert actual != null && new IntSeq(out).isSuffixOf(actual) : ctx + "\n\n" + s.index + "\n\n" + in + "|" + IntQueue.toString(out) + "|" + actual;
                });
            }
        }
        return true;
    }

    public static <C> void printState(StringBuilder sb, State<C> s) {
        if (s.transitions == null) {
            assert s.incoming == null;
            sb.append(s.index).append(" ELIMINATED\n");
        } else {
            sb.append(s.index).append(":'").append(IntQueue.toString(s.out)).append("' ").append(s.kind).append(s.isInitial ? " INIT" : "").append('\n');
            sb.append("{\n");
            sb.append(s.ptt);
            sb.append("}\n");
            for (int j = 0; j < s.transitions.length; j++) {
                final Edge<C> e = s.transitions[j];
                if (e != null) {
                    sb.append("\t").append(j).append(":'").append(IntQueue.toString(e.out)).append("'->").append(e.target.index).append('\n');
                }
            }
            for (Map.Entry<State<C>, ArrayList<Integer>> ptr : s.incoming.entrySet()) {
                for (int symbol : ptr.getValue()) {
                    sb.append("\t").append("<-").append(symbol).append(" ").append(ptr.getKey().index).append('\n');
                }
            }
        }
    }

    public static <C> void printStateCopy(StringBuilder sb, StateCopy<C> s) {
        if (s.transitions == null) {
            assert s.incoming == null;
            sb.append(s.original.index).append(" ELIMINATED\n");
        } else {
            sb.append(s.original.index).append(":'").append(IntQueue.toString(s.out)).append("' ").append(s.kind).append('\n');
            for (int j = 0; j < s.transitions.length; j++) {
                for (Edge<C> e : s.transitions[j]) {
                    sb.append("\t").append(j).append(":'").append(IntQueue.toString(e.out)).append("'->").append(e.target.index).append('\n');
                }
            }
            for (Map.Entry<State<C>, ArrayList<Integer>> ptr : s.incoming.entrySet()) {
                for (int symbol : ptr.getValue()) {
                    sb.append("\t").append("<-").append(symbol).append(" ").append(ptr.getKey().index).append('\n');
                }
            }
        }
    }

    public static class FoldContext<C> {
        final ArrayList<Index> score;
        //        final boolean[] notMergeable;
        final ArrayList<State<C>> states;
        final HashMap<State<C>, StateCopy<C>> mutated = new HashMap<>();
        final LinkedHashMap<State<C>, State<C>> mergedWith = new LinkedHashMap<>();
        int iter = 0;

        public StateCopy<C> mutated(State<C> s) {
            return mutated.computeIfAbsent(s, StateCopy::new);
        }

        public FoldContext(ArrayList<Index> score, ArrayList<State<C>> states) {
            this.score = score;
//            this.notMergeable = notMergeable;
            this.states = states;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < states.size(); i++) {
                final State<C> s = states.get(i);
                assert s.index == i;
                printState(sb, s);
            }
            sb.append("Copied\n");
            for (StateCopy<C> s : mutated.values()) {
                printStateCopy(sb, s);
            }
            return sb.toString();
        }


        public State<C> mergeDestination(State<C> a) {
            assert Util.forall(mergedWith.keySet(), s -> {
                final StateCopy<C> c = mutated.get(s);
                return c.incoming == null && c.transitions == null;
            });
            while (true) {
                State<C> n = mergedWith.get(a);
                if (n == null) return a;
                a = n;
            }
        }
    }

    private static <C> boolean ostiaMerge(State<C> a, State<C> b, FoldContext<C> ctx, BiFunction<C, C, C> merge) {
        assert a != b;
        if (ostiaFold(a, b, ctx)) {
            final BitSet wasMerged = new BitSet(ctx.states.size());
            for (Map.Entry<State<C>, State<C>> ab : ctx.mergedWith.entrySet()) {
                final State<C> mergeSource = ab.getKey();
                final State<C> mergeDestination = ab.getValue();
                mergeDestination.ptt = merge.apply(mergeDestination.ptt, mergeSource.ptt);
                wasMerged.set(mergeSource.index);
            }
            ctx.score.removeIf(i -> {
                final IntPair c = MatrixIndexing.lowerTriagleCell(i.stateIndices);
                return //isNotMergeable(c.l, c.r, ctx.notMergeable) ||
                        wasMerged.get(c.r) ||
                                wasMerged.get(c.l);
            });
            for (Map.Entry<State<C>, StateCopy<C>> e : ctx.mutated.entrySet()) {
                final State<C> s = e.getKey();
                final StateCopy<C> c = e.getValue();
                if (c.transitions == null) {
                    s.incoming = null;
                    s.transitions = null;
                    continue;
                }
                s.changeIteration = ctx.iter;
                s.incoming = c.incoming;
                s.kind = c.kind;
                for (int symbol = 0; symbol < c.transitions.length; symbol++) {
                    final NondetTransitions<C> tr = c.transitions[symbol];
                    assert tr.size() < 2;
                    s.transitions[symbol] = tr.size() == 1 ? tr.get(0) : null;
                }
                s.isInitial = c.isInitial;
                s.out = c.out;
            }
            return true;
        }
        return false;
    }


    private static <C> boolean mergeStates(StateCopy<C> copyA, StateCopy<C> copyB, FoldContext<C> ctx) {
        assert copyA != copyB;
        final State<C> a = copyA.original;
        final State<C> b = copyB.original;
        assert a != b;
        final State<C> prev = ctx.mergedWith.put(b, a);
        assert prev == null;
        // tiny optimisation
//        if (isNotMergeable(a.index, b.index, ctx.notMergeable)) {
//            return false;
//        }
        //merge state output and kind
        if (copyB.kind == OSTIAState.Kind.ACCEPTING) {
            if (copyA.kind == OSTIAState.Kind.UNKNOWN) {
                copyA.out = copyB.out;
                copyA.kind = OSTIAState.Kind.ACCEPTING;
            } else if (copyA.kind == OSTIAState.Kind.REJECTING) {
                return false;
            } else if (!IntQueue.equals(copyA.out, copyB.out)) {
                return false;
            }
        } else if (copyB.kind == OSTIAState.Kind.REJECTING) {
            if (copyA.kind == OSTIAState.Kind.ACCEPTING) {
                return false;
            } else if (copyA.kind == OSTIAState.Kind.UNKNOWN) {
                copyA.kind = OSTIAState.Kind.REJECTING;
            }
        }
        //merge initial states
        if (copyB.isInitial) {
            copyA.isInitial = true;
        }
        //merge incoming edges
        for (Map.Entry<State<C>, ArrayList<Integer>> ptr : copyB.incoming.entrySet()) {
            final State<C> src = ptr.getKey();
            final ArrayList<Integer> incomingToB = ptr.getValue();
            assert !incomingToB.isEmpty();
            final ArrayList<Integer> aList = copyA.incoming.get(src);
            if (aList == null) {
                copyA.incoming.put(src, new ArrayList<>(incomingToB));
            } else {
                assert !aList.isEmpty();
                for (int symbol : incomingToB) {
                    if (!aList.contains(symbol)) aList.add(symbol);
                }
            }
            for (int symbol : incomingToB) {
                final StateCopy<C> mutSrc = ctx.mutated.computeIfAbsent(src, StateCopy::new);
                assert Util.exists(mutSrc.transitions[symbol], t -> t.target == b);
                for (Edge<C> edge : mutSrc.transitions[symbol]) {
                    if (edge.target == b) edge.target = a;
                }
            }
        }
        //merge outgoing edges
        for (int symbol = 0; symbol < copyB.transitions.length; symbol++) {
            final NondetTransitions<C> edges = copyB.transitions[symbol];
            for (Edge<C> edge : edges) {
                final StateCopy<C> target = ctx.mutated.computeIfAbsent(edge.target, StateCopy::new);
                final ArrayList<Integer> incomingFromB = target.incoming.remove(b);
                if (incomingFromB != null) {
                    target.incoming.compute(a, (k, v) -> v == null ? incomingFromB : Util.addAllIfAbsent(v, incomingFromB));
                } else {
                    assert target.incoming.containsKey(a);//already merged because some edge was duplicate
                }
            }
            copyA.transitions[symbol].addAll(edges);
        }
        copyB.transitions = null;
        copyB.incoming = null;
        return true;
    }


    private static <C> boolean ostiaFold(State<C> a, State<C> b, FoldContext<C> ctx) {
        final Stack<StateCopy<C>> toDeterminize = new Stack<>();
        final StateCopy<C> copyA = ctx.mutated(a);
        final StateCopy<C> copyB = ctx.mutated(b);
        if (!mergeStates(copyA, copyB, ctx)) {
            return false;
        }
        toDeterminize.add(copyA);
        int stackHeight = 0;
        while (stackHeight < toDeterminize.size()) {
            final StateCopy<C> nondeterministicState = toDeterminize.get(stackHeight++);
            assert (nondeterministicState.transitions == null) == (nondeterministicState.incoming == null);
            for (int i = 0; nondeterministicState.transitions != null && i < nondeterministicState.transitions.length; i++) {

                final NondetTransitions<C> nondeterministicTransitions = nondeterministicState.transitions[i];
                if (nondeterministicTransitions.size() <= 1) continue;
                final Edge<C> transitionA = nondeterministicTransitions.get(0);
                final StateCopy<C> targetA = ctx.mutated(transitionA.target);
                boolean hadMerge = false;
                while (nondeterministicTransitions.size() > 1) {
                    final int lastIndex = nondeterministicTransitions.size() - 1;
                    final Edge<C> transitionB = nondeterministicTransitions.get(lastIndex);
                    final IntQueue pushedBackA;
                    final IntQueue pushedBackB;
                    if (!transitionA.isKnown) {
                        transitionA.isKnown = transitionB.isKnown;
                        transitionA.out = transitionB.out;
                        pushedBackA = pushedBackB = null;
                    } else if (!transitionB.isKnown) {
                        pushedBackA = pushedBackB = null;
                    } else {
                        IntQueue commonPrefixA = transitionA.out;
                        IntQueue commonPrefixB = transitionB.out;
                        while (commonPrefixB != null && commonPrefixA != null &&
                                commonPrefixB.value == commonPrefixA.value) {
                            commonPrefixB = commonPrefixB.next;
                            commonPrefixA = commonPrefixA.next;
                        }
                        pushedBackA = commonPrefixA;
                        pushedBackB = commonPrefixB;
                    }


                    final StateCopy<C> targetB = ctx.mutated(transitionB.target);
                    assert (transitionA.target == transitionB.target) == (targetA == targetB);
                    if (targetA == targetB) {
                        assert (pushedBackA == null && pushedBackB == null) || !IntQueue.equals(pushedBackA, pushedBackB);
                        if (pushedBackA != null || pushedBackB != null) {
                            return false;
                        }
                    } else {
                        if (!targetA.prepend(pushedBackA, ctx)) {
                            return false;
                        }
                        if (!targetB.prepend(pushedBackB, ctx)) {
                            return false;
                        }
                        if (!mergeStates(targetA, targetB, ctx)) {
                            return false;
                        }
                        hadMerge = true;
                    }
                    final Edge<C> removed = nondeterministicTransitions.remove(lastIndex);
                    assert removed == transitionB;
                }
                assert nondeterministicTransitions.size() <= 1;
                if (hadMerge) {
                    toDeterminize.add(targetA);
                }
                assert (nondeterministicState.transitions == null) == (nondeterministicState.incoming == null);
            }
        }
        assert Util.forall(toDeterminize, s -> s.transitions == null || Util.forall(s.transitions, t -> t.size() <= 1)) : toDeterminize + "\n" + ctx;
        return true;
    }

    public static <C> @Nullable IntSeq run(State<C> init, Iterable<Integer> input) {
        return run(init, input.iterator());
    }

    public static <C> @Nullable IntSeq run(State<C> init, Iterator<Integer> input) {
        final List<Integer> output = new ArrayList<>();
        State<C> iter = init;
        while (input.hasNext()) {
            final Integer integer = input.next();
            if (integer == null) return null;
            if (integer >= iter.transitions.length) return null;
            final Edge<C> edge = iter.transitions[integer];
            if (edge == null) {
                return null;
            }
            iter = edge.target;
            IntQueue q = edge.out;
            while (q != null) {
                output.add(q.value);
                q = q.next;
            }
        }
        if (iter.kind != OSTIAState.Kind.ACCEPTING) {
            return null;
        }
        IntQueue q = iter.out;
        while (q != null) {
            output.add(q.value);
            q = q.next;
        }
        int[] arr = new int[output.size()];
        for (int i = 0; i < output.size(); i++) {
            arr[i] = output.get(i);
        }
        return new IntSeq(arr);
    }

    static class Edge<C> {
        boolean isKnown;
        @Nullable IntQueue out;
        State<C> target;

        IntQueue cutSuffix(int len) {
            IntQueue ahead = out;
            for (int i = 0; i <= len; i++) {
                if (ahead == null) {
                    final IntQueue whole = out;
                    out = null;
                    return whole;
                }
                ahead = ahead.next();
            }
            IntQueue oneBeforeSuffix = out;
            while (ahead != null) {
                oneBeforeSuffix = oneBeforeSuffix.next();
                ahead = ahead.next();
            }
            final IntQueue suffix = oneBeforeSuffix.next;
            oneBeforeSuffix.next = null;
            return suffix;
        }

        Edge() {
        }

        Edge(Edge<C> edge) {
            out = IntQueue.copyAndConcat(edge.out, null);
            target = edge.target;
            isKnown = edge.isKnown;
        }

        @Override
        public String toString() {
            return String.valueOf(target);
        }
    }


    static class StateParent<C> {
        OSTIAState.Kind kind = OSTIAState.Kind.UNKNOWN;
        @Nullable IntQueue out;
        /**
         * Incoming transitions, excluding loops
         */
        HashMap<State<C>, ArrayList<Integer>> incoming = new HashMap<>();
        boolean isInitial;

        @Override
        public String toString() {
            return String.valueOf(out);
        }
    }

    static class NondetTransitions<C> extends ArrayList<Edge<C>> {
        public NondetTransitions() {
            super(0);
        }

        public NondetTransitions(Edge<C> e) {
            super(1);
            assert e != null;
            add(e);
        }

    }

    static class StateCopy<C> extends StateParent<C> {

        final State<C> original;
        NondetTransitions<C>[] transitions;

        @Override
        public String toString() {
            return original.index + "";
        }

        StateCopy(State<C> original) {
            this.original = original;
            this.out = original.out;
            this.transitions = copyTransitions(original.transitions);
            this.kind = original.kind;
            for (Map.Entry<State<C>, ArrayList<Integer>> e : original.incoming.entrySet()) {
                assert !e.getValue().isEmpty();
                this.incoming.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            this.isInitial = original.isInitial;
        }

        private static <C> @Nullable NondetTransitions<C>[] copyTransitions(@Nullable Edge<C>[] transitions) {
            final @Nullable NondetTransitions<C>[] copy = new NondetTransitions[transitions.length];
            for (int i = 0; i < copy.length; i++) {
                @Nullable Edge<C> edge = transitions[i];
                copy[i] = edge == null ? new NondetTransitions<>() : new NondetTransitions<>(new Edge<>(edge));
            }
            return copy;
        }


        /**
         * The IntQueue is consumed and should not be reused after calling this method.
         */
        boolean prepend(@Nullable IntQueue prefix, FoldContext<C> ctx) {
            if (prefix != null) {//The output of all incoming edges needs to be
                //pushed back as well.
                final ArrayList<Integer> loopback = incoming.get(original);
                assert loopback == null || !loopback.isEmpty();
                if (loopback != null) {//cannot pushback on a looping transition
                    assert Util.forall(loopback, i -> Util.exists(transitions[i], e -> e.target == original));
                    return false;
                }
                if (isInitial) {//cannot perform pushback on initial states
                    return false;
                }
                final int len = net.alagris.core.Queue.len(prefix);
                for (Map.Entry<State<C>, ArrayList<Integer>> ptr : incoming.entrySet()) {
                    for (int symbol : ptr.getValue()) {
                        final StateCopy<C> source = ctx.mutated.computeIfAbsent(ptr.getKey(), StateCopy::new);
                        assert Util.exists(source.transitions[symbol], t -> t.target == original);
                        for (final Edge<C> incomingToThis : source.transitions[symbol]) {
                            if (incomingToThis.target == original) {
                                assert incomingToThis.isKnown;
                                final IntQueue suffix = incomingToThis.cutSuffix(len);
                                if (!IntQueue.equals(suffix, prefix)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                for (NondetTransitions<C> edges : transitions) {
                    for (Edge<C> edge : edges) {
                        edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                    }
                }
                if (kind == OSTIAState.Kind.ACCEPTING) {
                    out = IntQueue.copyAndConcat(prefix, out);
                }
            }
            return true;
        }

    }

    public static class StatePTT {
        public OSTIAState.Kind kind = OSTIAState.Kind.UNKNOWN;
        public IntQueue out;
        @Nullable
        public StatePTT[] transitions;

        public StatePTT(int alphSize) {
            this.transitions = new StatePTT[alphSize];
        }

        public StatePTT(IntQueue prefix, StatePTT copy) {
            assert (copy.kind == OSTIAState.Kind.ACCEPTING) || (copy.out == null) : copy;
            this.transitions = new StatePTT[copy.transitions.length];
            this.kind = copy.kind;
            if (kind == OSTIAState.Kind.ACCEPTING) this.out = IntQueue.copyAndConcat(prefix, copy.out);
            for (int i = 0; i < copy.transitions.length; i++) {
                if (copy.transitions[i] != null) {
                    this.transitions[i] = new StatePTT(prefix, copy.transitions[i]);
                }
            }
        }

        public StatePTT(StatePTT copy) {
            assert (copy.kind == OSTIAState.Kind.ACCEPTING) || (copy.out == null) : copy;
            this.transitions = new StatePTT[copy.transitions.length];
            this.kind = copy.kind;
            this.out = copy.out;
            for (int i = 0; i < copy.transitions.length; i++) {
                if (copy.transitions[i] != null) {
                    this.transitions[i] = new StatePTT(copy.transitions[i]);
                }
            }
        }

        public StatePTT add(StatePTT ptt) {
            assert (ptt.kind == OSTIAState.Kind.ACCEPTING) || (ptt.out == null) : ptt;
            assert (kind == OSTIAState.Kind.ACCEPTING) || (out == null) : this;
            assert ptt.transitions.length == transitions.length;
            assert kind == OSTIAState.Kind.UNKNOWN || ptt.kind == OSTIAState.Kind.UNKNOWN || kind == ptt.kind;
            assert kind != OSTIAState.Kind.ACCEPTING || ptt.kind != OSTIAState.Kind.ACCEPTING || net.alagris.core.Queue.suffixOf(out, ptt.out) || net.alagris.core.Queue.suffixOf(ptt.out, out) : IntQueue.toString(out) + "|" + IntQueue.toString(ptt.out);
            if (kind == OSTIAState.Kind.UNKNOWN) {
                kind = ptt.kind;
                out = ptt.out;
            } else if (ptt.kind == OSTIAState.Kind.ACCEPTING) {
                if (kind == OSTIAState.Kind.ACCEPTING) {
                    if (net.alagris.core.Queue.len(ptt.out) > net.alagris.core.Queue.len(out)) {
                        out = ptt.out;
                    }
                } else {
                    assert kind == OSTIAState.Kind.REJECTING;
                    assert false;
                }
            }
            for (int i = 0; i < ptt.transitions.length; i++) {
                if (ptt.transitions[i] != null) {
                    if (transitions[i] == null) {
                        transitions[i] = new StatePTT(ptt.transitions[i]);
                    } else {
                        transitions[i].add(ptt.transitions[i]);
                    }
                }
            }
            return this;
        }

        public int overlappingSubtrees(StatePTT ptt) {
            assert this != ptt;
            assert (ptt.kind == OSTIAState.Kind.ACCEPTING) || (ptt.out == null) : ptt;
            assert (kind == OSTIAState.Kind.ACCEPTING) || (out == null) : this;
            if (ptt.kind == OSTIAState.Kind.ACCEPTING && kind == OSTIAState.Kind.REJECTING) return -1;
            if (ptt.kind == OSTIAState.Kind.REJECTING && kind == OSTIAState.Kind.ACCEPTING) return -1;
            int sum;
            if (kind == OSTIAState.Kind.ACCEPTING && ptt.kind == OSTIAState.Kind.ACCEPTING) {
                if (net.alagris.core.Queue.equals(out, ptt.out)) {
                    sum = 2 + net.alagris.core.Queue.len(out);
                } else {
                    return -1;
                }
            } else {
                sum = 1;
            }
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i] != null && ptt.transitions[i] != null) {
                    int subTreeSum = transitions[i].overlappingInputsAndOutputs(ptt.transitions[i]);
                    if (subTreeSum == -1) return -1;
                    sum += subTreeSum;
                }
            }
            return sum;
        }

        /**
         * Number of overlapping states
         */
        public int overlappingInputsAndOutputs(StatePTT ptt) {
            assert this != ptt;
            assert (ptt.kind == OSTIAState.Kind.ACCEPTING) || (ptt.out == null) : ptt;
            assert (kind == OSTIAState.Kind.ACCEPTING) || (out == null) : this;
            if (ptt.kind == OSTIAState.Kind.ACCEPTING && kind == OSTIAState.Kind.REJECTING) return -1;
            if (ptt.kind == OSTIAState.Kind.REJECTING && kind == OSTIAState.Kind.ACCEPTING) return -1;
            int sum;
            if (kind == OSTIAState.Kind.ACCEPTING && ptt.kind == OSTIAState.Kind.ACCEPTING) {
                if (net.alagris.core.Queue.equals(out, ptt.out)) {
                    sum = 2 + net.alagris.core.Queue.len(out);
                } else {
                    return -1;
                }
            } else {
                sum = 1;
            }
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i] != null && ptt.transitions[i] != null) {
                    int subTreeSum = transitions[i].overlappingInputsAndOutputs(ptt.transitions[i]);
                    if (subTreeSum == -1) return -1;
                    sum += subTreeSum;
                }
            }
            return sum;
        }

        /**
         * Number of overlapping states
         */
        public int overlappingOutputs(StatePTT ptt) {
            assert this != ptt;
            assert (ptt.kind == OSTIAState.Kind.ACCEPTING) || (ptt.out == null) : ptt;
            assert (kind == OSTIAState.Kind.ACCEPTING) || (out == null) : this;
            if (ptt.kind == OSTIAState.Kind.ACCEPTING && kind == OSTIAState.Kind.REJECTING) return -1;
            if (ptt.kind == OSTIAState.Kind.REJECTING && kind == OSTIAState.Kind.ACCEPTING) return -1;
            if (kind == OSTIAState.Kind.ACCEPTING && ptt.kind == OSTIAState.Kind.ACCEPTING && !net.alagris.core.Queue.equals(out, ptt.out))
                return -1;
            int sum = 0;
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i] != null && ptt.transitions[i] != null) {
                    int subTreeSum = transitions[i].overlappingOutputs(ptt.transitions[i]);
                    if (subTreeSum == -1) return -1;
                    sum += subTreeSum;
                }
            }
            if (sum > 0 || (kind == OSTIAState.Kind.ACCEPTING && ptt.kind == OSTIAState.Kind.ACCEPTING)) {
                sum += 1;
            }
            return sum;
        }

        public void toString(StringBuilder sb, int indent) {
            sb.append(':').append(kind).append(" ").append(IntQueue.toString(out)).append('\n');
            for (int i = 0; i < transitions.length; i++) {
                if (transitions[i] != null) {
                    for (int j = 0; j < indent; j++) sb.append("  ");
                    sb.append(i);
                    transitions[i].toString(sb, indent + 1);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, 0);
            return sb.toString();
        }

        public void forEach(BiConsumer<IntSeq, IntQueue> callback) {
            forEach(new Stack<>(), callback);
        }

        public void forEach(Stack<Integer> string, BiConsumer<IntSeq, IntQueue> callback) {
            if (kind == OSTIAState.Kind.ACCEPTING) {
                final int[] in = new int[string.size()];
                for (int i = 0; i < in.length; i++) {
                    in[i] = string.get(i);
                }
                callback.accept(new IntSeq(in, 0, in.length), out);
            }
            for (int symbol = 0; symbol < transitions.length; symbol++) {
                if (transitions[symbol] != null) {
                    string.push(symbol);
                    transitions[symbol].forEach(string, callback);
                    string.pop();
                }
            }
        }
    }

    public static StatePTT buildSamplePtt(State<StatePTT> s) {
        final StatePTT ptt = new StatePTT(s.transitions.length);
        ptt.kind = s.kind;
        if (ptt.kind == OSTIAState.Kind.ACCEPTING) ptt.out = IntQueue.reverseCopyAndConcat(s.out, null);
        for (int i = 0; i < ptt.transitions.length; i++) {
            final Edge<StatePTT> e = s.transitions[i];
            if (e != null) {
                ptt.transitions[i] = new StatePTT(e.out, buildSamplePtt(e.target));
            }
        }
        s.ptt = ptt;
        return ptt;
    }

    public static class State<C> extends StateParent<C> implements OSTIAState<Edge<C>, State<C>> {
        public int index;
        public @Nullable Edge<C>[] transitions;
        public C ptt;
        public final IntSeq shortest;
        int changeIteration = 0;

        public State(int alphabetSize, IntSeq shortest) {
            super.out = null;
            this.transitions = new Edge[alphabetSize];
            this.shortest = shortest;
        }


        @Override
        public String toString() {
            return index + " " + kind;
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public void setKind(Kind kind) {
            this.kind = kind;
        }

        @Override
        public Edge<C> transition(int i) {
            return transitions[i];
        }

        @Override
        public boolean isKnown(Edge<C> edge) {
            return edge.isKnown;
        }

        @Override
        public void setKnown(Edge<C> edge, boolean b) {
            edge.isKnown = b;
        }

        @Override
        public IntQueue getOutput(Edge<C> edge) {
            return edge.out;
        }

        @Override
        public int transitionCount() {
            return transitions.length;
        }

        @Override
        public void setOutput(Edge<C> edge, IntQueue intQueue) {
            edge.out = intQueue;
        }

        @Override
        public IntQueue getStateOutput() {
            return out;
        }

        @Override
        public void setStateOutput(IntQueue intQueue) {
            out = intQueue;
        }

        @Override
        public State<C> getTarget(Edge<C> edge) {
            return edge.target;
        }


        @Override
        public Edge<C> edgeConstructor() {
            return new Edge<>();
        }

        @Override
        public void setChild(int symbol, Edge<C> edge) {
            final State<C> target = new State<C>(transitions.length, shortest.concat(new IntSeq(symbol)));
            edge.target = target;
            assert transitions[symbol] == null;
            transitions[symbol] = edge;
            final ArrayList<Integer> prev = target.incoming.put(this, Util.singeltonArrayList(symbol));
            assert prev == null;
        }

        @Override
        public void pushback(IntQueue prefix) {
            for (@Nullable Edge<C> edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
            if (kind == Kind.ACCEPTING) {
                out = IntQueue.copyAndConcat(prefix, out);
            }
        }

    }


    public static int compatibleOutputs(State<StatePTT> a, State<StatePTT> b) {
        final Stack<Integer> s = new Stack<>();
        int s0 = compatibleOutputs(s, a, b.ptt);
        if (s0 == -1) return -1;
        assert s.isEmpty();
        int s1 = compatibleOutputs(s, b, a.ptt);
        if (s1 == -1) return -1;
        return s0 + s1;
    }

    public static int compatibleOutputs(Stack<Integer> accumulated, State<?> a, StatePTT b) {
        assert (b.kind == OSTIAState.Kind.ACCEPTING) || (b.out == null) : a.ptt;
        assert (a.kind == OSTIAState.Kind.ACCEPTING) || (a.out == null) : a;
        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.REJECTING) return -1;
        if (a.kind == OSTIAState.Kind.REJECTING && b.kind == OSTIAState.Kind.ACCEPTING) return -1;
        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.ACCEPTING && -1 == equal(accumulated, a.out, b.out)) {
            return -1;
        }

        int sum = 0;
        for (int i = 0; i < a.transitions.length; i++) {
            final Edge<?> e = a.transitions[i];
            if (e != null && b.transitions[i] != null) {
                int len = accumulated.size();
                net.alagris.core.Queue.forEach(e.out, (j, n) -> accumulated.push(n.value));
                int subTreeSum = compatibleOutputs(accumulated, e.target, b.transitions[i]);
                Util.removeTail(accumulated, len);
                if (subTreeSum == -1) return -1;
                sum += subTreeSum;
            }
        }
        if (sum > 0 || (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.ACCEPTING)) {
            sum += 1;
        }
        return sum;
    }


    public static <C> int deepOverlap(State<C> a, State<C> b, ArrayList<State<C>> states) {
        assert a != b;
        int i = 0;
        final boolean[] triangle = new boolean[MatrixIndexing.lowerTriangleSize(states.size())];
        final Stack<Integer> s = new Stack<>();
        final int init = MatrixIndexing.lowerTriangleIndex(a.index, b.index);
        s.push(init);
        triangle[init] = true;
        while (!s.isEmpty()) {
            final int idx = s.pop();
            final IntPair pair = MatrixIndexing.lowerTriagleCell(idx);
            final State<?> l = states.get(pair.l);
            final State<?> r = states.get(pair.r);
            if (l.getKind() == OSTIAState.Kind.REJECTING && r.getKind() == OSTIAState.Kind.ACCEPTING) return -1;
            if (l.getKind() == OSTIAState.Kind.ACCEPTING && r.getKind() == OSTIAState.Kind.REJECTING) return -1;
            for (int symbol = 0; symbol < l.transitions.length; symbol++) {
                final Edge<?> el = l.transition(symbol);
                final Edge<?> er = r.transition(symbol);
                if (el != null && er != null && el.target != er.target) {
                    final int target = MatrixIndexing.lowerTriangleIndex(el.target.index, er.target.index);
                    if (!triangle[target]) {
                        triangle[target] = true;
                        i++;
                    }
                }
            }
        }
        return i;
    }

//    private interface Compatibility{


    public static int compatibleInputs(State<StatePTT> a, State<StatePTT> b) {
        return compatibleInputs(a, b, a.ptt, b.ptt);
    }

    public static int compatibleInputs(State<?> a, State<?> b, StatePTT aPtt, StatePTT bPtt) {
        assert (b.kind == OSTIAState.Kind.ACCEPTING) || (b.out == null) : a.ptt;
        assert (a.kind == OSTIAState.Kind.ACCEPTING) || (a.out == null) : a;
        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.REJECTING) return -1;
        if (a.kind == OSTIAState.Kind.REJECTING && b.kind == OSTIAState.Kind.ACCEPTING) return -1;
        int sum = 1;

        for (int i = 0; i < a.transitions.length; i++) {
            final Edge<?> eA = a.transitions[i];
            final Edge<?> eB = a.transitions[i];
            final StatePTT pttA = aPtt == null ? null : aPtt.transitions[i];
            final StatePTT pttB = bPtt == null ? null : bPtt.transitions[i];
            if (eA != null && eB != null && (pttA != null || pttB != null)) {
                int subTreeSum = compatibleInputs(eA.target, eB.target, pttA, pttB);
                if (subTreeSum == -1) return -1;
                sum += subTreeSum;
            }
        }
        return sum;
    }

    public static int compatibleInputsAndOutputs(State<StatePTT> a, State<StatePTT> b) {
        final Stack<Integer> sa = new Stack<>();
        final Stack<Integer> sb = new Stack<>();
        return compatibleInputsAndOutputs(sa, sb, a, b, a.ptt, b.ptt);
    }

    public static int compatibleInputsAndOutputs(Stack<Integer> accumulatedA, Stack<Integer> accumulatedB, State<?> a, State<?> b, StatePTT aPtt, StatePTT bPtt) {
        assert (b.kind == OSTIAState.Kind.ACCEPTING) || (b.out == null) : a.ptt;
        assert (a.kind == OSTIAState.Kind.ACCEPTING) || (a.out == null) : a;
        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.REJECTING) return -1;
        if (a.kind == OSTIAState.Kind.REJECTING && b.kind == OSTIAState.Kind.ACCEPTING) return -1;
        int sum = 1;
        int lenA = accumulatedA.size();
        int lenB = accumulatedB.size();
        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.ACCEPTING) {
            net.alagris.core.Queue.forEach(a.out, (j, n) -> accumulatedA.push(n.value));
            net.alagris.core.Queue.forEach(b.out, (j, n) -> accumulatedB.push(n.value));
            final boolean eq = accumulatedA.equals(accumulatedB);
            Util.removeTail(accumulatedA, lenA);
            Util.removeTail(accumulatedB, lenB);
            if (!eq) {
                return -1;
            }
        }

        for (int i = 0; i < a.transitions.length; i++) {
            final Edge<?> eA = a.transitions[i];
            final Edge<?> eB = a.transitions[i];
            final StatePTT pttA = aPtt == null ? null : aPtt.transitions[i];
            final StatePTT pttB = bPtt == null ? null : bPtt.transitions[i];
            if (eA != null && eB != null && (pttA != null || pttB != null)) {
                net.alagris.core.Queue.forEach(eA.out, (j, n) -> accumulatedA.push(n.value));
                Queue.forEach(eB.out, (j, n) -> accumulatedB.push(n.value));
                int subTreeSum = compatibleInputsAndOutputs(accumulatedA, accumulatedB, eA.target, eB.target, pttA, pttB);
                Util.removeTail(accumulatedA, lenA);
                Util.removeTail(accumulatedB, lenB);
                if (subTreeSum == -1) return -1;
                sum += subTreeSum;
            }
        }
        return sum;
    }
//    }


//    public static int compatibleInputs(State<StatePTT> a, State<StatePTT> b) {
//        final Stack<Integer> s = new Stack<>();
//        int s0 = compatibleInputs(s, a, b.ptt);
//        if (s0 == -1) return -1;
//        assert s.isEmpty();
//        int s1 = compatibleInputs(s, b, a.ptt);
//        if (s1 == -1) return -1;
//        return s0 + s1;
//    }
//
//    public static int compatibleInputs(Stack<Integer> accumulated, State<?> a, StatePTT b) {
//        assert (b.kind == OSTIAState.Kind.ACCEPTING) || (b.out == null) : a.ptt;
//        assert (a.kind == OSTIAState.Kind.ACCEPTING) || (a.out == null) : a;
//        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.REJECTING) return -1;
//        if (a.kind == OSTIAState.Kind.REJECTING && b.kind == OSTIAState.Kind.ACCEPTING) return -1;
//        int sum = 1;
//        if (a.kind == OSTIAState.Kind.ACCEPTING && b.kind == OSTIAState.Kind.ACCEPTING && equal(accumulated, a.out, b.out) == -1) {
//            return -1;
//        }
//
//        for (int i = 0; i < a.transitions.length; i++) {
//            final Edge<?> e = a.transitions[i];
//            if (e != null && b.transitions[i] != null) {
//                int len = accumulated.size();
//                Queue.forEach(e.out, (j, n) -> accumulated.push(n.value));
//                int subTreeSum = compatibleInputs(accumulated, e.target, b.transitions[i]);
//                Util.removeTail(accumulated, len);
//                if (subTreeSum == -1) return -1;
//                sum += subTreeSum;
//            }
//        }
//        return sum;
//    }

    private static int equal(Stack<Integer> prefix, IntQueue suffix, IntQueue str) {
        for (int val : prefix) {
            if (str == null || !Objects.equals(str.val(), val)) {
                return -1;
            }
            str = str.next();
        }
        int i = 0;
        IntQueue o = suffix;
        while (str != null && o != null) {
            if (!Objects.equals(str.val(), o.val())) {
                return -1;
            }
            str = str.next();
            o = o.next();
            i++;
        }
        if (o != null || str != null) {
            return -1;
        }
        return prefix.size() + i;
    }

}