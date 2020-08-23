package net.alagris.learn;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.alagris.learn.__.F2;
import net.alagris.learn.__.F3;
import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;
import net.alagris.learn.__.P2;

public class HashNFA<Q, Sigma> implements MutableNFA<Q, Sigma> {
    private final HashSet<Q> accepting = new HashSet<>();
    private final HashSet<Q> initial = new HashSet<>();
    private final HashMap<P2<Q, Q>, HashSet<Sigma>> labels = new HashMap<>();
    private final HashMap<Q, HashMap<Sigma, HashSet<Q>>> delta = new HashMap<>();
    private final HashMap<Q, HashMap<Sigma, HashSet<Q>>> reverseDelta = new HashMap<>();

    @Override
    public Iterator<Q> iterator() {
        return delta.keySet().iterator();
    }

    @Override
    public MutableNFA.States<Q> accepting() {
        return MutableNFA.States.fromSet(accepting);
    }

    @Override
    public MutableNFA.States<Q> initial() {
        return MutableNFA.States.fromSet(initial);
    }

    @Override
    public MutableNFA.Transitions<Q, Sigma> getIncoming(Q from) {
        return get(reverseDelta, delta, labels, (q1, q2) -> P2.of(q2, q1), P2::b, P2::a, from);
    }

    @Override
    public MutableNFA.Transitions<Q, Sigma> getOutgoing(Q from) {
        return get(delta, reverseDelta, labels, P2::of, P2::a, P2::b, from);
    }

    private <P> MutableNFA.Transitions<Q, Sigma> get(HashMap<Q, HashMap<Sigma, HashSet<Q>>> delta,
            HashMap<Q, HashMap<Sigma, HashSet<Q>>> reverseDelta, HashMap<P, HashSet<Sigma>> labels, F3<Q, Q, P> pairing,
            F2<P, Q> proj1, F2<P, Q> proj2, Q from) {

        return new MutableNFA.Transitions<Q, Sigma>() {
            HashMap<Sigma, HashSet<Q>> outgoing = delta.get(from);

            @Override
            public Iterator<MutableNFA.Targets<Q, Sigma>> mutIterator() {
                if (outgoing == null)
                    return Collections.emptyIterator();
                return I.map(outgoing.entrySet().iterator(), e -> getStates(e.getKey(), e.getValue()));
            }

            @Override
            public Iterator<NFA.Targets<Q, Sigma>> iterator() {
                if (outgoing == null)
                    return Collections.emptyIterator();
                return I.map(outgoing.entrySet().iterator(), e -> getStates(e.getKey(), e.getValue()));
            }

            @Override
            public Labels<Q, Sigma> getLabels(Q to) {
                return new Labels<Q, Sigma>() {
                    HashSet<Sigma> labs = labels.get(pairing.f(from, to));

                    @Override
                    public Iterator<Sigma> iterator() {
                        if (labs == null)
                            return Collections.emptyIterator();
                        return labs.iterator();
                    }

                    @Override
                    public boolean contains(Sigma sigma) {
                        if (labs == null)
                            return false;
                        return labs.contains(sigma);
                    }

                    @Override
                    public boolean remove(Sigma sigma) {
                        if (labs == null)
                            return false;
                        if (labs.remove(sigma)) {
                            outgoing.get(sigma).remove(to);
                            reverseDelta.get(to).get(sigma).remove(from);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean add(Sigma sigma) {
                        if (labs == null) {
                            labels.put(pairing.f(from, to), labs = new HashSet<>());
                            outgoing = delta.computeIfAbsent(from, u -> new HashMap<>());
                        }
                        if (labs.add(sigma)) {
                            HM.append(outgoing, sigma, to);
                            HM.append(reverseDelta.get(to), sigma, from);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public Q getTarget() {
                        return to;
                    }

                    @Override
                    public Q getSource() {
                        return from;
                    }
                };
            }

            private Targets<Q, Sigma> getStates(Sigma sigma, HashSet<Q> s) {
                return new Targets<Q, Sigma>() {
                    HashSet<Q> states = s;

                    @Override
                    public boolean contains(Q to) {
                        if (states == null)
                            return false;
                        return states.contains(to);
                    }

                    @Override
                    public boolean remove(Q to) {
                        if (states == null)
                            return false;

                        if (states.remove(to)) {
                            HM.removeAndCheck(reverseDelta.get(to), sigma, from);
                            HM.removeAndCheck(labels, pairing.f(from, to), sigma);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean add(Q to) {
                        if (states == null) {
                            if (outgoing == null) {
                                delta.put(from, outgoing = new HashMap<>());
                            }
                            outgoing.put(sigma, states = new HashSet<>());
                        }
                        if (states.add(to)) {
                            HM.appendAndCheck(reverseDelta.get(to), sigma, from);
                            HM.appendAndCheck(labels, pairing.f(from, to), sigma);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public Iterator<Q> iterator() {
                        return states == null ? Collections.emptyIterator() : states.iterator();
                    }

                    @Override
                    public Sigma getSigma() {
                        return sigma;
                    }

                    @Override
                    public Q getSource() {
                        return from;
                    }

                    @Override
                    public void drop() {
                        if (states != null) {
                            HashSet<Q> copy = new HashSet<>(states);
                            for (Q q : copy) {
                                remove(q);
                            }
                        }
                    }

                    @Override
                    public int size() {
                        return states == null ? 0 : states.size();
                    }

                };
            }

            @Override
            public Targets<Q, Sigma> getStates(Sigma sigma) {
                return getStates(sigma, outgoing == null ? null : outgoing.get(sigma));
            }
        };
    }

    @Override
    public boolean drop(Q state) {
        for (MutableNFA.Targets<Q, Sigma> tran : (Iterable<MutableNFA.Targets<Q, Sigma>>) getOutgoing(
                state)::mutIterator) {
            tran.drop();
        }
        initial().remove(state);
        accepting().remove(state);
        return false;
    }
}
