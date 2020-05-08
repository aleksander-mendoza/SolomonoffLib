package net.alagris;

import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.Collectors;

import net.alagris.Glushkov.G;
import net.alagris.Glushkov.Renamed;

public class Simple {

    public static interface Eps {
        public Eps append(String output);

        public Eps union(Eps other);

        public Eps concat(Eps other);

        public Eps kleene();

        public Mealy glushkov(Ptr<Integer> ptr);

        public Eps addAfter(int weight);

        public Eps addBefore(int weight);
    }

    private static Eps union(OnlyEps lhs, OnlyEps rhs) {
        if(lhs.weight()>rhs.weight() || rhs.epsilonOutput==null) {
            return lhs;
        }else if(lhs.weight()<rhs.weight() || lhs.epsilonOutput==null) {
            return rhs;
        }else if(Objects.equals(lhs.epsilonOutput,rhs.epsilonOutput)){
            return lhs;            
        }else {
            throw new IllegalStateException("Both sides match empty word and produce different output");
        }
    }

    private static Eps union(OnlyEps lhs, OnlyRegex rhs) {
        if(lhs.weight()>rhs.regex.emptyWordWeight() || rhs.regex.emptyWordOutput==null) {
            rhs.regex.emptyWordOutput = lhs.epsilonOutput; 
            rhs.regex.emptyWordWeight = lhs.weight();
        }else if(lhs.weight()<rhs.regex.emptyWordWeight() || lhs.epsilonOutput==null) {
            //pass
        }else if(Objects.equals(lhs.epsilonOutput,rhs.regex.emptyWordOutput)){
            //pass        
        }else {
            throw new IllegalStateException("Both sides match empty word and produce different output");
        }
        return new OnlyRegex(rhs.regex);
    }

    private static Eps union(OnlyRegex lhs, OnlyRegex rhs) {
        return new OnlyRegex(new Glushkov.Union(lhs.regex, rhs.regex));
    }

    private static Eps concat(OnlyEps lhs, OnlyEps rhs) {
        return new OnlyEps(Glushkov.concat(lhs.epsilonOutput, rhs.epsilonOutput), lhs.weight()+rhs.weight());
    }

    private static Eps concat(OnlyEps lhs, OnlyRegex rhs) {
        return rhs.prepend(lhs.epsilonOutput).addBefore(lhs.weight());
    }

    private static Eps concat(OnlyRegex lhs, OnlyRegex rhs) {
        return new OnlyRegex(new Glushkov.Concat(lhs.regex, rhs.regex));
    }

    private static Eps concat(OnlyRegex lhs, OnlyEps rhs) {
        return lhs.append(rhs.epsilonOutput).addAfter(rhs.weight());
    }

    public static class OnlyEps implements Eps {
        int weight = 0;
        String epsilonOutput;

        public OnlyEps(String epsilonOutput) {
            this.epsilonOutput = epsilonOutput;
        }
        public int weight() {
            return epsilonOutput==null?0:weight;
        }
        public OnlyEps(String epsilonOutput, int weight) {
            this.epsilonOutput = epsilonOutput;
            this.weight = weight;
        }

        public Eps append(String output) {
            epsilonOutput = Glushkov.concat(epsilonOutput, output);
            return this;
        }

        @Override
        public Eps union(Eps other) {
            if (other instanceof OnlyEps) {
                return Simple.union(this, (OnlyEps) other);
            } else {
                return Simple.union(this, (OnlyRegex) other);
            }
        }

        @Override
        public Eps concat(Eps other) {
            if (other instanceof OnlyEps) {
                return Simple.concat(this, (OnlyEps) other);
            } else {
                return Simple.concat(this, (OnlyRegex) other);
            }
        }

        @Override
        public Eps kleene() {
            if (epsilonOutput != null && !epsilonOutput.isEmpty()) {
                throw new IllegalStateException("Empty word prints non-empty output \"" + epsilonOutput
                        + "\" under Kleene closure, resulting in inifitely many outputs!");
            }
            return new OnlyEps(epsilonOutput);//weight is zeroed-out
        }

        @Override
        public Mealy glushkov(Ptr<Integer> ptr) {
            return new Mealy(new Mealy.Tran[][] { new Mealy.Tran[] {} }, new String[] { epsilonOutput },new int[] {0}, 0);
        }

        @Override
        public Eps addAfter(int weight) {
            if(epsilonOutput!=null)this.weight += weight;
            return this;
        }

        @Override
        public Eps addBefore(int weight) {
            if(epsilonOutput!=null)this.weight += weight;
            return this;
        }

        @Override
        public String toString() {
            return "\"\":\""+epsilonOutput+"\"";
        }
    }

    public static class OnlyRegex implements Eps {
        final G regex;

        public OnlyRegex(G regex) {
            this.regex = regex;
        }

        public Eps prepend(String pre) {
            regex.prepend(pre);
            return this;
        }

        @Override
        public Eps append(String output) {
            regex.append(output);
            return this;
        }

        @Override
        public Eps union(Eps other) {
            if (other instanceof OnlyEps) {
                return Simple.union((OnlyEps) other, this);
            } else {
                return Simple.union(this, (OnlyRegex) other);
            }
        }

        @Override
        public Eps concat(Eps other) {
            if (other instanceof OnlyEps) {
                return Simple.concat(this, (OnlyEps) other);
            } else {
                return Simple.concat(this, (OnlyRegex) other);
            }
        }

        @Override
        public Eps kleene() {
            return new OnlyRegex(new Glushkov.Kleene(regex));
        }

        @Override
        public Mealy glushkov(Ptr<Integer> ptr) {
            final int stateCount = ptr.v;
            final Renamed[] indexToState = new Renamed[stateCount];
            regex.collectStates(indexToState);
            final String[][] outputMatrix = new String[stateCount][];
            final int[][] weightMatrix = new int[stateCount][];
            for (int fromState = 0; fromState < stateCount; fromState++) {
                outputMatrix[fromState] = new String[stateCount];
                weightMatrix[fromState] = new int[stateCount];
            }
            regex.collectTransitions(outputMatrix,weightMatrix);
            return Mealy.compile(regex.emptyWordOutput,regex.emptyWordWeight, regex.start, outputMatrix,weightMatrix, regex.end, indexToState);
        }

        @Override
        public Eps addAfter(int weight) {
            regex.addAfter(weight);
            return this;
        }

        @Override
        public Eps addBefore(int weight) {
            regex.addBefore(weight);
            return this;
        }
        
        @Override
        public String toString() {
            return regex.toString();
        }
    }

    public static class P {
        String output;
        int index;

        public P(String output, int index) {
            this.output = output;
            this.index = index;
        }

    }

    public interface BacktrackContext {
        P next();
    }

    public static class Ptr<T> {
        public Ptr(T i) {
            v = i;
        }

        T v;
    }

    public interface A {

        Eps removeEpsilons(Ptr<Integer> stateCount);

        BacktrackContext backtrack(List<Integer> input, int index);

    }

    public static class Union implements A {
        final A lhs, rhs;

        public Union(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return lhs.removeEpsilons(stateCount).union(rhs.removeEpsilons(stateCount));
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            return new BacktrackContext() {
                boolean wasLeftChecked = false;
                final BacktrackContext right = rhs.backtrack(input, index);
                final BacktrackContext left = lhs.backtrack(input, index);

                @Override
                public P next() {
                    if (wasLeftChecked) {
                        return right.next();
                    } else {
                        final P next = left.next();
                        if (next == null) {
                            wasLeftChecked = true;
                            return right.next();
                        } else {
                            return next;
                        }
                    }
                }
            };

        }

        @Override
        public String toString() {
            return lhs + "|" + rhs;
        }
    }

    public static class Concat implements A {
        final A lhs, rhs;

        public Concat(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return lhs.removeEpsilons(stateCount).concat(rhs.removeEpsilons(stateCount));
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            return new BacktrackContext() {
                final BacktrackContext left = lhs.backtrack(input, index);
                P currentLeft = left.next();
                BacktrackContext right = currentLeft == null ? null : rhs.backtrack(input, currentLeft.index);

                @Override
                public P next() {
                    while (currentLeft != null) {
                        final P nextRight = right.next();
                        if (nextRight == null) {
                            currentLeft = left.next();
                            right = rhs.backtrack(input, index);
                            continue;
                        } else {
                            nextRight.output = currentLeft.output + nextRight.output;
                            return nextRight;
                        }
                    }
                    return null;
                }
            };

        }

        @Override
        public String toString() {
            return (lhs instanceof Union ? "(" + lhs + ")" : lhs.toString()) + " "
                    + (rhs instanceof Union ? "(" + rhs + ")" : rhs.toString());
        }

    }

    public static class Kleene implements A {
        final A nested;

        public Kleene(A nested) {
            this.nested = nested;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return nested.removeEpsilons(stateCount).kleene();
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            if (index > input.size())
                return () -> null;
            return new BacktrackContext() {
                final BacktrackContext backtrack = nested.backtrack(input, index);
                P next = backtrack.next();
                BacktrackContext deeper = next == null ? null : Kleene.this.backtrack(input, next.index);

                @Override
                public P next() {
                    if (deeper == null)
                        return null;
                    while (true) {
                        P nextDeeper = deeper.next();
                        if (nextDeeper == null) {
                            next = backtrack.next();
                            if (next == null) {
                                deeper = null;
                                return new P("", index);
                            } else {
                                deeper = Kleene.this.backtrack(input, next.index);
                                continue;
                            }
                        } else {
                            nextDeeper.output = next.output + nextDeeper.output;
                            return nextDeeper;
                        }
                    }
                }
            };

        }

        @Override
        public String toString() {
            return nested instanceof Union || nested instanceof Concat ? "(" + nested + ")*" : (nested + "*");
        }
    }

    public static class Product implements A {

        private final A nested;
        private final String output;

        public Product(A nested, String output) {
            this.nested = nested;
            this.output = output;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return nested.removeEpsilons(stateCount).append(output);
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            final BacktrackContext deeper = nested.backtrack(input, index);
            return () -> {
                P next = deeper.next();
                next.output = next.output + output;
                return next;
            };
        }

        @Override
        public String toString() {
            return (!(nested instanceof Atomic || nested instanceof Range) ? "(" + nested + ")" : nested.toString())
                    + ":\"" + output + "\"";
        }

    }

    public static class WeightBefore implements A {

        private final A nested;
        private final int weight;

        public WeightBefore(A nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return nested.removeEpsilons(stateCount).addBefore(weight);
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            return nested.backtrack(input, index);
        }

        @Override
        public String toString() {
            return " " + weight + " "
                    + (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString());
        }

    }

    public static class WeightAfter implements A {

        private final A nested;
        private final int weight;

        public WeightAfter(A nested, int weight) {
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return nested.removeEpsilons(stateCount).addAfter(weight);
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            return nested.backtrack(input, index);
        }

        @Override
        public String toString() {
            return (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString()) + " "
                    + weight + " ";
        }

    }

    public static class Atomic implements A {

        private final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            OfInt i = literal.codePoints().iterator();
            if (!i.hasNext()) {
                return new OnlyEps("");
            }
            G root = new Glushkov.Renamed(stateCount.v++, i.next());
            while (i.hasNext()) {
                int u = i.next();
                root = new Glushkov.Concat(root, new Glushkov.Renamed(stateCount.v++, u));
            }
            return new OnlyRegex(root);
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            for (int i : (Iterable<Integer>) () -> literal.codePoints().iterator()) {
                if (index >= input.size())
                    return () -> null;
                int next = input.get(index++);
                if (i == next) {
                    // good
                } else {
                    return () -> null;
                }
            }
            final int end = index;
            return new BacktrackContext() {
                boolean returned = false;

                @Override
                public P next() {
                    if (returned) {
                        return null;
                    }
                    returned = true;
                    return new P("", end);
                }
            };
        }

        @Override
        public String toString() {
            return "\"" + literal + "\"";
        }
    }

    public static class Range implements A {

        final private int from;
        final private int to;

        public Range(int from, int to) {
            this.from = Math.min(from, to);
            this.to = Math.max(from, to);
        }

        @Override
        public Eps removeEpsilons(Ptr<Integer> stateCount) {
            return new OnlyRegex(new Glushkov.Renamed(stateCount.v++, from, to));
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            final int next = input.get(index);
            if (from <= next && next <= to) {
                return new BacktrackContext() {
                    boolean returned = false;

                    @Override
                    public P next() {
                        if (returned)
                            return null;
                        returned = true;
                        return new P("", index + 1);
                    }
                };
            }
            return () -> null;
        }

        @Override
        public String toString() {
            return "[" + from + "-" + to + "]";
        }

    }

    public static String backtrack(String input, A ast) {
        List<Integer> list = input.codePoints().boxed().collect(Collectors.toList());
        BacktrackContext iter = ast.backtrack(list, 0);

        P result = iter.next();
        while (result != null && result.index < list.size()) {
            result = iter.next();
        }
        if (result == null)
            return null;
        assert result.index == list.size() : result.index + " != " + list.size();
        return result.output;
    }

}
