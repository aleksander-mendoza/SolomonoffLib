package net.alagris;

import de.learnlib.algorithms.rpni.BlueFringeEDSMDFA;
import de.learnlib.algorithms.rpni.BlueFringeMDLDFA;
import de.learnlib.algorithms.rpni.BlueFringeRPNIDFA;
import de.learnlib.algorithms.rpni.BlueFringeRPNIMealy;
import de.learnlib.api.algorithm.PassiveLearningAlgorithm;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.commons.util.Pair;
import net.automatalib.graphs.UniversalGraph;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LearnLibCompatibility {


    public static Pair<Alphabet<Integer>,DFA<?,Integer>> rpni(List<Pair<String, String>> informant){
        ArrayList<Pair<IntSeq, Boolean>> text = unicodeText(informant);
        Alphabet<Integer> alph = minNecessaryInputAlphabet(text);
        return Pair.of(alph,addTextSamples(text, new BlueFringeRPNIDFA<>(alph)).computeModel());
    }

    public static Pair<Alphabet<Integer>,DFA<?,Integer>> rpniEDSM(List<Pair<String, String>> informant){
        ArrayList<Pair<IntSeq, Boolean>> text = unicodeText(informant);
        Alphabet<Integer> alph = minNecessaryInputAlphabet(text);
        return Pair.of(alph,addTextSamples(text, new BlueFringeEDSMDFA<>(alph)).computeModel());
    }

    public static Pair<Alphabet<Integer>,DFA<?,Integer>> rpniMDL(List<Pair<String, String>> informant){
        ArrayList<Pair<IntSeq, Boolean>> text = unicodeText(informant);
        Alphabet<Integer> alph = minNecessaryInputAlphabet(text);
        return Pair.of(alph,addTextSamples(text, new BlueFringeMDLDFA<>(alph)).computeModel());
    }

    public static Pair<Alphabet<Integer>,MealyMachine<?, Integer, ?, Integer>> rpniMealy(List<Pair<String, String>> informant){
        ArrayList<Pair<IntSeq, IntSeq>> i = unicodeInformant(informant);
        Alphabet<Integer> alph = minNecessaryInputAlphabet(i);
        return Pair.of(alph,addInformantSamples(i, new BlueFringeRPNIMealy<>(alph)).computeModel());
    }

    public static <L extends PassiveLearningAlgorithm.PassiveDFALearner<Integer>> L addTextSamples(List<Pair<IntSeq, Boolean>> informant, L learner) {
        for(final Pair<IntSeq, Boolean> sample:informant){
            learner.addSample(Word.fromList(sample.getFirst()),sample.getSecond());
        }
        return learner;
    }

    public static ArrayList<Pair<IntSeq, Boolean>> unicodeText(List<Pair<String, String>> informant) {
        ArrayList<Pair<IntSeq, Boolean>> converted = new ArrayList<>(informant.size());
        for(final Pair<String, String> sample:informant){
            converted.add(Pair.of(new IntSeq(sample.getFirst()),sample.getSecond()!=null));
        }
        return converted;
    }

    public static <K> Alphabet<Integer> minNecessaryInputAlphabet(ArrayList<Pair<IntSeq, K>> informant){
        HashSet<Integer> usedSymbols = new HashSet<>();
        informant.forEach(p-> usedSymbols.addAll(p.getFirst()));
        return Alphabets.fromCollection(usedSymbols);
    }

    public static <K> Alphabet<Integer> minNecessaryOutputAlphabet(List<Pair<K, IntSeq>> informant){
        HashSet<Integer> usedSymbols = new HashSet<>();
        informant.forEach(p-> usedSymbols.addAll(p.getSecond()));
        return Alphabets.fromCollection(usedSymbols);
    }

    public static ArrayList<Pair<IntSeq, IntSeq>> unicodeInformant(List<Pair<String, String>> informant) {
        ArrayList<Pair<IntSeq, IntSeq>> converted = new ArrayList<>(informant.size());
        for(final Pair<String, String> sample:informant){
            if(sample.getSecond()==null){
                converted.add(Pair.of(new IntSeq(sample.getFirst()), new IntSeq(0)));
            }else {
                converted.add(Pair.of(new IntSeq(sample.getFirst()), new IntSeq(sample.getSecond())));
            }
        }
        return converted;
    }


    public static <L extends PassiveLearningAlgorithm.PassiveMealyLearner<Integer,Integer>> L addInformantSamples(List<Pair<IntSeq, IntSeq>> informant, L learner) {
        for(final Pair<IntSeq, IntSeq> sample:informant){
            learner.addSample(Word.fromList(sample.getFirst()),Word.fromList(sample.getSecond()));
        }
        return learner;
    }


    public static <S,V, E, P, In, Out, W, N, G extends IntermediateGraph<V, E, P, N>>
    G dfaToIntermediate(Specification<V, E, P, In, Out, W, N, G> spec, Alphabet<In> alph, DFA<S,In> dfa,
                        Function<S,V> stateMeta,Function<In,E> edgeConstructor,
                        Function<S,P> finalEdgeConstructor){
        final UniversalGraph<S, TransitionEdge<In, S>, Boolean, TransitionEdge.Property<In, Void>> tr = dfa.transitionGraphView(alph);
        final G g = spec.createEmptyGraph();
        final HashMap<S,N> stateToNewState = new HashMap<>();
        for(S state:tr.getNodes()) {
            stateToNewState.put(state, g.create(stateMeta.apply(state)));
        }
        for(Map.Entry<S, N> stateAndNewState:stateToNewState.entrySet()){
            final N source = stateAndNewState.getValue();
            for(TransitionEdge<In, S> e:tr.outgoingEdges(stateAndNewState.getKey()) ){
                final N target = stateToNewState.get(tr.getTarget(e));
                g.add(source,edgeConstructor.apply(e.getInput()),target);
            }
            if(dfa.isAccepting(stateAndNewState.getKey())){
                g.setFinalEdge(source,finalEdgeConstructor.apply(stateAndNewState.getKey()));
            }
        }
        final N init = stateToNewState.get(dfa.getInitialState());
        for(TransitionEdge<In, S> e:tr.outgoingEdges(dfa.getInitialState())){
            g.addInitialEdge(init,edgeConstructor.apply(e.getInput()));
        }

        return g;
    }


    public static <S,T,V, E, P, In,O, Out, W, N, G extends IntermediateGraph<V, E, P, N>>
    G mealyToIntermediate(Specification<V, E, P, In, Out, W, N, G> spec, Alphabet<In> alph, MealyMachine<S, In, T, O> mealy,
                          Function<S,V> stateMeta, BiFunction<In,O,E> edgeConstructor,
                          Function<S,P> finalEdgeConstructor){
        final UniversalGraph<S, TransitionEdge<In, T>, Void, TransitionEdge.Property<In, O>> tr = mealy.transitionGraphView(alph);
        final G g = spec.createEmptyGraph();
        final HashMap<S,N> stateToNewState = new HashMap<>();
        for(S state:tr.getNodes()) {
            stateToNewState.put(state, g.create(stateMeta.apply(state)));
        }
        for(Map.Entry<S, N> stateAndNewState:stateToNewState.entrySet()){
            final N source = stateAndNewState.getValue();
            for(TransitionEdge<In, T> e:tr.outgoingEdges(stateAndNewState.getKey()) ){
                final N target = stateToNewState.get(tr.getTarget(e));
                g.add(source,edgeConstructor.apply(e.getInput(),tr.getEdgeProperty(e).getProperty()),target);
            }
            g.setFinalEdge(source,finalEdgeConstructor.apply(stateAndNewState.getKey()));
        }
        final N init = stateToNewState.get(mealy.getInitialState());
        for(TransitionEdge<In, T> e:tr.outgoingEdges(mealy.getInitialState())){
            g.addInitialEdge(init,edgeConstructor.apply(e.getInput(),tr.getEdgeProperty(e).getProperty()));
        }

        return g;
    }

}


