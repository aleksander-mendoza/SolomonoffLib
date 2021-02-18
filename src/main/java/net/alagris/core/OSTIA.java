package net.alagris.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.alagris.lib.ExternalFunctionsFromSolomonoff;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OSTIA {


    public static <V, N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    Specification.CustomGraph<State, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P, V> asGraph(LexUnicodeSpecification<N, G> specs,
                                                                                                               State transducer,
                                                                                                               Function<Integer, Integer> indexToSymbol,
                                                                                                               Function<IntSeq, V> shortestAsMeta) {
        return asGraph(specs, transducer, indexToSymbol, (in, out) -> new LexUnicodeSpecification.E(in - 1, in, out, 0), IntSeq::new, shortestAsMeta);
    }

    public static <V, E, P, In, Out, W, N, G extends IntermediateGraph<?, E, P, N>>
    Specification.CustomGraph<State, Integer, E, P, V> asGraph(Specification<?, E, P, In, Out, W, N, G> specs,
                                                               State transducer,
                                                               Function<Integer, In> indexToSymbol,
                                                               BiFunction<In, Out, E> fullEdge,
                                                               Function<IntQueue, Out> convertOutput,
                                                               Function<IntSeq, V> shortestAsMeta) {
        return new Specification.CustomGraph<State, Integer, E, P, V>() {

            @Override
            public State init() {
                return transducer;
            }

            @Override
            public P stateOutput(State state) {
                if (state.kind == OSTIAState.Kind.ACCEPTING) {
                    final Out fin = convertOutput.apply(state.out);
                    return specs.createPartialEdge(fin, specs.weightNeutralElement());
                }
                return null;
            }

            @Override
            public Iterator<Integer> outgoing(State state) {
                return new Iterator<Integer>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < state.transitions.length;
                    }

                    @Override
                    public Integer next() {
                        final Integer out = state.transitions[i] == null ? null : i;
                        i++;
                        return out;
                    }
                };
            }

            @Override
            public State target(State state, Integer transition) {
                return state.transitions[transition].target;
            }

            @Override
            public E edge(State state, Integer transition) {
                final Out out = convertOutput.apply(state.transitions[transition].out);
                final In in = indexToSymbol.apply(transition);
                return fullEdge.apply(in, out);
            }

            @Override
            public V meta(State state) {
                return shortestAsMeta.apply(state.shortest);
            }
        };
    }

    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alphabetSize, IntSeq.Epsilon);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            OSTIAState.buildPttOnward(root, inout.l(), inout.r());
        }
        return root;
    }
//
//    private static void buildPttOnward(State ptt, IntSeq input, boolean rejecting, @Nullable IntQueue output) {
//        State pttIter = ptt;
//        assert !rejecting || output == null;
//        @Nullable IntQueue outputIter = output;
//
//        for (int i = 0; i < input.size(); i++) {//input index
//            final int symbol = input.get(i);
//            final Edge edge;
//            if (pttIter.transitions[symbol] == null) {
//                edge = new Edge();
//                if (rejecting) {
//                    edge.isKnown = false;
//                } else {
//                    edge.out = outputIter;
//                    edge.isKnown = true;
//                    outputIter = null;
//                }
//                edge.target = new State(pttIter.transitions.length, pttIter.shortest.concat(new IntSeq(symbol)));
//                pttIter.transitions[symbol] = edge;
//
//            } else {
//                edge = pttIter.transitions[symbol];
//                if (!rejecting) {
//                    if (edge.isKnown) {
//                        IntQueue commonPrefixEdge = edge.out;
//                        IntQueue commonPrefixEdgePrev = null;
//                        IntQueue commonPrefixInformant = outputIter;
//                        while (commonPrefixEdge != null && commonPrefixInformant != null &&
//                                commonPrefixEdge.value == commonPrefixInformant.value) {
//                            commonPrefixInformant = commonPrefixInformant.next;
//                            commonPrefixEdgePrev = commonPrefixEdge;
//                            commonPrefixEdge = commonPrefixEdge.next;
//                        }
//                        /*
//                        informant=x
//                        edge.out=y
//                        ->
//                        informant=lcp(x,y)^-1 x
//                        edge=lcp(x,y)
//                        pushback=lcp(x,y)^-1 y
//                        */
//                        if (commonPrefixEdgePrev == null) {
//                            edge.out = null;
//                        } else {
//                            commonPrefixEdgePrev.next = null;
//                        }
//                        edge.target.pushback(commonPrefixEdge);
//                        outputIter = commonPrefixInformant;
//                    } else {
//                        edge.out = outputIter;
//                        edge.isKnown = true;
//                        outputIter = null;
//                    }
//                }
//            }
//            pttIter = edge.target;
//        }
//        if (pttIter.kind == OSTIAState.Kind.ACCEPTING) {
//            if (!IntQueue.equals(pttIter.out, outputIter)) {
//                throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.out +
//                        "' but training sample has remaining suffix '" + outputIter + '\'');
//            }
//            if (rejecting) {
//                throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.out +
//                        "' but training sample tells to reject");
//            }
//        } else if (pttIter.kind == OSTIAState.Kind.REJECTING) {
//            if (!rejecting) {
//                throw new IllegalArgumentException("For input '" + input + "' the state rejects but training sample " +
//                        "has remaining suffix '" + pttIter.out +
//                        "'");
//            }
//        } else {
//            assert pttIter.kind == OSTIAState.Kind.UNKNOWN;
//            pttIter.kind = rejecting ? OSTIAState.Kind.REJECTING : OSTIAState.Kind.ACCEPTING;
//            pttIter.out = outputIter;
//        }
//
//
//    }

    private static void addBlueStates(State parent, Queue<Blue> blue) {
        for (int i = 0; i < parent.transitions.length; i++) {
            final Edge transition = parent.transitions[i];
            if (transition != null) {
                assert !contains(blue, transition.target);
                assert transition.target != parent;
                blue.add(new Blue(parent, i));
            }
        }
    }

    public static void ostia(State transducer) {
        final Queue<Blue> blue = new LinkedList<>();
        final Set<State> red = new LinkedHashSet<>();
        assert isTree(transducer, new HashSet<>());
        red.add(transducer);
        addBlueStates(transducer, blue);
        assert uniqueItems(blue);
        assert disjoint(blue, red);
        assert validateBlueAndRed(transducer, red, blue);
        blue:
        while (!blue.isEmpty()) {
            final @NonNull Blue next = blue.poll();
            final @Nullable State blueState = next.state();
            assert blueState != null;
            assert isTree(blueState, new HashSet<>());
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);

            for (State redState : red) {
                if (ostiaMerge(next, redState, blue, red)) {
                    assert disjoint(blue, red);
                    assert uniqueItems(blue);
                    continue blue;
                }
            }
            assert isTree(blueState, new HashSet<>());
            assert uniqueItems(blue);
            addBlueStates(blueState, blue);
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);
            red.add(blueState);
            assert disjoint(blue, red);
            assert validateBlueAndRed(transducer, red, blue);
        }
    }

    private static boolean ostiaMerge(Blue blue, State redState, Queue<Blue> blueToVisit, Set<State> red) {
        final Map<State, StateCopy> merged = new HashMap<>();
        final List<Blue> reachedBlueStates = new ArrayList<>();
        if (ostiaFold(redState, null, blue.parent, blue.symbol, merged, reachedBlueStates)) {

            for (Map.Entry<State, StateCopy> mergedRedState : merged.entrySet()) {
                assert mergedRedState.getKey() == mergedRedState.getValue().original;
                mergedRedState.getValue().assign();
            }
            for (Blue reachedBlueCandidate : reachedBlueStates) {
                if (red.contains(reachedBlueCandidate.parent)) {
                    assert !contains(blueToVisit, reachedBlueCandidate.state());
                    blueToVisit.add(reachedBlueCandidate);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean ostiaFold(State red,
                                     @Nullable IntQueue pushedBack,
                                     State blueParent,
                                     int symbolIncomingToBlue,
                                     Map<State, StateCopy> mergedStates,
                                     List<Blue> reachedBlueStates) {
        final Edge incomingTransition = blueParent.transitions[symbolIncomingToBlue];
        assert incomingTransition != null;
        final State blueState = incomingTransition.target;
        assert red != blueState;
        assert !mergedStates.containsKey(blueState);

        final StateCopy mergedRedState = mergedStates.computeIfAbsent(red, StateCopy::new);
        final StateCopy mergedBlueState = new StateCopy(blueState);
        final Edge mergedIncomingTransition =
                mergedStates.computeIfAbsent(blueParent, StateCopy::new).transitions[symbolIncomingToBlue];
        assert mergedIncomingTransition != null;
        mergedIncomingTransition.target = red;

        final StateCopy prevBlue = mergedStates.put(blueState, mergedBlueState);
        assert prevBlue == null;

        mergedBlueState.prepend(pushedBack);
        if (mergedBlueState.kind == OSTIAState.Kind.ACCEPTING) {
            if (mergedRedState.kind == OSTIAState.Kind.UNKNOWN) {
                mergedRedState.out = mergedBlueState.out;
                mergedRedState.kind = OSTIAState.Kind.ACCEPTING;
            } else if (mergedRedState.kind == OSTIAState.Kind.REJECTING) {
                return false;
            } else if (!IntQueue.equals(mergedRedState.out, mergedBlueState.out)) {
                return false;
            }
        } else if (mergedBlueState.kind == OSTIAState.Kind.REJECTING) {
            if (mergedRedState.kind == OSTIAState.Kind.ACCEPTING) {
                return false;
            } else if (mergedRedState.kind == OSTIAState.Kind.UNKNOWN) {
                mergedRedState.kind = OSTIAState.Kind.REJECTING;
            }
        }
        for (int i = 0; i < mergedRedState.transitions.length; i++) {
            final Edge transitionBlue = mergedBlueState.transitions[i];
            if (transitionBlue != null) {
                final Edge transitionRed = mergedRedState.transitions[i];
                if (transitionRed == null) {
                    mergedRedState.transitions[i] = new Edge(transitionBlue);
                    reachedBlueStates.add(new Blue(red, i));
                } else {
                    if (transitionRed.isKnown) {
                        IntQueue commonPrefixRed = transitionRed.out;
                        IntQueue commonPrefixBlue = transitionBlue.out;
                        IntQueue commonPrefixBluePrev = null;
                        while (commonPrefixBlue != null && commonPrefixRed != null &&
                                commonPrefixBlue.value == commonPrefixRed.value) {
                            commonPrefixBluePrev = commonPrefixBlue;
                            commonPrefixBlue = commonPrefixBlue.next;
                            commonPrefixRed = commonPrefixRed.next;
                        }
                        assert commonPrefixBluePrev == null || commonPrefixBluePrev.next == commonPrefixBlue;
                        if (commonPrefixRed == null) {//check if no leftover output remains on red edge
                            if (commonPrefixBluePrev == null) {
                                transitionBlue.out = null;
                            } else {
                                commonPrefixBluePrev.next = null;
                            }
                            assert Objects.equals(Optional.ofNullable(mergedBlueState.transitions[i]).map(e -> e.target),
                                    Optional.ofNullable(blueState.transitions[i]).map(e -> e.target));
                            if (!ostiaFold(transitionRed.target,
                                    commonPrefixBlue,
                                    blueState,
                                    i,
                                    mergedStates,
                                    reachedBlueStates)) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        transitionRed.isKnown = transitionBlue.isKnown;
                        transitionRed.out = transitionBlue.out;
                        if (!ostiaFold(transitionRed.target,
                                null,
                                blueState,
                                i,
                                mergedStates,
                                reachedBlueStates)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static @Nullable IntSeq run(State init, Iterable<Integer> input) {
        return run(init, input.iterator());
    }

    public static @Nullable IntSeq run(State init, Iterator<Integer> input) {
        final List<Integer> output = new ArrayList<>();
        State iter = init;
        while (input.hasNext()) {
            final Integer i = input.next();
            if (i == null) return null;
            if (i >= iter.transitions.length) return null;
            final Edge edge = iter.transitions[i];
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

    // Assertion methods

    private static boolean disjoint(Queue<Blue> blue, Set<State> red) {
        for (Blue b : blue) {
            if (red.contains(b.state())) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(Queue<Blue> blue, @Nullable State state) {
        return Util.exists(blue, b -> Objects.equals(state, b.state()));
    }

    private static boolean uniqueItems(Queue<Blue> blue) {
        return Util.unique(blue, Blue::state);
    }

    private static boolean validateBlueAndRed(State root, Set<State> red, Queue<Blue> blue) {
        final Set<State> reachable = new HashSet<>();
        isTree(root, reachable);
        for (State r : red) {
            for (Edge edge : r.transitions) {
                assert edge == null || contains(blue, edge.target) ^ red.contains(edge.target);
            }
            assert reachable.contains(r);
        }
        for (Blue b : blue) {
            assert red.contains(b.parent);
            assert reachable.contains(b.state());
        }
        return true;
    }

    private static boolean isTree(State root, Set<State> nodes) {
        final Queue<State> toVisit = new ArrayDeque<>();
        toVisit.add(root);
        boolean isTree = true;
        while (!toVisit.isEmpty()) {
            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
            final @NonNull State s = toVisit.poll();
            if (nodes.add(s)) {
                for (Edge edge : s.transitions) {
                    if (edge != null) {
                        toVisit.add(edge.target);
                    }
                }
            } else {
                isTree = false;
            }

        }
        return isTree;
    }

    static class Edge {
        boolean isKnown;
        @Nullable IntQueue out;
        State target;

        Edge() {
        }

        Edge(Edge edge) {
            out = IntQueue.copyAndConcat(edge.out, null);
            target = edge.target;
            isKnown = edge.isKnown;
        }

        @Override
        public String toString() {
            return String.valueOf(target);
        }
    }

    static class Blue {

        final State parent;
        final int symbol;

        Blue(State parent, int symbol) {
            this.symbol = symbol;
            this.parent = parent;
        }

        @Nullable State state() {
            final @Nullable Edge edge = parent.transitions[symbol];
            assert edge != null;
            return edge.target;
        }

        @Override
        public String toString() {
            return String.valueOf(state());
        }
    }

    static class StateParent {

        OSTIAState.Kind kind = OSTIAState.Kind.UNKNOWN;
        @Nullable IntQueue out;
        @Nullable Edge[] transitions;

        @Override
        public String toString() {
            return String.valueOf(out);
        }
    }

    static class StateCopy extends StateParent {

        final State original;

        StateCopy(State original) {
            super.out = IntQueue.copyAndConcat(original.out, null);
            super.transitions = copyTransitions(original.transitions);
            this.original = original;
            this.kind = original.kind;
        }

        private static @Nullable Edge[] copyTransitions(@Nullable Edge[] transitions) {
            final @Nullable Edge[] copy = new Edge[transitions.length];
            for (int i = 0; i < copy.length; i++) {
                @Nullable Edge edge = transitions[i];
                copy[i] = edge == null ? null : new Edge(edge);
            }
            return copy;
        }

        void assign() {
            original.out = out;
            original.kind = kind;
            original.transitions = transitions;
        }

        /**
         * The IntQueue is consumed and should not be reused after calling this method.
         */
        void prepend(@Nullable IntQueue prefix) {
            for (@Nullable Edge edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
            if (kind == OSTIAState.Kind.ACCEPTING) {//UNKNOWN
//                out = prefix;
//                kind = ACCEPTING;
//            } else {
                out = IntQueue.copyAndConcat(prefix, out);
            }
        }
    }

    public static class State extends StateParent implements OSTIAState<Edge, State> {

        final IntSeq shortest;

        State(int alphabetSize, IntSeq shortest) {
            super.out = null;
            super.transitions = new Edge[alphabetSize];
            this.shortest = shortest;
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
        public Edge transition(int symbol) {
            return transitions[symbol];
        }

        @Override
        public boolean isKnown(Edge edge) {
            return edge.isKnown;
        }

        @Override
        public void setKnown(Edge edge, boolean isKnown) {
            edge.isKnown = isKnown;
        }

        @Override
        public IntQueue getOutput(Edge edge) {
            return edge.out;
        }

        @Override
        public void setOutput(Edge edge, IntQueue out) {
            edge.out = out;
        }

        @Override
        public void pushback(IntQueue prefix) {
            for (@Nullable Edge edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
            if (kind == Kind.ACCEPTING) {
                out = IntQueue.copyAndConcat(prefix, out);
            }
        }

        @Override
        public IntQueue getStateOutput() {
            return out;
        }

        @Override
        public void setStateOutput(IntQueue out) {
            this.out = out;
        }

        @Override
        public State getTarget(Edge edge) {
            return edge.target;
        }

        @Override
        public void setChild(int symbol, Edge edge) {
            edge.target = new OSTIA.State(transitions.length, shortest.concat(new IntSeq(symbol)));
            transitions[symbol] = edge;
        }

        @Override
        public Edge edgeConstructor() {
            return new Edge();
        }
    }
}