package net.alagris.learn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jgrapht.graph.DirectedPseudograph;

import net.alagris.learn.__.HM;
import net.alagris.learn.__.I;

public class GraphNFA<Q, Sigma> extends DirectedPseudograph<Q, GraphNFA.Edge<Q, Sigma>>
        implements MutableNFA<Q, Sigma> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GraphNFA() {
        super(null, null, false);
    }

    private final HashSet<Q> accepting = new HashSet<>();
    private final HashSet<Q> initial = new HashSet<>();

    public static class Edge<Q, Sigma> {
        final @NonNull Q from, to;
        final @NonNull Sigma sigma;

        public Edge(Q from, Q to, Sigma sigma) {
            this.from = from;
            this.to = to;
            this.sigma = sigma;
        }

        @Override
        public int hashCode() {
            return from.hashCode() + 21 * to.hashCode() + 91 * sigma.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            @SuppressWarnings("unchecked")
            Edge<Q, Sigma> o = (Edge<Q, Sigma>) obj;
            return o.from.equals(from) && o.to.equals(to) && o.sigma.equals(sigma);
        }

        @Override
        public String toString() {
            return sigma.toString();
        }

    }

    public boolean containsEdge(Q from, Sigma sigma, Q to) {
        return super.containsEdge(new Edge<>(from, to, sigma));
    }

    public boolean removeEdge(Q from, Sigma sigma, Q to) {
        return super.removeEdge(new Edge<>(from, to, sigma));
    }

    public Set<Edge<Q, Sigma>> incomingEdgesTo(Q to) {
        return super.incomingEdgesOf(to);
    }

    public Set<Edge<Q, Sigma>> outgoingEdgesFrom(Q from) {
        return super.outgoingEdgesOf(from);
    }

    public I<Edge<Q, Sigma>> incomingEdgesTo(Q to, Sigma sigma) {
        return __.I.filter(__.iter(incomingEdgesTo(to)), e -> e.sigma.equals(sigma));
    }

    public I<Edge<Q, Sigma>> outgoingEdgesFrom(Q to, Sigma sigma) {
        return __.I.filter(__.iter(outgoingEdgesFrom(to)), e -> e.sigma.equals(sigma));
    }

    public Set<Edge<Q, Sigma>> incomingEdgeSetTo(Q to, Sigma sigma) {
        return __.filterInPlace(incomingEdgesTo(to), e -> !e.sigma.equals(sigma));
    }

    public Set<Edge<Q, Sigma>> outgoingEdgeSetFrom(Q from, Sigma sigma) {
        return __.filterInPlace(outgoingEdgesFrom(from), e -> !e.sigma.equals(sigma));
    }

    public Targets<Q, Sigma> incomingTargets(Q to, Sigma sigma) {
        return new Targets<Q, Sigma>() {

            @Override
            public boolean contains(Q from) {
                return containsEdge(from, sigma, to);
            }

            @Override
            public boolean remove(Q from) {
                return removeEdge(from, sigma, to);
            }

            @Override
            public boolean add(Q from) {
                return addTransition(from, sigma, to);
            }

            @Override
            public void drop() {
                __.I.foreach(incomingEdgesTo(to, sigma), incoming -> removeEdge(incoming));
            }

            @Override
            public int size() {
                return __.I.count(incomingEdgesTo(to, sigma));
            }

            @Override
            public Iterator<Q> iterator() {
                return __.I.worse(__.I.map(incomingEdgesTo(to, sigma), e -> e.from));
            }

            @Override
            public Sigma getSigma() {
                return sigma;
            }

            @Override
            public Q getSource() {
                return to;
            }
        };
    }

    public Labels<Q, Sigma> getLabels(Q from, Q to) {
        return new Labels<Q, Sigma>() {
            @Override
            public Iterator<Sigma> iterator() {
                return __.I.map(getAllEdges(from, to).iterator(), e -> e.sigma);
            }

            @Override
            public boolean contains(Sigma sigma) {
                return containsEdge(from, sigma, to);
            }

            @Override
            public boolean remove(Sigma sigma) {
                return removeEdge(from, sigma, to);
            }

            @Override
            public boolean add(Sigma sigma) {
                return addTransition(from, sigma, to);
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

    public Targets<Q, Sigma> outgoingTargets(Q from, Sigma sigma) {
        return new Targets<Q, Sigma>() {

            @Override
            public boolean contains(Q to) {
                return containsEdge(from, sigma, to);
            }

            @Override
            public boolean remove(Q to) {
                return removeEdge(from, sigma, to);
            }

            @Override
            public boolean add(Q to) {
                return addTransition(from, sigma, to);
            }

            @Override
            public void drop() {
                __.I.foreach(outgoingEdgesFrom(from, sigma), incoming -> removeEdge(incoming));
            }

            @Override
            public int size() {
                return __.I.count(outgoingEdgesFrom(from, sigma));
            }

            @Override
            public Iterator<Q> iterator() {
                return __.I.worse(__.I.map(outgoingEdgesFrom(from, sigma), e -> e.to));
            }

            @Override
            public Sigma getSigma() {
                return sigma;
            }

            @Override
            public Q getSource() {
                return from;
            }
        };
    }

    public Edge<Q, Sigma> remove(Q from, Sigma sigma, Q to) {
        Edge<Q, Sigma> e = new Edge<>(from, to, sigma);
        removeEdge(e);
        return e;
    }

    public Edge<Q, Sigma> putTransition(Q from, Sigma sigma, Q to) {
        Edge<Q, Sigma> e = new Edge<>(from, to, sigma);
        addEdge(from, to, e);
        return e;
    }

    public boolean addTransition(Q from, Sigma sigma, Q to) {
        Edge<Q, Sigma> e = new Edge<>(from, to, sigma);
        return addEdge(from, to, e);
    }

    @Override
    public Iterator<Q> iterator() {
        return vertexSet().iterator();
    }

    public HashMap<Sigma, HashSet<Edge<Q, Sigma>>> groupBySigma(Set<Edge<Q, Sigma>> edges) {
        HashMap<Sigma, HashSet<Edge<Q, Sigma>>> sigmaToState = new HashMap<>();
        for (Edge<Q, Sigma> edge : edges) {
            HM.append(sigmaToState, edge.sigma, edge);
        }
        return sigmaToState;
    }

    public HashSet<Sigma> collectSigma(Set<Edge<Q, Sigma>> edges) {
        HashSet<Sigma> sigmas = new HashSet<>();
        for (Edge<Q, Sigma> edge : edges) {
            sigmas.add(edge.sigma);
        }
        return sigmas;
    }

    @Override
    public MutableNFA.Transitions<Q, Sigma> getOutgoing(Q from) {
        return new Transitions<Q, Sigma>() {
            @Override
            public Iterator<Targets<Q, Sigma>> mutIterator() {
                return __.I.map(collectSigma(outgoingEdgesFrom(from)).iterator(), this::getStates);
            }

            @Override
            public Iterator<NFA.Targets<Q, Sigma>> iterator() {
                return __.I.map(collectSigma(outgoingEdgesFrom(from)).iterator(), this::getStates);
            }

            @Override
            public Labels<Q, Sigma> getLabels(Q to) {
                return GraphNFA.this.getLabels(from, to);
            }

            @Override
            public Targets<Q, Sigma> getStates(Sigma sigma) {
                return outgoingTargets(from, sigma);
            }
        };
    }

    @Override
    public MutableNFA.Transitions<Q, Sigma> getIncoming(Q to) {
        return new Transitions<Q, Sigma>() {
            @Override
            public Iterator<Targets<Q, Sigma>> mutIterator() {
                return __.I.map(collectSigma(incomingEdgesTo(to)).iterator(), this::getStates);
            }

            @Override
            public Iterator<NFA.Targets<Q, Sigma>> iterator() {
                return __.I.map(collectSigma(incomingEdgesTo(to)).iterator(), this::getStates);
            }

            @Override
            public Labels<Q, Sigma> getLabels(Q from) {
                return GraphNFA.this.getLabels(from, to);
            }

            @Override
            public Targets<Q, Sigma> getStates(Sigma sigma) {
                return incomingTargets(to, sigma);
            }
        };
    }

    @Override
    public boolean drop(Q q) {
        accepting.remove(q);
        initial.remove(q);
        return removeVertex(q);
    }

    @Override
    public MutableNFA.States<Q> accepting() {
        return MutableNFA.States.fromSet(accepting);
    }

    @Override
    public MutableNFA.States<Q> initial() {
        return MutableNFA.States.fromSet(initial);
    }

    public boolean addState(Q q) {
        return addVertex(q);
    }

}
