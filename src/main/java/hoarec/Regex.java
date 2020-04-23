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
            return new Glushkov.Union(lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount));
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
            return new Glushkov.Concat(lhs.glushkovRename(stateCount), rhs.glushkovRename(stateCount));
        }
    }

    public static class Kleene extends R {
        final R nested;

        public Kleene(R nested) {
            this.nested = nested;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return new Glushkov.Kleene(nested.glushkovRename(stateCount));
        }
    }

    public static class Atomic extends R {

        final String literal;

        public Atomic(String literal) {
            this.literal = literal;
        }

        @Override
        public G glushkovRename(Ptr<Integer> stateCount) {
            return glushkovRename(stateCount, "");
        }

        public G glushkovRename(Ptr<Integer> stateCount, String output) {
            OfInt i = literal.codePoints().iterator();
            if (!i.hasNext()) {
                return new Glushkov.Renamed(stateCount.v++, -1, output);
            }
            G root = new Glushkov.Renamed(stateCount.v++, i.next(), "");
            while (i.hasNext()) {
                int u = i.next();
                root = new Glushkov.Concat(root, new Glushkov.Renamed(stateCount.v++, u, i.hasNext() ? "" : output));
            }
            return root;
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
