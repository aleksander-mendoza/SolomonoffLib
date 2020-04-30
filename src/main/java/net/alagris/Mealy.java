package net.alagris;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.alagris.Glushkov.Renamed;

public class Mealy {

    public static class Transition {
        /**
         * If inputFromInclusive>inputToInclusive then it's an epsilon transition.
         */
        int inputFromInclusive, inputToInclusive;
        /**
         * epsilon is encoded as "", empty language is encoded as null. By default all
         * transitions output empty language. Concatenation of empty language with
         * anything, yields empty language. If automaton returns empty language as
         * output, then it is equivalent to rejection. Therefore those two facts imply
         * that if automaton returns empty language at any point, then it rejects (or in
         * non-deterministic case, the particular computation branch rejects)
         */
        String output;
    }

    public static class Tran {
        final int inputFromInclusive, inputToInclusive, toState;
        final String output;

        public Tran(Transition t, int toState) {
            assert t.output != null;
            inputFromInclusive = t.inputFromInclusive;
            inputToInclusive = t.inputToInclusive;
            assert inputFromInclusive <= inputToInclusive : "Epsilon transititon! Shouldn't be here!";
            this.toState = toState;
            output = t.output;
        }

        public Tran(int inputFromInclusive, int inputToInclusive, int toState, String output) {
            this.inputFromInclusive = inputFromInclusive;
            this.inputToInclusive = inputToInclusive;
            this.toState = toState;
            this.output = output;
        }

    }

    /** epsilon-free transitions! */
    final Tran[][] tranisitons;
    final String[] mooreOutput;
    final int initialState;

    public static Mealy compile(String emptyWordOutput, HashMap<Integer, String> startStates, String[][] matrix,
            HashMap<Integer, String> endStates, Renamed[] indexToState) {

        final int stateCount = matrix.length + 1;
        final int initialState = matrix.length;
        final Tran[][] transitions = new Tran[stateCount][];
        for (int state = 0; state < matrix.length; state++) {
            final String[] outgoing = matrix[state];
            final ArrayList<Tran> filteredOutgoing = new ArrayList<>();
            assert outgoing.length == matrix.length : "Length " + outgoing.length + " != " + matrix.length;
            assert outgoing.length == transitions.length - 1 : outgoing.length + " != " + (transitions.length - 1);
            for (int target = 0; target < outgoing.length; target++) {
                final String tranOutput = outgoing[target];
                if (tranOutput != null) {
                    final Renamed targetState = indexToState[target];
                    filteredOutgoing.add(new Tran(targetState.inputFrom, targetState.inputTo, target, tranOutput));
                }
            }
            filteredOutgoing.sort((a, b) -> Integer.compare(a.inputFromInclusive, b.inputFromInclusive));
            transitions[state] = filteredOutgoing.toArray(new Tran[0]);
        }
        final Tran[] initialTransitions = new Tran[startStates.size()];
        {
            int i = 0;
            for (Entry<Integer, String> startEntry : startStates.entrySet()) {
                final int toState = startEntry.getKey();
                final Renamed original = indexToState[toState];
                initialTransitions[i++] = new Tran(original.inputFrom, original.inputTo, toState,
                        startEntry.getValue());
            }
            Arrays.sort(initialTransitions, (a, b) -> Integer.compare(a.inputFromInclusive, b.inputFromInclusive));
        }
        transitions[initialState] = initialTransitions;
        final String[] mooreOutput = new String[transitions.length];
        mooreOutput[initialState] = emptyWordOutput;
        for (Entry<Integer, String> endEntry : endStates.entrySet()) {
            mooreOutput[endEntry.getKey()] = endEntry.getValue();
        }
        return new Mealy(transitions, mooreOutput, initialState);
    }

    public static class Bits extends BitSet {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        private final int len;

        public Bits(int len) {
            super(len);
            this.len = len;
        }

        public int len() {
            return len;
        }

    }

    public void checkForNondeterminism() {
        final HashSet<String> visitedSuperpositions = new HashSet<>();
        final Stack<Bits> toVisit = new Stack<>();
        final Bits initial = new Bits(stateCount());
        initial.set(initialState);
        toVisit.push(initial);
        while (!toVisit.isEmpty()) {
            final Bits next = toVisit.pop();
            checkIfSuperpositionAcceptsInMultiplePlaces(next);
            final HashSet<Integer> inputsThatNeedChecking = superpositionAllTransitionsAligned(next);
            for (int input : inputsThatNeedChecking) {
                final Bits afterTransitioning = superpositionTransition(next, input);
                final String serializedAfterTransitioning = afterTransitioning.toString();
                if (visitedSuperpositions.add(serializedAfterTransitioning)) {
                    toVisit.add(afterTransitioning);
                }
            }
        }
    }

    public void checkIfSuperpositionAcceptsInMultiplePlaces(Bits superposition) {
        int acceptingState = -1;
        for (int state : (Iterable<Integer>) () -> superposition.stream().iterator()) {
            if (mooreOutput[state] != null) {
                if (acceptingState == -1) {
                    acceptingState = state;
                } else {
                    throw new IllegalStateException("It's possible to accept both " + state + " and " + acceptingState);
                }
            }
        }
    }

    /**
     * Returns list of all inputs at which something changes among transitions. For
     * instance, if all states in the superposition have only one transition [a-h],
     * then the output will contain only [a,h+1]. But if there is at least one state
     * which has different transition, say [e-o], then the output will contain
     * [a,e,h+1,o+1].
     */
    public HashSet<Integer> superpositionAllTransitionsAligned(Bits superposition) {
        final HashSet<Integer> checkPoints = new HashSet<>();
        checkPoints.add(Integer.MIN_VALUE);
        for (int state : (Iterable<Integer>) () -> superposition.stream().iterator()) {
            for (Tran transition : tranisitons[state]) {
                checkPoints.add(transition.inputFromInclusive);
                checkPoints.add(transition.inputToInclusive + 1);
            }
        }
        return checkPoints;
    }

    public Bits superpositionTransition(Bits superposition, int input) {
        assert superposition.len() == stateCount() : "Wrong size " + superposition.length() + " != " + stateCount();
        final Bits output = new Bits(stateCount());
        for (int state : (Iterable<Integer>) () -> superposition.stream().iterator()) {
            It<Tran> transitionsTaken = transitionsFor(state, input);
            Tran next = transitionsTaken.next();
            while (next != null) {
                if (output.get(next.toState)) {
                    throw new IllegalStateException(
                            "Superposition " + superposition + " has two conflicting transitions to " + next.toState
                                    + " over input " + input + " and one of them comes from " + state);
                }
                output.set(next.toState);

                next = transitionsTaken.next();
            }
        }
        return output;
    }

    public interface It<T> {
        T next();
    }

    public It<Tran> transitionsFor(int state, int input) {
        final Tran[] trans = tranisitons[state];
        int i = trans.length - 1;
        for (; i >= 0; i--) {
            if (trans[i].inputFromInclusive <= input) {
                break;
            }
        }
        final int last = i;
        return new It<Tran>() {
            int index = last;

            @Override
            public Tran next() {
                if (index < 0)
                    return null;
                if (input <= trans[index].inputToInclusive) {
                    return trans[index--];
                } else {
                    return null;
                }
            }
        };
    }

    private int stateCount() {
        return tranisitons.length;
    }

    public String evaluate(String input) {
        final List<Integer> list = input.codePoints().boxed().collect(Collectors.toList());
        return evaluate(list.size(), list.iterator());
    }

    public String evaluate(int inputLength, Iterator<Integer> input) {

        class T {
            int sourceState;
            String transitionOutput;

            public T(int sourceState, String transitionOutput) {
                this.sourceState = sourceState;
                this.transitionOutput = transitionOutput;
            }

        }
        final T[][] superpositionComputation = new T[inputLength + 1][];
        for (int i = 0; i < superpositionComputation.length; i++) {
            superpositionComputation[i] = new T[stateCount()];
        }
        superpositionComputation[0][initialState] = new T(-1, null);
        for (int charIndex = 0; charIndex < inputLength; charIndex++) {
            final int nextChar = input.next();
            final T[] fromSuperposition = superpositionComputation[charIndex];
            final T[] toSuperposition = superpositionComputation[charIndex + 1];
            for (int state = 0; state < stateCount(); state++) {
                if (fromSuperposition[state] != null) {
                    final It<Tran> transitions = transitionsFor(state, nextChar);
                    Tran nextTran = transitions.next();
                    while (nextTran != null) {
                        if (toSuperposition[nextTran.toState] != null) {
                            throw new IllegalStateException("Reached state " + nextTran.toState + " from both " + state
                                    + " (outputting " + nextTran.output + ") and "
                                    + toSuperposition[nextTran.toState].sourceState + " (outputting "
                                    + toSuperposition[nextTran.toState].transitionOutput + ")");
                        }
                        toSuperposition[nextTran.toState] = new T(state, nextTran.output);
                        nextTran = transitions.next();
                    }

                }
            }
        }
        final T[] last = superpositionComputation[inputLength];
        int acceptingState = -1;
        for (int state = 0; state < stateCount(); state++) {
            if (last[state] != null) {
                if (acceptingState == -1) {
                    acceptingState = state;
                } else {
                    throw new IllegalStateException(
                            "Both states " + acceptingState + " and " + state + " accepted simultaneously!");
                }
            }
        }
        if (acceptingState == -1)
            return null;
        final StringBuilder output = new StringBuilder(mooreOutput[acceptingState]);
        int backtrackedState = acceptingState;
        for (int charIndex = inputLength; charIndex > 0; charIndex--) {
            T pointer = superpositionComputation[charIndex][backtrackedState];
            backtrackedState = pointer.sourceState;
            output.insert(0, pointer.transitionOutput);
        }
        assert backtrackedState == initialState;
        return output.toString();

    }

    public Mealy(Tran[][] transitions, String[] mooreOutput, int initialState) {
        assert 0 <= initialState && initialState < mooreOutput.length;
        assert transitions.length == mooreOutput.length;
        this.tranisitons = transitions;
        this.mooreOutput = mooreOutput;
        this.initialState = initialState;
    }
}
