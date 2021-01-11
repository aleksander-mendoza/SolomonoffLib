package net.alagris;

import de.learnlib.algorithms.rpni.BlueFringeEDSMDFA;
import de.learnlib.algorithms.rpni.BlueFringeMDLDFA;
import de.learnlib.algorithms.rpni.BlueFringeRPNIDFA;
import de.learnlib.algorithms.rpni.BlueFringeRPNIMealy;
import de.learnlib.api.algorithm.PassiveLearningAlgorithm;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.UniversalGraph;
import net.automatalib.visualization.DefaultVisualizationHelper;
import net.automatalib.visualization.Visualization;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LearnLibCompatibility {

	public static Pair<Alphabet<Integer>, DFA<?, Integer>> rpni(List<Pair<IntSeq, IntSeq>> informant) {
		Alphabet<Integer> alph = minNecessaryInputAlphabet(informant);
		return Pair.of(alph, addTextSamples(informant, new BlueFringeRPNIDFA<>(alph)).computeModel());
	}

	public static Pair<Alphabet<Integer>, DFA<?, Integer>> rpniEDSM(List<Pair<IntSeq, IntSeq>> informant) {
		Alphabet<Integer> alph = minNecessaryInputAlphabet(informant);
		return Pair.of(alph, addTextSamples(informant, new BlueFringeEDSMDFA<>(alph)).computeModel());
	}

	public static Pair<Alphabet<Integer>, DFA<?, Integer>> rpniMDL(List<Pair<IntSeq, IntSeq>> informant) {
		Alphabet<Integer> alph = minNecessaryInputAlphabet(informant);
		return Pair.of(alph, addTextSamples(informant, new BlueFringeMDLDFA<>(alph)).computeModel());
	}

	public static Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> rpniMealy(
			List<Pair<IntSeq, IntSeq>> informant) {
		ArrayList<Pair<IntSeq, IntSeq>> i = unicodeInformant(informant);
		Alphabet<Integer> alph = minNecessaryInputAlphabet(i);
		return Pair.of(alph, addInformantSamples(i, new BlueFringeRPNIMealy<>(alph)).computeModel());
	}

	public static <L extends PassiveLearningAlgorithm.PassiveDFALearner<Integer>> L addTextSamples(
			List<Pair<IntSeq, IntSeq>> informant, L learner) {
		for (final Pair<IntSeq, IntSeq> sample : informant) {
			learner.addSample(Word.fromList(sample.l()), null != sample.r());
		}
		return learner;
	}

	public static void inferAlphabet(Iterator<Pair<IntSeq, IntSeq>> informant,
			Map<Integer, Integer> symbolToUniqueIndex) {
		while (informant.hasNext()) {
			for (int symbol : informant.next().l()) {
				symbolToUniqueIndex.computeIfAbsent(symbol, s -> symbolToUniqueIndex.size());
			}
		}
	}

	public static Iterator<Pair<IntSeq, IntSeq>> mapSymbolsToIndices(Iterator<Pair<IntSeq, IntSeq>> informant,
			Map<Integer, Integer> inputSymbolToUniqueIndex) {
		return new Iterator<Pair<IntSeq, IntSeq>>() {
			@Override
			public boolean hasNext() {
				return informant.hasNext();
			}

			@Override
			public Pair<IntSeq, IntSeq> next() {
				final Pair<IntSeq, IntSeq> element = informant.next();
				final int[] in = new int[element.l().size()];
				for (int i = 0; i < in.length; i++) {
					in[i] = inputSymbolToUniqueIndex.get(element.l().get(i));
				}
				return Pair.of(new IntSeq(in), element.r());
			}
		};
	}

	public static <K> Alphabet<Integer> minNecessaryInputAlphabet(List<Pair<IntSeq, K>> informant) {
		HashSet<Integer> usedSymbols = new HashSet<>();
		informant.forEach(p -> usedSymbols.addAll(p.l()));
		return Alphabets.fromCollection(usedSymbols);
	}

	public static <K> Alphabet<Integer> minNecessaryOutputAlphabet(List<Pair<K, IntSeq>> informant) {
		HashSet<Integer> usedSymbols = new HashSet<>();
		informant.forEach(p -> usedSymbols.addAll(p.r()));
		return Alphabets.fromCollection(usedSymbols);
	}

	public static ArrayList<Pair<IntSeq, IntSeq>> unicodeInformant(List<Pair<IntSeq, IntSeq>> informant) {
		ArrayList<Pair<IntSeq, IntSeq>> converted = new ArrayList<>(informant.size());
		for (final Pair<IntSeq, IntSeq> sample : informant) {
			if (sample.r() == null) {
				converted.add(Pair.of(sample.l(), new IntSeq(0)));
			} else {
				converted.add(Pair.of(sample.l(), sample.r()));
			}
		}
		return converted;
	}

	public static <L extends PassiveLearningAlgorithm.PassiveMealyLearner<Integer, Integer>> L addInformantSamples(
			List<Pair<IntSeq, IntSeq>> informant, L learner) {
		for (final Pair<IntSeq, IntSeq> sample : informant) {
			learner.addSample(Word.fromList(sample.l()), Word.fromList(sample.r()));
		}
		return learner;
	}

	public static <S, V, E, P, In, Out, W, N, G extends IntermediateGraph<V, E, P, N>> G dfaToIntermediate(
			Specification<V, E, P, In, Out, W, N, G> spec, Alphabet<In> alph, DFA<S, In> dfa, Function<S, V> stateMeta,
			Function<In, E> edgeConstructor, Function<S, P> finalEdgeConstructor) {
		final UniversalGraph<S, TransitionEdge<In, S>, Boolean, TransitionEdge.Property<In, Void>> tr = dfa
				.transitionGraphView(alph);
		final G g = spec.createEmptyGraph();
		final HashMap<S, N> stateToNewState = new HashMap<>();
		for (S state : tr.getNodes()) {
			stateToNewState.put(state, g.create(stateMeta.apply(state)));
		}
		for (Map.Entry<S, N> stateAndNewState : stateToNewState.entrySet()) {
			final N source = stateAndNewState.getValue();
			for (TransitionEdge<In, S> e : tr.outgoingEdges(stateAndNewState.getKey())) {
				final N target = stateToNewState.get(tr.getTarget(e));
				g.add(source, edgeConstructor.apply(e.getInput()), target);
			}
			if (dfa.isAccepting(stateAndNewState.getKey())) {
				g.setFinalEdge(source, finalEdgeConstructor.apply(stateAndNewState.getKey()));
			}
		}
		for (TransitionEdge<In, S> e : tr.outgoingEdges(dfa.getInitialState())) {
			g.addInitialEdge(stateToNewState.get(tr.getTarget(e)), edgeConstructor.apply(e.getInput()));
		}

		return g;
	}

	public static <S, T, V, E, P, In, O, Out, W, N, G extends IntermediateGraph<V, E, P, N>> G mealyToIntermediate(
			Specification<V, E, P, In, Out, W, N, G> spec,
			Alphabet<In> alph,
			MealyMachine<S, In, T, O> mealy,
			Function<S, V> stateMeta,
			BiFunction<In, O, E> edgeConstructor,
			Function<S, P> finalEdgeConstructor) {
		final UniversalGraph<S, TransitionEdge<In, T>, Void, TransitionEdge.Property<In, O>> tr = mealy
				.transitionGraphView(alph);
		final G g = spec.createEmptyGraph();
		final HashMap<S, N> stateToNewState = new HashMap<>();
		for (S state : tr.getNodes()) {
			final N n = g.create(stateMeta.apply(state));
			stateToNewState.put(state, n);
		}
		for (TransitionEdge<In, T> e : tr.outgoingEdges(mealy.getInitialState())) {
			g.addInitialEdge(stateToNewState.get(tr.getTarget(e)), edgeConstructor.apply(e.getInput(), tr.getEdgeProperty(e).getProperty()));
		}

		for (Map.Entry<S, N> stateAndNewState : stateToNewState.entrySet()) {
			final N source = stateAndNewState.getValue();
			for (TransitionEdge<In, T> e : tr.outgoingEdges(stateAndNewState.getKey())) {
				final N target = stateToNewState.get(tr.getTarget(e));
				g.add(source, edgeConstructor.apply(e.getInput(), tr.getEdgeProperty(e).getProperty()), target);
			}
			g.setFinalEdge(source, finalEdgeConstructor.apply(stateAndNewState.getKey()));
		}



		return g;
	}

	public interface Edge<N, E, P> {
		N getTarget();
	}

	public static class EdgeP<N, E, P> implements Edge<N, E, P> {
		public final P edge;
		public final N target;

		public EdgeP(P edge, N target) {
			this.edge = edge;
			this.target = target;
		}

		@Override
		public String toString() {
			return Objects.toString(edge);
		}

		@Override
		public N getTarget() {
			return target;
		}
	}

	public static class EdgeE<N, E, P> implements Edge<N, E, P> {
		public final E edge;
		public final N target;

		public EdgeE(Map.Entry<E, N> e) {
			this(e.getKey(), e.getValue());
		}

		public EdgeE(E edge, N target) {
			this.edge = edge;
			this.target = target;
		}

		@Override
		public String toString() {
			return Objects.toString(edge);
		}

		@Override
		public N getTarget() {
			return target;
		}
	}

	public static <S, T, V, E, P, In, O, Out, W, N, G extends IntermediateGraph<V, E, P, N>> Graph<State<N,V,P>, Edge<N, E, P>> intermediateAsGraph(
			G g, V initialState) {

		return new Graph<State<N,V,P>, Edge<N, E, P>>() {
			final N initState = g.makeUniqueInitialState(initialState);
			final HashMap<N,State<N,V,P>> stateToVertex = new HashMap<>();

			{
				SinglyLinkedGraph.collect(true, g, initState,n->{
					if(stateToVertex.containsKey(n)){
						return false;
					}
					stateToVertex.put(n,new State<>(n,g.getState(n),g.getFinalEdge(n)));
					return true;
				},i->null,(i,j)->null);
			}
			final DefaultVisualizationHelper<State<N, V, P>, Edge<N, E, P>> helper = new DefaultVisualizationHelper<State<N, V, P>, Edge<N, E, P>>(){
				@Override
				protected Collection<State<N, V, P>> initialNodes() {
					return Collections.singleton(stateToVertex.get(initState));
				}
			};
			@Override
			public VisualizationHelper<State<N, V, P>, Edge<N, E, P>> getVisualizationHelper() {
				return helper;
			}
			@Override
			public Collection<State<N,V,P>> getNodes() {
				return stateToVertex.values();
			}

			@Override
			public Collection<Edge<N, E, P>> getOutgoingEdges(State<N,V,P> node) {

				final Set<Map.Entry<E, N>> edges = g.outgoing(node.state).entrySet();
				final HashSet<Edge<N, E, P>> edgesConverted = new HashSet<>(edges.size());
				for (Map.Entry<E, N> e : edges)
					edgesConverted.add(new EdgeE<>(e));
				return edgesConverted;
			}

			@Override
			public State<N,V,P> getTarget(Edge<N, E, P> edge) {
				return stateToVertex.get(edge.getTarget());
			}
		};
	}

	static class State<N, V, P> {
		final N state;
		final V meta;
		final P fin;

		State(N state, V meta, P fin) {
			this.state = state;
			this.meta = meta;
			this.fin = fin;
		}

		@Override
		public String toString() {
			return meta + ":" + fin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			State<?, ?, ?> state1 = (State<?, ?, ?>) o;
			return Objects.equals(state, state1.state);
		}

		@Override
		public int hashCode() {
			return state.hashCode();
		}
	}

	public static <V, E, P, In> Graph<State<Integer, V, P>, Edge<Integer, E, P>> optimisedAsGraph(
			Specification.RangedGraph<V, In, E, P> g) {

		return new Graph<State<Integer, V, P>, Edge<Integer, E, P>>() {

			final DefaultVisualizationHelper<State<Integer, V, P>, Edge<Integer, E, P>> helper = new DefaultVisualizationHelper<State<Integer, V, P>, Edge<Integer, E, P>>(){
				@Override
				protected Collection<State<Integer, V, P>> initialNodes() {
					final int index = g.initial;
					return Collections.singleton(new State<>(index, g.state(index), g.getFinalEdge(index)));
				}
			};
			@Override
			public VisualizationHelper<State<Integer, V, P>, Edge<Integer, E, P>> getVisualizationHelper() {
				return helper;
			}

			@Override
			public Collection<State<Integer, V, P>> getNodes() {
				return new AbstractList<State<Integer, V, P>>() {
					@Override
					public State<Integer, V, P> get(int index) {
						return new State<Integer, V, P>(index, g.state(index), g.getFinalEdge(index));
					}

					@Override
					public int size() {
						return g.size();
					}
				};
			}

			@Override
			public Collection<Edge<Integer, E, P>> getOutgoingEdges(State<Integer, V, P> node) {
				final HashMap<E, Edge<Integer, E, P>> edges = new HashMap<>();
				for (Specification.Range<In, List<Specification.RangedGraph.Trans<E>>> range : g.graph
						.get(node.state)) {
					for (Specification.RangedGraph.Trans<E> edge : range.edges()) {
						edges.computeIfAbsent(edge.edge, k -> new EdgeE<>(k, edge.targetState));
					}
				}
				return edges.values();
			}

			@Override
			public State<Integer, V, P> getTarget(Edge<Integer, E, P> edge) {
				return new State<>(edge.getTarget(), g.state(edge.getTarget()), g.getFinalEdge(edge.getTarget()));
			}
		};
	}

	public static <S, T, V, E, P, In, O, Out, W, N, G extends IntermediateGraph<V, E, P, N>> void visualize(G graph,
			V initState) {
		Visualization.visualize(intermediateAsGraph(graph, initState));
	}

	public static <S, T, V, E, P, In, O, Out, W, N, G extends IntermediateGraph<V, E, P, N>> void visualize(
			Specification.RangedGraph<V, In, E, P> g) {
		Visualization.visualize(optimisedAsGraph(g));
	}

}
