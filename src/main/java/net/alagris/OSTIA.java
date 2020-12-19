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

public class OSTIA {

    static class Out {
        IntQueue str;

        public Out(IntQueue str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str.toString();
        }
    }


    /**
     * builds onward prefix tree transducer
     */
    private static void buildPttOnward(State ptt, IntSeq input, IntQueue output) {
        for (int i = 0; i < input.size(); i++) {//input index
            final int symbol = input.get(i);
            if (ptt.transitions[symbol] == null) {
                final State.Edge edge = ptt.transitions[symbol] = new State.Edge();
                edge.out = output;
                output = null;
                ptt = edge.target = new State(ptt.transitions.length,ptt.shortest.concat(new IntSeq(symbol)));
            } else {
                final State.Edge edge = ptt.transitions[symbol];
                IntQueue commonPrefixEdge = edge.out;
                IntQueue commonPrefixEdgePrev = null;
                IntQueue commonPrefixInformant = output;
                while (commonPrefixEdge != null && commonPrefixInformant != null
                        && commonPrefixEdge.value == commonPrefixInformant.value) {
                    commonPrefixInformant = commonPrefixInformant.next;
                    commonPrefixEdgePrev = commonPrefixEdge;
                    commonPrefixEdge = commonPrefixEdge.next;
                }
                /*
                informant=x
                edge.out=y
                ->
                informant=lcp(x,y)^-1 x
                edge=lcp(x,y)
                pushback=lcp(x,y)^-1 y
                */
                if (commonPrefixEdgePrev == null) {
                    edge.out = null;
                }else{
                    commonPrefixEdgePrev.next = null;
                }
                edge.target.prependToSubsequentialOutputIfNotNone(commonPrefixEdge);
                output = commonPrefixInformant;
                ptt = edge.target;
            }
        }
        if (ptt.out != null && !IntQueue.equals(ptt.out.str, output)) throw new IllegalArgumentException();
        ptt.out = new Out(output);
    }


    public static void inferAlphabet(Iterator<Pair<IntSeq, IntSeq>> informant,Map<Integer,Integer> symbolToUniqueIndex) {
        while(informant.hasNext()){
            for(int symbol:informant.next().l()){
                symbolToUniqueIndex.computeIfAbsent(symbol,s->symbolToUniqueIndex.size());
            }
        }
    }
    public static Iterator<Pair<IntSeq, IntSeq>> mapSymbolsToIndices(Iterator<Pair<IntSeq, IntSeq>> informant,
                                                               Map<Integer,Integer> inputSymbolToUniqueIndex) {
        return new Iterator<Pair<IntSeq, IntSeq>>() {
            @Override
            public boolean hasNext() {
                return informant.hasNext();
            }

            @Override
            public Pair<IntSeq, IntSeq> next() {
                final Pair<IntSeq, IntSeq> element = informant.next();
                final int[] in = new int[element.l().size()];
                for(int i=0;i<in.length;i++){
                    in[i] = inputSymbolToUniqueIndex.get(element.l().get(i));
                }
                return Pair.of(new IntSeq(in),element.r());
            }
        };
    }


    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alphabetSize,IntSeq.Epsilon);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            buildPttOnward(root, inout.l(), IntQueue.asQueue(inout.r()));
        }
        return root;
    }

    static class State {
        public void assign(State other) {
            out = other.out;
            transitions = other.transitions;
        }

        static class Edge {
            IntQueue out;
            State target;

            public Edge() {

            }

            public Edge(Edge edge) {
                out = IntQueue.copyAndConcat(edge.out,null);
                target = edge.target;
            }

            @Override
            public String toString() {
                return target.toString();
            }
        }

        public Out out;
        public Edge[] transitions;
        final IntSeq shortest;

        State(int alphabetSize,IntSeq shortest) {
            transitions = new Edge[alphabetSize];
            this.shortest = shortest;
        }

        State(State copy) {
            this.shortest = copy.shortest;
            transitions = copyTransitions(copy.transitions);
            out = copy.out == null ? null : new Out(IntQueue.copyAndConcat(copy.out.str, null));
        }
        void prepend(IntQueue prefix) {
            prependToEdges(prefix);
            prependToSubsequentialOutput(prefix);
        }
        void prependToSubsequentialOutputIfNotNone(IntQueue prefix) {
            prependToEdges(prefix);
            if (out != null) {
                out.str = IntQueue.copyAndConcat(prefix, out.str);
            }
        }
        void prependToEdges(IntQueue prefix) {
            for (Edge edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
        }
        /**
         * The IntQueue is consumed and should not be reused after calling this method
         */
        void prependToSubsequentialOutput(IntQueue prefix) {
            if (out == null) {
                out = new Out(prefix);
            } else {
                out.str = IntQueue.copyAndConcat(prefix, out.str);
            }
        }

        @Override
        public String toString() {
            return shortest.toString();
        }
    }

    static class Blue {
        State parent;
        int symbol;

        State state() {
            return parent.transitions[symbol].target;
        }

        public Blue(State parent, int symbol) {
            this.symbol = symbol;
            this.parent = parent;
        }

        @Override
        public String toString() {
            return state().toString();
        }
    }

    static void addBlueStates(State parent, java.util.Queue<Blue> blue) {
        for (int i = 0; i < parent.transitions.length; i++)
            if (parent.transitions[i] != null)
                blue.add(new Blue(parent, i));
    }

    static State.Edge[] copyTransitions(State.Edge[] transitions) {
        final State.Edge[] copy = new State.Edge[transitions.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = transitions[i] == null ? null : new State.Edge(transitions[i]);
        }
        return copy;
    }
    static class VV{

    }


    public static void ostia(State transducer) {
        final java.util.Queue<Blue> blue = new LinkedList<>();
        final ArrayList<State> red = new ArrayList<>();
        red.add(transducer);
        addBlueStates(transducer, blue);
        blue:while (!blue.isEmpty()) {
            final Blue next = blue.poll();
            final State blueState = next.state();
            for (State redState : red) {
                if (ostiaMerge(next, redState, blue)){
                    continue blue;
                }
            }
            addBlueStates(blueState,blue);
            red.add(blueState);
        }
    }

    private static boolean ostiaMerge(Blue blue, State redState, java.util.Queue<Blue> blueToVisit) {
        final HashMap<State, State> merged = new HashMap<>();
        final ArrayList<Blue> reachedBlueStates = new ArrayList<>();
        if (ostiaFold(redState,null,blue.parent,blue.symbol , merged, reachedBlueStates)) {
            for (Map.Entry<State, State> mergedRedState : merged.entrySet()) {
                mergedRedState.getKey().assign(mergedRedState.getValue());
            }
            blueToVisit.addAll(reachedBlueStates);
            return true;
        }
        return false;
    }

    private static boolean ostiaFold(State red, IntQueue pushedBack,State blueParent,int symbolIncomingToBlue, HashMap<State, State> mergedStates, ArrayList<Blue> reachedBlueStates) {
        final State mergedRedState = mergedStates.computeIfAbsent(red, State::new);
        final State blueState = blueParent.transitions[symbolIncomingToBlue].target;
        final State mergedBlueState = new State(blueState);
        assert !mergedStates.containsKey(blueState);
        mergedStates.computeIfAbsent(blueParent, State::new).transitions[symbolIncomingToBlue].target = red;
        final State prevBlue = mergedStates.put(blueState,mergedBlueState);
        assert prevBlue == null;
        mergedBlueState.prepend(pushedBack);
        if(mergedBlueState.out!=null) {
            if (mergedRedState.out == null) {
                mergedRedState.out = mergedBlueState.out;
            } else if (!IntQueue.equals(mergedRedState.out.str, mergedBlueState.out.str)){
                return false;
            }
        }
        for (int i = 0; i < mergedRedState.transitions.length; i++) {
            final State.Edge transitionBlue = mergedBlueState.transitions[i];
            if (transitionBlue != null) {
                final State.Edge transitionRed = mergedRedState.transitions[i];
                if (transitionRed == null) {
                    mergedRedState.transitions[i] = new State.Edge(transitionBlue);
                    reachedBlueStates.add(new Blue(blueState, i));
                } else {
                    IntQueue commonPrefixRed = transitionRed.out;
                    IntQueue commonPrefixBlue = transitionBlue.out;
                    IntQueue commonPrefixBluePrev = null;
                    while (commonPrefixBlue != null && commonPrefixRed != null
                            && commonPrefixBlue.value == commonPrefixRed.value) {
                        commonPrefixBluePrev = commonPrefixBlue;
                        commonPrefixBlue = commonPrefixBlue.next;
                        commonPrefixRed = commonPrefixRed.next;
                    }
                    assert commonPrefixBluePrev==null?
                            commonPrefixBlue==transitionBlue.out:
                            commonPrefixBluePrev.next==commonPrefixBlue;
                    if (commonPrefixRed == null) {
                        if (commonPrefixBluePrev == null) {
                            transitionBlue.out = null;
                        } else {
                            commonPrefixBluePrev.next = null;
                        }
                        if (!ostiaFold(transitionRed.target, commonPrefixBlue, mergedBlueState,i, mergedStates, reachedBlueStates)) {
                            return false;
                        }

                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static ArrayList<Integer> run(State init, Iterator<Integer> input) {
        ArrayList<Integer> output = new ArrayList<>();
        while (input.hasNext()) {
            final State.Edge edge = init.transitions[input.next()];
            if (edge == null) return null;
            init = edge.target;
            IntQueue q = edge.out;
            while (q != null) {
                output.add(q.value);
                q = q.next;
            }
        }
        if (init.out == null) return null;
        IntQueue q = init.out.str;
        while (q != null) {
            output.add(q.value);
            q = q.next;
        }
        return output;
    }


}

