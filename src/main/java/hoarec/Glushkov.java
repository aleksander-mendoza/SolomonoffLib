package hoarec;

import java.util.HashMap;
import java.util.HashSet;


public class Glushkov {


    class PD {
        /** Can be found at the beginning of string? */
        boolean p;
        /** Can be found at the end of string? */
        boolean d;
    }

    public interface G {
        boolean acceptsEmptyWord();

        HashMap<Integer,String> getStartStates();

        HashMap<Integer,String> getEndStates();

    }

    public static class Union implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final HashMap<Integer,String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer,String> getStartStates() {
            return start;
        }

        public HashMap<Integer,String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Union(G lhs, G rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() || rhs.acceptsEmptyWord();
            start.addAll(lhs.getStartStates());
            start.addAll(rhs.getStartStates());
            end.addAll(lhs.getEndStates());
            end.addAll(rhs.getEndStates());
        }


    }

    public static class Concat implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final HashMap<Integer,String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer,String> getStartStates() {
            return start;
        }

        public HashMap<Integer,String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Concat(G lhs, G rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() && rhs.acceptsEmptyWord();
            start.addAll(lhs.getStartStates());
            if (lhs.acceptsEmptyWord())
                start.addAll(rhs.getStartStates());
            end.addAll(rhs.getEndStates());
            if (rhs.acceptsEmptyWord())
                end.addAll(lhs.getEndStates());
        }


    }

    public static class Kleene implements G {
        final G nested;
        final HashMap<Integer,String> start = new HashSet<>(), end = new HashSet<>();

        public HashMap<Integer,String> getStartStates() {
            return start;
        }

        public HashMap<Integer,String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return true;
        }

        public Kleene(G nested) {
            this.nested = nested;
            start.addAll(nested.getStartStates());
            end.addAll(nested.getEndStates());
        }

    }

    public static class Renamed implements G {
        final int stateId;
        final int input;
        final HashMap<Integer,String> end = new HashMap<>(1), start = new HashMap<>(1);

        @Override
        public boolean acceptsEmptyWord() {
            return input == -1;
        }

        public Renamed(int stateId, int input, String preoutput,String postoutput) {
            this.stateId = stateId;
            this.input = input;
            end.put(stateId,postoutput);
            start.put(stateId,preoutput);
        }

        @Override
        public HashMap<Integer,String> getStartStates() {
            return start;
        }

        @Override
        public HashMap<Integer,String> getEndStates() {
            return end;
        }

    }
}
