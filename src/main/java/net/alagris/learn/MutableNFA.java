package net.alagris.learn;

import java.util.HashSet;
import java.util.Iterator;

import net.alagris.learn.NFA.Targets;

public interface MutableNFA<Q, Sigma> extends NFA<Q, Sigma> {

    public interface States<Q> extends NFA.States<Q> {
        boolean contains(Q q);

        int size();

        boolean remove(Q q);

        boolean add(Q q);

        /** Removes all states */
        void drop();

        public static <Q> States<Q> fromSet(HashSet<Q> set) {
            return new States<Q>() {

                @Override
                public Iterator<Q> iterator() {
                    return set.iterator();
                }

                @Override
                public boolean contains(Q q) {
                    return set.contains(q);
                }

                @Override
                public boolean remove(Q q) {
                    return set.remove(q);
                }

                @Override
                public boolean add(Q q) {
                    return set.add(q);
                }

                @Override
                public void drop() {
                    set.clear();
                }

                @Override
                public int size() {
                    return set.size();
                }

            };
        }
    }

    public interface Targets<Q, Sigma> extends MutableNFA.States<Q>, NFA.Targets<Q, Sigma> {
        Sigma getSigma();

        Q getSource();
    }

    public interface Labels<Q, Sigma> extends NFA.Labels<Q, Sigma> {
        boolean contains(Sigma q);

        Q getTarget();

        Q getSource();

        boolean remove(Sigma q);

        boolean add(Sigma q);
    }

    public interface Transitions<Q, Sigma> extends NFA.Transitions<Q, Sigma> {
        MutableNFA.Labels<Q, Sigma> getLabels(Q to);

        MutableNFA.Targets<Q, Sigma> getStates(Sigma sigma);
        
        Iterator<MutableNFA.Targets<Q, Sigma>> mutIterator();
    }

    MutableNFA.Transitions<Q, Sigma> getOutgoing(Q from);

    MutableNFA.Transitions<Q, Sigma> getIncoming(Q to);

    boolean drop(Q state);

    MutableNFA.States<Q> accepting();

    MutableNFA.States<Q> initial();

}
