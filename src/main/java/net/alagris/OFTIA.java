/* Copyright (C) 2013-2020 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.alagris;

import net.automatalib.commons.util.Pair;

import java.util.*;

public class OFTIA {
    private static Iterator<Integer> iter(IntSeq seq, int offset) {
        return new Iterator<Integer>() {
            int i = offset;

            @Override
            public boolean hasNext() {
                return i < seq.size();
            }

            @Override
            public Integer next() {
                return seq.get(i++);
            }
        };
    }

    static class IntQueue {
        int value;
        IntQueue next;

        @Override
        public String toString() {
            return OFTIA.toString(this);
        }
    }

    static IntQueue concat(IntQueue q, IntQueue tail) {
        if (q == null) return tail;
        final IntQueue first = q;
        while (q.next != null) {
            q = q.next;
        }
        q.next = tail;
        return first;
    }

    static IntQueue copyAndConcat(IntQueue q, IntQueue tail) {
        if (q == null) return tail;
        final IntQueue root = new IntQueue();
        root.value = q.value;
        IntQueue curr = root;
        q = q.next;
        while (q != null) {
            curr.next = new IntQueue();
            curr = curr.next;
            curr.value = q.value;
            q = q.next;
        }
        curr.next = tail;
        return root;
    }


    private static boolean eq(IntQueue a, IntQueue b) {
        while (a != null && b != null) {
            if (a.value != b.value) return false;
            a = a.next;
            b = b.next;
        }
        return a == null && b == null;
    }

    private static IntQueue asQueue(IntSeq str, int fromInclusive, int toExclusive) {
        IntQueue q = null;
        assert fromInclusive <= str.size();
        assert 0 <= fromInclusive;
        assert toExclusive < str.size();
        assert 0 <= toExclusive;
        for (int i = toExclusive - 1; i >= fromInclusive; i--) {
            IntQueue next = new IntQueue();
            next.value = str.get(i);
            next.next = q;
            q = next;
        }
        return q;
    }

    private static class InformantAndSamples {
        final List<Pair<IntSeq, IntSeq>> informant;
        /**
         * To every pair of input-output strings associate list of states that accept it
         */
        final HashMap<Pair<IntSeq, IntSeq>, List<State>> orbits = new HashMap<>();
        /**
         * To every state associate list of all input-output strings that are accepted by it
         */
        final HashMap<State, HashMap<IntSeq, IntSeq>> samples = new HashMap<>();

        private InformantAndSamples(List<Pair<IntSeq, IntSeq>> informant) {
            this.informant = informant;
        }

        IntSeq out(int index) {
            return informant.get(index).getSecond();
        }

        IntSeq in(int index) {
            return informant.get(index).getFirst();
        }

        public int size() {
            return informant.size();
        }

        public Pair<IntSeq, IntSeq> get(int index) {
            return informant.get(index);
        }

        public void putSample(int informantIndex, int outputOffset, int inputOffset, State state) {
            final Pair<IntSeq, IntSeq> sample = get(informantIndex);
            final Pair<IntSeq, IntSeq> brzozowskiDerivative = Pair.of(sample.getFirst().sub(inputOffset),
                    sample.getSecond().sub(outputOffset));
            orbits.computeIfAbsent(brzozowskiDerivative, p -> new ArrayList<>()).add(state);
            final IntSeq prev = put(samples.computeIfAbsent(state, p -> new HashMap<>()), brzozowskiDerivative);
            assert prev == null;
        }
    }

    /**
     * builds onward prefix tree transducer
     */
    private static void buildPttOnward(State ptt, InformantAndSamples is,
                                       int informantBegin, int informantEnd,
                                       int outputOffset, int inputOffset) {
        assert informantBegin < informantEnd;
        if (is.in(informantBegin).size() == inputOffset
                && is.out(informantBegin).size() == outputOffset) {
            ptt.isAccepting = true;
        }

        while (informantBegin < informantEnd) {
            assert is.size() > 0;
            assert is.out(informantBegin).subList(0, outputOffset).equals(is.out(informantEnd - 1).subList(0, outputOffset));
            assert is.in(informantBegin).subList(0, inputOffset).equals(is.in(informantEnd - 1).subList(0, inputOffset));
            Pair<IntSeq, IntSeq> fromIn = is.get(informantBegin);
            int symbolIn = fromIn.getFirst().get(inputOffset);
            is.putSample(informantBegin, outputOffset, inputOffset, ptt);
            if (fromIn.getSecond().size() == outputOffset) {
                int i = informantBegin + 1;
                while (i < informantEnd && is.in(i).get(inputOffset) == symbolIn) {
                    is.putSample(i, outputOffset, inputOffset, ptt);
                    i++;
                }
                final State target = ptt.put(symbolIn, null);
                buildPttOnward(target, is, informantBegin, i, outputOffset, inputOffset + 1);
                informantBegin = i;
            } else {
                while (informantBegin < informantEnd && is.in(informantBegin).get(inputOffset) == symbolIn) {
                    Pair<IntSeq, IntSeq> fromOut = is.get(informantBegin);
                    int symbolOut = fromOut.getFirst().get(outputOffset);
                    int i = informantBegin + 1;
                    while (i < informantEnd && is.in(i).get(inputOffset) == symbolIn
                            && is.out(i).get(outputOffset) == symbolOut) {
                        is.putSample(i, outputOffset, inputOffset, ptt);
                        i++;
                    }
                    Pair<IntSeq, IntSeq> toOut = is.get(i - 1);
                    int lcp = fromOut.getSecond().lcp(toOut.getSecond());
                    assert outputOffset < lcp;
                    assert fromOut.getSecond().subList(0, lcp).equals(toOut.getSecond().subList(0, lcp));
                    final State target = ptt.put(symbolIn, asQueue(fromOut.getSecond(), outputOffset, lcp));
                    buildPttOnward(target, is, informantBegin, i, lcp, inputOffset + 1);
                    informantBegin = i;
                }
            }
        }
    }

    /**
     * assumes that there are no duplicates. Just filter them out before calling this method
     */
    public static State buildPtt(List<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State();
        informant.sort((a, b) -> {
            final int c = a.getFirst().lexLenCompareTo(b.getFirst());
            if (c == 0) throw new IllegalArgumentException("Informant breaks functionality (" + a + ") (" + b + ")");
            return c;
        });
        buildPttOnward(root, new InformantAndSamples(informant), 0, informant.size(), 0, 0);
        return root;
    }

    static class State {
        public void assign(State other) {
            transitions = other.transitions;
        }

        public State put(int symbolIn, IntQueue output) {
            final State target = new State();
            transitions.computeIfAbsent(symbolIn, e->new Edges()).outgoing.add(new Edge(output,target));
            target.transitions.computeIfAbsent(symbolIn,e->new Edges()).incoming.add(this);
            return target;
        }

        static class Edge {
            IntQueue out;
            State target;

            public Edge(IntQueue out, State target) {
                this.out = out;
                this.target = target;
            }

            public Edge(Edge edge) {
                out = edge.out;
                target = edge.target;
            }
        }

        private boolean isAccepting = false;

        static class Edges {
            final List<Edge> outgoing;
            final List<State> incoming;

            public Edges() {
                outgoing = new ArrayList<>();
                incoming = new ArrayList<>();
            }

            public Edges(Edges copy) {
                outgoing = new ArrayList<>(copy.outgoing.size());
                incoming = new ArrayList<>(copy.incoming.size());
                copy.outgoing.forEach(e -> outgoing.add(new State.Edge(e)));
                incoming.addAll(copy.incoming);
            }


        }

        private HashMap<Integer, Edges> transitions = new HashMap<>();
        /**
         * counter for incoming transitions
         */
        private int incoming = 1;

        State() {
        }

        State(State copy) {
            transitions = copyTransitions(copy.transitions);
        }

        /**
         * The IntQueue is consumed and should not be reused after calling this method
         */
        void prepend(IntQueue prefix) {
            for (Edges edges : transitions.values()) {
                for (Edge edge : edges.outgoing) {
                    edge.out = copyAndConcat(prefix, edge.out);
                }
            }
        }

    }


    static HashMap<Integer, State.Edges> copyTransitions(HashMap<Integer, State.Edges> transitions) {
        final HashMap<Integer, State.Edges> copy = new HashMap<>(transitions.size());
        for (Map.Entry<Integer, State.Edges> entry : transitions.entrySet()) {
            copy.put(entry.getKey(), new State.Edges(entry.getValue()));
        }
        return copy;
    }

    public static <K, V> Pair<K, V> of(Map.Entry<K, V> e) {
        return Pair.of(e.getKey(), e.getValue());
    }

    public static <K, V> V put(HashMap<K, V> map, Map.Entry<K, V> entry) {
        return map.put(entry.getKey(), entry.getValue());
    }

    public static <K, V> V put(HashMap<K, V> map, Pair<K, V> entry) {
        return map.put(entry.getFirst(), entry.getSecond());
    }

    static class SamplesAndPairs {
        final HashMap<State, HashMap<IntSeq, IntSeq>> samples;
        final HashMap<Pair<IntSeq, IntSeq>, List<State>> orbits;

        static class Intersection {
            HashMap<IntSeq, IntSeq> intersection = new HashMap<>();

            public void add(Pair<IntSeq, IntSeq> pair) {
                add(pair.getFirst(), pair.getSecond());
            }

            public void add(Map.Entry<IntSeq, IntSeq> pair) {
                add(pair.getKey(), pair.getValue());
            }

            public void add(IntSeq in, IntSeq out) {
                if (intersection == null) return;
                final IntSeq prev = intersection.put(in, out);
                if (prev != null && !prev.equals(out)) {
                    intersection = null;
                }
            }
        }

        /**
         * Pairs of states that should be merged. It is guaranteed that for every pair
         * (state1,state2) in thus map, the hash codes are in sorted order, that is
         * state1.hashCode()&lt;state2.hashCode() <br/>
         */
        final HashMap<Pair<State, State>, Intersection> pairs = new HashMap<>();

        SamplesAndPairs(HashMap<Pair<IntSeq, IntSeq>, List<State>> orbits, HashMap<State, HashMap<IntSeq, IntSeq>> samples) {
            this.samples = samples;
            this.orbits = orbits;
            for (Map.Entry<Pair<IntSeq, IntSeq>, List<State>> sampleAndStates : orbits.entrySet()) {
                final List<State> states = sampleAndStates.getValue();
                states.sort(Comparator.comparingInt(Object::hashCode));
                assert Specification.isStrictlyIncreasing(states, Comparator.comparingInt(Object::hashCode));
                for (int i = 0; i < states.size(); i++) {
                    for (int j = i + 1; j < states.size(); j++) {
                        assert states.get(i).hashCode() < states.get(j).hashCode();
                        pairs.computeIfAbsent(Pair.of(states.get(i), states.get(j)), p -> new Intersection()).add(sampleAndStates.getKey());
                    }
                }
            }
        }


        /**
         * Merges A into B, so that as a result A is removed and B retains all samples of A
         */
        void mergeSamples(State a, State b) {
            HashMap<IntSeq, IntSeq> samplesB = samples.get(b);
            for (Map.Entry<IntSeq, IntSeq> sampleA : samples.get(a).entrySet()) {
                final IntSeq prevOut = put(samplesB, sampleA);
                if (prevOut == null) {
                    final List<State> orbitA = orbits.get(of(sampleA));
                    assert !orbitA.contains(b);
                    int i = 0;
                    for (; i < orbitA.size(); i++) {
                        if (orbitA.get(i).hashCode() < b.hashCode()) {
                            pairs.computeIfAbsent(Pair.of(orbitA.get(i), b), p -> new Intersection()).add(sampleA);
                        } else {
                            break;
                        }
                    }
                    State prev = b;
                    for (; i < orbitA.size(); i++) {
                        final State curr = orbitA.get(i);
                        assert b.hashCode() < curr.hashCode();
                        pairs.computeIfAbsent(Pair.of(b, curr), p -> new Intersection()).add(sampleA);
                        orbitA.set(i, prev);
                        prev = curr;
                    }
                    orbitA.add(prev);
                    assert Specification.isStrictlyIncreasing(orbitA, Comparator.comparingInt(Object::hashCode));
                } else {
                    assert prevOut.equals(sampleA.getValue()) : "You should not merge states that conflict!";
                    assert orbits.get(of(sampleA)).contains(b);
                }
            }
            samples.remove(a);
            for (State c : samples.keySet()) {
                pairs.remove(c.hashCode() < a.hashCode() ? Pair.of(c, a) : Pair.of(a, c));
            }
        }


    }

    public static void oftia(State ptt) {

    }


    private static String toString(IntQueue ints) {
        if (ints == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(ints.value);
        ints = ints.next;
        while (ints != null) {
            sb.append(" ").append(ints.value);
            ints = ints.next;
        }
        return sb.toString();
    }



}

