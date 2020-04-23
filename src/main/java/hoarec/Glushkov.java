package hoarec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Map.Entry;

public class Glushkov {

    class PD {
        /** Can be found at the beginning of string? */
        boolean p;
        /** Can be found at the end of string? */
        boolean d;
    }

    public interface G {
        boolean acceptsEmptyWord();

        String emptyWordOutput();

        HashMap<Integer, String> getStartStates();

        HashMap<Integer, String> getEndStates();

    }

    private static void appendToLhs(HashMap<Integer, String> lhsInPlace, HashMap<Integer, String> rhs) {
        for (Entry<Integer, String> rhsEntry : rhs.entrySet()) {
            final String rhsV = rhsEntry.getValue();
            lhsInPlace.compute(rhsEntry.getKey(), (k, v) -> v == null ? rhsV : (v + rhsV));
        }
    }

    private static void prependToRhs(HashMap<Integer, String> lhs, HashMap<Integer, String> rhsInPlace) {
        for (Entry<Integer, String> lhsEntry : lhs.entrySet()) {
            final String lhsV = lhsEntry.getValue();
            rhsInPlace.compute(lhsEntry.getKey(), (k, v) -> v == null ? lhsV : (lhsV + v));
        }
    }

    private static void append(HashMap<Integer, String> lhs, String rhs) {
        if (lhs.equals(""))
            return;
        lhs.replaceAll((k, v) -> v + rhs);
    }

    private static void prepend(String lhs, HashMap<Integer, String> rhs) {
        if (lhs.equals(""))
            return;
        rhs.replaceAll((k, v) -> lhs + v);
    }

    public static class Union implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final String emptyWordOutput;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Union(String pre,G lhs, G rhs,String post) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() || rhs.acceptsEmptyWord();
            if(!Objects.equals(lhs.emptyWordOutput(), rhs.emptyWordOutput())) {
                throw new IllegalStateException("nondeterminism!");
            }
            emptyWordOutput = lhs.emptyWordOutput();
            
            start.putAll(lhs.getStartStates());
            start.putAll(rhs.getStartStates());
            end.putAll(lhs.getEndStates());
            end.putAll(rhs.getEndStates());
            prepend(pre,start);
            append(end,post);
            
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

    }

    public static class Concat implements G {
        final G lhs, rhs;
        final boolean emptyWord;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return emptyWord;
        }

        public Concat(String pre, G lhs, G rhs, String post) {
            this.lhs = lhs;
            this.rhs = rhs;
            emptyWord = lhs.acceptsEmptyWord() && rhs.acceptsEmptyWord();
            start.putAll(lhs.getStartStates());
            if (lhs.acceptsEmptyWord()) {
                
                for(Entry<Integer, String> rhsStart:rhs.getStartStates().entrySet()) {
                    final String emptyWordOutput = lhs.emptyWordOutput();
                    start.put(rhsStart.getKey(),emptyWordOutput+rhsStart.getValue());    
                }
                
            }
            end.putAll(rhs.getEndStates());
            if (rhs.acceptsEmptyWord()) {
                for(Entry<Integer, String> lhsEnd:lhs.getStartStates().entrySet()) {
                    final String emptyWordOutput = rhs.emptyWordOutput();
                    end.put(lhsEnd.getKey(),lhsEnd.getValue()+emptyWordOutput);    
                }
            }
            
            prepend(pre,start);
            append(end,post);
        }

        @Override
        public String emptyWordOutput() {
            return null;
        }

    }

    public static class Kleene implements G {
        final G nested;
        final HashMap<Integer, String> start = new HashMap<>(), end = new HashMap<>();
        final String emptyWordOutput;

        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public boolean acceptsEmptyWord() {
            return true;
        }

        public Kleene(String pre, G nested, String post) {
            this.nested = nested;
            start.putAll(nested.getStartStates());
            prepend(pre, start);
            end.putAll(nested.getEndStates());
            append(end, post);
            if (pre == null)
                throw new NullPointerException();
            if (post == null)
                throw new NullPointerException();
            emptyWordOutput = pre + post;
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

    }

    public static class Renamed implements G {
        final int stateId;
        final int input;
        final HashMap<Integer, String> end = new HashMap<>(1), start = new HashMap<>(1);
        final String emptyWordOutput;

        @Override
        public boolean acceptsEmptyWord() {
            return input == -1;
        }

        public Renamed(int stateId, int input, String preoutput, String postoutput) {
            this.stateId = stateId;
            this.input = input;
            if (preoutput == null)
                throw new NullPointerException();
            if (postoutput == null)
                throw new NullPointerException();
            end.put(stateId, postoutput);
            start.put(stateId, preoutput);
            emptyWordOutput = acceptsEmptyWord() ? preoutput + postoutput : null;
        }

        @Override
        public HashMap<Integer, String> getStartStates() {
            return start;
        }

        @Override
        public HashMap<Integer, String> getEndStates() {
            return end;
        }

        @Override
        public String emptyWordOutput() {
            return emptyWordOutput;
        }

    }
}
