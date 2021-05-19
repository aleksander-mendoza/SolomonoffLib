package net.alagris.core.learn;

import net.alagris.core.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Queue;
import java.util.*;

public class OSTIACompress {


    public static State buildPtt(IntEmbedding alph, Iterator<Pair<IntSeq, IntSeq>> informant) {
        final State root = new State(alph.size());
        while (informant.hasNext()) {
            Pair<IntSeq, IntSeq> inout = informant.next();
            if(inout.r()!=null) {
                OSTIAState.buildPttOnward(root, alph, inout.l(), inout.r());
            }
        }
        return root;
    }


    public static void addBlueStates(State parent, Queue<Blue> blue) {
        for (int i = 0; i < parent.transitions.length; i++) {
            final Edge transition = parent.transitions[i];
            if (transition != null) {
                assert !contains(blue, transition.target);
                assert transition.target != parent;
                blue.add(new Blue(parent, i));
            }
        }
    }


    public static void ostia(State transducer) {
        final Queue<Blue> blue = new LinkedList<>();
        final Set<State> red = new LinkedHashSet<>();
        assert OSTIAState.isTree(transducer);
        red.add(transducer);
        addBlueStates(transducer, blue);
        assert uniqueItems(blue);
        assert disjoint(blue, red);
        assert validateBlueAndRed(transducer, red, blue);
        blue:
        while (!blue.isEmpty()) {
            final Blue next = blue.poll();
            final State blueState = next.state();
            assert blueState != null;
            assert OSTIAState.isTree(blueState);
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);

            for (State redState : red) {
                if (ostiaFold(redState, blueState)) {
                    next.parent.transitions[next.symbol].target = redState;
                    assert disjoint(blue, red);
                    assert uniqueItems(blue);
                    continue blue;
                }
            }
            assert OSTIAState.isTree(blueState);
            assert uniqueItems(blue);
            addBlueStates(blueState, blue);
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);
            red.add(blueState);
            assert disjoint(blue, red);
            assert validateBlueAndRed(transducer, red, blue);
        }
    }



    public static boolean ostiaFold(State red, State blue) {
        assert red != blue;
        assert blue.kind!= OSTIAState.Kind.REJECTING; // UNKNOWN is treated here as REJECTING by default
        assert red.kind!= OSTIAState.Kind.REJECTING;
        if(blue.kind!=red.kind)return false;
        if (blue.kind == OSTIAState.Kind.ACCEPTING) {
            if (!IntQueue.equals(red.out, blue.out)) {
                return false;
            }
        }
        for (int i = 0; i < red.transitions.length; i++) {
            final Edge transitionBlue = blue.transitions[i];
            final Edge transitionRed = red.transitions[i];
            if (transitionBlue == null) {
                if(transitionRed != null){
                    return false;
                }
            }else{
                if(transitionRed == null)return false;
                if(!IntQueue.equals(transitionBlue.out,transitionRed.out)){
                   return false;
                }
                if(!ostiaFold(transitionRed.target,transitionBlue.target)){
                    return false;
                }
            }
        }
        return true;
    }


    // Assertion methods

    public static boolean disjoint(Queue<Blue> blue, Set<State> red) {
        for (Blue b : blue) {
            if (red.contains(b.state())) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(Queue<Blue> blue, @Nullable State state) {
        return Util.exists(blue, b -> Objects.equals(state, b.state()));
    }

    public static boolean uniqueItems(Queue<Blue> blue) {
        return Util.unique(blue, Blue::state);
    }

    public static boolean validateBlueAndRed(State root, Set<State> red, Queue<Blue> blue) {
        final Set<State> reachable = OSTIAState.collect(root);
        for (State r : red) {
            for (Edge edge : r.transitions) {
                assert edge == null || contains(blue, edge.target) ^ red.contains(edge.target);
            }
            assert reachable.contains(r);
        }
        for (Blue b : blue) {
            assert red.contains(b.parent);
            assert reachable.contains(b.state());
        }
        return true;
    }



    static class Edge {
        @Nullable IntQueue out;
        State target;

        Edge() {
        }

        Edge(Edge edge) {
            out = IntQueue.copyAndConcat(edge.out, null);
            target = edge.target;
        }

        @Override
        public String toString() {
            return String.valueOf(target);
        }
    }

    static class Blue {

        final State parent;
        final int symbol;

        Blue(State parent, int symbol) {
            this.symbol = symbol;
            this.parent = parent;
        }

        State state() {
            final Edge edge = parent.transitions[symbol];
            assert edge != null;
            return edge.target;
        }

        @Override
        public String toString() {
            return String.valueOf(state());
        }
    }

    public static class State implements OSTIAState<Edge, State> {

        OSTIAState.Kind kind = Kind.UNKNOWN;
        @Nullable IntQueue out;
        @Nullable Edge[] transitions;

        @Override
        public String toString() {
            return String.valueOf(out);
        }

        State(int alphabetSize) {
            out = null;
            transitions = new Edge[alphabetSize];
        }

        @Override
        public Kind getKind() {
            return kind;
        }

        @Override
        public void setKind(Kind kind) {
            this.kind = kind==Kind.REJECTING?Kind.UNKNOWN:kind;
        }

        @Override
        public Edge transition(int symbol) {
            return transitions[symbol];
        }

        @Override
        public boolean isKnown(Edge edge) {
            return true;
        }

        @Override
        public void setKnown(Edge edge, boolean isKnown) {
        }

        @Override
        public IntQueue getOutput(Edge edge) {
            return edge.out;
        }

        @Override
        public int transitionCount() {
            return transitions.length;
        }

        @Override
        public void setOutput(Edge edge, IntQueue out) {
            edge.out = out;
        }

        @Override
        public void pushback(IntQueue prefix) {
            for (Edge edge : transitions) {
                if (edge != null) {
                    edge.out = IntQueue.copyAndConcat(prefix, edge.out);
                }
            }
            if (kind == Kind.ACCEPTING) {
                out = IntQueue.copyAndConcat(prefix, out);
            }
        }

        @Override
        public IntQueue getStateOutput() {
            return out;
        }

        @Override
        public void setStateOutput(IntQueue out) {
            this.out = out;
        }

        @Override
        public State getTarget(Edge edge) {
            return edge.target;
        }

        @Override
        public void setChild(int symbol, Edge edge) {
            edge.target = new OSTIACompress.State(transitions.length);
            transitions[symbol] = edge;
        }

        @Override
        public Edge edgeConstructor() {
            return new Edge();
        }

    }


}