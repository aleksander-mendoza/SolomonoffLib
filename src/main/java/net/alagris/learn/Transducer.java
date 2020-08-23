package net.alagris.learn;

import java.util.Iterator;

public interface Transducer<Q,In,Out> extends NFA<Q, __.P2<In, Out>>{

    public interface Labels<Q, In, Out> extends NFA.Labels<Q, __.P2<In, Out>>, Iterable<__.P2<In, Out>> {
        boolean contains(__.P2<In, Out> sigma);
        
        boolean containsInput(In in);
        
        boolean containsOutput(Out out);
        
        Iterator<Out> outputIterator(In in);
        
        Q getTarget();

        Q getSource();
    }

    
    public interface TargetsWithOutput<Q, In,Out> extends Iterable<Q> {
        In getSigma();
        
        Iterator<Out> outputIterator(Q q);
        
        boolean hasOutput(Q q,Out out);
        
        Q getSource();
        
        boolean contains(Q q);
        
        int size();
    }
    
    public interface Transitions<Q, In, Out> extends NFA.Transitions<Q, __.P2<In, Out>>,Iterable<NFA.Targets<Q, __.P2<In, Out>>> {
        Transducer.Labels<Q, In, Out> getLabels(Q to);

        NFA.Targets<Q, __.P2<In, Out>> getStates(__.P2<In, Out> sigma);
        
        Transducer.TargetsWithOutput<Q, In,Out> getStatesForInput(In input);
        
    }

    Transitions<Q, In, Out> getOutgoing(Q from);

    Transitions<Q, In, Out> getIncoming(Q to);

    NFA.States<Q> accepting();

    NFA.States<Q> initial();
}
