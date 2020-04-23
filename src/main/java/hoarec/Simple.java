package hoarec;

import java.util.Objects;

import hoarec.Library.AST;
import hoarec.Regex.R;

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
            if(regex instanceof Regex.Atomic) {
                return ((Regex.Atomic)regex).literal.isEmpty();
            }
            return false;
        }

        public String sum() {
            return pre+post;
        }
    }

    public interface A extends AST {

        Triple normalize();
    }

    public static class Union implements A {
        final A lhs, rhs;

        public Union(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Triple normalize() {
            final Triple l = lhs.normalize();
            final Triple r = rhs.normalize();
            final String pre;
            if (l.pre.equals(r.pre)) {
                pre = l.pre;
                l.pre = "";
                r.pre = "";
            } else {
                pre = "";
            }
            final String post;
            if (l.post.equals(r.post)) {
                post = l.post;
                l.post = "";
                r.post = "";
            } else {
                post = "";
            }
            return new Triple(pre, new Regex.Union(l.collapse(), r.collapse()), post);
        }

    }

    public static class Concat implements A {
        final A lhs, rhs;

        public Concat(A lhs, A rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Triple normalize() {
            final Triple l = lhs.normalize();
            final Triple r = rhs.normalize();
            if(l.isEpsilon()) {
                final String pre = l.sum() + r.pre;
                final String post = r.post;
                return new Triple(pre, r.regex, post);
            }
            if(r.isEpsilon()) {
                final String pre = l.pre;
                final String post = l.post + r.sum();
                return new Triple(pre, l.regex, post);
            }
            
            final String pre = l.pre;
            l.pre = "";
            final String post = r.post;
            r.post = "";

            if (!l.post.isEmpty()) {
                r.prepend(r.post);
                l.post = "";
            }

            return new Triple(pre, new Regex.Concat(l.collapse(), r.collapse()), post);
        }

    }

    public static class Kleene implements A {
        final A nested;

        public Kleene(A nested) {
            this.nested = nested;
        }

        @Override
        public Triple normalize() {
            return new Triple(null, nested.normalize().collapse(), null);
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
        public Triple normalize() {
            return nested.normalize().append(output);
        }

    }

    public static class Atomic implements A {

        private final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public Triple normalize() {
            return new Triple("", new Regex.Atomic(literal), "");
        }

    }

    static class Var implements A {
        final String id;

        public Var(String id) {
            this.id = id;
        }

        @Override
        public Triple normalize() {
            throw new UnsupportedOperationException();
        }

    }
}
