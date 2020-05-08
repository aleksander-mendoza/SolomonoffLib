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

        public OutputWeight(String s, int weight) {
            output = new StringBuilder(s);
            this.weight = weight;
        }

        public OutputWeight(OutputWeight v) {
            output = new StringBuilder(v.output);
            weight = v.weight;
        }

        private final StringBuilder output;
        private int weight = 0;

        public String getOutput() {
            return output.toString();
        }

        @Override
        public String toString() {
            return output.toString() + " " + weight;
        }

        public int weight() {
            return weight;
        }
    }

    public static abstract class G {
        String emptyWordOutput;
        int emptyWordWeight = 0;
        final HashMap<Integer, OutputWeight> start = new HashMap<>(), end = new HashMap<>();

        public abstract void collectTransitions(String[][] outputMatrix,int[][] weightMatrix);

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

        public void addAfter(int weight) {
            if (emptyWordOutput != null)
                emptyWordWeight += weight;
            end.forEach((k, v) -> v.weight += weight);
        }

        public void addBefore(int weight) {
            if (emptyWordOutput != null)
                emptyWordWeight += weight;
            start.forEach((k, v) -> v.weight += weight);
        }

        public int emptyWordWeight() {
            return emptyWordOutput == null ? 0 : emptyWordWeight;
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
            if (rhs.emptyWordWeight() > lhs.emptyWordWeight() || lhs.emptyWordOutput == null) {
                emptyWordOutput = rhs.emptyWordOutput;
                emptyWordWeight = rhs.emptyWordWeight();
            } else if (lhs.emptyWordWeight() > rhs.emptyWordWeight() || rhs.emptyWordOutput == null) {
                emptyWordOutput = lhs.emptyWordOutput;
                emptyWordWeight = lhs.emptyWordWeight();
            } else if (Objects.equals(lhs.emptyWordOutput, rhs.emptyWordOutput)) {
                // pass
            } else {
                throw new IllegalStateException("Both lhs and rhs accept empty word but produce different outputs!");
            }
            putStart(lhs.start);
            putStart(rhs.start);
            putEnd(lhs.end);
            putEnd(rhs.end);
        }

        @Override
        public void collectTransitions(String[][] outputMatrix,int[][] weightMatrix) {
            lhs.collectTransitions(outputMatrix,weightMatrix);
            rhs.collectTransitions(outputMatrix,weightMatrix);

        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            lhs.collectStates(indexToState);
            rhs.collectStates(indexToState);
        }

        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append("| ").append(emptyWordOutput).append(" ").append(emptyWordWeight).append(" ").append(start).append(" ").append(end).append("\n");
            lhs.serialize(sb, indent + 1);
            rhs.serialize(sb, indent + 1);
        }

    }

    public static class Concat extends G {
        final G lhs, rhs;

        public Concat(G lhs, G rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWordOutput = Glushkov.concat(lhs.emptyWordOutput, rhs.emptyWordOutput);
            emptyWordWeight = lhs.emptyWordWeight() + rhs.emptyWordWeight();

            putStart(lhs.start);
            if (lhs.emptyWordOutput != null) {
                for (Entry<Integer, OutputWeight> rhsStart : rhs.start.entrySet()) {
                    OutputWeight previous = start.put(rhsStart.getKey(),
                            new OutputWeight(lhs.emptyWordOutput + rhsStart.getValue().getOutput(),
                                    lhs.emptyWordWeight() + rhsStart.getValue().weight()));
                    assert previous == null : previous;
                }
            }
            putEnd(rhs.end);
            if (rhs.emptyWordOutput != null) {
                for (Entry<Integer, OutputWeight> lhsEnd : lhs.end.entrySet()) {
                    OutputWeight previous = end.put(lhsEnd.getKey(),
                            new OutputWeight(lhsEnd.getValue().getOutput() + rhs.emptyWordOutput,
                                    lhsEnd.getValue().weight() + rhs.emptyWordWeight() ) );
                    assert previous == null : previous;
                }
            }

        }

        @Override
        public void collectTransitions(String[][] outputMatrix,int[][] weightMatrix) {
            lhs.collectTransitions(outputMatrix,weightMatrix);
            rhs.collectTransitions(outputMatrix,weightMatrix);
            transitionProduct(outputMatrix,weightMatrix, lhs.end, rhs.start);
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            lhs.collectStates(indexToState);
            rhs.collectStates(indexToState);
        }

        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append(". ").append(emptyWordOutput).append(" ").append(emptyWordWeight).append(" ").append(start).append(" ").append(end).append("\n");
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
            emptyWordWeight = 0;
        }

        @Override
        public void collectTransitions(String[][] outputMatrix,int[][] weightMatrix) {
            nested.collectTransitions(outputMatrix,weightMatrix);
            transitionProduct(outputMatrix,weightMatrix, nested.end, nested.start);
        }

        @Override
        public void collectStates(Renamed[] indexToState) {
            nested.collectStates(indexToState);
        }

        @Override
        public void serialize(StringBuilder sb, int indent) {
            MealyParser.ind(sb, indent);
            sb.append("* ").append(emptyWordOutput).append(" ").append(emptyWordWeight).append(" ").append(start).append(" ").append(end).append("\n");
            nested.serialize(sb, indent + 1);
        }
    }

    private static void transitionProduct(String[][] outputMatrix,int[][] weightMatrix, HashMap<Integer, OutputWeight> from,
            HashMap<Integer, OutputWeight> to) {
        for (Entry<Integer, OutputWeight> fromE : from.entrySet()) {
            final int fromState = fromE.getKey();
            for (Entry<Integer, OutputWeight> toE : to.entrySet()) {
                final int toState = toE.getKey();
                final String transOutput = outputMatrix[fromState][toState];
                final String output = fromE.getValue().getOutput() + toE.getValue().getOutput();
                assert transOutput == null : "The transition from " + fromState + " to " + toState
                        + " should be uniquely determined!";
                outputMatrix[fromState][toState] = output;
                weightMatrix[fromState][toState] = fromE.getValue().weight() + toE.getValue().weight();
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
            emptyWordOutput=null;
            emptyWordWeight=0;
        }

        @Override
        public void collectTransitions(String[][] outputMatrix,int[][] weightMatrix) {
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
