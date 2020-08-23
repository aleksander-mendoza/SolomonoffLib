package net.alagris.learn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.alagris.learn.NFA.Targets;
import net.alagris.learn.NFA.Transitions;
import net.alagris.learn.__.F3;
import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;
import net.alagris.learn.__.O;
import net.alagris.learn.__.P2;

public class KReversible {

    private KReversible() {
    }
    public static <Q, Sigma> NFA<Q, Sigma> mergeDeterministic(MutableNFA<Q, Sigma> nfa, Q q1, Q q2) {
        return makeDeterministic(NFAUtils.merge(nfa, q1, q2), q1);

    }

    private static <Q, Sigma> void find1Violators(NFA<Q, Sigma> nfa, Map<P2<Q, Q>, P2<Q, Q>> violators,
            P2<Q, Q> violatorPair, P2<Q, Q> original) {
        I.foreach(NFAUtils.zipTransitions(nfa.getIncoming(violatorPair.a()), nfa.getIncoming(violatorPair.b())), p -> {
            for (Q e1 : p.a()) {
                for (Q e2 : p.b()) {
                    violators.put(P2.unordered(e1, e2), original);
                }
            }
            return __.U;
        });
    }

    public static <Q, Sigma> boolean are0Violators(NFA<Q, Sigma> nfa, int k, Q q1, Q q2) {
        return nfa.accepting().contains(q1) && nfa.accepting().contains(q2)
                || haveCommonTargetOverTheSameSigma(nfa.getOutgoing(q1), nfa.getOutgoing(q2));
    }

    public static <Q, Sigma> HashMap<P2<Q, Q>, P2<Q, Q>> find0Violators(NFA<Q, Sigma> nfa) {
        HashMap<P2<Q, Q>, P2<Q, Q>> zeroViolators = new HashMap<>();
        for (Q q1 : nfa.accepting()) {
            for (Q q2 : nfa.accepting()) {
                P2<Q, Q> p = P2.unordered(q1, q2);
                zeroViolators.put(p, p);
            }
            Transitions<Q, Sigma> incoming = nfa.getIncoming(q1);
            for (Targets<Q, Sigma> perSigma : incoming) {
                for (Q i1 : perSigma) {
                    for (Q i2 : perSigma) {
                        P2<Q, Q> p = P2.unordered(i1, i2);
                        zeroViolators.put(p, p);
                    }
                }
            }
        }
        return zeroViolators;
    }

    public static <Q, Sigma> O<P2<Q, Q>> findKViolators(NFA<Q, Sigma> nfa, int k) {
        return findKViolators(nfa, k, find0Violators(nfa));
    }

    public static <Q, Sigma> O<P2<Q, Q>> findKViolators(NFA<Q, Sigma> nfa, int k,
            HashMap<P2<Q, Q>, P2<Q, Q>> nViolatorsToOriginal) {
        HashMap<P2<Q, Q>, P2<Q, Q>> visitedToOriginal = new HashMap<>();
        for (int n = 0; n < k; n++) {
            HashMap<P2<Q, Q>, P2<Q, Q>> nPlus1ViolatorsToOriginal = new HashMap<>();
            for (Entry<P2<Q, Q>, P2<Q, Q>> violatorsAndOriginl : nViolatorsToOriginal.entrySet()) {
                find1Violators(nfa, nPlus1ViolatorsToOriginal, violatorsAndOriginl.getKey(),
                        violatorsAndOriginl.getValue());
            }
            if (nPlus1ViolatorsToOriginal.isEmpty()) {
                return __.none();// no need to check further
            }
            visitedToOriginal.putAll(nViolatorsToOriginal);
            O<P2<Q, Q>> common = HM.anyCommon(nPlus1ViolatorsToOriginal.keySet(), visitedToOriginal.keySet());
            if (common.isSome()) {
                return __.some(nPlus1ViolatorsToOriginal.get(common.force()));
                // if there is a loop of violators, then
                // the automaton is not k-reversible for
                // any k
            }
            nViolatorsToOriginal = nPlus1ViolatorsToOriginal;
        }
        // by this point n==k
        // so if there are still any nViolators left then the states q1 and q2 are
        // kViolators.
        return __.any(nViolatorsToOriginal.values());
    }

    public static <Q, Sigma> NFA<Q, Sigma> makeDeterministic(MutableNFA<Q, Sigma> nfa, Q q) {
        for (Targets<Q, Sigma> sigma : nfa.getOutgoing(q)) {
            if (sigma.size() > 1) {
                final Iterator<Q> iter = sigma.iterator();
                final Q first = iter.next();
                while (iter.hasNext()) {
                    nfa = NFAUtils.merge(nfa, first, iter.next());
                }
                makeDeterministic(nfa, first);
            }
        }
        return nfa;
    }


    public static <Q, Sigma> NFA<Q, Sigma> learn(Text<Sigma> samples, int k, Q initialState,
            F3<Q, Sigma, Q> stringToState) {
        MutableNFA<Q, Sigma> nfa = NFAUtils.prefixTreeAutomaton(samples, initialState, stringToState);
        O<P2<Q, Q>> violators;
        while ((violators = findKViolators(nfa, k)).isSome()) {
            P2<Q, Q> p = violators.force();
            mergeDeterministic(nfa, p.a(), p.b());
        }
        return nfa;
    }

    private static <Q, Sigma> boolean haveCommonTargetOverTheSameSigma(Transitions<Q, Sigma> q1,
            Transitions<Q, Sigma> q2) {
        return I.find(NFAUtils.zipTransitions(q1, q2), p -> NFAUtils.intersect(p.a(), p.b())).isSome();
    }

}
