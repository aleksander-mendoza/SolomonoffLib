package net.alagris;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.Collectors;

import net.alagris.EpsilonFree.E;
import net.alagris.MealyParser.AST;

public class Simple {

    public static class Eps {
        E epsilonFree;
        String epsilonOutput;

        public Eps(E epsilonFree, String epsilonOutput) {
            this.epsilonFree = epsilonFree;
            this.epsilonOutput = epsilonOutput;
        }

        public Eps append(String output) {
            epsilonOutput = Glushkov.concat(epsilonOutput, output);
            if(epsilonFree!=null)epsilonFree.append(output);
            return this;
        }

    }
    
    public static String longestCommonPrefix(String a, String b) {
        final int[] buff = new int[Math.min(a.length(), b.length())];
        final OfInt ac = a.codePoints().iterator();
        final OfInt bc = b.codePoints().iterator();
        int i = 0;
        while (ac.hasNext() && bc.hasNext()) {
            final int ai = ac.nextInt();
            final int bi = bc.nextInt();
            if (ai == bi) {
                buff[i++] = ai;
            } else {
                break;
            }
        }
        return new String(buff, 0, i);
    }

    public static String trueReverse(final String input) {
        final Deque<Integer> queue = new ArrayDeque<>();
        input.codePoints().forEach(queue::addFirst);

        final StringBuilder sb = new StringBuilder();
        queue.forEach(sb::appendCodePoint);

        return sb.toString();
    }

    public static String longestCommonSuffix(String a, String b) {
        return longestCommonPrefix(trueReverse(a), trueReverse(b));
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

    public interface A {

        Eps removeEpsilons();

        BacktrackContext backtrack(List<Integer> input, int index);
        
    }

    public static class Union implements A {
        final A lhs, rhs;

        public Union(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Eps removeEpsilons() {
            final Eps l = lhs.removeEpsilons();
            final Eps r = rhs.removeEpsilons();
            if (l.epsilonOutput != null && r.epsilonOutput != null
                    && !l.epsilonOutput.equals(r.epsilonOutput))
                throw new IllegalStateException("Two sides of union have different outputs for empty word! \""
                        + l.epsilonOutput + "\" != \"" + r.epsilonOutput + "\"");

            if (l.epsilonFree == null) {
                r.epsilonOutput = l.epsilonOutput;
                return r;
            }
            if (r.epsilonFree == null) {
                l.epsilonOutput = r.epsilonOutput;
                return l;
            }
            final String pre = longestCommonPrefix(l.epsilonFree.pre, r.epsilonFree.pre);
            l.epsilonFree.pre = l.epsilonFree.pre.substring(pre.length());
            r.epsilonFree.pre = r.epsilonFree.pre.substring(pre.length());
            
            final String post = longestCommonSuffix(l.epsilonFree.post, r.epsilonFree.post);
            l.epsilonFree.post = l.epsilonFree.post.substring(0, l.epsilonFree.post.length() - post.length());
            r.epsilonFree.post = r.epsilonFree.post.substring(0, r.epsilonFree.post.length() - post.length());
            
            final String epsilon = l.epsilonOutput == null ? r.epsilonOutput : l.epsilonOutput;
            EpsilonFree.Union out = new EpsilonFree.Union(pre, l.epsilonFree, r.epsilonFree, post, epsilon);
            return new Eps(out, out.epsilonOutput());
        
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
        public Eps removeEpsilons() {

            final Eps l = lhs.removeEpsilons();
            final Eps r = rhs.removeEpsilons();
            final String epsilon = Glushkov.concat(l.epsilonOutput, r.epsilonOutput);
            if(l.epsilonFree == null && r.epsilonFree == null) {
                return new Eps(null, epsilon);
            }
            if (l.epsilonFree == null) {
                r.epsilonFree.prepend(l.epsilonOutput);
                r.epsilonOutput = epsilon;
                return r;
            }
            if (r.epsilonFree == null) {
                l.epsilonFree.append(r.epsilonOutput);
                l.epsilonOutput = epsilon;
                return l;
            }
            EpsilonFree.Concat out = new EpsilonFree.Concat("", l.epsilonFree, r.epsilonFree, "", epsilon);
            return new Eps(out, out.epsilonOutput());
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
        public Eps removeEpsilons() {
            Eps deeper = nested.removeEpsilons();
            if (deeper.epsilonOutput != null && !deeper.epsilonOutput.isEmpty())
                throw new IllegalStateException(
                        "Empty word output is retuned under Kleene closure, causing infinite nondeterminism!");
            if (deeper.epsilonFree == null) {
                assert deeper.epsilonOutput.isEmpty();
                return new Eps(null, deeper.epsilonOutput);
            }
            EpsilonFree.Kleene out = new EpsilonFree.Kleene("", deeper.epsilonFree, "");
            return new Eps(out, out.epsilonOutput());
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
            return nested instanceof Union || nested instanceof Concat ? "(" + nested + ")*" : (nested+"*");
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
        public Eps removeEpsilons() {
            return nested.removeEpsilons().append(output);
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
            return (!(nested instanceof Atomic || nested instanceof Range) ?"(" + nested + ")" : nested.toString())+":\""+output+"\"";
        }
        

    }

    public static class Atomic implements A {

        private final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public Eps removeEpsilons() {
            if (literal.isEmpty()) {
                return new Eps(null, "");
            }
            EpsilonFree.Atomic out = new EpsilonFree.Atomic("", literal, "");
            return new Eps(out, out.epsilonOutput());
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
            return "\""+literal+"\"";
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
        public Eps removeEpsilons() {
            return new Eps(new EpsilonFree.Range("",from, to,""),null);
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
            return "["+from+"-"+to+"]";
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
