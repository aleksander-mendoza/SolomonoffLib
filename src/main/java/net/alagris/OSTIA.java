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

    public static IntSeq seq(int... ints) {
        return new IntSeq(ints);
    }



    static class IntQueue {
        int value;
        IntQueue next;

        @Override
        public String toString() {
            return OSTIA.toString(this);
        }


        public static int len(IntQueue q){
            int len =0;
            while(q!=null){
                len++;
                q = q.next;
            }
            return len;
        }
        public static int[] arr(IntQueue q){
            final int[] arr = new int[len(q)];
            for(int i=0;i<arr.length ;i++){
                arr[i] = q.value;
                q = q.next;
            }
            return arr;
        }
    }
    public static boolean hasCycle(IntQueue q){
        final HashSet<IntQueue> elements = new HashSet<>();
        while(q!=null){
            if(!elements.add(q)){
                return true;
            }
            q = q.next;
        }
        return false;
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
        assert !hasCycle(q) && !hasCycle(tail);
        if (q == null) return tail;
        final IntQueue first = q;
        while (q.next != null) {
            q = q.next;
        }
        q.next = tail;
        assert !hasCycle(first);
        return first;
    }

    static IntQueue copyAndConcat(IntQueue q, IntQueue tail) {
        assert !hasCycle(q) && !hasCycle(tail);
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
        assert !hasCycle(root);
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
                edge.target.prepend(commonPrefixEdge);
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
        assert !hasCycle(q);
        return q;
    }


    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alphabetSize,IntSeq.Epsilon);
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            buildPttOnward(root, inout.l(), asQueue(inout.r(), 0));
//            System.out.println("Adding "+inout);
//            printTree(root,0);
        }
        return root;
    }

//    static void printTree(State root,int indent){
//        System.out.println("-> "+(root.out==null?"#":root.out.str));
//        for(int s=0;s<root.transitions.length;s++) {
//            if(root.transitions[s]!=null) {
//                for (int i = 0; i < indent; i++) System.out.print("  ");
//                System.out.print(s+":"+toString(root.transitions[s].out)+" ");
//                if(root.transitions[s].target!=null)printTree(root.transitions[s].target,indent+1);
//            }
//        }
//    }

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
                out = copyAndConcat(edge.out,null);
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
            out = copy.out == null ? null : new Out(copyAndConcat(copy.out.str, null));
        }

        /**
         * The IntQueue is consumed and should not be reused after calling this method
         */
        void prepend(IntQueue prefix) {
            for (Edge edge : transitions) {
                if (edge != null) {
                    edge.out = copyAndConcat(prefix, edge.out);
                }
            }
            if (out == null) {
                out = new Out(prefix);
            } else {
                out.str = copyAndConcat(prefix, out.str);
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
    public static void collect(State transducer){
        STATES.clear();
        final Stack<State> toVisit = new Stack<>();
        toVisit.push(transducer);
        STATES.put(transducer,new VV());
        while(!toVisit.isEmpty()){
            final State s = toVisit.pop();
            for(int i=0;i<s.transitions.length;i++){
                final State.Edge e = s.transitions[i];
                if(e!=null){
                    toVisit.push(e.target);
                    STATES.put(e.target,new VV());
                }
            }
        }
    }

    private static HashMap<State,VV> STATES = new HashMap<>();
    public static void ostia(State transducer,boolean visualize) {
        collect(transducer);
        final java.util.Queue<Blue> blue = new LinkedList<>();
        final ArrayList<State> red = new ArrayList<>();
        red.add(transducer);
        HashMapIntermediateGraph.LexUnicodeSpecification spec = new HashMapIntermediateGraph.LexUnicodeSpecification();
        addBlueStates(transducer, blue);
//        System.out.println(transducer);
        if(visualize)LearnLibCompatibility.visualize(spec.compileOSTIA(transducer,'a'));
        //*''*  0:1   *'\0'*  0:02  *<1>*   1:  *<0 0 1>*   1:11   *<0 0 1 1>*
        //Merged: <0 0 1> <0 1>
        //*<0 0 1>* -1:1 1 1-> *<0 0 1 1>*
        //*<0 1>* -1:1 1-> *<0 1 1>*
        blue:while (!blue.isEmpty()) {
            final Blue next = blue.poll();
            final State blueState = next.state();
            assert STATES.containsKey(blueState):blueState;
            for (State redState : red) {
                assert STATES.containsKey(redState):blueState;
                if (ostiaMerge(next, redState, blue)){
                    System.out.println("Merged: "+blueState.shortest+" "+redState.shortest+" "+red+" "+blue);
//                    System.out.println(transducer);

                    if(visualize) LearnLibCompatibility.visualize(spec.compileOSTIA(transducer,'a'));
                    continue blue;
                }else{
                    System.out.println("Fail: "+blueState.shortest+" "+redState.shortest+" "+red+" "+blue);
                }
            }
            addBlueStates(blueState,blue);
            red.add(blueState);
        }
        System.out.println(transducer);
    }

    private static boolean ostiaMerge(Blue blue, State redState, java.util.Queue<Blue> blueToVisit) {
        final HashMap<State, State> merged = new HashMap<>();
        final ArrayList<Blue> reachedBlueStates = new ArrayList<>();
        if (ostiaFold(redState,null,blue.parent,blue.symbol , merged, reachedBlueStates)) {
            for (Map.Entry<State, State> mergedRedState : merged.entrySet()) {
                assert Specification.find(Arrays.asList(mergedRedState.getValue().transitions),e->e!=null&&e.target!=null&&!STATES.containsKey(e.target))==null;
                System.out.println("Assign to "+mergedRedState.getKey()+" edges "+ Arrays.toString(mergedRedState.getValue().transitions));
                mergedRedState.getKey().assign(mergedRedState.getValue());
            }
            System.out.println("Add blue "+reachedBlueStates);
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
            } else if (!eq(mergedRedState.out.str, mergedBlueState.out.str)){
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
        final HashSet<State> collected = new HashSet<>();
        toVisit.push(init);
        collected.add(init);
        while (!toVisit.empty()) {
            final State next = toVisit.pop();
            for (State.Edge edge : next.transitions) {
                if (edge != null && collected.add(edge.target)) {
                    toVisit.add(edge.target);
                }
            }
        }
        final StringBuilder sb = new StringBuilder("init *"+init.shortest+"*\n");
        for (State state : collected) {
            for (int symbol = 0; symbol < state.transitions.length; symbol++) {
                final State.Edge edge = state.transitions[symbol];
                if (edge != null) {
                    sb.append("*").append(state.shortest)
                            .append("* -")
                            .append(symbol)
                            .append(":")
                            .append(edge.out)
                            .append("-> *")
                            .append(edge.target.shortest)
                            .append("*\n");
                }
            }
            sb.append("*").append(state.shortest)
                    .append("*:")
                    .append(state.out == null ? null : toString(state.out.str))
                    .append("\n");

        }
        return sb.toString();
    }


}

