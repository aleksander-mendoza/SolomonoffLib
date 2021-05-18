package net.alagris.core.learn;

import static net.alagris.core.learn.OSTIA.*;

import net.alagris.core.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Queue;
import java.util.*;

public class OSTIAWithDomain {


    static class Product {
        final ArrayList<Integer>[] sparseMatrix;
        final Stack<Pair.IntPair> pairsToVisit = new Stack<>();
        final ArrayList<State> states;

        <E> Product(State transducer, IntEmbedding alph,
                    Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                    Specification.RangedGraph<?, Integer, E, ?> domain) {
            states = OSTIAState.indexAllStates(transducer, (i, s) -> s.index = i);
            sparseMatrix = new ArrayList[states.size()];
            for (int i = 0; i < sparseMatrix.length; i++) sparseMatrix[i] = new ArrayList<>();
            pairsToVisit.push(Pair.of(transducer.index, domain.initial));
            sparseMatrix[transducer.index].add(domain.initial);
            while (!pairsToVisit.isEmpty()) {
                final Pair.IntPair idx = pairsToVisit.pop();
                final State transducerState = states.get(idx.l);
                final int domainState = idx.r;
                if (transducerState.getKind() == OSTIAState.Kind.ACCEPTING && !domain.isAccepting(domainState)) {
                    throw new IllegalStateException("Training data does not conform to provided domain");
                }
                final List<Specification.Range<Integer, List<Specification.RangedGraph.Trans<E>>>> tr = specs.getTransOrSink(domain,domainState);
                for (int i = 0; i < transducerState.transitionCount(); i++) {
                    if (transducerState.transition(i) != null) {
                        final int transducerTargetState = transducerState.transition(i).target.index;
                        final int symbol = alph.retrieve(i);
                        final int domainTargetState = specs.deltaBinarySearchDeterministicTarget(tr, symbol);
                        if (!sparseMatrix[transducerTargetState].contains(domainTargetState)) {
                            sparseMatrix[transducerTargetState].add(domainTargetState);
                            pairsToVisit.push(Pair.of(transducerTargetState, domainTargetState));
                        }
                    }
                }
            }
        }


        boolean mockPushIfAbsent(int transducerState, int domainState, ArrayList<Integer>[] mockSparseMatrix) {
            assert mockSparseMatrix[transducerState]==null||Collections.disjoint(sparseMatrix[transducerState],mockSparseMatrix[transducerState]):sparseMatrix[transducerState]+" "+mockSparseMatrix[transducerState];
            if (sparseMatrix[transducerState].contains(domainState)) {
                return false;
            }
            ArrayList<Integer> a = mockSparseMatrix[transducerState];
            if (a == null) {
                a = mockSparseMatrix[transducerState] = new ArrayList<>();
            } else if (a.contains(domainState)) {
                return false;
            }
            a.add(domainState);
            pairsToVisit.push(Pair.of(transducerState, domainState));
            assert Collections.disjoint(sparseMatrix[transducerState],mockSparseMatrix[transducerState]):sparseMatrix[transducerState]+" "+mockSparseMatrix[transducerState];
            return true;
        }

        <E> boolean product(IntEmbedding alph, State redState,
                            State blueStateParent,
                            int transitionIncomingToBlueState,
                            Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                            Specification.RangedGraph<?, Integer, E, ?> domain,
                            ArrayList<Integer>[] mockSparseMatrix) {
            while (!pairsToVisit.isEmpty()) {
                final Pair.IntPair idx = pairsToVisit.pop();
                final State transducerState = states.get(idx.l);
                final int domainState = idx.r;
                if (transducerState.getKind() == OSTIAState.Kind.ACCEPTING && !domain.isAccepting(domainState)) {
                    pairsToVisit.clear();
                    assert pairsToVisit.isEmpty();
                    return false;
                }
                final List<Specification.Range<Integer, List<Specification.RangedGraph.Trans<E>>>> tr = specs.getTransOrSink(domain,domainState);

                if(transducerState==blueStateParent){
                    for (int i = 0; i < transducerState.transitionCount(); i++) {
                        if (transducerState.transition(i) != null) {
                            final int transducerTargetState = (i==transitionIncomingToBlueState?redState: transducerState.transition(i).target).index;
                            final int symbol = alph.retrieve(i);
                            final int domainTargetState = specs.deltaBinarySearchDeterministicTarget(tr, symbol);
                            mockPushIfAbsent(transducerTargetState, domainTargetState, mockSparseMatrix);
                        }
                    }
                }else {
                    for (int i = 0; i < transducerState.transitionCount(); i++) {
                        if (transducerState.transition(i) != null) {
                            final int transducerTargetState = transducerState.transition(i).target.index;
                            final int symbol = alph.retrieve(i);
                            final int domainTargetState = specs.deltaBinarySearchDeterministicTarget(tr, symbol);
                            mockPushIfAbsent(transducerTargetState, domainTargetState, mockSparseMatrix);
                        }
                    }
                }
            }
            assert pairsToVisit.isEmpty();
            return true;
        }


        public <E> ArrayList<Integer>[] merge(State redState, State blueStateParent,
                                              int transitionIncomingToBlueState,
                                              State blueState,
                                              IntEmbedding alph,
                                              Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                                              Specification.RangedGraph<?, Integer, E, ?> domain) {
            assert pairsToVisit.isEmpty();
            assert blueStateParent.transition(transitionIncomingToBlueState).target==blueState;
            final ArrayList<Integer>[] mockSparseMatrix = new ArrayList[sparseMatrix.length];
            final Stack<Pair<State, State>> merges = new Stack<>();
            merges.push(Pair.of(redState, blueState));
            for (int domainStateBlue : sparseMatrix[blueState.index]) {
                mockPushIfAbsent(redState.index, domainStateBlue, mockSparseMatrix);
            }
            while (!merges.isEmpty()) {
                final Pair<State, State> p = merges.pop();
                final State red = p.l();
                final State blue = p.r();
                for (int domainStateRed : sparseMatrix[red.index]) {
                    mockPushIfAbsent(blue.index, domainStateRed, mockSparseMatrix);
                }
                assert blue!=blueStateParent;//no cycles among blue states
                if(red==blueStateParent){
                    for (int i = 0; i < red.transitionCount(); i++) {
                        final Edge redE = red.transitions[i];
                        final Edge blueE = blue.transitions[i];
                        if (redE != null && blueE != null) {
                            merges.push(Pair.of(i==transitionIncomingToBlueState?redState:redE.target, blueE.target));
                        }
                    }
                }else {
                    for (int i = 0; i < red.transitionCount(); i++) {
                        final Edge redE = red.transitions[i];
                        final Edge blueE = blue.transitions[i];
                        if (redE != null && blueE != null) {
                            merges.push(Pair.of(redE.target, blueE.target));
                        }
                    }
                }
            }
            if (product(alph,redState,blueStateParent,transitionIncomingToBlueState, specs, domain, mockSparseMatrix)) {
                assert pairsToVisit.isEmpty();
                return mockSparseMatrix;
            }
            assert pairsToVisit.isEmpty();
            return null;
        }

        public void apply(ArrayList<Integer>[] mockSparseMatrix, Map<State, StateCopy> merged) {
            for (State redState : merged.keySet()) {
                final ArrayList<Integer> newlyReachableDomainStates = mockSparseMatrix[redState.index];
                if(newlyReachableDomainStates!=null) {
                    assert Collections.disjoint(sparseMatrix[redState.index], newlyReachableDomainStates) : sparseMatrix[redState.index] + " " + newlyReachableDomainStates + " " + redState;
                    sparseMatrix[redState.index].addAll(newlyReachableDomainStates);
                }
            }
        }
    }

    public static <E> void ostiaWithDomain(State transducer,
                                           IntEmbedding alph,
                                           Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                                           Specification.RangedGraph<?, Integer, E, ?> domain) {

        final Queue<Blue> blue = new LinkedList<>();
        final Set<State> red = new LinkedHashSet<>();
        assert OSTIAState.isTree(transducer);
        red.add(transducer);
        OSTIA.addBlueStates(transducer, blue);
        assert uniqueItems(blue);
        assert disjoint(blue, red);
        assert validateBlueAndRed(transducer, red, blue);
        assert domain.isDeterministic() == null;
        final Product domainProductTransducer = new Product(transducer, alph, specs, domain);

        blue:
        while (!blue.isEmpty()) {
            final @NonNull Blue next = blue.poll();
            final @Nullable State blueState = next.state();
            assert blueState != null;
            assert OSTIAState.isTree(blueState);
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);

            for (State redState : red) {
                final ArrayList<Integer>[] mockSparseMatrix = domainProductTransducer.merge(redState,next.parent,next.symbol, blueState, alph, specs, domain);
                if (mockSparseMatrix != null) {
                    final Map<State, StateCopy> merged = ostiaMerge(next, redState, blue, red);
                    if (merged != null) {
                        domainProductTransducer.apply(mockSparseMatrix, merged);
                        assert disjoint(blue, red);
                        assert uniqueItems(blue);
                        continue blue;
                    }
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


}