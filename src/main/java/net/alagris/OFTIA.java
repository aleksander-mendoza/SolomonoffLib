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

import java.util.*;

public class OFTIA {
    static class Edge {
        State target;
        IntSeq output;
    }

//    static class Ambiguity extends HashSet<IntPair> {
//
//    }

    static class State extends HashMap<Integer, Edge> {
        boolean accepting;
        HashMap<IntSeq,IntSeq> blue = new HashMap<>();

//        IntSeq turnToRedAndInferLcp(){
//            accepting = blue.remove(IntSeq.Epsilon)!=null;
//
//            blue = null;
//
//        }
    }



    public static <A,B>Map.Entry<A,B> any(Map<A,B> map){
        Iterator<Map.Entry<A, B>> i =map.entrySet().iterator();
        return i.hasNext()?i.next():null;
    }


    private static boolean partialDerivativeContainsEmptyOutput(int nextIn,HashMap<IntSeq,IntSeq> blue){
        final IntSeq out =  blue.get(new IntSeq(nextIn));
        return out.isEmpty();
    }
    public static void expandBlue(State root) {

        final HashMap<Integer,HashMap<Integer,State>> brzozowskiDerivatives = new HashMap<>();

        for(Map.Entry<IntSeq, IntSeq> entry:root.blue.entrySet()){
            final int nextIn = entry.getKey().at(0);
            final IntSeq suffixIn = entry.getKey().sub(1);
            final IntSeq suffixOut;
            final State derivative;
            final HashMap<Integer, State> partialDerivative = brzozowskiDerivatives.computeIfAbsent(nextIn,(k)->new HashMap<>());
            if(partialDerivative.containsKey(null) || (partialDerivative.isEmpty() && partialDerivativeContainsEmptyOutput(nextIn,root.blue))){
                derivative = partialDerivative.get(null);
                suffixOut = entry.getValue();
            }else{
                final int nextOut = entry.getValue().at(0);
                suffixOut = entry.getValue().sub(1);
                derivative = partialDerivative.get(nextOut);
            }
            derivative.blue.put(suffixIn,suffixOut);
        }
//
//        for(Map.Entry<Integer, HashMap<Integer, HashMap<IntSeq, IntSeq>>> in:brzozowskiDerivatives.entrySet()){
//            final int nextIn = in.getKey();
//            assert !in.getValue().containsKey(null)||in.getValue().size()==1;
//            for(Map.Entry<Integer, HashMap<IntSeq, IntSeq>> out:in.getValue().entrySet()){
//                root.
//                if(out.getKey()==null){
//
//                }else{
//                    final int nextOut = out.getKey();
//
//                }
//            }
//        }


    }

   /*
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


    IntSeq out(List<Pair<IntSeq, IntSeq>> informant, int index) {
        return informant.get(index).r();
    }

    IntSeq in(List<Pair<IntSeq, IntSeq>> informant, int index) {
        return informant.get(index).l();
    }

    static class Informant {
        final IntSeq in, out;
        Informant next;

        Informant(IntSeq in, IntSeq out, Informant next) {
            this.in = in;
            this.out = out;
            this.next = next;
        }
    }

    private static void buildPttOnward(State ptt, Informant first, int outputOffset, int inputOffset) {
        assert first!=null;
        if (first.in.size() == inputOffset
                && first.out.size() == outputOffset) {
            ptt.isAccepting = true;
            first = first.next;
        }

        while (first != null) {
            assert first.in.size() > inputOffset;
            assert first.next==null||first.out.sub(0, outputOffset).equals(first.next.out.sub(0, outputOffset));
            assert first.next==null||first.in.sub(0, inputOffset).equals(first.next.in.sub(0, inputOffset));
            final int symbolIn = first.in.get(inputOffset);
            Informant toRemainingFirst;
            Informant toRemainingLast;
            if (first.in.size() == inputOffset + 1) {
                final IntQueue out = IntQueue.asQueue(first.out, outputOffset, first.out.size());
                final Informant toAcceptingFirst = first;
                Informant toAcceptingLast = first;
                toRemainingFirst = null;
                toRemainingLast = null;
                Informant curr = first.next;
                while (curr!=null && curr.in.get(inputOffset) == symbolIn) {
                    assert curr.in.size() > inputOffset;
                    assert first.out.sub(0, outputOffset).equals(curr.out.sub(0, outputOffset));
                    assert first.in.sub(0, inputOffset).equals(curr.in.sub(0, inputOffset));
                    if(first.out.isPrefixOf(outputOffset,curr.out) ){
                        toAcceptingLast.next = curr;
                    }else{
                        if(toRemainingLast==null){
                            toRemainingFirst = curr;
                            toRemainingLast = curr;
                        }else{
                            toRemainingLast.next = curr;
                        }
                    }
                    curr = curr.next;
                }
                toAcceptingLast.next = null;
                if(toRemainingLast!=null)toRemainingLast.next = null;
                final State targetAccepting = ptt.put(symbolIn, out);
                assert toAcceptingLast!=null;
                assert toAcceptingLast.next==null;
                buildPttOnward(targetAccepting, toAcceptingFirst, first.out.size(), inputOffset + 1);
                first = curr;
            }else{
                Informant curr = first.next;
                toRemainingFirst = first;
                toRemainingLast = first;
                while (curr!=null && curr.in.get(inputOffset) == symbolIn) {
                    assert curr.in.size() > inputOffset;
                    assert first.out.sub(0, outputOffset).equals(curr.out.sub(0, outputOffset));
                    assert first.in.sub(0, inputOffset).equals(curr.in.sub(0, inputOffset));
                    toRemainingLast = curr;
                    curr = curr.next;
                }
                toRemainingLast.next = null;
                first = curr;
            }
            assert (toRemainingLast==null)==(toRemainingFirst==null);
            if( toRemainingLast!=null) {
                assert toRemainingLast.out.sub(0, outputOffset).equals(toRemainingFirst.out.sub(0, outputOffset));
                assert toRemainingLast.in.sub(0, inputOffset).equals(toRemainingFirst.in.sub(0, inputOffset));
                assert toRemainingLast.in.at(inputOffset) == symbolIn;
                assert toRemainingFirst.in.at(inputOffset) == symbolIn;
                assert toRemainingLast.next == null;
                assert first == null || first.in.at(inputOffset) != symbolIn;
                final int lcp = toRemainingFirst.out.lcp(toRemainingLast.out);
                assert outputOffset < lcp;
                final IntQueue out = IntQueue.asQueue(toRemainingFirst.out,outputOffset,lcp);
                final State targetRemaining = ptt.put(symbolIn, out);
                assert toRemainingFirst.out.sub(0, lcp).equals(toRemainingLast.out.sub(0, lcp));
                assert toRemainingLast!=null;
                assert toRemainingLast.next==null;
                buildPttOnward(targetRemaining, toRemainingFirst, lcp, inputOffset + 1);
            }
        }
    }

    public static State buildPtt(ArrayList<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State();
        informant.sort((a, b) -> {
            final int c = a.l().compareTo(b.r());
            if (c == 0)
                throw new IllegalArgumentException("Informant breaks functionality (" + a + ") (" + b + ")");
            return c;
        });
        final int len = informant.size();
        Informant last = new Informant(informant.get(len-1).l(), informant.get(len-1).r(), null);
        Informant first = last;
        for (int i = informant.size() - 2; i >= 0; i--) {
            final Pair<IntSeq, IntSeq> sample = informant.get(i);
            first = new Informant(sample.l(), sample.r(), first);
        }
        buildPttOnward(root, first, 0, 0);
        return root;
    }


    static class State {
        public void assign(State other) {
            transitions = other.transitions;
        }

        public State put(int symbolIn, IntQueue output) {
            final State target = new State();
            transitions.computeIfAbsent(symbolIn, e -> new Edges()).outgoing.add(new Edge(output, target));
            target.transitions.computeIfAbsent(symbolIn, e -> new Edges()).incoming.add(this);
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
        private int incoming = 1;

        State() {
        }

        State(State copy) {
            transitions = copyTransitions(copy.transitions);
        }

        void prepend(IntQueue prefix) {
            for (Edges edges : transitions.values()) {
                for (Edge edge : edges.outgoing) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
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
*/

}

