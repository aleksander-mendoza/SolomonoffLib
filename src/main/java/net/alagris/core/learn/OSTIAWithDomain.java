package net.alagris.core.learn;

import static net.alagris.core.learn.OSTIA.*;

import net.alagris.core.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Queue;
import java.util.*;

public class OSTIAWithDomain {

//    public static int col(int idx,Specification.RangedGraph<?, Integer,?, ?> domain){
//        return MatrixIndexing.rectMatrixCol(idx,domain.size()+1)-1;
//    }
//    public static int row(int idx,Specification.RangedGraph<?, Integer,?, ?> domain){
//        return MatrixIndexing.rectMatrixCol(idx,domain.size()+1);
//    }
//    public static int idx(int row,int col,Specification.RangedGraph<?, Integer,?, ?> domain){
//        return MatrixIndexing.rectMatrixIndex(row,col+1/*sink state*/,domain.size()+1);
//    }

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
            while (!pairsToVisit.isEmpty()) {
                final Pair.IntPair idx = pairsToVisit.pop();
                final State s0 = states.get(idx.l);
                final int s1 = idx.r;
                if (s0.getKind() == OSTIAState.Kind.ACCEPTING && !domain.isAccepting(s1)) {
                    throw new IllegalStateException("Training data does not conform to provided domain");
                }
                final ArrayList<Specification.Range<Integer, List<Specification.RangedGraph.Trans<E>>>> tr = domain.graph.get(s1);
                for (int i = 0; i < s0.transitionCount(); i++) {
                    if (s0.transition(i) != null) {
                        final int target0 = s0.transition(i).target.index;
                        final int symbol = alph.retrieve(i);
                        final int target1 = specs.deltaBinarySearchDeterministicTarget(tr, symbol);
                        if (!sparseMatrix[target0].contains(target1)) {
                            sparseMatrix[target0].add(target1);
                            pairsToVisit.push(Pair.of(target0, target1));
                        }
                    }
                }
            }
        }


        boolean mockPushIfAbsent(int transducerState, int domainState, ArrayList<Integer>[] mockSparseMatrix) {
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
            return true;
        }

        <E> boolean product(IntEmbedding alph,
                            Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                            Specification.RangedGraph<?, Integer, E, ?> domain,
                            ArrayList<Integer>[] mockSparseMatrix) {
            while (!pairsToVisit.isEmpty()) {
                final Pair.IntPair idx = pairsToVisit.pop();
                final State s0 = states.get(idx.l);
                final int s1 = idx.r;
                if (s0.getKind() == OSTIAState.Kind.ACCEPTING && !domain.isAccepting(s1)) {
                    return false;
                }
                final ArrayList<Specification.Range<Integer, List<Specification.RangedGraph.Trans<E>>>> tr = domain.graph.get(s1);
                for (int i = 0; i < s0.transitionCount(); i++) {
                    if (s0.transition(i) != null) {
                        final int target0 = s0.transition(i).target.index;
                        final int symbol = alph.retrieve(i);
                        final int target1 = specs.deltaBinarySearchDeterministicTarget(tr, symbol);
                        mockPushIfAbsent(target0, target1, mockSparseMatrix);
                    }
                }
            }
            return true;
        }


        public <E> ArrayList<Integer>[] merge(State redState, State blueState,
                                              IntEmbedding alph,
                                              Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                                              Specification.RangedGraph<?, Integer, E, ?> domain) {
            assert pairsToVisit.isEmpty();
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
                for (int i = 0; i < red.transitionCount(); i++) {
                    final Edge redE = red.transitions[i];
                    final Edge blueE = blue.transitions[i];
                    if (redE != null && blueE != null) {
                        merges.push(Pair.of(redE.target, blueE.target));
                    }
                }
            }
            if (product(alph, specs, domain, sparseMatrix)) {
                return sparseMatrix;
            }
            return null;
        }

        public void apply(ArrayList<Integer>[] mockSparseMatrix, Map<State, StateCopy> merged) {
            for (State redState : merged.keySet()) {
                assert Collections.disjoint(sparseMatrix[redState.index],mockSparseMatrix[redState.index]);
                sparseMatrix[redState.index].addAll(mockSparseMatrix[redState.index]);
            }
        }
    }

    public static <E> void ostiaWithDomain(State transducer,
                                           IntEmbedding alph,
                                           Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                                           Specification.RangedGraph<?, Integer, E, ?> domain) {

        final Queue<Blue> blue = new LinkedList<>();
        final Set<State> red = new LinkedHashSet<>();
        assert isTree(transducer, new HashSet<>());
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
            assert isTree(blueState, new HashSet<>());
            assert uniqueItems(blue);
            assert !contains(blue, blueState);
            assert disjoint(blue, red);

            for (State redState : red) {
                final ArrayList<Integer>[] mockSparseMatrix = domainProductTransducer.merge(redState, blueState, alph, specs, domain);
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
            assert isTree(blueState, new HashSet<>());
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