package hoarec;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.PrimitiveIterator.OfInt;

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
    
    public static String longestCommonPrefix(String a,String b) {
        final int[] buff = new int[Math.min(a.length(),b.length())];
        final OfInt ac = a.codePoints().iterator();
        final OfInt bc = b.codePoints().iterator();
        int i=0;
        while(ac.hasNext() && bc.hasNext()) {
            final int ai = ac.nextInt();
            final int bi = bc.nextInt();
            if(ai==bi) {
                buff[i++] = ai;
            }else {
                break;
            }
        }
        return new String(buff,0,i);
    }
    
    public static String trueReverse(final String input)
    {
        final Deque<Integer> queue = new ArrayDeque<>();
        input.codePoints().forEach(queue::addFirst);

        final StringBuilder sb = new StringBuilder();
        queue.forEach(sb::appendCodePoint);

        return sb.toString();
    }
    
    public static String longestCommonSuffix(String a,String b) {
        return longestCommonPrefix(trueReverse(a), trueReverse(b));
    }

    public interface A extends AST {

        Triple removeEpsilons();
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
            l.post = l.post.substring(0,l.post.length()-post.length());
            r.post = r.post.substring(0,r.post.length()-post.length());
            
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
        public Triple removeEpsilons() {
            final Triple l = lhs.removeEpsilons();
            final Triple r = rhs.removeEpsilons();
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
        public Triple removeEpsilons() {
            return new Triple(null, nested.removeEpsilons().collapse(), null);
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

    }
}
