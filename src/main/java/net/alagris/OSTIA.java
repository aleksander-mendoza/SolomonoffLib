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

    interface IntSeq {
        int size();

        int get(int index);
    }
    private static IntSeq seq(int... ints) {
        return new IntSeq() {
            @Override
            public int size() {
                return ints.length;
            }

            @Override
            public int get(int index) {
                return ints[index];
            }

            @Override
            public String toString() {
                return Arrays.toString(ints);
            }
        };
    }

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
            return OSTIA.toString(this);
        }
    }

    static class Out {
        IntQueue str;

        public Out(IntQueue str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return OSTIA.toString(str);
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
                ptt = edge.target = new State(ptt.transitions.length);
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
                if (commonPrefixEdgePrev != null) {
                    commonPrefixEdgePrev.next = null;
                }
                edge.target.prependAndConsume(commonPrefixEdge);
                output = commonPrefixInformant;
                ptt = edge.target;
            }
        }
        if (ptt.out != null && !eq(ptt.out.str, output)) throw new IllegalArgumentException();
        ptt.out = new Out(output);
    }

    private static boolean eq(IntQueue a, IntQueue b) {
        while (a != null && b != null) {
            if (a.value != b.value) return false;
            a = a.next;
            b = b.next;
        }
        return a == null && b == null;
    }

    private static IntQueue asQueue(IntSeq str, int offset) {
        IntQueue q = null;
        for (int i = str.size() - 1; i >= offset; i--) {
            IntQueue next = new IntQueue();
            next.value = str.get(i);
            next.next = q;
            q = next;
        }
        return q;
    }


    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alphabetSize);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            buildPttOnward(root, inout.l(), asQueue(inout.r(), 0));
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
                out = edge.out;
                target = edge.target;
            }
        }

        Out out;
        private Edge[] transitions;

        State(int alphabetSize) {
            transitions = new Edge[alphabetSize];
        }

        State(State copy) {
            transitions = copyTransitions(copy.transitions);
            out = copy.out == null ? null : new Out(copyAndConcat(copy.out.str, null));
        }

        /**
         * The IntQueue is consumed and should not be reused after calling this method
         */
        void prependAndConsume(IntQueue prefix) {
            for (Edge edge : transitions) {
                if (edge != null) {
                    edge.out = copyAndConcat(prefix, edge.out);
                }
            }
            if (out == null) {
                out = new Out(prefix);
            } else {
                out.str = concat(prefix, out.str);
            }
        }

        @Override
        public String toString() {
            return OSTIA.toString(this);
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
    }

    static void addBlueStates(State parent, Stack<Blue> blue) {
        for (int i = 0; i < parent.transitions.length; i++)
            if (parent.transitions[i] != null)
                blue.push(new Blue(parent, i));
    }

    static State.Edge[] copyTransitions(State.Edge[] transitions) {
        final State.Edge[] copy = new State.Edge[transitions.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = transitions[i] == null ? null : new State.Edge(transitions[i]);
        }
        return copy;
    }

    public static void ostia(State transducer) {
        final Stack<Blue> blue = new Stack<>();
        final ArrayList<State> red = new ArrayList<>();
        red.add(transducer);
        addBlueStates(transducer, blue);
        blue:while (!blue.isEmpty()) {
            final Blue next = blue.pop();
            final State blueState = next.state();
            for (State redState : red) {
                if (ostiaMerge(next, blueState, redState, blue)) continue blue;
            }
            addBlueStates(blueState,blue);
            red.add(blueState);
        }
    }

    static class Debug {
        final State init;
        final HashMap<State, State> merged;

        Debug(State init, HashMap<State, State> merged) {
            this.init = init;
            this.merged = merged;
        }

        @Override
        public String toString() {
            final Stack<State> toVisit = new Stack<>();
            final HashMap<State, Integer> enumeration = new HashMap<>();
            final State mergedInit = merged.getOrDefault(init, init);
            toVisit.push(mergedInit);
            enumeration.put(mergedInit, 0);
            while (!toVisit.empty()) {
                final State next = toVisit.pop();
                for (State.Edge edge : next.transitions) {
                    if (edge != null) {
                        final State mergedTarget = merged.getOrDefault(edge.target, edge.target);
                        if (null == enumeration.putIfAbsent(mergedTarget, enumeration.size())) {
                            toVisit.add(mergedTarget);
                        }
                    }
                }
            }
            final StringBuilder sb = new StringBuilder();
            for (Map.Entry<State, Integer> stateAndIndex : enumeration.entrySet()) {
                for (int symbol = 0; symbol < stateAndIndex.getKey().transitions.length; symbol++) {
                    final State.Edge edge = stateAndIndex.getKey().transitions[symbol];
                    if (edge != null) {
                        sb.append(stateAndIndex.getValue())
                                .append(" -")
                                .append(symbol)
                                .append(":")
                                .append(edge.out)
                                .append("-> ")
                                .append(enumeration.get(merged.getOrDefault(edge.target, edge.target)))
                                .append("\n");
                    }
                }
                final State thisMerged = merged.getOrDefault(stateAndIndex.getKey(), stateAndIndex.getKey());
                sb.append(stateAndIndex.getValue())
                        .append(":")
                        .append(thisMerged.out == null ? null : OSTIA.toString(thisMerged.out.str))
                        .append("\n");

            }
            return sb.toString();

        }
    }

    private static boolean ostiaMerge(Blue next, State blueState, State redState, Stack<Blue> blueToVisit) {
        if (redState.out != null && blueState.out != null && !eq(redState.out.str, blueState.out.str)) {
            return false;
        }
        final HashMap<State, State> merged = new HashMap<>();
        merged.computeIfAbsent(next.parent, State::new).transitions[next.symbol].target = redState;
        final ArrayList<Blue> reachedBlueStates = new ArrayList<>();
        final Debug redDebug = new Debug(redState, merged);
        final Debug blueDebug = new Debug(blueState, merged);
        if (ostiaFold(redState, new State(blueState), merged, reachedBlueStates)) {
            for (Map.Entry<State, State> mergedRedState : merged.entrySet()) {
                mergedRedState.getKey().assign(mergedRedState.getValue());
            }
            blueToVisit.addAll(reachedBlueStates);
            return true;
        }
        return false;
    }

    private static boolean ostiaFold(State red, State blue, HashMap<State, State> mergedStates, ArrayList<Blue> reachedBlueStates) {
        final Debug redDebug = new Debug(red, mergedStates);
        final Debug blueDebug = new Debug(blue, mergedStates);
        final State mergedState = mergedStates.computeIfAbsent(red, State::new);
        if(blue.out!=null) {
            if (mergedState.out == null) {
                mergedState.out = blue.out;
            } else if (!eq(mergedState.out.str, blue.out.str)){
                return false;
            }
        }

        for (int i = 0; i < mergedState.transitions.length; i++) {
            final State.Edge transitionBlue = blue.transitions[i];
            if (transitionBlue != null) {
                final State.Edge transitionRed = mergedState.transitions[i];
                if (transitionRed == null) {
                    mergedState.transitions[i] = new State.Edge(transitionBlue);
                    reachedBlueStates.add(new Blue(blue, i));
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
                    if (commonPrefixRed == null) {
                        final IntQueue blueSuffixToPushBack;
                        if (commonPrefixBluePrev == null) {
                            blueSuffixToPushBack = commonPrefixBlue;
                        } else {
                            blueSuffixToPushBack = commonPrefixBluePrev.next;
                            commonPrefixBluePrev.next = null;
                        }
                        final State blueTarget = new State(transitionBlue.target);
                        blueTarget.prependAndConsume(blueSuffixToPushBack);
                        if (!ostiaFold(transitionRed.target, blueTarget, mergedStates, reachedBlueStates)) {
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

    private static String toString(State init) {
        final Stack<State> toVisit = new Stack<>();
        final HashMap<State, Integer> enumeration = new HashMap<>();
        toVisit.push(init);
        enumeration.put(init, 0);
        while (!toVisit.empty()) {
            final State next = toVisit.pop();
            for (State.Edge edge : next.transitions) {
                if (edge != null && null == enumeration.putIfAbsent(edge.target, enumeration.size())) {
                    toVisit.add(edge.target);
                }
            }
        }
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<State, Integer> stateAndIndex : enumeration.entrySet()) {
            for (int symbol = 0; symbol < stateAndIndex.getKey().transitions.length; symbol++) {
                final State.Edge edge = stateAndIndex.getKey().transitions[symbol];
                if (edge != null) {
                    sb.append(stateAndIndex.getValue())
                            .append(" -")
                            .append(symbol)
                            .append(":")
                            .append(edge.out)
                            .append("-> ")
                            .append(enumeration.get(edge.target))
                            .append("\n");
                }
            }
            sb.append(stateAndIndex.getValue())
                    .append(":")
                    .append(stateAndIndex.getKey().out == null ? null : toString(stateAndIndex.getKey().out.str))
                    .append("\n");

        }
        return sb.toString();
    }

    public static void main(String[] args) {
        class Case {
            final List<Pair<IntSeq, IntSeq>> informant;
            final List<Pair<IntSeq, IntSeq>> tests;

            public Case(String informant, String tests) {
               this.informant = parse(informant);
               this.tests = parse(tests+" "+informant);
            }
            List<Pair<IntSeq, IntSeq>> parse(String pairs){
                final String[] elements = pairs.trim().split(" ");
                ArrayList<Pair<IntSeq, IntSeq>> list = new ArrayList<>(elements.length);
                for(String element: elements){
                    final String[] pair = element.split(":",-1);
                    list.add(Pair.of(parseBinary(pair[0]),parseBinary(pair[1])));
                }
                return list;
            }
            IntSeq parseBinary(String binary){
                final int[] in = new int[binary.length()];
                for(int i=0;i<binary.length();i++){
                    in[i] = binary.charAt(i)-'0';
                }
                return seq(in);
            }
        }
        Case[] cases = {
                new Case("0:",""),
                new Case("0:010",""),
                new Case("0: 00:00",""),
                new Case("0: 00:00 000:0000",""),
                new Case("0: 00:00 000:0000","0000:000000 00000:00000000"),
                new Case("0:00 00:0000","000:000000 0000:00000000"),
                new Case("0:00 00:0000 1:1 11:11 111:111 1111:1111","11111:11111 000:000000 0000:00000000"),
                new Case("0:00 00:0000 1:1 11:11","11111:11111 000:000000 0000:00000000"),
        };
        for (Case caze : cases) {
            final State tr = buildPtt(2, caze.informant.iterator());
            System.out.println(toString(tr));
            System.out.println();
            for (Pair<IntSeq, IntSeq> inOut : caze.informant) {
                ArrayList<Integer> out = run(tr, iter(inOut.l(), 0));
                assert out != null : out + " " + inOut + "\n" + toString(tr);
                assert out.size() == inOut.r().size() : out + " " + inOut + "\n" + toString(tr);
                for (int i = 0; i < out.size(); i++) {
                    assert out.get(i) == inOut.r().get(i) : out + " " + inOut + "\n" + toString(tr);
                }
            }
            ostia(tr);
            System.out.println(toString(tr));
            System.out.println("=========");
            for (Pair<IntSeq, IntSeq> inOut : caze.tests) {
                ArrayList<Integer> out = run(tr, iter(inOut.l(), 0));
                assert out != null : out + " " + inOut + "\n" + toString(tr);
                assert out.size() == inOut.r().size() : out + " " + inOut + "\n" + toString(tr);
                for (int i = 0; i < out.size(); i++) {
                    assert out.get(i) == inOut.r().get(i) : out + " " + inOut + "\n" + toString(tr);
                }
            }
        }
    }

}

