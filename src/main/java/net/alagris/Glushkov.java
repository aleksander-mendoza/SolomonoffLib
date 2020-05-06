package net.alagris;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;

import net.alagris.Simple.Eps;

public class Glushkov {

    public static abstract class G {
        public abstract String emptyWordOutput();

        /** Contain initial states and pre outputs */
        public abstract HashMap<Integer, String> getStartStates();

        /** Contain final states and post outputs */
        public abstract HashMap<Integer, String> getEndStates();

        public abstract void collectTransitions(String[][] matrix);

        public abstract void collectStates(Renamed[] indexToState);

        
        public abstract void serialize(StringBuilder sb, int indent);
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            serialize(sb, 0);
            return sb.toString();
        }
    }

    public static String concat(String a, String b) {
        if (a == null || b == null)
            return null;
        return a + b;
    }

    public static boolean prefix(String prefix, String superstring) {
        if (prefix == null && superstring == null)
            return true;
        return superstring == null ? false : superstring.startsWith(prefix);
    }

    public static boolean suffix(String superstring, String suffix) {
        if (suffix == null && superstring == null)
            return true;
        return superstring == null ? false : superstring.endsWith(suffix);
    }

    private static void appendToLhs(HashMap<Integer, String> lhsInPlace, HashMap<Integer, String> rhs) {
        for (Entry<Integer, String> rhsEntry : rhs.entrySet()) {
            final String rhsV = rhsEntry.getValue();
            lhsInPlace.compute(rhsEntry.getKey(), (k, v) -> v == null ? rhsV : concat(v, rhsV));
        }
    }

    private static void prependToRhs(HashMap<Integer, String> lhs, HashMap<Integer, String> rhsInPlace) {
        for (Entry<Integer, String> lhsEntry : lhs.entrySet()) {
            final String lhsV = lhsEntry.getValue();
            rhsInPlace.compute(lhsEntry.getKey(), (k, v) -> v == null ? lhsV : concat(lhsV, v));
        }
    }

    private static void append(HashMap<Integer, String> lhs, String rhs) {
        if (rhs.equals(""))
            return;
        lhs.replaceAll((k, v) -> concat(v, rhs));
    }

    private static void prepend(String lhs, HashMap<Integer, String> rhs) {
        if (lhs.equals(""))
            return;
        rhs.replaceAll((k, v) -> concat(lhs, v));
    }

    
    public static class Union extends G {
        final G lhs, rhs;
        final String emptyWordOutput;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        public Union(String pre, G lhs, G rhs, String post, String epsilon) {
            assert pre != null;
            assert post != null;
            assert lhs.emptyWordOutput() == null || rhs.emptyWordOutput() == null
                    || lhs.emptyWordOutput().equals(rhs.emptyWordOutput());
            assert Objects.equals(
                    concat(concat(pre, rhs.emptyWordOutput() == null ? lhs.emptyWordOutput() : rhs.emptyWordOutput()),
                            post),
                    epsilon);
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWordOutput = epsilon;
            assert Collections.disjoint(lhs.getStartStates().keySet(), rhs.getStartStates().keySet());
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
        public void collectTransitions(String[][] matrix) {
            lhs.collectTransitions(matrix);
            rhs.collectTransitions(matrix);

        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            lhs.collectStates(indexToState);
            rhs.collectStates(indexToState);
        }
        
        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append("| ").append(emptyWordOutput).append(" ").append(start).append(" ").append(end).append("\n");
            lhs.serialize(sb, indent+1);
            rhs.serialize(sb, indent+1);
        }

    }

    public static class Concat extends G {
        final G lhs, rhs;
        final String emptyWordOutput;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        public Concat(String pre, G lhs, G rhs, String post, String epsilon) {
            assert pre != null;
            assert post != null;

            assert Objects.equals(concat(concat(concat(pre, lhs.emptyWordOutput()), rhs.emptyWordOutput()), post),
                    epsilon);
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWordOutput = epsilon;
            start.putAll(lhs.getStartStates());
            if (lhs.emptyWordOutput() != null) {

                for (Entry<Integer, String> rhsStart : rhs.getStartStates().entrySet()) {
                    String previous = start.put(rhsStart.getKey(), lhs.emptyWordOutput() + rhsStart.getValue());
                    assert previous == null : previous;
                }

            }
            end.putAll(rhs.getEndStates());
            if (rhs.emptyWordOutput() != null) {
                for (Entry<Integer, String> lhsEnd : lhs.getEndStates().entrySet()) {
                    String previous = end.put(lhsEnd.getKey(), lhsEnd.getValue() + rhs.emptyWordOutput());
                    assert previous == null : previous;
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
        public void collectTransitions(String[][] matrix) {
            lhs.collectTransitions(matrix);
            rhs.collectTransitions(matrix);
            transitionProduct(matrix, lhs.getEndStates(), rhs.getStartStates());
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            lhs.collectStates(indexToState);
            rhs.collectStates(indexToState);
        }
        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append(". ").append(emptyWordOutput).append(" ").append(start).append(" ").append(end).append("\n");
            lhs.serialize(sb, indent+1);
            rhs.serialize(sb, indent+1);
        }
    }

    public static class Kleene extends G {
        final G nested;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();
        final String emptyWordOutput;

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        public Kleene(String pre, G nested, String post, String epsilon) {
            assert pre != null;
            assert post != null;
            assert prefix(pre, epsilon) : pre + " not prefix of " + epsilon;
            assert suffix(epsilon, post) : post + " not suffix of " + epsilon;
            assert epsilon == null || pre.length() + post.length() >= epsilon.length() : epsilon + " !~ " + pre + " ++ "
                    + post;
            assert Objects.equals(
                    concat(concat(pre, nested.emptyWordOutput() == null ? "" : nested.emptyWordOutput()), post),
                    epsilon) : pre + " " + nested.emptyWordOutput() + " " + post + " != " + epsilon;
            assert nested.emptyWordOutput() == null || nested.emptyWordOutput().isEmpty() : nested.emptyWordOutput();
            this.nested = nested;
            start.putAll(nested.getStartStates());
            prepend(pre, start);
            end.putAll(nested.getEndStates());
            append(end, post);
            emptyWordOutput = epsilon;
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

        @Override
        public void collectTransitions(String[][] matrix) {
            nested.collectTransitions(matrix);
            transitionProduct(matrix, nested.getEndStates(), nested.getStartStates());
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            nested.collectStates(indexToState);
        }
        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append("* ").append(emptyWordOutput).append(" ").append(start).append(" ").append(end).append("\n");
            nested.serialize(sb, indent+1);
        }
    }

    private static void transitionProduct(String[][] matrix, HashMap<Integer, String> from,
            HashMap<Integer, String> to) {
        for (Entry<Integer, String> fromE : from.entrySet()) {
            final int fromState = fromE.getKey();
            for (Entry<Integer, String> toE : to.entrySet()) {
                final int toState = toE.getKey();
                final String transOutput = matrix[fromState][toState];
                final String output = fromE.getValue() + toE.getValue();
                assert transOutput == null : "The transition from "+fromState+" to "+toState+" should be uniquely determined!";
                matrix[fromState][toState] = output;
            }
        }
    }

    public static class Renamed extends G {
        final int stateId;
        final int inputFrom, inputTo;
        final HashMap<Integer, String> end = new HashMap<>(1), start = new HashMap<>(1);

        public Renamed(int stateId, int input, String preoutput, String postoutput) {
            this(stateId, input, input, preoutput, postoutput);
        }

        public Renamed(int stateId, int inputFrom, int inputTo, String preoutput, String postoutput) {
            assert preoutput != null;
            assert postoutput != null;
            assert inputFrom <= inputTo : "Epsilon transitions shouldn't be here! Remove epsilons and put their output in post-output or emptyWordOutput!";
            assert stateId >= 0;
            this.stateId = stateId;
            this.inputFrom = inputFrom;
            this.inputTo = inputTo;
            end.put(stateId, postoutput);
            start.put(stateId, preoutput);
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
            return null;
        }


        @Override
        public void collectTransitions(String[][] matrix) {
            // pass
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            indexToState[stateId] = this;
        }
        
        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append(stateId + "[" + (char)inputFrom + "-" + (char)inputTo + "] "+start+" "+end+"\n");
        }
    }

    public static Mealy glushkov(Eps regex) {
        if (regex.epsilonFree == null) {
            return new Mealy(new Mealy.Tran[][] { new Mealy.Tran[] {} }, new String[] { regex.epsilonOutput }, 0);
        } else {
            final EpsilonFree.Ptr<Integer> ptr = new EpsilonFree.Ptr<>();
            ptr.v = 0;
            final G renamed = regex.epsilonFree.glushkovRename(ptr);
            final int stateCount = ptr.v;
            final Renamed[] indexToState = new Renamed[stateCount];
            renamed.collectStates(indexToState);
            final String[][] matrix = new String[stateCount][];
            for (int fromState = 0; fromState < stateCount; fromState++) {
                matrix[fromState] = new String[stateCount];
            }
            renamed.collectTransitions(matrix);
            return Mealy.compile(renamed.emptyWordOutput(), renamed.getStartStates(), matrix, renamed.getEndStates(),
                    indexToState);
        }
    }
}
