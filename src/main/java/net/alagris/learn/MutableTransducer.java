package net.alagris.learn;

import java.util.Iterator;

public interface MutableTransducer<Q, In, Out>
        extends Transducer<Q, In, Out>, MutableNFA<Q, __.P2<In, Out>>, NFA<Q, __.P2<In, Out>> {

    public interface Labels<Q, In, Out> extends Transducer.Labels<Q, In, Out>, MutableNFA.Labels<Q, __.P2<In, Out>> {
        boolean contains(__.P2<In, Out> q);

        boolean containsInput(In in);

        Q getTarget();

        Q getSource();

        /**
         * Returns true if successfully removed any output associated with given input.
         * If no output was previously present then returns false
         */
        boolean removeForInput(In in);

        boolean remove(__.P2<In, Out> sigma);

        boolean add(__.P2<In, Out> q);
    }

    public interface TargetsWithOutput<Q, In, Out>
            extends Transducer.TargetsWithOutput<Q, In, Out>{
        In getSigma();
        
        boolean remove(Q q);

        /** Removes all states */
        void drop();

        boolean addOutput(Q q, Out out);

        boolean removeOutput(Q q, Out out);

        boolean setSingleOutput(Q q, Out out);
        
        Q getSource();
    }

    public interface Transitions<Q, In, Out>
            extends Transducer.Transitions<Q, In, Out>, MutableNFA.Transitions<Q, __.P2<In, Out>> {
        MutableTransducer.Labels<Q, In, Out> getLabels(Q to);

        MutableNFA.Targets<Q, __.P2<In, Out>> getStates(__.P2<In, Out> sigma);

        MutableTransducer.TargetsWithOutput<Q, In, Out> getStatesForInput(In in);

        Iterator<MutableNFA.Targets<Q, __.P2<In, Out>>> mutIterator();

    }

    MutableTransducer.Transitions<Q, In, Out> getOutgoing(Q from);

    MutableTransducer.Transitions<Q, In, Out> getIncoming(Q to);

    boolean drop(Q state);

    MutableNFA.States<Q> accepting();

    MutableNFA.States<Q> initial();

}
