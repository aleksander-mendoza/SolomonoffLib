package net.alagris;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.StringJoiner;

import de.learnlib.api.algorithm.PassiveLearningAlgorithm;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.words.Alphabet;
import net.automatalib.words.GrowingAlphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.GrowingMapAlphabet;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
public class OSTIA {


    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alphabetSize, IntSeq.Epsilon);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            buildPttOnward(root, inout.l(), IntQueue.asQueue(inout.r()));
        }
        return root;
    }

    private static void buildPttOnward(State ptt, IntSeq input, @Nullable IntQueue output) {
        State pttIter = ptt;
        @Nullable IntQueue outputIter = output;

        for (int i = 0; i < input.size(); i++) {//input index
            final int symbol = input.get(i);
            final Edge edge;
            if (pttIter.transitions[symbol] == null) {
                edge = new Edge();
                edge.out = outputIter;
                edge.target = new State(pttIter.transitions.length,ptt.shortest.concat(new IntSeq(symbol)));
                pttIter.transitions[symbol] = edge;
                outputIter = null;
            } else {
                edge = pttIter.transitions[symbol];
                IntQueue commonPrefixEdge = edge.out;
                IntQueue commonPrefixEdgePrev = null;
                IntQueue commonPrefixInformant = outputIter;
                while (commonPrefixEdge != null && commonPrefixInformant != null &&
                       commonPrefixEdge.value == commonPrefixInformant.value) {
                    commonPrefixInformant = commonPrefixInformant.next;
                    commonPrefixEdgePrev = commonPrefixEdge;
                    commonPrefixEdge = commonPrefixEdge.next;
                }
                /*
                informant=x
                edge.out=y
                ->
                informant=lcp(x,y)^-1 x
                edge=lcp(x,y)
                pushback=lcp(x,y)^-1 y
                */
                if (commonPrefixEdgePrev == null) {
                    edge.out = null;
                } else {
                    commonPrefixEdgePrev.next = null;
                }
                edge.target.prependButIgnoreMissingStateOutput(commonPrefixEdge);
                outputIter = commonPrefixInformant;
            }
            pttIter = edge.target;
        }
        if (pttIter.out != null && !IntQueue.equals(pttIter.out.str, outputIter)) {
            throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.out +
                                               "' but training sample has remaining suffix '" + outputIter + '\'');
        }
        pttIter.out = new Out(outputIter);
    }

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
            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
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
        if (mergedBlueState.out != null) {
            if (mergedRedState.out == null) {
                mergedRedState.out = mergedBlueState.out;
            } else if (!IntQueue.equals(mergedRedState.out.str, mergedBlueState.out.str)) {
                return false;
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
                    if (commonPrefixRed == null) {
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
                }
            }
        }
        return true;
    }
    public static @Nullable IntSeq run(State init, IntSeq input) {
        final List<Integer> output = new ArrayList<>();
        State iter = init;
        for (int i = 0; i < input.size(); i++) {
            final Edge edge = iter.transitions[input.get(i)];
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
        if (iter.out == null) {
            return null;
        }
        IntQueue q = iter.out.str;
        while (q != null) {
            output.add(q.value);
            q = q.next;
        }
        int[] arr = new int[output.size()];
        for(int i=0;i<output.size();i++) {
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
        for (Blue b : blue) {
            if (Objects.equals(state, b.state())) {
                return true;
            }
        }
        return false;
    }

    private static boolean uniqueItems(Queue<Blue> blue) {
        final Set<@Nullable State> unique = new HashSet<>();
        for (Blue b : blue) {
            if (!unique.add(b.state())) {
                return false;
            }
        }
        return true;
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
    static class Out {

        @Nullable IntQueue str;

        Out(@Nullable IntQueue str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return String.valueOf(str);
        }
    }
    static class Edge {

        @Nullable IntQueue out;
        State target;

        Edge() {}

        Edge(Edge edge) {
            out = IntQueue.copyAndConcat(edge.out, null);
            target = edge.target;
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

        @Nullable Out out;
        @Nullable Edge[] transitions;

        @Override
        public String toString() {
            return String.valueOf(out);
        }
    }
    static class StateCopy extends StateParent {

        final State original;

        StateCopy(State original) {
            super.out = original.out == null ? null : new Out(IntQueue.copyAndConcat(original.out.str, null));
            super.transitions = copyTransitions(original.transitions);
            this.original = original;
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
            if (out == null) {
                out = new Out(prefix);
            } else {
                out.str = IntQueue.copyAndConcat(prefix, out.str);
            }
        }
    }
    public static class State extends StateParent {

    	final IntSeq shortest;
        State(int alphabetSize,IntSeq shortest) {
            super.out = null;
            super.transitions = new Edge[alphabetSize];
            this.shortest = shortest;
        }

        /**
         * The IntQueue is consumed and should not be reused after calling this method.
         */
        void prependButIgnoreMissingStateOutput(@Nullable IntQueue prefix) {
            for (@Nullable Edge edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
            if (out != null) {
                out.str = IntQueue.copyAndConcat(prefix, out.str);
            }
        }
    }
}