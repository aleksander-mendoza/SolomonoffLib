package net.alagris.learn;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.alagris.learn.NFA.States;
import net.alagris.learn.NFA.Targets;
import net.alagris.learn.NFA.Transitions;
import net.alagris.learn.__.F2;
import net.alagris.learn.__.F3;
import net.alagris.learn.__.I;
import net.alagris.learn.__.Iff;
import net.alagris.learn.__.P2;
import net.alagris.learn.__.S;
import net.alagris.learn.__.U;
import net.alagris.learn.__.V2;
import net.alagris.learn.__.W;

public class NFAUtils {

    private NFAUtils() {
    }
    
    public <Q,Sigma> GraphNFA<Q,Sigma> convertToGraphBacked(NFA<Q,Sigma> nfa) {
        GraphNFA<Q,Sigma> g = new  GraphNFA<>();
        for(Q q:nfa) {
            g.addState(q);
        }
        for(Q from:nfa) {
            for(Targets<Q, Sigma> trans:nfa.getOutgoing(from)){
                for(Q to:trans) {
                    g.putTransition(from, trans.getSigma(), to);
                }
            }
        }
        return g;
    }
    
    


    /**
     * @return <tt>true</tt> if this automaton did not already contain the specified
     *         transition
     */
    public static <Q, Sigma> boolean setTransition(MutableNFA<Q, Sigma> nfa, Q from, Sigma sigma, Q to) {
        return nfa.getOutgoing(from).getStates(sigma).add(to);
    }

    /**
     * @return <tt>true</tt> if the automaton contained transition.
     */
    public static <Q, Sigma> boolean removeTransition(MutableNFA<Q, Sigma> nfa, Q from, Sigma sigma, Q to) {
        return nfa.getOutgoing(from).getStates(sigma).remove(to);
    }

    /**
     * @return <tt>true</tt> if the automaton contained transition. If it did not,
     *         then no redirection should occur and false will be returned.
     */
    public static <Q, Sigma> boolean redirectEdge(MutableNFA<Q, Sigma> nfa, Q from, Q to, Sigma sigma, Q newTo) {
        MutableNFA.Targets<Q, Sigma> states = nfa.getOutgoing(from).getStates(sigma);
        if (states.remove(to)) {
            states.add(newTo);
            return true;
        } else {
            return false;
        }
    }

    public static <Q, Sigma> boolean isEmpty(States<Q> states) {
        return states.iterator().hasNext();
    }

    public static <Q, Sigma> Q computeIfEmptyOrGetAny(MutableNFA.States<Q> states, F2<U, Q> newState) {
        Iterator<Q> iter = states.iterator();
        if (iter.hasNext()) {
            return iter.next();
        } else {
            Q to = newState.f(__.U);
            states.add(to);
            return to;
        }
    }

    public static <Q, Sigma> NFA<Q, Sigma> reverse(NFA<Q, Sigma> nfa) {
        return new NFA<Q, Sigma>() {

            @Override
            public Iterator<Q> iterator() {
                return nfa.iterator();
            }

            @Override
            public Transitions<Q, Sigma> getOutgoing(Q from) {
                return nfa.getIncoming(from);
            }

            @Override
            public Transitions<Q, Sigma> getIncoming(Q to) {
                return nfa.getOutgoing(to);
            }

            @Override
            public States<Q> accepting() {
                return nfa.initial();
            }

            @Override
            public States<Q> initial() {
                return nfa.accepting();
            }

        };
    }
    
    public static <Q, Sigma> NFA<Q, Sigma> reverseMut(MutableNFA<Q, Sigma> nfa) {
        return new MutableNFA<Q, Sigma>() {

            @Override
            public Iterator<Q> iterator() {
                return nfa.iterator();
            }

            @Override
            public MutableNFA.Transitions<Q, Sigma> getOutgoing(Q from) {
                return nfa.getIncoming(from);
            }

            @Override
            public MutableNFA.Transitions<Q, Sigma> getIncoming(Q to) {
                return nfa.getOutgoing(to);
            }

            @Override
            public MutableNFA.States<Q> accepting() {
                return nfa.initial();
            }

            @Override
            public MutableNFA.States<Q> initial() {
                return nfa.accepting();
            }

            @Override
            public boolean drop(Q state) {
                return nfa.drop(state);
            }

        };
    }

    public static <Q, Sigma> Iterator<P2<Targets<Q, Sigma>, Targets<Q, Sigma>>> zipTransitions(Transitions<Q, Sigma> q1,
            Transitions<Q, Sigma> q2) {
        return I.map(q1.iterator(), t -> P2.of(t, q2.getStates(t.getSigma())));
    }
    
    public static <Q, Sigma> Iterator<P2<MutableNFA.Targets<Q, Sigma>, MutableNFA.Targets<Q, Sigma>>> zipTransitionsMut(MutableNFA.Transitions<Q, Sigma> q1,
            MutableNFA.Transitions<Q, Sigma> q2) {
        return I.map(q1.mutIterator(), t -> P2.of(t, q2.getStates(t.getSigma())));
    }

    public static <Q, Sigma> U mergeIntoQ1(MutableNFA.Targets<Q, Sigma> q1, MutableNFA.Targets<Q, Sigma> q2) {
        for (Q e2 : q2) {
            q1.add(e2);
        }
        return __.U;
    }

    public static <Q, Sigma> U mergeIntoQ1(MutableNFA.Transitions<Q, Sigma> q1, MutableNFA.Transitions<Q, Sigma> q2) {
        return I.foreach(zipTransitionsMut(q1, q2), p -> __.u(mergeIntoQ1(p.a(), p.b())));
    }

    /** merges q1 and q2 into q1 */
    public static <Q, Sigma> MutableNFA<Q, Sigma> merge(MutableNFA<Q, Sigma> nfa, Q q1, Q q2) {
        if (q1 == q2)
            throw new IllegalArgumentException();
        mergeIntoQ1(nfa.getIncoming(q1), nfa.getIncoming(q2));
        mergeIntoQ1(nfa.getOutgoing(q1), nfa.getOutgoing(q2));
        if (nfa.initial().remove(q2)) {
            nfa.initial().add(q2);
        }
        if (nfa.accepting().remove(q2)) {
            nfa.accepting().add(q2);
        }
        nfa.drop(q2);
        return nfa;
    }

    public static <Q, Sigma> boolean intersect(States<Q> q1, States<Q> q2) {
        for (Q q : q1) {
            if (q2.contains(q))
                return true;
        }
        return false;
    }

    /** Iterates automaton in breath-first order and runs callback on each state */
    public static <Q, Sigma> void breathSearch(NFA<Q, Sigma> nfa, F3<Set<Q>, Q, Boolean> shouldSearchContinue) {
        Queue<Q> fifo = new LinkedList<>();
        HashSet<Q> visited = new HashSet<>();
        Set<Q> immutable = Collections.unmodifiableSet(visited);
        for (Q q2 : nfa.initial()) {
            fifo.add(q2);
            if (!shouldSearchContinue.f(immutable, q2)) {
                return;
            }
        }

        while (!fifo.isEmpty()) {
            Q q = fifo.poll();
            for (Targets<Q, Sigma> e : nfa.getOutgoing(q)) {
                for (Q q2 : e) {
                    if (visited.add(q2)) {
                        fifo.add(q2);
                        if (!shouldSearchContinue.f(immutable, q2)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public static <Q, Sigma> MutableNFA<Q, Sigma> prefixTreeAutomaton(Text<Sigma> samples, Q initialState,
            F3<Q, Sigma, Q> stringToState) {
        MutableNFA<Q, Sigma> nfa = new HashNFA<>();

        nfa.initial().add(initialState);
        I.foreach(samples,
                sample -> nfa.accepting()
                        .add(I.fold(initialState, I.seq(sample),
                                (state, nextChar) -> NFAUtils.computeIfEmptyOrGetAny(
                                        nfa.getOutgoing(state).getStates(nextChar),
                                        u -> stringToState.f(state, nextChar)))));
        return nfa;
    }

    public static <Q, Sigma> HashMap<Q, Integer> enumerateQ(NFA<Q, Sigma> nfa) {
        return I.enumerate(__.iter(nfa));
    }

    /**
     * Given any configuration (subset of Q) and symbol in Sigma, evaluates the
     * configuration at next step.
     * 
     * @param currentSubset - the configuration before reading input
     * @param nextSubset    - here will be stored the configuration after reading
     *                      the input
     */
    public static <Q, Sigma> void deltaHashed(NFA<Q, Sigma> nfa, Set<Q> currentSubset, Set<Q> nextSubset, Sigma input) {
        for (Q q : currentSubset) {
            for (Q next : nfa.getOutgoing(q).getStates(input)) {
                nextSubset.add(next);
            }
        }
    }

    /**
     * Given any configuration (subset of Q) and symbol in Sigma, evaluates the
     * configuration at next step.
     * 
     * @param currentSubset - the configuration before reading input
     * @param nextSubset    - here will be stored the configuration after reading
     *                      the input
     */
    public static <Q, Sigma> void deltaBoolArr(NFA<Q, Sigma> nfa, Iff<Q, Integer> toInt, S<Boolean> currentSubset,
            W<Boolean> nextSubset, Sigma input) {
        S.foreachenum(currentSubset, (i, q) -> {
            if (q) {
                for (Q next : nfa.getOutgoing(toInt.i(i)).getStates(input)) {
                    nextSubset.set(toInt.f(next), true);
                }
            }
            return __.U;
        });
    }

    /**
     * Implements transitive closure of delta. The <tt>C</tt> type is meant to
     * represent data structure for configuration (subset of Q)
     * 
     * @param empty - this is needed because this algorithm operates on two sets
     *              (and rotates between them).
     */
    public static <Q, Sigma> Set<Q> deltaTransitiveHashed(NFA<Q, Sigma> nfa, Set<Q> initial, Set<Q> empty,
            S<Sigma> input) {
        return S.fold(V2.of(initial, empty), input, (config, next) -> {
            deltaHashed(nfa, config.a, config.b, next);
            return V2.swap(config);
        }).a();
    }
    
}
