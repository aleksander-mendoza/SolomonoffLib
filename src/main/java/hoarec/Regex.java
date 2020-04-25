package hoarec;

import java.util.PrimitiveIterator.OfInt;

import hoarec.Glushkov.G;
import hoarec.Glushkov.Renamed;

public class Regex {

    

    public static class Ptr<T> {
        T v;
    }

    public static abstract class R {
        String pre, post;

        abstract G glushkovRename(Ptr<Integer> stateCount);

    }

    public static class Union extends R {
        final R lhs, rhs;

        public Union(R lhs, R rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Union(pre, lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount), post);
        }
    }

    public static class Concat extends R {
        final R lhs, rhs;

        public Concat(R lhs, R rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Concat(pre, lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount), post);
        }
    }

    public static class Kleene extends R {
        final R nested;

        public Kleene(R nested) {
            this.nested = nested;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Kleene(pre, nested.glushkovRename(stateCount), post);
        }
    }

    public static class Atomic extends R {

        final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            OfInt i = literal.codePoints().iterator();
            if (!i.hasNext()) {
                return new Glushkov.Renamed(stateCount.v++, pre, post);
            }
            G root = new Glushkov.Renamed(stateCount.v++, i.next(), i.hasNext() ? "" : pre, i.hasNext() ? "" : post);
            while (i.hasNext()) {
                int u = i.next();
                root = new Glushkov.Concat(i.hasNext() ? "" : pre, root,
                        new Glushkov.Renamed(stateCount.v++, u, "", ""), i.hasNext() ? "" : post);
            }
            return root;
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
        G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Renamed(stateCount.v++, from,to, pre, post);
        }

    }

    public static class Var extends R {
        final String id;

        public Var(String id) {
            this.id = id;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            throw new UnsupportedOperationException("Variables not yet supported!");
        }
    }
}
