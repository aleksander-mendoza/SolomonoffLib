package net.alagris;

import java.util.Objects;

import net.alagris.EpsilonFree.E;

public class Regex {

    public static class Eps {
        E epsilonFree;
        String epsilonOutput;

        public Eps(E epsilonFree, String epsilonOutput) {
            this.epsilonFree = epsilonFree;
            this.epsilonOutput = epsilonOutput;
        }

    }

    public static abstract class R {
        String pre = "", post = "";

        abstract Eps pullEpsilonMappings();

    }

    public static class Union extends R {
        final R lhs, rhs;

        public Union(R lhs, R rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        Eps pullEpsilonMappings() {
            final Eps l = lhs.pullEpsilonMappings();
            final Eps r = rhs.pullEpsilonMappings();
            if ((l.epsilonOutput != null || r.epsilonOutput != null)
                    && !Objects.equals(l.epsilonOutput, r.epsilonOutput))
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
            final String epsilon = l.epsilonOutput == null ? r.epsilonOutput : l.epsilonOutput;
            EpsilonFree.Union out = new EpsilonFree.Union(pre, l.epsilonFree, r.epsilonFree, post, epsilon);
            return new Eps(out, out.epsilonOutput());
        }

        @Override
        public String toString() {
            return (pre.isEmpty()?"":"(")+pre+"{"+lhs+"|"+rhs+"}"+post+(post.isEmpty()?"":")");
        }
    }

    public static class Concat extends R {
        final R lhs, rhs;

        public Concat(R lhs, R rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        Eps pullEpsilonMappings() {
            final Eps l = lhs.pullEpsilonMappings();
            final Eps r = rhs.pullEpsilonMappings();
            final String epsilon = Glushkov.concat(l.epsilonOutput, r.epsilonOutput);
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
            EpsilonFree.Concat out = new EpsilonFree.Concat(pre, l.epsilonFree, r.epsilonFree, post, epsilon);
            return new Eps(out, out.epsilonOutput());
        }

        @Override
        public String toString() {
            return (pre.isEmpty()?"":"(")+pre+"{"+lhs+" "+rhs+"}"+post+(post.isEmpty()?"":")");
        }
    }

    public static class Kleene extends R {
        final R nested;

        public Kleene(R nested) {
            this.nested = nested;
        }

        @Override
        Eps pullEpsilonMappings() {
            Eps deeper = nested.pullEpsilonMappings();
            if (deeper.epsilonOutput != null && !deeper.epsilonOutput.isEmpty())
                throw new IllegalStateException(
                        "Empty word output is retuned under Kleene closure, causing infinite nondeterminism!");
            if (deeper.epsilonFree == null)
                return new Eps(null, deeper.epsilonOutput);
            EpsilonFree.Kleene out = new EpsilonFree.Kleene(pre, deeper.epsilonFree, post);
            return new Eps(out, out.epsilonOutput());
        }
        
        @Override
        public String toString() {
            return (pre.isEmpty()?"":"(")+pre+"{"+nested+"*}"+post+(post.isEmpty()?"":")");
        }

    }

    public static class Atomic extends R {

        final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        Eps pullEpsilonMappings() {
            if (literal.isEmpty()) {
                return new Eps(null, Glushkov.concat(pre, post));
            }
            EpsilonFree.Atomic out = new EpsilonFree.Atomic(pre, literal, post);
            assert out.epsilonOutput() == null;
            return new Eps(out, out.epsilonOutput());
        }
        
        @Override
        public String toString() {
            return (pre.isEmpty()?"":"(")+pre+"{"+literal+"}"+post+(post.isEmpty()?"":")");
        }
    }

    public static class Range extends R {

        private final int from;
        private final int to;

        public Range(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        Eps pullEpsilonMappings() {
            EpsilonFree.Range out = new EpsilonFree.Range(pre, from, to, post);
            assert out.epsilonOutput() == null;
            return new Eps(out, out.epsilonOutput());
        }
        
        @Override
        public String toString() {
            return (pre.isEmpty()?"":"(")+pre+"{["+from+"-"+to+"]}"+post+(post.isEmpty()?"":")");
        }

    }

}
