package net.alagris.core.learn;

import java.util.*;
import java.util.Queue;

import net.alagris.core.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class OSTIA {


    public static State buildPtt(IntEmbedding alph, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alph.size(), IntSeq.Epsilon);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            OSTIAState.buildPttOnward(root, alph, inout.l(), inout.r());
        }
        return root;
    }


    public static void addBlueStates(State parent, Queue<Blue> blue) {
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
                if (null!=ostiaMerge(next, redState, blue, red)) {
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


    public static Map<State, StateCopy> ostiaMerge(Blue blue, State redState, Queue<Blue> blueToVisit, Set<State> red) {
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
            return merged;
        }
        return null;
    }

    public static boolean ostiaFold(State red,
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


    // Assertion methods

    public static boolean disjoint(Queue<Blue> blue, Set<State> red) {
        for (Blue b : blue) {
            if (red.contains(b.state())) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(Queue<Blue> blue, @Nullable State state) {
        return Util.exists(blue, b -> Objects.equals(state, b.state()));
    }

    public static boolean uniqueItems(Queue<Blue> blue) {
        return Util.unique(blue, Blue::state);
    }

    public static boolean validateBlueAndRed(State root, Set<State> red, Queue<Blue> blue) {
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

    public static boolean isTree(State root, Set<State> nodes) {
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

        public final IntSeq shortest;
        int index;

        State(int alphabetSize, IntSeq shortest) {
            super.out = null;
            super.transitions = new Edge[alphabetSize];
            this.shortest = shortest;
        }

        @Override
        public String toString() {
            return "State{" +
                    "shortest=" + shortest +
                    ", index=" + index +
                    '}';
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
        public int transitionCount() {
            return transitions.length;
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

        public IntSeq shortest() {
            return shortest;
        }
    }


}