package net.alagris;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;

import net.alagris.Simple.Eps;

public class Glushkov {

    public static class OutputWeight {
        public OutputWeight() {
            output = new StringBuilder();
        }
        
        public OutputWeight(String s) {
            output = new StringBuilder(s);
        }

        public OutputWeight(OutputWeight v) {
            output = new StringBuilder(v.output);
            weight = v.weight;
        }

        private final StringBuilder output;
        private int weight = 0;
        
        
        public String getOutput(){
            return output.toString();
        }
            
        @Override
        public String toString() {
            return output.toString();
        }
    }

    public static abstract class G {
        String emptyWordOutput;
        int emptyWordPreWeight = 0, emptyWordPostWeight = 0;
        final HashMap<Integer, OutputWeight> start = new HashMap<>(), end = new HashMap<>();

        public abstract void collectTransitions(String[][] matrix);

        public abstract void collectStates(Renamed[] indexToState);

        public abstract void serialize(StringBuilder sb, int indent);

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            serialize(sb, 0);
            return sb.toString();
        }

        public void append(String post) {
            emptyWordOutput = Glushkov.concat(emptyWordOutput, post);
            end.forEach((k, v) -> v.output.append(post));

        }

        public void prepend(String pre) {
            emptyWordOutput = Glushkov.concat(pre, emptyWordOutput);
            end.forEach((k, v) -> v.output.insert(0, pre));
        }

        public void putStart(HashMap<Integer, OutputWeight> start) {
            start.forEach((k, v) -> this.start.put(k, new OutputWeight(v)));
        }

        public void putEnd(HashMap<Integer, OutputWeight> end) {
            end.forEach((k, v) -> this.end.put(k, new OutputWeight(v)));
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

    public static class Union extends G {
        final G lhs, rhs;

        public Union(G lhs, G rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            if (lhs.emptyWordOutput == null) {
                emptyWordOutput = rhs.emptyWordOutput;
                emptyWordPostWeight = rhs.emptyWordPostWeight;
                emptyWordPreWeight = rhs.emptyWordPreWeight;
            } else {
                if (rhs.emptyWordOutput == null) {
                    emptyWordOutput = lhs.emptyWordOutput;
                    emptyWordPostWeight = lhs.emptyWordPostWeight;
                    emptyWordPreWeight = lhs.emptyWordPreWeight;
                } else {
                    throw new IllegalStateException("Both lhs and rhs accept empty word!");
                }
            }
            putStart(lhs.start);
            putStart(rhs.start);
            putEnd(lhs.end);
            putEnd(rhs.end);
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
            lhs.serialize(sb, indent + 1);
            rhs.serialize(sb, indent + 1);
        }

    }

    public static class Concat extends G {
        final G lhs, rhs;

        public Concat(G lhs, G rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWordOutput = Glushkov.concat(lhs.emptyWordOutput,rhs.emptyWordOutput);
            
            putStart(lhs.start);
            if (lhs.emptyWordOutput != null) {
                for (Entry<Integer, OutputWeight> rhsStart : rhs.start.entrySet()) {
                    OutputWeight previous = start.put(rhsStart.getKey(), new OutputWeight(lhs.emptyWordOutput + rhsStart.getValue()));
                    assert previous == null : previous;
                }
            }
            putEnd(rhs.end);
            if (rhs.emptyWordOutput != null) {
                for (Entry<Integer, OutputWeight> lhsEnd : lhs.end.entrySet()) {
                    OutputWeight previous = end.put(lhsEnd.getKey(), new OutputWeight(lhsEnd.getValue() + rhs.emptyWordOutput));
                    assert previous == null : previous;
                }
            }

        }

        @Override
        public void collectTransitions(String[][] matrix) {
            lhs.collectTransitions(matrix);
            rhs.collectTransitions(matrix);
            transitionProduct(matrix, lhs.end, rhs.start);
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
            lhs.serialize(sb, indent + 1);
            rhs.serialize(sb, indent + 1);
        }
    }

    public static class Kleene extends G {
        final G nested;

        public Kleene(G nested) {
            if (nested.emptyWordOutput != null && !nested.emptyWordOutput.isEmpty()) {
                throw new IllegalStateException("Empty word prints non-empty output \"" + nested.emptyWordOutput
                        + "\" under Kleene closure, resulting in inifitely many outputs!");
            }
            this.nested = nested;
            putStart(nested.start);
            putEnd(nested.end);
            emptyWordOutput = "";
        }

        @Override
        public void collectTransitions(String[][] matrix) {
            nested.collectTransitions(matrix);
            transitionProduct(matrix, nested.end, nested.start);
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            nested.collectStates(indexToState);
        }

        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append("* ").append(emptyWordOutput).append(" ").append(start).append(" ").append(end).append("\n");
            nested.serialize(sb, indent + 1);
        }
    }

    private static void transitionProduct(String[][] matrix, HashMap<Integer, OutputWeight> from,
            HashMap<Integer, OutputWeight> to) {
        for (Entry<Integer, OutputWeight> fromE : from.entrySet()) {
            final int fromState = fromE.getKey();
            for (Entry<Integer, OutputWeight> toE : to.entrySet()) {
                final int toState = toE.getKey();
                final String transOutput = matrix[fromState][toState];
                final String output = fromE.getValue().output.toString() + toE.getValue().output.toString();
                assert transOutput == null : "The transition from " + fromState + " to " + toState
                        + " should be uniquely determined!";
                matrix[fromState][toState] = output;
            }
        }
    }

    public static class Renamed extends G {
        final int stateId;
        final int inputFrom, inputTo;

        public Renamed(int stateId, int input) {
            this(stateId, input, input);
        }

        public Renamed(int stateId, int inputFrom, int inputTo) {
            assert inputFrom <= inputTo : "Epsilon transitions shouldn't be here! Remove epsilons and put their output in post-output or emptyWordOutput!";
            assert stateId >= 0;
            this.stateId = stateId;
            this.inputFrom = inputFrom;
            this.inputTo = inputTo;
            end.put(stateId, new OutputWeight());
            start.put(stateId, new OutputWeight());
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
            sb.append(stateId + "[" + (char) inputFrom + "-" + (char) inputTo + "] " + start + " " + end + "\n");
        }
    }

}
