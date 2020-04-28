package net.alagris;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.stream.Collectors;

import net.alagris.MealyParser.AST;
import net.alagris.Regex.R;

public class Simple {

    private static class Triple {
        String pre;// pre output
        R regex;
        String post;// post output

        public Triple(String pre, R regex, String post) {
            this.pre = pre;
            this.regex = regex;
            this.post = post;
        }

        R collapse() {
            regex.pre = pre;
            regex.post = post;
            return regex;
        }

        public Triple append(String output) {
            post = post + output;
            return this;
        }

        public Triple prepend(String output) {
            pre = output + pre;
            return this;
        }

        public boolean isEpsilon() {
            if (regex instanceof Regex.Atomic) {
                return ((Regex.Atomic) regex).literal.isEmpty();
            }
            return false;
        }

        public String sum() {
            return pre + post;
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

    public interface A extends AST {

        Triple removeEpsilons();

        BacktrackContext backtrack(List<Integer> input, int index);
    }

    public static class Union implements A {
        final A lhs, rhs;

        public Union(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Triple removeEpsilons() {
            final Triple l = lhs.removeEpsilons();
            final Triple r = rhs.removeEpsilons();
            final String pre = longestCommonPrefix(l.pre, r.pre);
            l.pre = l.pre.substring(pre.length());
            r.pre = r.pre.substring(pre.length());

            final String post = longestCommonSuffix(l.post, r.post);
            l.post = l.post.substring(0, l.post.length() - post.length());
            r.post = r.post.substring(0, r.post.length() - post.length());

            return new Triple(pre, new Regex.Union(l.collapse(), r.collapse()), post);
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
        public Triple removeEpsilons() {
            final Triple l = lhs.removeEpsilons();
            final Triple r = rhs.removeEpsilons();
            if (l.isEpsilon()) {
                final String pre = l.sum() + r.pre;
                final String post = r.post;
                return new Triple(pre, r.regex, post);
            }
            if (r.isEpsilon()) {
                final String pre = l.pre;
                final String post = l.post + r.sum();
                return new Triple(pre, l.regex, post);
            }

            final String pre = l.pre;
            l.pre = "";
            final String post = r.post;
            r.post = "";

            if (!l.post.isEmpty()) {
                r.prepend(l.post);
                l.post = "";
            }

            return new Triple(pre, new Regex.Concat(l.collapse(), r.collapse()), post);
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
        public Triple removeEpsilons() {
            return new Triple("", new Regex.Kleene(nested.removeEpsilons().collapse()), "");
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
        public Triple removeEpsilons() {
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
        public Triple removeEpsilons() {
            return new Triple("", new Regex.Atomic(literal), "");
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
        public Triple removeEpsilons() {
            return new Triple("", new Regex.Range(from, to), "");
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

    static class Var implements A {
        final String id;

        public Var(String id) {
            this.id = id;
        }

        @Override
        public Triple removeEpsilons() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BacktrackContext backtrack(List<Integer> input, int index) {
            throw new UnsupportedOperationException();
        }

    }

    public static R removeEpsilon(A ast) {
        final Triple simplified = ast.removeEpsilons();
        return simplified.collapse();
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
