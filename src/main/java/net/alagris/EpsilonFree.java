package net.alagris;

import java.util.PrimitiveIterator.OfInt;

import net.alagris.Glushkov.G;
import net.alagris.Glushkov.Renamed;

public class EpsilonFree {

    public static class Ptr<T> {
        T v;
    }

    public static abstract class E {
        String pre, post, epsilon;

        public E(String pre, String post, String epsilon) {
            this.pre = pre;
            this.post = post;
            this.epsilon = Glushkov.concat(Glushkov.concat(pre, epsilon), post);
        }

        public abstract G glushkovRename(Ptr<Integer> stateCount);

        public void prepend(String str) {
            pre = Glushkov.concat(str, getPre());
            epsilon = Glushkov.concat(str, epsilon);
        }

        public void append(String str) {
            post = Glushkov.concat(getPost(), str);
            epsilon = Glushkov.concat(epsilon, str);
        }

        public String epsilonOutput() {
            return epsilon;
        }

        public String getPre() {
            return pre;
        }

        public String getPost() {
            return post;
        }

    }

    public static class Union extends E {
        final E lhs, rhs;

        public Union(String pre, E lhs, E rhs, String post, String epsilon) {
            super(pre, post, epsilon);
            this.lhs = lhs;
            this.rhs = rhs;
            if (rhs == null)
                throw new NullPointerException();
            if (lhs == null)
                throw new NullPointerException();
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Union(getPre(), lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount),
                    getPost(), epsilonOutput());
        }

        @Override
        public String toString() {
            return (pre.isEmpty() ? "" : "{" + pre + "}") + lhs.toString() + "|" + rhs.toString()
                    + (post.isEmpty() ? "" : "{" + post + "}");
        }
    }

    public static class Concat extends E {
        final E lhs, rhs;

        public Concat(String pre, E lhs, E rhs, String post, String epsilon) {
            super(pre, post, epsilon);
            if (lhs == null)
                throw new NullPointerException();
            if (rhs == null)
                throw new NullPointerException();
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Concat(getPre(), lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount),
                    getPost(), epsilonOutput());
        }

        @Override
        public String toString() {
            return (pre.isEmpty() ? "" : "{" + pre + "}")
                    + (lhs instanceof Union ? "(" + lhs.toString() + ")" : lhs.toString()) + " "
                    + (rhs instanceof Union ? "(" + rhs.toString() + ")" : rhs.toString())
                    + (post.isEmpty() ? "" : "{" + post + "}");
        }
    }

    public static class Kleene extends E {
        final E nested;

        public Kleene(String pre, E nested, String post) {
            super(pre, post, "");
            if (nested == null)
                throw new NullPointerException();
            this.nested = nested;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Kleene(getPre(), nested.glushkovRename(stateCount), getPost(), epsilonOutput());
        }

        @Override
        public String toString() {
            return (pre.isEmpty() ? "" : "{" + pre + "}")
                    + (nested instanceof Union || nested instanceof Concat ? "(" + nested.toString() + ")"
                            : nested.toString())
                    + "*" + (post.isEmpty() ? "" : "{" + post + "}");
        }
    }

    public static class Atomic extends E {

        final String literal;

        public Atomic(String pre, String literal, String post) {
            super(pre, post, null);
            if (literal.isEmpty())
                throw new IllegalStateException("Epsilon shouldn't be here!");
            this.literal = literal;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            OfInt i = literal.codePoints().iterator();
            if (!i.hasNext()) {
                throw new IllegalStateException("Epsilon shouldn't be here!");
            }
            G root = new Glushkov.Renamed(stateCount.v++, i.next(), i.hasNext() ? "" : getPre(),
                    i.hasNext() ? "" : getPost());
            while (i.hasNext()) {
                int u = i.next();
                root = new Glushkov.Concat(i.hasNext() ? "" : getPre(), root,
                        new Glushkov.Renamed(stateCount.v++, u, "", ""), i.hasNext() ? "" : getPost(), null);
            }
            return root;
        }

        @Override
        public String toString() {
            return (pre.isEmpty() ? "" : "{" + pre + "}") + literal + (post.isEmpty() ? "" : "{" + post + "}");
        }
        
        
    }

    public static class Range extends E {

        private final int from;
        private final int to;

        public Range(String pre, int from, int to, String post) {
            super(pre, post, null);
            this.from = from;
            this.to = to;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Renamed(stateCount.v++, from, to, getPre(), getPost());
        }

        @Override
        public String toString() {
            return (pre.isEmpty() ? "" : "{" + pre + "}") + "[" + from + "-" + to + "]"
                    + (post.isEmpty() ? "" : "{" + post + "}");
        }
    }

}
