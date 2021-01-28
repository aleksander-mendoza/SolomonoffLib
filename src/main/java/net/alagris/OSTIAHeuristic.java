//package net.alagris;
//
//import org.checkerframework.checker.nullness.qual.NonNull;
//import org.checkerframework.checker.nullness.qual.Nullable;
//
//import java.util.Queue;
//import java.util.*;
//
//public class OSTIAHeuristic {
//
//
//    public static State buildPtt(int alphabetSize, Iterator<Pair<IntSeq, IntSeq>> informant) {
//        final State root = new State(alphabetSize, IntSeq.Epsilon, new StatePTT(alphabetSize));
//        while (informant.hasNext()) {
//            Pair<IntSeq, IntSeq> inout = informant.next();
//            if(inout.r()==null){
//                buildPttOnward(root, inout.l(), true, null);
//            }else{
//                buildPttOnward(root, inout.l(), false, inout.r());
//            }
//
//        }
//        return root;
//    }
//
//    private static void buildPttOnward(State ptt, IntSeq input, boolean rejecting, IntSeq out) {
//        @Nullable IntQueue output = IntQueue.asQueue(out);
//        State pttIter = ptt;
//        assert !rejecting || output==null;
//        @Nullable IntQueue outputIter = output;
//
//        for (int i = 0; i < input.size(); i++) {//input index
//            final int symbol = input.get(i);
//            final Edge edge;
//            if (pttIter.transitions[symbol] == null) {
//                edge = new Edge();
//                if(rejecting){
//                    edge.isKnown = false;
//                }else {
//                    edge.out = outputIter;
//                    edge.isKnown = true;
//                    outputIter = null;
//                }
//                final StatePTT p = new StatePTT(pttIter.transitions.length);
//                edge.target = new State(pttIter.transitions.length,pttIter.shortest.concat(new IntSeq(symbol)),p);
//                pttIter.transitions[symbol] = edge;
//                pttIter.ptt.transitions[symbol] = p;
//            } else {
//                edge = pttIter.transitions[symbol];
//                if(!rejecting) {
//                    if (edge.isKnown) {
//                        IntQueue commonPrefixEdge = edge.out;
//                        IntQueue commonPrefixEdgePrev = null;
//                        IntQueue commonPrefixInformant = outputIter;
//                        while (commonPrefixEdge != null && commonPrefixInformant != null &&
//                                commonPrefixEdge.value == commonPrefixInformant.value) {
//                            commonPrefixInformant = commonPrefixInformant.next;
//                            commonPrefixEdgePrev = commonPrefixEdge;
//                            commonPrefixEdge = commonPrefixEdge.next;
//                        }
//                        /*
//                        informant=x
//                        edge.out=y
//                        ->
//                        informant=lcp(x,y)^-1 x
//                        edge=lcp(x,y)
//                        pushback=lcp(x,y)^-1 y
//                        */
//                        if (commonPrefixEdgePrev == null) {
//                            edge.out = null;
//                        } else {
//                            commonPrefixEdgePrev.next = null;
//                        }
//                        edge.target.prependButIgnoreMissingStateOutput(commonPrefixEdge);
//                        outputIter = commonPrefixInformant;
//                    } else {
//                        edge.out = outputIter;
//                        edge.isKnown = true;
//                        outputIter = null;
//                    }
//                }
//            }
//            pttIter = edge.target;
//        }
//        if(pttIter.kind == State.ACCEPTING){
//            if ( !IntQueue.equals(pttIter.out, outputIter)) {
//                throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.out +
//                        "' but training sample has remaining suffix '" + outputIter + '\'');
//            }
//            if(rejecting){
//                throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.out +
//                        "' but training sample tells to reject");
//            }
//        }else if(pttIter.kind == State.REJECTING){
//            if(!rejecting){
//                throw new IllegalArgumentException("For input '" + input + "' the state rejects but training sample " +
//                        "has remaining suffix '" + pttIter.out +
//                        "'");
//            }
//        }else{
//            assert pttIter.kind == State.UNKNOWN;
//            pttIter.kind = rejecting? State.REJECTING : State.ACCEPTING;
//            pttIter.out = outputIter;
//            pttIter.ptt.kind = pttIter.kind;
//            pttIter.ptt.out = out;
//        }
//
//
//    }
//
//    public static void ostia(State transducer) {
//        final Queue<Blue> blue = new LinkedList<>();
//        final Set<State> red = new LinkedHashSet<>();
//        red.add(transducer);
//        addBlueStates(transducer, blue);
//        blue:
//        while (!blue.isEmpty()) {
//            @SuppressWarnings("nullness") // false positive https://github.com/typetools/checker-framework/issues/399
//            final @NonNull Blue next = blue.poll();
//            final @Nullable State blueState = next.state();
//            assert blueState != null;
//
//            for (State redState : red) {
//                if (ostiaMerge(next, redState, blue, red)) {
//                    continue blue;
//                }
//            }
//            addBlueStates(blueState, blue);
//            red.add(blueState);
//        }
//    }
//
//    private static boolean ostiaMerge(State a, State b) {
//        if (ostiaFold(a,b,null,null)) {
//            for (Map.Entry<State, StateCopy> mergedRedState : merged.entrySet()) {
//                assert mergedRedState.getKey() == mergedRedState.getValue().original;
//                mergedRedState.getValue().assign();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    private static boolean ostiaFold(State a,
//                                     State b,
//                                     @Nullable IntQueue pushedBackA,
//                                     @Nullable IntQueue pushedBackB) {
//
//        final State mergedA = a.mergeDestination();
//        final State mergedB = b.mergeDestination();
//        assert mergedA!=null;
//        assert mergedB!=null;
//
//        final Edge mergedIncomingTransition =
//                mergedStates.computeIfAbsent(blueParent, StateCopy::new).transitions[symbolIncomingToBlue];
//        assert mergedIncomingTransition != null;
//        mergedIncomingTransition.target = red;
//
//        final StateCopy prevBlue = mergedStates.put(blueState, mergedBlueState);
//        assert prevBlue == null;
//
//        mergedBlueState.prepend(pushedBack);
//        if (mergedBlueState.kind == State.ACCEPTING) {
//            if (mergedRedState.kind == State.UNKNOWN) {
//                mergedRedState.out = mergedBlueState.out;
//                mergedRedState.kind = State.ACCEPTING;
//            }else if(mergedRedState.kind == State.REJECTING){
//                return false;
//            } else if (!IntQueue.equals(mergedRedState.out, mergedBlueState.out)) {
//                return false;
//            }
//        }else if(mergedBlueState.kind == State.REJECTING){
//            if(mergedRedState.kind == State.ACCEPTING){
//                return false;
//            }else if(mergedRedState.kind == State.UNKNOWN){
//                mergedRedState.kind = State.REJECTING;
//            }
//        }
//        for (int i = 0; i < mergedRedState.transitions.length; i++) {
//            final Edge transitionBlue = mergedBlueState.transitions[i];
//            if (transitionBlue != null) {
//                final Edge transitionRed = mergedRedState.transitions[i];
//                if (transitionRed == null) {
//                    mergedRedState.transitions[i] = new Edge(transitionBlue);
//                    reachedBlueStates.add(new Blue(red, i));
//                } else {
//                    if(transitionRed.isKnown) {
//                        IntQueue commonPrefixRed = transitionRed.out;
//                        IntQueue commonPrefixBlue = transitionBlue.out;
//                        IntQueue commonPrefixBluePrev = null;
//                        while (commonPrefixBlue != null && commonPrefixRed != null &&
//                                commonPrefixBlue.value == commonPrefixRed.value) {
//                            commonPrefixBluePrev = commonPrefixBlue;
//                            commonPrefixBlue = commonPrefixBlue.next;
//                            commonPrefixRed = commonPrefixRed.next;
//                        }
//                        assert commonPrefixBluePrev == null || commonPrefixBluePrev.next == commonPrefixBlue;
//                        if (commonPrefixRed == null) {//check if no leftover output remains on red edge
//                            if (commonPrefixBluePrev == null) {
//                                transitionBlue.out = null;
//                            } else {
//                                commonPrefixBluePrev.next = null;
//                            }
//                            assert Objects.equals(Optional.ofNullable(mergedBlueState.transitions[i]).map(e -> e.target),
//                                    Optional.ofNullable(blueState.transitions[i]).map(e -> e.target));
//                            if (!ostiaFold(transitionRed.target,
//                                    commonPrefixBlue,
//                                    blueState,
//                                    i,
//                                    mergedStates,
//                                    reachedBlueStates)) {
//                                return false;
//                            }
//                        } else {
//                            return false;
//                        }
//                    }else{
//                        transitionRed.isKnown = transitionBlue.isKnown;
//                        transitionRed.out = transitionBlue.out;
//                        if (!ostiaFold(transitionRed.target,
//                                null,
//                                blueState,
//                                i,
//                                mergedStates,
//                                reachedBlueStates)) {
//                            return false;
//                        }
//                    }
//                }
//            }
//        }
//        return true;
//    }
//    public static @Nullable IntSeq run(State init, IntSeq input) {
//        final List<Integer> output = new ArrayList<>();
//        State iter = init;
//        for (int i = 0; i < input.size(); i++) {
//            final Edge edge = iter.transitions[input.get(i)];
//            if (edge == null) {
//                return null;
//            }
//            iter = edge.target;
//            IntQueue q = edge.out;
//            while (q != null) {
//                output.add(q.value);
//                q = q.next;
//            }
//        }
//        if (iter.kind != State.ACCEPTING) {
//            return null;
//        }
//        IntQueue q = iter.out;
//        while (q != null) {
//            output.add(q.value);
//            q = q.next;
//        }
//        int[] arr = new int[output.size()];
//        for(int i=0;i<output.size();i++) {
//        	arr[i] = output.get(i);
//        }
//        return new IntSeq(arr);
//    }
//
//    // Assertion methods
//
//    static class Edge {
//        boolean isKnown;
//        @Nullable IntQueue out;
//        State target;
//
//        Edge() {}
//
//        Edge(Edge edge) {
//            out = IntQueue.copyAndConcat(edge.out, null);
//            target = edge.target;
//            isKnown = edge.isKnown;
//        }
//
//        @Override
//        public String toString() {
//            return String.valueOf(target);
//        }
//    }
//
//    static class StateParent {
//
//        static final int UNKNOWN=0;
//        static final int ACCEPTING=1;
//        static final int REJECTING=2;
//        int kind=UNKNOWN;
//        @Nullable IntQueue out;
//        @Nullable Edge[] transitions;
//
//        @Override
//        public String toString() {
//            return String.valueOf(out);
//        }
//    }
//    static class StateCopy extends StateParent {
//
//        StateCopy(State original) {
//            this.out = original.out;
//            this.transitions = copyTransitions(original.transitions);
//            this.kind = original.kind;
//        }
//        private static @Nullable Edge[] copyTransitions(@Nullable Edge[] transitions) {
//            final @Nullable Edge[] copy = new Edge[transitions.length];
//            for (int i = 0; i < copy.length; i++) {
//                @Nullable Edge edge = transitions[i];
//                copy[i] = edge == null ? null : new Edge(edge);
//            }
//            return copy;
//        }
//
//        /**
//         * The IntQueue is consumed and should not be reused after calling this method.
//         */
//        void prepend(@Nullable IntQueue prefix) {
//            for (@Nullable Edge edge : transitions) {
//                if (edge != null) {
//                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
//                }
//            }
//            if (kind == ACCEPTING) {
//                out = IntQueue.copyAndConcat(prefix, out);
//            }
//        }
//    }
//
//    static class StatePTT {
//        int kind = State.UNKNOWN;
//        IntSeq out;
//        @Nullable StatePTT[] transitions;
//        StatePTT(int alphSize) {
//            this.transitions = new StatePTT[alphSize];
//        }
//
//    }
//
//    public static class State extends StateParent {
//
//        State mergedWith;
//        StateCopy mutated;
//        final StatePTT ptt;
//    	final IntSeq shortest;
//        State(int alphabetSize,IntSeq shortest,StatePTT ptt) {
//            this.ptt = ptt;
//            super.out = null;
//            super.transitions = new Edge[alphabetSize];
//            this.shortest = shortest;
//        }
//
//        State mergeDestination(){
//            State curr = this;
//            while(curr.mergedWith!=null){
//                curr = curr.mergedWith;
//            }
//            return curr;
//        }
//
//        /**
//         * The IntQueue is consumed and should not be reused after calling this method.
//         */
//        void prependButIgnoreMissingStateOutput(@Nullable IntQueue prefix) {
//            for (@Nullable Edge edge : transitions) {
//                if (edge != null) {
//                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
//                }
//            }
//            if (kind == ACCEPTING) {
//                out = IntQueue.copyAndConcat(prefix, out);
//            }
//        }
//    }
//}