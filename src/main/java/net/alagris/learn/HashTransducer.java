package net.alagris.learn;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.google.common.base.Objects;

import net.alagris.learn.__.F2;
import net.alagris.learn.__.F3;
import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;
import net.alagris.learn.__.P2;

public class HashTransducer<Q, In, Out> implements MutableTransducer<Q, In, Out> {
    private final HashSet<Q> accepting = new HashSet<>();
    private final HashSet<Q> initial = new HashSet<>();
    private final HashMap<P2<Q, Q>, HashMap<In, Out>> labels = new HashMap<>();
    private final HashMap<Q, HashMap<In, HashMap<Q, Out>>> delta = new HashMap<>();
    private final HashMap<Q, HashMap<In, HashMap<Q, Out>>> reverseDelta = new HashMap<>();

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
    public MutableTransducer.Transitions<Q, In, Out> getIncoming(Q from) {
        return get(reverseDelta, delta, labels, (q1, q2) -> P2.of(q2, q1), P2::b, P2::a, from);
    }

    @Override
    public MutableTransducer.Transitions<Q, In, Out> getOutgoing(Q from) {
        return get(delta, reverseDelta, labels, P2::of, P2::a, P2::b, from);
    }

    private <P> MutableTransducer.Transitions<Q, In, Out> get(HashMap<Q, HashMap<In, HashMap<Q, Out>>> delta,
            HashMap<Q, HashMap<In, HashMap<Q, Out>>> reverseDelta, HashMap<P, HashMap<In, Out>> labels,
            F3<Q, Q, P> pairing, F2<P, Q> proj1, F2<P, Q> proj2, Q from) {

        return new MutableTransducer.Transitions<Q, In, Out>() {
            HashMap<In, HashMap<Q, Out>> outgoing = delta.get(from);

            public HashMap<P2<In, Out>, HashSet<Q>> group() {
                if (outgoing == null)
                    return null;
                final HashMap<P2<In, Out>, HashSet<Q>> groupped = new HashMap<>();
                for (Entry<In, HashMap<Q, Out>> entry : outgoing.entrySet()) {
                    for (Entry<Q, Out> stateAndOutput : entry.getValue().entrySet()) {
                        HM.append(groupped, P2.of(entry.getKey(), stateAndOutput.getValue()), stateAndOutput.getKey());
                    }
                }
                return groupped;
            }

            @Override
            public Iterator<MutableNFA.Targets<Q, P2<In, Out>>> mutIterator() {
                Map<P2<In, Out>, HashSet<Q>> m = __.coalsece(group(), Collections.emptyMap());
                return I.map(m.entrySet().iterator(), e -> getStates(e.getKey(), e.getValue()));
            }

            @Override
            public Iterator<NFA.Targets<Q, P2<In, Out>>> iterator() {
                Map<P2<In, Out>, HashSet<Q>> m = __.coalsece(group(), Collections.emptyMap());
                return I.map(m.entrySet().iterator(), e -> getStates(e.getKey(), e.getValue()));
            }

            @Override
            public Labels<Q, In, Out> getLabels(Q to) {
                return new Labels<Q, In, Out>() {
                    HashMap<In, Out> labs = labels.get(pairing.f(from, to));

                    @Override
                    public Iterator<P2<In, Out>> iterator() {
                        if (labs == null)
                            return Collections.emptyIterator();
                        return __.I.map(labs.entrySet().iterator(), P2::of);
                    }

                    @Override
                    public boolean contains(P2<In, Out> sigma) {
                        if (labs == null)
                            return false;
                        return Objects.equal(labs.get(sigma.a()), sigma.b());
                    }

                    @Override
                    public boolean remove(P2<In, Out> sigma) {
                        if (labs == null)
                            return false;
                        if (labs.remove(sigma.a(), sigma.b())) {
                            outgoing.get(sigma.a()).remove(to);
                            reverseDelta.get(to).get(sigma.a()).remove(from);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean add(P2<In, Out> sigma) {
                        if (labs == null) {
                            labels.put(pairing.f(from, to), labs = new HashMap<>());
                            if (outgoing == null) {
                                outgoing = delta.computeIfAbsent(from, u -> new HashMap<>());
                            }
                        }
                        Out prevOut = labs.put(sigma.a(), sigma.b());
                        if (prevOut == null) {
                            HM.putNested(outgoing, sigma.a(), to, sigma.b());
                            HM.putNested(reverseDelta.get(to), sigma.a(), from, sigma.b());
                            return true;
                        } else if (prevOut.equals(sigma.b())) {
                            return false;
                        } else {
                            throw new UnsupportedOperationException("Source state, input symbol and target state must "
                                    + "uniquely determined output symbol." + " Tried to add " + sigma.b()
                                    + " but already had " + prevOut);
                        }

                    }

                    @Override
                    public Q getTarget() {
                        return to;
                    }

                    @Override
                    public Q getSource() {
                        return from;
                    }

                    @Override
                    public boolean containsOutput(Out out) {
                        return labs == null ? false : labs.containsValue(out);
                    }

                    @Override
                    public Iterator<Out> outputIterator(In in) {
                        return labs == null ? Collections.emptyIterator() : __.singletonIterator(labs.get(in));
                    }

                    @Override
                    public boolean containsInput(In in) {
                        return labs == null ? false : labs.containsKey(in);
                    }

                    @Override
                    public boolean removeForInput(In in) {
                        return labs == null ? false : null != labs.remove(in);
                    }
                };
            }

            private MutableNFA.Targets<Q, P2<In, Out>> getStates(P2<In, Out> sigma, HashSet<Q> states) {
                return new MutableNFA.Targets<Q, P2<In, Out>>() {

                    @Override
                    public boolean contains(Q to) {
                        return states.contains(to);
                    }

                    @Override
                    public boolean remove(Q to) {
                        if (states.remove(to)) {
                            HM.removeAndCheckKey(delta.get(from), sigma.a(), to);
                            HM.removeAndCheckKey(reverseDelta.get(to), sigma.a(), from);
                            HM.removeAndCheckKey(labels, pairing.f(from, to), sigma.a());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean add(Q to) {
                        if (states.add(to)) {
                            HM.putNested(delta.get(from), sigma.a(), to, sigma.b());
                            HM.putNested(reverseDelta.get(to), sigma.a(), from, sigma.b());
                            HM.putNested(labels, pairing.f(from, to), sigma.a(), sigma.b());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public Iterator<Q> iterator() {
                        return states == null ? Collections.emptyIterator() : states.iterator();
                    }

                    @Override
                    public @NonNull P2<In, Out> getSigma() {
                        return sigma;
                    }

                    @Override
                    public @NonNull Q getSource() {
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
            public MutableNFA.Targets<Q, P2<In, Out>> getStates(P2<In, Out> sigma) {
                HashSet<Q> states = new HashSet<>();
                if (outgoing == null)
                    return getStates(sigma, states);
                for (Entry<Q, Out> e : outgoing.get(sigma.a()).entrySet()) {
                    if (e.getValue().equals(sigma.b()))
                        states.add(e.getKey());
                }
                return getStates(sigma, states);
            }

            @Override
            public TargetsWithOutput<Q, In, Out> getStatesForInput(In in) {
                return new TargetsWithOutput<Q, In, Out>() {
                    HashMap<Q, Out> states = outgoing == null ? null : outgoing.get(in);

                    @Override
                    public Iterator<Out> outputIterator(Q q) {
                        return states == null ? Collections.emptyIterator() : states.values().iterator();
                    }

                    @Override
                    public boolean hasOutput(Q q, Out out) {
                        return states == null ? false : Objects.equal(out, states.get(q));
                    }

                    @Override
                    public boolean contains(Q q) {
                        return states == null ? false : states.containsKey(q);
                    }

                    @Override
                    public int size() {
                        return states == null ? 0 : states.size();
                    }

                    @Override
                    public Iterator<Q> iterator() {
                        return states == null ? Collections.emptyIterator() : states.keySet().iterator();
                    }

                    @Override
                    public boolean remove(Q q) {
                        return states == null ? false : null != states.remove(q);
                    }

                    @Override
                    public void drop() {
                        if (states != null) {
                            HashSet<Q> copy = new HashSet<>(states.keySet());
                            for (Q q : copy) {
                                remove(q);
                            }
                        }
                    }

                    @Override
                    public In getSigma() {
                        return in;
                    }

                    @Override
                    public boolean addOutput(Q to, Out out) {
                        if (states == null) {
                            if (outgoing == null) {
                                delta.put(from, outgoing = new HashMap<>());
                            }
                            outgoing.put(in, states = new HashMap<>());
                        }
                        Out prev = states.putIfAbsent(to, out);
                        if (prev == null) {
                            HM.putNested(reverseDelta.get(to), in, from, out);
                            HM.putNested(labels, pairing.f(from, to), in, out);
                            return true;
                        }else if(prev.equals(out)) {
                            return false;    
                        }else {
                            throw new UnsupportedOperationException("Source state, input symbol and target state must "
                                    + "uniquely determined output symbol." + " Tried to add " + in
                                    + " but already had " + prev);
                        }
                        
                    }

                    @Override
                    public boolean removeOutput(Q q, Out out) {
                        return states==null?false:states.remove(q, out);
                    }

                    @Override
                    public boolean setSingleOutput(Q to, Out out) {
                        if (states == null) {
                            if (outgoing == null) {
                                delta.put(from, outgoing = new HashMap<>());
                            }
                            outgoing.put(in, states = new HashMap<>());
                        }
                        Out prev = states.put(to, out);
                        if(prev.equals(out)) {
                            return false;    
                        }else {
                            HM.putNested(reverseDelta.get(to), in, from, out);
                            HM.putNested(labels, pairing.f(from, to), in, out);
                            return true;
                        
                        }
                    }

                    @Override
                    public Q getSource() {
                        return from;
                    }

                };
            }

        };
    }

    @Override
    public boolean drop(Q state) {
        for (MutableNFA.Targets<Q, P2<In, Out>> tran : (Iterable<MutableNFA.Targets<Q, P2<In, Out>>>) getOutgoing(
                state)::mutIterator) {
            tran.drop();
        }
        initial().remove(state);
        accepting().remove(state);
        return false;
    }
}
