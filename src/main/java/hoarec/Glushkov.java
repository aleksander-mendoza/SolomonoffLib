package hoarec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import hoarec.Mealy.Transition;

import java.util.Map.Entry;

public class Glushkov {

   

    public interface G {
        boolean acceptsEmptyWord();

        String emptyWordOutput();

        /** Contain initial states and pre outputs */
        HashMap<Integer, String> getStartStates();

        /** Contain final states and post outputs */
        HashMap<Integer, String> getEndStates();

        void collectTransitions(Transition[][] matrix, Renamed[] indexToState);

    }

    private static void appendToLhs(HashMap<Integer, String> lhsInPlace, HashMap<Integer, String> rhs) {
        for (Entry<Integer, String> rhsEntry : rhs.entrySet()) {
            final String rhsV = rhsEntry.getValue();
            lhsInPlace.compute(rhsEntry.getKey(), (k, v) -> v == null ? rhsV : (v + rhsV));
        }
    }

    private static void prependToRhs(HashMap<Integer, String> lhs, HashMap<Integer, String> rhsInPlace) {
        for (Entry<Integer, String> lhsEntry : lhs.entrySet()) {
            final String lhsV = lhsEntry.getValue();
            rhsInPlace.compute(lhsEntry.getKey(), (k, v) -> v == null ? lhsV : (lhsV + v));
        }
    }

    private static void append(HashMap<Integer, String> lhs, String rhs) {
        if (rhs.equals(""))
            return;
        lhs.replaceAll((k, v) -> v + rhs);
    }

    private static void prepend(String lhs, HashMap<Integer, String> rhs) {
        if (lhs.equals(""))
            return;
        rhs.replaceAll((k, v) -> lhs + v);
    }

    public static class Union implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final String emptyWordOutput;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Union(String pre, G lhs, G rhs, String post) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() || rhs.acceptsEmptyWord();
            if (!Objects.equals(lhs.emptyWordOutput(), rhs.emptyWordOutput())) {
                throw new IllegalStateException("nondeterminism!");
            }
            emptyWordOutput = lhs.emptyWordOutput();

            start.putAll(lhs.getStartStates());
            start.putAll(rhs.getStartStates());
            end.putAll(lhs.getEndStates());
            end.putAll(rhs.getEndStates());
            prepend(pre, start);
            append(end, post);

        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

        @Override
        public void collectTransitions(Transition[][] matrix, Renamed[] indexToState) {
            lhs.collectTransitions(matrix, indexToState);
            rhs.collectTransitions(matrix, indexToState);
            
        }

    }

    public static class Concat implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final String emptyWordOutput;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Concat(String pre, G lhs, G rhs, String post) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() && rhs.acceptsEmptyWord();
            emptyWordOutput = emptyWord ? pre + lhs.emptyWordOutput() + rhs.emptyWordOutput() + post : null;
            start.putAll(lhs.getStartStates());
            if (lhs.acceptsEmptyWord()) {

                for (Entry<Integer, String> rhsStart : rhs.getStartStates().entrySet()) {
                    final String emptyWordOutput = lhs.emptyWordOutput();
                    start.put(rhsStart.getKey(), emptyWordOutput + rhsStart.getValue());
                }

            }
            end.putAll(rhs.getEndStates());
            if (rhs.acceptsEmptyWord()) {
                for (Entry<Integer, String> lhsEnd : lhs.getStartStates().entrySet()) {
                    final String emptyWordOutput = rhs.emptyWordOutput();
                    end.put(lhsEnd.getKey(), lhsEnd.getValue() + emptyWordOutput);
                }
            }

            prepend(pre, start);
            append(end, post);
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

        @Override
        public void collectTransitions(Transition[][] matrix, Renamed[] indexToState) {
            lhs.collectTransitions(matrix, indexToState);
            rhs.collectTransitions(matrix, indexToState);
            transitionProduct(matrix, indexToState, lhs.getEndStates(), rhs.getStartStates());
        }

    }

    public static class Kleene implements G {
        final G nested;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();
        final String emptyWordOutput;

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return true;
        }

        public Kleene(String pre, G nested, String post) {
            this.nested = nested;
            start.putAll(nested.getStartStates());
            prepend(pre, start);
            end.putAll(nested.getEndStates());
            append(end, post);
            if (pre == null)
                throw new NullPointerException();
            if (post == null)
                throw new NullPointerException();
            if (nested.acceptsEmptyWord() && !nested.emptyWordOutput().isEmpty())
                throw new IllegalStateException(
                        "Nondeterminism caused by empty word output under Kleene closure: " + nested.emptyWordOutput());
            emptyWordOutput = pre + post;
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

        @Override
        public void collectTransitions(Transition[][] matrix, Renamed[] indexToState) {
            nested.collectTransitions(matrix, indexToState);
            transitionProduct(matrix, indexToState, nested.getEndStates(), nested.getStartStates());
        }

    }

    private static void transitionProduct(Transition[][] matrix, Renamed[] indexToState, HashMap<Integer, String> from,
            HashMap<Integer, String> to) {
        for (Entry<Integer, String> fromE : from.entrySet()) {
            final int fromState = fromE.getKey();
            final Renamed sourceState = indexToState[fromState];
            for (Entry<Integer, String> toE : to.entrySet()) {
                final int toState = toE.getKey();
                final Renamed targetState = indexToState[toState];
                final Transition trans = matrix[fromState][toState];
                final String output = fromE.getValue() + toE.getValue();
                if (trans.output == null) {// fresh new transition
                    trans.output = output;
                    trans.inputFromInclusive = targetState.inputFrom;
                    trans.inputToInclusive = targetState.inputTo;
                } else {// mutating already set transition
                    if (!trans.output.equals(output)) {
                        throw new IllegalStateException(
                                "Transitioning from " + sourceState + " to " + targetState + " is nondeterministic!");
                    }
                    assert trans.inputFromInclusive == targetState.inputFrom : "Transitioning from " + sourceState
                            + " to " + targetState + " is bugged!";
                    assert trans.inputToInclusive == targetState.inputTo : "Transitioning from " + sourceState + " to "
                            + targetState + " is bugged!";
                }
            }
        }
    }

    public static class Renamed implements G {
        final int stateId;
        final int inputFrom, inputTo;
        final HashMap<Integer, String> end = new HashMap<>(1), start = new HashMap<>(1);
        final String emptyWordOutput;

        @Override
        public boolean acceptsEmptyWord() {
            return inputFrom > inputTo;// this encodes epsilon
        }

        public Renamed(int stateId, String preoutput, String postoutput) {
            this(stateId, 1, 0, preoutput, postoutput);
        }

        public Renamed(int stateId, int input, String preoutput, String postoutput) {
            this(stateId, input, input, preoutput, postoutput);
        }

        public Renamed(int stateId, int inputFrom, int inputTo, String preoutput, String postoutput) {
            this.stateId = stateId;
            this.inputFrom = inputFrom;
            this.inputTo = inputTo;
            if (preoutput == null)
                throw new NullPointerException();
            if (postoutput == null)
                throw new NullPointerException();
            end.put(stateId, postoutput);
            start.put(stateId, preoutput);
            emptyWordOutput = acceptsEmptyWord() ? preoutput + postoutput : null;
        }

        @Override
        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        @Override
        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

        @Override
        public String toString() {
            return stateId + "[" + inputFrom + "-" + inputTo + "]";
        }

        @Override
        public void collectTransitions(Transition[][] matrix, Renamed[] indexToState) {
            // pass
        }

    }
}
