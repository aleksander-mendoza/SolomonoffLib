package net.alagris.learn;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import net.alagris.learn.NFA.States;
import net.alagris.learn.__.F2;
import net.alagris.learn.__.F3;
import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;
import net.alagris.learn.__.Iff;
import net.alagris.learn.__.P2;
import net.alagris.learn.__.S;
import net.alagris.learn.__.U;
import net.alagris.learn.__.V2;
import net.alagris.learn.__.W;

public interface NFA<Q, Sigma> extends Iterable<Q> {

    public interface States<Q> extends Iterable<Q> {
        boolean contains(Q q);

        int size();
        
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
                public int size() {
                    return set.size();
                }

            };
        }

    }

    public interface Targets<Q, Sigma> extends States<Q> {

        Sigma getSigma();

        Q getSource();

    }

    public interface Labels<Q, Sigma> extends Iterable<Sigma> {
        boolean contains(Sigma sigma);

        Q getTarget();

        Q getSource();
    }

    public interface Transitions<Q, Sigma> extends Iterable<Targets<Q, Sigma>> {
        Labels<Q, Sigma> getLabels(Q to);

        Targets<Q, Sigma> getStates(Sigma sigma);
    }

    Transitions<Q, Sigma> getOutgoing(Q from);

    Transitions<Q, Sigma> getIncoming(Q to);

    States<Q> accepting();

    States<Q> initial();

    

    
    

}