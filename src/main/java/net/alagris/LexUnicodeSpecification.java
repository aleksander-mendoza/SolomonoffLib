package net.alagris;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.alagris.Pair.IntPair;

import net.alagris.CompilationError.WeightConflictingToThirdState;
import net.alagris.LexUnicodeSpecification.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Specification for ranged transducer over integers (unicode) values and with
 * arctic lexicographic semiring over integers
 */
public abstract class LexUnicodeSpecification<N, G extends IntermediateGraph<Pos, E, P, N>>
		implements Specification<Pos, E, P, Integer, IntSeq, Integer, N, G>,
		ParseSpecs<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> {

	public final int MINIMAL, MAXIMAL;
	public final boolean eagerMinimisation;
	private final HashMap<String, ExternalFunction<G>> externalFunc = new HashMap<>();
	private final HashMap<String, ExternalOperation<G>> externalOp = new HashMap<>();
	private final ExternalPipelineFunction externalPipelineFunction;
	public final HashMap<String, Var<N, G>> variableAssignments = new HashMap<>();
	public final HashMap<String, LexPipeline<N, G>> pipelines = new HashMap<>();

	public static class Var<N, G extends IntermediateGraph<Pos, E, P, N>> {
		public final G graph;
		public final String name;
		public final Pos pos;
		/**
		 * If true, then exponential operation !! will always be implicitly assumed for
		 * this variable.
		 */
		public final boolean alwaysCopy;
		private Specification.RangedGraph<Pos, Integer, E, P> optimal;

		public Specification.RangedGraph<Pos, Integer, E, P> getOptimal() {
			return optimal;
		}

		public Var(G graph, String name, Pos pos, boolean alwaysCopy) {
			this.graph = graph;
			this.name = name;
			this.pos = pos;
			this.alwaysCopy = alwaysCopy;
		}

		@Override
		public String toString() {
			return "Var{" + "graph=" + graph + ", name='" + name + '\'' + ", pos=" + pos + '}';
		}
	}

	@Override
	public E cloneFullEdge(E e) {
		return new E(e.fromExclusive,e.toInclusive,e.out,e.weight);
	}

	@Override
	public P clonePartialEdge(P p) {
		return new P(p.out,p.weight);
	}

	@Override
	public Integer successor(Integer integer) {
		if (integer >= maximal())
			throw new IllegalArgumentException("No successor for max value");
		return integer + 1;
	}

	public Integer predecessor(Integer integer) {
		if (integer <= minimal())
			throw new IllegalArgumentException("No predecessor for min value");
		return integer - 1;
	}

	public interface ExternalPipelineFunction {
		Function<IntSeq, IntSeq> make(String funcName, List<Pair<IntSeq, IntSeq>> args);
	}

	/**
	 * @param eagerMinimisation This will cause automata to be minimized as soon as
	 *                          they are parsed/registered (that is, the
	 *                          {@link LexUnicodeSpecification#pseudoMinimize} will
	 *                          be automatically called from
	 *                          {@link LexUnicodeSpecification#introduceVariable})
	 */
	public LexUnicodeSpecification(boolean eagerMinimisation, int minimal, int maximal,
			ExternalPipelineFunction externalPipelineFunction) {
		MINIMAL = minimal;
		MAXIMAL = maximal;
		this.eagerMinimisation = eagerMinimisation;
		this.externalPipelineFunction = externalPipelineFunction;
	}

	@Override
	public G externalFunction(Pos pos, String functionName, List<Pair<IntSeq, IntSeq>> args) throws CompilationError {
		final ExternalFunction<G> f = externalFunc.get(functionName);
		if (f == null)
			throw new CompilationError.UndefinedExternalFunc(functionName, pos);
		return f.call(pos, args);
	}

	@Override
	public G externalOperation(Pos pos, String functionName, List<G> args) throws CompilationError {
		final ExternalOperation<G> f = externalOp.get(functionName);
		if (f == null)
			throw new CompilationError.UndefinedExternalFunc(functionName, pos);
		return f.call(pos, args);
	}

	@Override
	public G getGraph(Var<N, G> variable) {
		return variable.graph;
	}

	@Override
	public Pos getDefinitionPos(Var<N, G> variable) {
		return variable.pos;
	}

	@Override
	public String getName(Var<N, G> variable) {
		return variable.name;
	}

	@Override
	public RangedGraph<Pos, Integer, E, P> getOptimised(Var<N, G> variable)
			throws CompilationError.WeightConflictingToThirdState {
		if (variable.optimal == null) {
			variable.optimal = optimiseGraph(variable.graph);
			reduceEdges(variable.optimal);
		}
		return variable.optimal;
	}

	@Override
	public void typecheckFunction(Pos typePos, String name, G in, G out) throws CompilationError {
		final Var<N, G> meta = borrowVariable(name);
		if (meta == null)
			throw new CompilationError.MissingFunction(typePos, name);
		final Pos graphPos = meta.pos;
		final RangedGraph<Pos, Integer, E, P> graph = getOptimised(meta);
		final RangedGraph<Pos, Integer, E, P> inOptimal = optimiseGraph(in);
		testDeterminism(name, inOptimal);
		final RangedGraph<Pos, Integer, E, P> outOptimal = optimiseGraph(out);
		testDeterminism(name, outOptimal);
		final Pair<Pos, Pos> counterexampleIn = isSubsetNondeterministic(inOptimal, graph);
		if (counterexampleIn != null) {
			throw new CompilationError.TypecheckException(counterexampleIn.r(), counterexampleIn.l(), name);
		}
		final Pair<Integer, Integer> counterexampleOut = isOutputSubset(graph, outOptimal);
		if (counterexampleOut != null) {
			throw new CompilationError.TypecheckException(graphPos, typePos, name);
		}

	}

	@Override
	public void typecheckProduct(Pos typePos, String name, G in, G out) throws CompilationError {
		final Var<N, G> meta = borrowVariable(name);
		if (meta == null)
			throw new CompilationError.MissingFunction(typePos, name);
		final Pos graphPos = meta.pos;
		final RangedGraph<Pos, Integer, E, P> graph = getOptimised(meta);
		final RangedGraph<Pos, Integer, E, P> inOptimal = optimiseGraph(in);
		testDeterminism(name, inOptimal);
		final RangedGraph<Pos, Integer, E, P> outOptimal = optimiseGraph(out);
		testDeterminism(name, outOptimal);
		final IntPair counterexampleIn = isSubset(true,graph, inOptimal, graph.initial, inOptimal.initial, new HashSet<>());
		if (counterexampleIn != null) {
			throw new CompilationError.TypecheckException(graph.state(counterexampleIn.l),
					inOptimal.state(counterexampleIn.r), name);
		}
		final Pair<Integer, Integer> counterexampleOut = isOutputSubset(graph, outOptimal);
		if (counterexampleOut != null) {
			throw new CompilationError.TypecheckException(graphPos, typePos, name);
		}
	}

	public interface ExternalFunction<G> {
		G call(Pos pos, List<Pair<IntSeq, IntSeq>> text) throws CompilationError;
	}

	public interface ExternalOperation<G> {
		G call(Pos pos, List<G> text) throws CompilationError;
	}

	/**
	 * returns previously registered function
	 */
	public ExternalFunction<G> registerExternalFunction(String name, ExternalFunction<G> f) {
		return externalFunc.put(name, f);
	}

	/**
	 * returns previously registered function
	 */
	public ExternalOperation<G> registerExternalOperation(String name, ExternalOperation<G> f) {
		return externalOp.put(name, f);
	}

	@Override
	public final Integer multiplyWeights(Integer lhs, Integer rhs) {
		return lhs + rhs;
	}

	@Override
	public final IntSeq multiplyOutputs(IntSeq lhs, IntSeq rhs) {
		return lhs == null ? null : (rhs == null ? null : lhs.concat(rhs));
	}

	@Override
	public Integer weightNeutralElement() {
		return 0;
	}

	@Override
	public final IntSeq outputNeutralElement() {
		return IntSeq.Epsilon;
	}

	@Override
	public final IntSeq output(E edge) {
		return edge.out;
	}

	@Override
	public final Integer weight(E edge) {
		return edge.weight;
	}

	@Override
	public final P multiplyPartialEdges(P edge, P edge2) {
		return new P(multiplyOutputs(edge.out, edge2.out), multiplyWeights(edge.weight, edge2.weight));
	}

	@Override
	public final P createPartialEdge(IntSeq s, Integer weight) {
		return new P(s, weight);
	}

	@Override
	public final E createFullEdge(Integer from, Integer to, P partialEdge) {
		return new E(from, to, partialEdge.out, partialEdge.weight);
	}

	@Override
	public E createFullEdgeOverSymbol(Integer symbol, P partialEdge) {
		assert maximal() >= symbol && symbol > minimal();
		return new E(symbol - 1, symbol, partialEdge.out, partialEdge.weight);
	}

	@Override
	public final E leftAction(P edge, E edge2) {
		return new E(edge2.fromExclusive, edge2.toInclusive, multiplyOutputs(edge.out, edge2.out),
				multiplyWeights(edge.weight, edge2.weight));
	}

	@Override
	public void leftActionInPlace(P edge, E edge2) {
		edge2.out = multiplyOutputs(edge.out, edge2.out);
		edge2.weight = multiplyWeights(edge.weight, edge2.weight);
	}

	@Override
	public final E rightAction(E edge, P edge2) {
		return new E(edge.fromExclusive, edge.toInclusive, multiplyOutputs(edge.out, edge2.out),
				multiplyWeights(edge.weight, edge2.weight));
	}

	@Override
	public void rightActionInPlace(E edge, P edge2) {
		edge.out = multiplyOutputs(edge.out, edge2.out);
		edge.weight = multiplyWeights(edge.weight, edge2.weight);
	}

	@Override
	public final Integer fromExclusive(E edge) {
		return edge.fromExclusive;
	}

	@Override
	public final Integer toInclusive(E edge) {
		return edge.toInclusive;
	}

	@Override
	public Integer minimal() {
		return MINIMAL;
	}

	@Override
	public Integer maximal() {
		return MAXIMAL;
	}

	@Override
	public final int compare(Integer point1, Integer point2) {
		return Integer.compare(point1, point2);
	}

	@Override
	public final P partialNeutralEdge() {
		return new P(IntSeq.Epsilon, 0);
	}

	@Override
	public final E fullNeutralEdge(Integer from, Integer to) {
		return new E(from, to, IntSeq.Epsilon, 0);
	}

	@Override
	public Iterator<Var<N, G>> iterateVariables() {
		return variableAssignments.values().iterator();
	}

	@Override
	public Var<N, G> introduceVariable(String name, Pos pos, G graph, boolean alwaysCopy) throws CompilationError {
		final Var<N, G> g = new Var<>(graph, name, pos, alwaysCopy);
		final Var<N, G> prev = variableAssignments.put(name, g);

		if (null != prev) {
			throw new CompilationError.DuplicateFunction(prev.pos, pos, name);
		}
		if (eagerMinimisation) {
			pseudoMinimize(graph);
		}
		return g;
	}

	@Override
	public Var<N, G> borrowVariable(String var) {
		return variableAssignments.get(var);
	}

	@Override
	public Var<N, G> consumeVariable(String varId) {
		class Ref {
			Var<N, G> meta;
		}
		final Ref ref = new Ref();
		variableAssignments.compute(varId, (k, meta) -> {
			ref.meta = meta;
			if (meta != null && meta.alwaysCopy) {
				return new Var<N, G>(deepClone(meta.graph), meta.name, meta.pos, true);
			} else {
				return null;
			}
		});
		return ref.meta;
	}

	@Override
	public Var<N, G> copyVariable(String var) {
		final Var<N, G> meta = variableAssignments.get(var);
		return meta == null ? null : new Var<>(deepClone(meta.graph), meta.name, meta.pos, meta.alwaysCopy);
	}

	@Override
	public final Specification<Pos, E, P, Integer, IntSeq, Integer, N, G> specification() {
		return this;
	}

	@Override
	public P epsilonUnion(@NonNull P eps1, @NonNull P eps2)
			throws IllegalArgumentException, UnsupportedOperationException {
		if (eps1.weight > eps2.weight)
			return eps1;
		if (eps1.weight < eps2.weight)
			return eps2;
		if (eps1.out.equals(eps2.out))
			return eps1;
		throw new IllegalArgumentException(
				"Both " + eps1 + " and " + eps2 + " have equal weights but different outputs");
	}

	@Override
	public P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException {
//        if (eps.weight != 0)
//            throw new IllegalArgumentException("Epsilon " + eps + " has non-zero weight under Kleene closure");
		if (!eps.out.isEmpty())
			throw new IllegalArgumentException("Epsilon " + eps + " has non-zero output under Kleene closure");
		return partialNeutralEdge();
	}

	@Override
	public Pos metaInfoGenerator(TerminalNode parseNode) {
		return new Pos(parseNode.getSymbol());
	}

	@Override
	public Pos metaInfoGenerator(ParserRuleContext parseNode) {
		return new Pos(parseNode.start);
	}

	@Override
	public Pos metaInfoNone() {
		return Pos.NONE;
	}

	@Override
	public final IntSeq parseStr(IntSeq ints) throws CompilationError {
		assert null == Specification.find(ints, i -> i < minimal() || i > maximal()) : ints;
		return ints;
	}

	@Override
	public final Integer parseW(int weight) {
		return weight;
	}

	@Override
	public final Pair<Integer, Integer> parseRangeInclusive(int codepointFromInclusive, int codepointToInclusive) {
		assert maximal() >= codepointFromInclusive && codepointFromInclusive > minimal();
		assert maximal() >= codepointToInclusive && codepointToInclusive > minimal();
		return Pair.of(codepointFromInclusive - 1, codepointToInclusive);
	}

	@Override
	public Pair<Integer, Integer> symbolAsRange(Integer symbol) {
		assert maximal() >= symbol && symbol > minimal();
		return parseRangeInclusive(symbol, symbol);
	}

	/**
	 * Full edge implementation
	 */
	public final static class E implements Comparable<E> {
		private final int fromExclusive, toInclusive;
		private IntSeq out;
		public int weight;

		public E(int from, int to, IntSeq out, int weight) {
			this.fromExclusive = Math.min(from, to);
			this.toInclusive = Math.max(from, to);
			assert fromExclusive < toInclusive : fromExclusive + " " + toInclusive;
			this.out = out;
			this.weight = weight;
		}

		public int getFromInclusive() {
			return fromExclusive;
		}

		public int getToExclsuive() {
			return toInclusive;
		}

		public IntSeq getOut() {
			return out;
		}

		public int getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("(");
			IntSeq.appendCodepointRange(sb, fromExclusive + 1, toInclusive);
			sb.append(":");
			sb.append(out.toStringLiteral());
			sb.append(" ").append(weight).append(")");
			return sb.toString();
		}

		@Override
		public int compareTo(E other) {
			int c0 = Integer.compare(fromExclusive, other.fromExclusive);
			if (c0 != 0)
				return c0;
			int c1 = Integer.compare(toInclusive, other.toInclusive);
			if (c1 != 0)
				return c1;
			int c2 = Integer.compare(weight, other.weight);
			if (c2 != 0)
				return c2;
			return out.compareTo(other.out);
		}

		boolean equalTo(E other) {
			return fromExclusive == other.fromExclusive && toInclusive == other.toInclusive && weight == other.weight
					&& out.equals(other.out);
		}

	}

	/**
	 * Partial edge implementation
	 */
	public final static class P {
		public IntSeq out;
		public int weight;

		public P(IntSeq out, Integer weight) {
			this.out = out;
			this.weight = weight;
		}

		public IntSeq getOut() {
			return out;
		}

		public int getWeight() {
			return weight;
		}

		@Override
		public String toString() {
			return "(" + out.toStringLiteral() + " " + weight + ')';
		}
	}

	public static class FunctionalityCounterexample<E, P, N> {
		public final N fromStateA;
		public final N fromStateB;
		public final BiBacktrackingNode trace;

		@Override
		public String toString() {
			return "FunctionalityCounterexample{" +
					"fromStateA=" + fromStateA +
					", fromStateB=" + fromStateB +
					", trace=" + strTrace() +
					'}';
		}

		public FunctionalityCounterexample(N fromStateA, N fromStateB, BiBacktrackingNode trace) {
			this.fromStateA = fromStateA;
			this.fromStateB = fromStateB;
			this.trace = trace;
		}

		public String strTrace() {
			BiBacktrackingNode t = trace;
			if(t==null)return "";
			final StringBuilder sb = new StringBuilder();
			while(t.source!=null){
				final StringBuilder tmp = new StringBuilder();
				IntSeq.appendRange(tmp,t.fromExclusive+1,t.toInclusive);
				t = t.source;
				sb.insert(0,tmp);
			}
			return sb.toString();
		}
	}

	public static class FunctionalityCounterexampleFinal<E, P, N> extends FunctionalityCounterexample<E, P, N> {
		public final P finalEdgeA;
		public final P finalEdgeB;

		public FunctionalityCounterexampleFinal(N fromStateA, N fromStateB, P finalEdgeA, P finalEdgeB,BiBacktrackingNode trace) {
			super(fromStateA, fromStateB, trace);
			this.finalEdgeA = finalEdgeA;
			this.finalEdgeB = finalEdgeB;
		}
	}

	public static class FunctionalityCounterexampleToThirdState<E, P, N> extends FunctionalityCounterexample<E, P, N> {
		public final E overEdgeA;
		public final E overEdgeB;
		public final N toStateC;


		public FunctionalityCounterexampleToThirdState(N fromStateA, N fromStateB, E overEdgeA, E overEdgeB,
				N toStateC,BiBacktrackingNode trace) {
			super(fromStateA, fromStateB, trace);
			this.overEdgeA = overEdgeA;
			this.overEdgeB = overEdgeB;
			this.toStateC = toStateC;
		}
	}



	public static class BiBacktrackingNode{

		public final int lhsTargetState,rhsTargetState;
		public final int fromExclusive, toInclusive;
		public final BiBacktrackingNode source;

		public BiBacktrackingNode(int lhsTargetState, int rhsTargetState, int fromExclusive, int toInclusive, BiBacktrackingNode source) {
			this.lhsTargetState = lhsTargetState;
			this.rhsTargetState = rhsTargetState;
			assert fromExclusive < toInclusive || (fromExclusive==toInclusive && source==null);
			this.fromExclusive = fromExclusive;
			this.toInclusive = toInclusive;
			this.source = source;
		}
		int l(){
			return lhsTargetState;
		}
		int r(){
			return rhsTargetState;
		}
	}

	/**
	 * Checks if automaton is strongly functional by searching for
	 * weight-conflicting transitions
	 */
	public FunctionalityCounterexample<E, P, Pos> isStronglyFunctional(Specification.RangedGraph<Pos, Integer, E, P> g) {
		return isStronglyFunctional(g,g.initial);
	}
	/**
	 * Checks if automaton is strongly functional by searching for
	 * weight-conflicting transitions
	 */
	public FunctionalityCounterexample<E, P, Pos> isStronglyFunctional(Specification.RangedGraph<Pos, Integer, E, P> g,
			int startpoint) {
		final HashMap<IntPair,BiBacktrackingNode> states = new HashMap<>();
		return collectProduct(false,g, g, startpoint, startpoint,
//				new HashSet<>(),
				(fromExclusive, toInclusive, targetLhs, edgeLhs, targetRhs, edgeRhs, source) -> {
					final IntPair biStates = Pair.of((int)targetLhs,targetRhs);
					final BiBacktrackingNode node = new BiBacktrackingNode(targetLhs,targetRhs,fromExclusive,toInclusive,source);
					final BiBacktrackingNode prev = states.putIfAbsent(biStates,node);
					if(prev==null){
						return node;
					}
					return null;
				},
				BiBacktrackingNode::l,
				BiBacktrackingNode::r,
				(state, fromExclusive, toInclusive, edgeA, edgeB) -> {
					// we can ignore sink state
					if (edgeA == null || edgeB == null)
						return null;
					// an edge cannot conflict with itself
					if (edgeA.getKey() == edgeB.getKey())
						return null;
					// only consider cases when both transitions lead to the same target state
					if (!edgeA.getValue().equals(edgeB.getValue()))
						return null;
					// weights that are not equal cannot conflict
					if (edgeA.getKey().weight != edgeB.getKey().weight)
						return null;
					// Bingo!
					return new FunctionalityCounterexampleToThirdState<>(g.state(state.l()), g.state(state.r()),
							edgeA.getKey(), edgeB.getKey(), g.state(edgeA.getValue()), state);
				}, (state) -> {
					if (!Objects.equals(state.l(), state.r())) {
						P finA = g.getFinalEdge(state.l());
						P finB = g.getFinalEdge(state.r());
						if (finA != null && finB != null && finA.weight == finB.weight) {
							return new FunctionalityCounterexampleFinal<>(g.state(state.l()), g.state(state.r()), finA,
									finB,state);
						}
					}
					return null;
				});
	}

	public interface ContinuationTest<Y> {
		/**
		 * return new carry if should continue, or null if this computation branch
		 * should no longer be continued.
		 */
		Y shouldContinue(Y carry, BacktrackingNode backtrack, int reachedState, int numOfActiveComputationBranches);
	}

	public interface RejectionCallback {
		/**
		 * This is called when computation branch dies due to automaton going to sink
		 * state
		 */
		void rejected(BacktrackingNode backtrack);
	}

	public interface AcceptanceCallback {
		/**
		 * This is called when computation branch dies due to automaton going to sink
		 * state
		 */
		void accepted(BacktrackingNode backtrack, int finalState);
	}

	public static class Carry<Y> {
		Y carry;
		final BacktrackingNode backtrack;

		Carry(Y carry, BacktrackingNode backtrack) {
			this.backtrack = backtrack;
			this.carry = carry;
		}
	}

	/**
	 * The algorithm enumerates all paths of automaton by running all of the
	 * possible computation branches "in parallel"
	 */
	public <Y> void generate(RangedGraph<Pos, Integer, E, P> g, Y initialCarry, // data carried by each computation branch
			ContinuationTest<Y> test, // allows for manually terminating some
			// computation branches
			AcceptanceCallback accept, // called when computation branch accepted
			RejectionCallback reject // called when computation branch dies due to missing transition
	// (this only concerns partial/nondeterministic automata)
	) {
		final Pair<HashMap<PowersetState, IdxAndTrans<Integer, E, RangedGraph.BiTrans<E>>>, RangedGraph<Pos, Integer, E, P>> p = powersetWithSuperstates(
				g, (i, trs) -> Specification.mapListLazy(trs, tr -> new RangedGraph.BiTrans<>(i, tr)));
		final RangedGraph<Pos, Integer, E, P> powersetGraph = p.r();
		final ArrayList<Range<Integer, List<RangedGraph.BiTrans<E>>>>[] nfaTransPerPowersetState = new ArrayList[p.l()
				.size()];
		for (Entry<PowersetState, IdxAndTrans<Integer, E, RangedGraph.BiTrans<E>>> entry : p.l().entrySet()) {
			final IdxAndTrans<Integer, E, RangedGraph.BiTrans<E>> powersetStateTransitions = entry.getValue();
			assert Specification.validate(powersetStateTransitions, entry.getKey(), powersetGraph, g);
			nfaTransPerPowersetState[powersetStateTransitions.index] = powersetStateTransitions.nfaTrans;
		}
		class ComputationBranch {
			final int powersetState;
			final HashMap<Integer, Carry<Y>> backtracksPerState = new HashMap<>();

			ComputationBranch(int powersetState) {
				this.powersetState = powersetState;
			}

			@Override
			public String toString() {
				return powersetState + " ~ " + backtracksPerState;
			}
		}
		/**
		 * Explores the computation tree on all possible inputs in breath-first order.
		 */
		final java.util.Queue<ComputationBranch> computationTree = new LinkedList<>();
		computationTree.add(new ComputationBranch(powersetGraph.initial));
		assert computationTree.peek() != null;
		computationTree.peek().backtracksPerState.put(g.initial, new Carry<Y>(initialCarry, null));
		int numOfActiveComputationBranches = 1;
		while (!computationTree.isEmpty()) {// this loop will end only if the language of transducer is
			// finite or when GeneratorCallback discontinues
			// all of the computation branches
			assert numOfActiveComputationBranches == Specification.fold(computationTree, 0,
					(b, sum) -> sum + b.backtracksPerState.size());
			final ComputationBranch computationBranch = computationTree.poll();
			assert computationBranch != null;
			final int numOfPoppedComputationBranches = computationBranch.backtracksPerState.size();
			final Iterator<Entry<Integer, Carry<Y>>> iter = computationBranch.backtracksPerState.entrySet().iterator();
			BacktrackingNode acceptedTrace = null;
			int acceptedState = -1;
			int acceptedWeight = Integer.MIN_VALUE;
			int removed = 0;
			while (iter.hasNext()) {
				final Map.Entry<Integer, Carry<Y>> stateAndNode = iter.next();
				final int state = stateAndNode.getKey();
				final Carry<Y> carry = stateAndNode.getValue();
				assert carry.carry != null;
				if (state == -1) {
					reject.rejected(carry.backtrack);
					iter.remove();
					removed++;
				} else {
					final P fin = g.getFinalEdge(state);
					if (fin != null && fin.weight > acceptedWeight) {
						acceptedWeight = fin.weight;
						acceptedTrace = carry.backtrack;
						acceptedState = state;
					}
					carry.carry = test.shouldContinue(carry.carry, carry.backtrack, state,
							numOfActiveComputationBranches - removed);
					if (carry.carry == null) {
						iter.remove();
						removed++;
					}
				}
			}
			if (acceptedState != -1) {
				accept.accepted(acceptedTrace, acceptedState);
			}
			int numOfPushedComputationBranches = 0;
			final int sourcePowersetState = computationBranch.powersetState;
			final ArrayList<Range<Integer, List<RangedGraph.BiTrans<E>>>> originalTransitions = nfaTransPerPowersetState[sourcePowersetState];
			final ArrayList<Range<Integer, List<RangedGraph.Trans<E>>>> powersetTransitions = powersetGraph.graph
					.get(sourcePowersetState);
			assert originalTransitions.size() == powersetTransitions.size() : originalTransitions + " "
					+ powersetTransitions;
			for (int i = 0; i < originalTransitions.size(); i++) {
				final Range<Integer, List<RangedGraph.Trans<E>>> powersetTransition = powersetTransitions.get(i);
				assert powersetTransition.input().equals(originalTransitions.get(i).input()) : originalTransitions + " "
						+ powersetTransitions;
				assert powersetTransition.edges().size() <= 1 : powersetTransition;
				final int targetPowersetState = powersetTransition.edges().size() == 0 ? -1
						: powersetTransition.edges().get(0).targetState;
				final ComputationBranch newBranch = new ComputationBranch(targetPowersetState);
				final HashMap<Integer, Carry<Y>> nextSuperposition = newBranch.backtracksPerState;
				for (RangedGraph.BiTrans<E> originalTransition : originalTransitions.get(i).edges()) {
					final int originalSourceState = originalTransition.sourceState;
					final int originalTargetState = originalTransition.targetState;
					final E originalEdge = originalTransition.edge;
					if (computationBranch.backtracksPerState.containsKey(originalSourceState)) {
						final Carry<Y> carry = computationBranch.backtracksPerState.get(originalSourceState);
						nextSuperposition.compute(originalTargetState, (key, prev) -> {
							if (prev == null)
								return new Carry<>(carry.carry, new BacktrackingNode(carry.backtrack, originalEdge));
							if (prev.backtrack.edge.weight < originalEdge.weight) {
								prev.backtrack.edge = originalEdge;
								prev.backtrack.prev = carry.backtrack;
							} else {
								assert prev.backtrack.edge.weight > originalEdge.weight
										|| prev.backtrack.edge.out.equals(originalEdge.out) : prev + " "
												+ originalTransition;
							}
							return prev;
						});
					}
				}
				if (!nextSuperposition.isEmpty()) {
					numOfPushedComputationBranches += newBranch.backtracksPerState.size();
					computationTree.add(newBranch);
				}
			}
			numOfActiveComputationBranches = numOfActiveComputationBranches - numOfPoppedComputationBranches
					+ numOfPushedComputationBranches;
		}
	}

	public void generateRandomSampleBoundedByLength(RangedGraph<Pos, Integer, E, P> g, int maxLength,
			double approxMaxAliveComputationBranches, Random rnd, AcceptanceCallback accept, RejectionCallback reject) {
		generate(g, 0, (carry, backtrack, reachedState, activeBranches) -> {
			if (rnd.nextDouble() > (approxMaxAliveComputationBranches / activeBranches))
				return null;
			assert LexUnicodeSpecification.BacktrackingNode.length(backtrack)==carry+1;
			assert carry <= maxLength;
			return carry < maxLength ? carry + 1 : null;
		}, accept, reject);
	}
	/**Shorter string have higher probability than the longer ones. Notice that the actual sample size might be less than the requested value,
	 * because the language recognized by transducer is too small. Even in cases when the language has the size of requested sample, not all of the member strings
	 * will be included (because they have random chance of being accepted, so that the sample is not prefix closed). As a result the sample is never
	 * guaranteed to be of the desired size. There even is a non-zero probability of producing empty sample despite the language being non-empty*/
	public  void generateRandomSampleOfSize(RangedGraph<Pos, Integer, E, P> g, int sampleSize,Random rnd, AcceptanceCallback accept, RejectionCallback reject) {
		if(sampleSize==0)return;
		assert sampleSize>0;
		final Object ALIVE = new Object();
		class Callback implements AcceptanceCallback, ContinuationTest<Object>{
			int alreadyAccepted = 0;
			@Override
			public Object shouldContinue(Object carry, BacktrackingNode backtrack, int reachedState, int activeBranches) {
				if (rnd.nextDouble() > ((sampleSize-alreadyAccepted) / (double)activeBranches))
					return null;
				return ALIVE;
			}
			@Override
			public void accepted(BacktrackingNode backtrack, int finalState){
				if(rnd.nextDouble() > Math.min(0.5,(sampleSize-alreadyAccepted)/(double)sampleSize))  {
					alreadyAccepted++;
					accept.accepted(backtrack, finalState);
				}
				
			}
		}
		Callback c = new Callback();
		generate(g, ALIVE, c , c, reject);
	}
	/**
	 * Here by characteristic sample, we understand set of strings that uses every
	 * edge (and as a result every state) in the transducer.
	 */
	ArrayList<BacktrackingHead> generateSmallCharacteristicSample(RangedGraph<Pos, Integer, E, P> g) {
		final HashSet<E> visited = new HashSet<>();
		final Object ALIVE = new Object();
		final Object DEAD = null;
		final ArrayList<BacktrackingHead> characteristicSample = new ArrayList<>();
		generate(g, ALIVE, (carry, backtrack, reachedState,
				numOfActiveComputationBranches) -> backtrack == null || visited.add(backtrack.edge) ? ALIVE : DEAD,
				(backtrack, finalState) -> characteristicSample
						.add(new BacktrackingHead(backtrack, g.getFinalEdge(finalState))),
				backtrack -> {
				});
		return characteristicSample;
	}

	/**
	 * Here by characteristic sample, we understand set of strings that explore all
	 * paths of length less or equal to number of states.
	 */
	ArrayList<BacktrackingHead> generateLargeCharacteristicSample(RangedGraph<Pos, Integer, E, P> g) {
		final ArrayList<BacktrackingHead> characteristicSample = new ArrayList<>();
		final Object ALIVE = new Object();
		final Object DEAD = null;
		generate(g, ALIVE, (carry, backtrack, reachedState,
				numOfActiveComputationBranches) -> BacktrackingNode.length(backtrack) <= g.size() ? ALIVE : DEAD,
				(backtrack, finalState) -> characteristicSample
						.add(new BacktrackingHead(backtrack, g.getFinalEdge(finalState))),
				backtrack -> {
				});
		return characteristicSample;
	}

	/**
	 * Here by characteristic sample, we understand set of strings that explore all
	 * paths of length less or equal to number of states.
	 */
	ArrayList<BacktrackingHead> generateSmallOstiaCharacteristicSample(RangedGraph<Pos, Integer, E, P> g) {
		final HashSet<E> visited = new HashSet<>();
		final ArrayList<BacktrackingHead> characteristicSample = new ArrayList<>();
		generate(g, 3, (carry, backtrack, reachedState, numOfActiveComputationBranches) -> {
			if (backtrack == null || visited.add(backtrack.edge)) {
				return 3;
			}
			if (carry > 0)
				return carry - 1;
			return null;
		}, (backtrack, finalState) -> characteristicSample
				.add(new BacktrackingHead(backtrack, g.getFinalEdge(finalState))), backtrack -> {
				});
		return characteristicSample;
	}

	/**
	 * Carries information about all consecutively takes transitions in reverse
	 * order.
	 */
	public static class BacktrackingNode {

		public E edge;
		/**
		 * null its if the beginning of path and there is no previous transition
		 */
		public BacktrackingNode prev;

		public BacktrackingNode(BacktrackingNode prev, E edge) {
			this.edge = edge;
			this.prev = prev;
		}

		public static int length(BacktrackingNode node) {
			int sum = 0;
			while (node != null) {
				sum++;
				node = node.prev;
			}
			return sum;
		}

		public static boolean matches(BacktrackingNode node, Seq<Integer> input) {
			int i = input.size() - 1;
			while (node != null && i >= 0) {
				final int in = input.get(i);
				if (node.edge.fromExclusive < in && in <= node.edge.toInclusive) {
					node = node.prev;
					i--;
				} else {
					return false;
				}
			}
			return node == null && i == -1;
		}

		public static String str(BacktrackingNode node) {
			if (node == null)
				return "";
			final StringBuilder sb = new StringBuilder(node.edge.toString());
			node = node.prev;
			while (node != null) {
				sb.insert(0, node.edge + " ");
				node = node.prev;
			}
			return sb.toString();
		}

		public static String strHuman(BacktrackingNode node) {
			if (node == null)
				return "";
			final StringBuilder sb = new StringBuilder();
			while (node != null) {
				StringBuilder tmp = new StringBuilder();
				IntSeq.appendRange(tmp,node.edge.fromExclusive+1,node.edge.toInclusive);
				sb.insert(0,tmp);
				node = node.prev;
			}
			return sb.toString();
		}

		public static IntSeq randMatchingInput(BacktrackingNode node, Random rnd) {
			return randMatchingInput(node, length(node), rnd);
		}

		/**
		 * Randomly choose some input that matches input labels of edges in this trace
		 */
		public static IntSeq randMatchingInput(BacktrackingNode node, int len, Random rnd) {
			assert len == length(node);
			final int[] arr = new int[len];
			while (node != null) {
				final E e = node.edge;
				final int i = rnd.nextInt(e.toInclusive - e.fromExclusive) + 1 + e.fromExclusive;
				assert e.toInclusive >= i && i > e.fromExclusive;
				arr[--len] = i;
				node = node.prev;
			}
			return new IntSeq(arr);
		}

		@Override
		public String toString() {
			return strHuman(this);
		}
	}

	public static class BacktrackingHead {
		public final P finalEdge;
		public final BacktrackingNode prev;

		public BacktrackingHead(BacktrackingNode prev, P edge) {
			this.prev = prev;
			this.finalEdge = edge;
		}

		public int inputSize() {
			return BacktrackingNode.length(prev);
		}

		public int outputSize(int minimalSymbol) {
			int sum = 0;
			for (int outSymbol : finalEdge.out) {
				if (outSymbol != minimalSymbol) {
					sum++;
				}
			}
			BacktrackingNode curr = prev;
			while (curr != null) {
				sum += curr.edge.out.size();
				curr = curr.prev;
			}
			return sum;
		}

		public IntSeq collect(Seq<Integer> input, int minimalSymbol) {
			assert input.size() == inputSize();
			int[] output = new int[outputSize(minimalSymbol)];
			if (collect(output, minimalSymbol, input)) {
				return new IntSeq(output);
			} else {
				return null;
			}
		}

		/**
		 * Returns true if input string correctly matched input labels on each edge.
		 * False otherwise
		 */
		public boolean collect(int[] output, int minimalSymbol, Seq<Integer> input) {
			assert output.length == outputSize(minimalSymbol);
			assert input.size() == inputSize();
			int i = output.length - 1;
			for (int outSymbolIdx = finalEdge.out.size() - 1; outSymbolIdx >= 0; outSymbolIdx--) {
				final int outSymbol = finalEdge.out.get(outSymbolIdx);
				if (outSymbol != minimalSymbol) {
					output[i--] = outSymbol;
				}
			}
			BacktrackingNode curr = prev;
			int inputIdx = input.size() - 1;
			while (curr != null) {
				final int in = input.get(inputIdx);
				if (curr.edge.fromExclusive < in && in <= curr.edge.toInclusive) {
					for (int outSymbolIdx = curr.edge.out.size() - 1; outSymbolIdx >= 0; outSymbolIdx--) {
						final int outSymbol = curr.edge.out.get(outSymbolIdx);
						if (outSymbol == minimalSymbol) {
							output[i--] = in;
						} else {
							output[i--] = outSymbol;
						}
					}
					curr = curr.prev;
					inputIdx--;
				} else {
					return false;
				}
			}
			assert i == -1;
			assert inputIdx == -1;
			return true;
		}

		@Override
		public String toString() {
			return BacktrackingNode.str(prev) + " " + finalEdge;
		}

		public String strHuman() {
			return BacktrackingNode.strHuman(prev);
		}

		public IntSeq randMatchingInput(Random rnd) {
			return BacktrackingNode.randMatchingInput(prev, rnd);
		}
	}

	public String evaluate(Specification.RangedGraph<?, Integer, E, P> graph, String input) {
		final IntSeq out = evaluate(graph, new IntSeq(input));
		return out == null ? null : out.toUnicodeString();
	}

	public IntSeq evaluate(Specification.RangedGraph<?, Integer, E, P> graph, IntSeq input) {
		return evaluate(graph, graph.initial, input);
	}

	/**
	 * Performs evaluation and uses hashtags outputs as reflections of input
	 */
	public IntSeq evaluate(Specification.RangedGraph<?, Integer, E, P> graph, int initial, IntSeq input) {
		final BacktrackingHead head = evaluate(graph, initial, input.iterator());
		return head == null ? null : head.collect(input, minimal());
	}

	public BacktrackingHead evaluate(RangedGraph<?, Integer, E, P> graph, Iterator<Integer> input) {
		return evaluate(graph, graph.initial, input);
	}

	/**
	 * Performs a very efficient evaluation algorithm for lexicographic ranged
	 * transducers. It's O(n^2) for dense nondeterministic automata, O(n) for
	 * deterministic automata and close to O(n) for sparse nondeterministic
	 * automata. The returned value is a singly linked list of taken transitions
	 * (first element of list is the last taken transition).
	 * <p>
	 * The automaton must be strongly functional (have no weight-conflicting
	 * transitions) in order for this algorithm to work. If automaton is not
	 * strongly functional, then the exact outcome is undefined (could be any of the
	 * equally best paths).
	 *
	 * @return singly linked list of all transitions taken by the best (with highest
	 *         weights) path. May be null if automaton does not accept
	 */
	public BacktrackingHead evaluate(RangedGraph<?, Integer, E, P> graph, int initial, Iterator<Integer> input) {

		HashMap<Integer, BacktrackingNode> thisList = new HashMap<>();
		HashMap<Integer, BacktrackingNode> nextList = new HashMap<>();
		if (initial != -1)
			thisList.put(initial, null);
		while (input.hasNext() && !thisList.isEmpty()) {
			final int in = input.next();
			deltaSuperposition(graph, in, thisList, nextList);
			final HashMap<Integer, BacktrackingNode> tmp = thisList;
			thisList = nextList;
			nextList = tmp;
			nextList.clear();
		}
		final Iterator<Map.Entry<Integer, BacktrackingNode>> iter = thisList.entrySet().iterator();
		if (iter.hasNext()) {
			Map.Entry<Integer, BacktrackingNode> first = iter.next();
			P bestFinalEdge = graph.accepting.get(first.getKey());
			BacktrackingNode bestPreviousNode = first.getValue();
			while (iter.hasNext()) {
				Map.Entry<Integer, BacktrackingNode> next = iter.next();
				P otherFinalEdge = graph.accepting.get(next.getKey());
				if (otherFinalEdge != null && (bestFinalEdge == null || otherFinalEdge.weight > bestFinalEdge.weight)) {
					bestFinalEdge = otherFinalEdge;
					bestPreviousNode = next.getValue();
				}
			}
			return bestFinalEdge == null ? null : new BacktrackingHead(bestPreviousNode, bestFinalEdge);
		} else {
			return null;
		}

	}

	public void deltaSuperposition(RangedGraph<?, Integer, E, P> graph, int input,
			HashMap<Integer, BacktrackingNode> thisSuperposition,
			HashMap<Integer, BacktrackingNode> nextSuperposition) {
		for (final Map.Entry<Integer, BacktrackingNode> stateAndNode : thisSuperposition.entrySet()) {
			final int state = stateAndNode.getKey();
			if (state == -1)
				continue;
			for (final RangedGraph.Trans<E> transition : binarySearch(graph, state, input)) {
				nextSuperposition.compute(transition.targetState, (key, prev) -> {
					if (prev == null)
						return new BacktrackingNode(stateAndNode.getValue(), transition.edge);
					if (prev.edge.weight < transition.edge.weight) {
						prev.edge = transition.edge;
						prev.prev = stateAndNode.getValue();
					} else {
						assert prev.edge.weight > transition.edge.weight
								|| prev.edge.out.equals(transition.edge.out) : prev + " " + transition;
					}
					return prev;
				});
			}
		}
	}

	public ParserListener<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> makeParser() {
		return new ParserListener<>(this);
	}

	/**
	 * @throws net.alagris.CompilationError if typechcking fails
	 */
	public void checkStrongFunctionality(RangedGraph<Pos, Integer, E, P> g)
			throws CompilationError.WeightConflictingFinal, CompilationError.WeightConflictingToThirdState {
		final FunctionalityCounterexample<E, P, Pos> weightConflictingTranitions = isStronglyFunctional(g, g.initial);
		if (weightConflictingTranitions != null) {
			if (weightConflictingTranitions instanceof FunctionalityCounterexampleFinal) {
				throw new CompilationError.WeightConflictingFinal(
						(FunctionalityCounterexampleFinal<E, P, ?>) weightConflictingTranitions);
			} else {
				throw new CompilationError.WeightConflictingToThirdState(
						(FunctionalityCounterexampleToThirdState<E, P, ?>) weightConflictingTranitions);
			}
		}
	}

	public void testDeterminism(String name, RangedGraph<Pos, Integer, E, P> inOptimal)
			throws CompilationError.NondeterminismException {
		final List<RangedGraph.Trans<E>> nondeterminismCounterexampleIn = inOptimal.isDeterministic();
		if (nondeterminismCounterexampleIn != null) {
			throw new CompilationError.NondeterminismException(
					inOptimal.state(nondeterminismCounterexampleIn.get(0).targetState),
					inOptimal.state(nondeterminismCounterexampleIn.get(1).targetState), name);
		}
	}

	public Pair<Integer, Integer> isOutputSubset(RangedGraph<Pos, Integer, E, P> lhs,
			RangedGraph<Pos, Integer, E, P> rhs) {
		return isOutputSubset(lhs, rhs, new HashSet<>(), IntSeq::iterator, e -> e.out.iterator());

	}

	public RangedGraph<Pos, Integer, E, P> optimiseVar(String varId) {
		return optimiseGraph(variableAssignments.get(varId).graph);
	}

	public void pseudoMinimize(G graph) throws CompilationError.WeightConflictingFinal {
//		assert isStronglyFunctional(optimiseGraph(graph))==null:isStronglyFunctional(optimiseGraph(graph));
		BiFunction<N, Map<E, N>, Integer> hash = (vertex, transitions) -> {
			ArrayList<Map.Entry<E, N>> edges = new ArrayList<>(transitions.size());
			edges.addAll(transitions.entrySet());
			edges.sort(Map.Entry.comparingByKey());
			int h = 0;
			for (Map.Entry<E, N> e : edges) {
				h = 31 * h + Objects.hash(e.getKey().fromExclusive, e.getKey().toInclusive, e.getValue());
			}
			return h;
		};
		graph.pseudoMinimize((vertex, transitions) -> {
			int h = hash.apply(vertex, transitions);
			P p = graph.getFinalEdge(vertex);
			return 31 * h + (p == null ? 0 : p.out.hashCode());
		}, hash, (trA, trB) -> {
			if (trA.size() != trB.size())
				return false;
			ArrayList<Map.Entry<E, N>> edgesA = new ArrayList<>(trA.size());
			edgesA.addAll(trA.entrySet());
			edgesA.sort(Map.Entry.comparingByKey());
			ArrayList<Map.Entry<E, N>> edgesB = new ArrayList<>(trB.size());
			edgesB.addAll(trB.entrySet());
			edgesB.sort(Map.Entry.comparingByKey());
			for (int i = 0; i < trA.size(); i++) {
				Map.Entry<E, N> a = edgesA.get(i);
				Map.Entry<E, N> b = edgesB.get(i);
				N targetStateA = a.getValue();
				N targetStateB = b.getValue();
				if (!a.getKey().equalTo(b.getKey()) || targetStateA != targetStateB) {
					return false;
				}
			}
			return true;
		}, (finA, finB) -> finA.out.equals(finB.out), (stateA, finA, stateB, finB) -> {
			if (finA.weight > finB.weight) {
				return finA;
			} else if (finA.weight < finB.weight) {
				return finB;
			} else if (finA.out.equals(finB.out)) {
				return finA;// doesn't matter, both are the same
			} else {
				throw new CompilationError.WeightConflictingFinal(
						new FunctionalityCounterexampleFinal<>(stateA, stateB, finA, finB, null));
			}
		},
		()->{
			return true;
//			final RangedGraph<Pos, Integer, E, P> i = optimiseGraph(graph);
//			final FunctionalityCounterexample<E, P, Pos> counterexample = isStronglyFunctional(i,i.initial);
//			if(counterexample!=null){
//				assert false:counterexample;
//			}
//			return counterexample==null;
		});
//		assert isStronglyFunctional(optimiseGraph(graph))==null;
	}

	/**
	 * Normally it should not happen, but some operations may introduce edges that
	 * both start in the same source state, end in th same target state, have
	 * overlapping input ranges and produce the same outputs. Such edges violate
	 * strong functionality, but can be easily removed without affecting the
	 * transducer. Any time there are two identical edges that only differ in
	 * weight, the highest one is chosen and all the remaining ones are removed.
	 */
	public void reduceEdges(RangedGraph<Pos, Integer, E, P> g) throws CompilationError.WeightConflictingToThirdState {
		for (int sourceState = 0; sourceState < g.graph.size(); sourceState++) {
			ArrayList<Range<Integer, List<RangedGraph.Trans<E>>>> state = g.graph.get(sourceState);
			for (Range<Integer, List<RangedGraph.Trans<E>>> range : state) {
				final List<RangedGraph.Trans<E>> tr = range.edges();
				/**
				 * All of the edges have the same input range and source state but may differ in
				 * outputs, weights and target states. The task is to find all those edges that
				 * also have the same target state and remove all except for the one with
				 * highest weight. However, if there are several edges with equal target state
				 * and equally highest weights, then make sure that their outputs are the same
				 * or otherwise throw a WeightConflictingException.
				 */
				if (tr.size() > 1) {
					// First sort by target state so that this way all edges are grouped by target
					// states.
					// Additionally among all the edges with equal target state, sort them with
					// respect to weight, so that
					// the last edge has the highest weight.
					tr.sort(Comparator.comparingInt((RangedGraph.Trans<E> x) -> x.targetState)
							.thenComparingInt(x -> x.edge.weight));
					int j = 0;
					for (int i = 1; i < tr.size(); i++) {
						RangedGraph.Trans<E> prev = tr.get(i - 1);
						RangedGraph.Trans<E> curr = tr.get(i);
						if (prev.targetState != curr.targetState) {
							// prev edge is the last edge leading to prev.targetState, hence it also has the
							// highest weight
							tr.set(j++, prev);
						} else if (prev.edge.weight == curr.edge.weight && !prev.edge.out.equals(curr.edge.out)) {
							throw new CompilationError.WeightConflictingToThirdState(
									new FunctionalityCounterexampleToThirdState<>(sourceState, sourceState, prev.edge,
											curr.edge, prev.targetState, null));
						}
					}
					tr.set(j, tr.get(tr.size() - 1));
					Specification.removeTail(tr, j + 1);
				}
			}
		}
	}

	public static final class LexPipeline<N, G extends IntermediateGraph<Pos, E, P, N>> {
		public LexPipeline(LexUnicodeSpecification<N, G> spec) {
			this.spec = spec;
		}

		public LexPipeline<N, G> appendAll(LexPipeline<N, G> other) throws CompilationError {
			if (other.nodes.isEmpty()) {
				if (other.hoareAssertion != null)
					appendLanguage(other.hoarePos, hoareAssertion);
			} else {
				for (Node node : other.nodes) {
					if (node instanceof AutomatonNode) {
						append(((AutomatonNode) node).g);
					} else {
						append(((ExternalNode) node).f);
					}
				}
				this.hoarePos = other.hoarePos;
				this.hoareAssertion = other.hoareAssertion;
			}
			return this;
		}

		private interface Node {

			IntSeq evaluate(IntSeq input);

			void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> g)
					throws CompilationError.CompositionTypecheckException;
		}

		private static final class AutomatonNode implements Node {
			final RangedGraph<Pos, Integer, E, P> g;
			private final LexUnicodeSpecification<?, ?> spec;

			private AutomatonNode(RangedGraph<Pos, Integer, E, P> g, LexUnicodeSpecification<?, ?> spec) {
				this.g = g;
				this.spec = spec;
			}

			@Override
			public IntSeq evaluate(IntSeq input) {
				return spec.evaluate(g, input);
			}

			@Override
			public void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> type)
					throws CompilationError.CompositionTypecheckException {
				Pair<Integer, Integer> counterexample = spec.isOutputSubset(g, type);
				if (counterexample != null) {
					Pos typePos = type.state(counterexample.r());
					throw new CompilationError.CompositionTypecheckException(g.state(counterexample.l()),
							typePos == null ? pos : typePos);
				}
			}
		}

		private static final class ExternalNode implements Node {
			final Function<IntSeq, IntSeq> f;

			private ExternalNode(Function<IntSeq, IntSeq> f) {
				this.f = f;
			}

			@Override
			public IntSeq evaluate(IntSeq input) {
				return f.apply(input);
			}

			@Override
			public void typecheckOutput(Pos pos, RangedGraph<Pos, Integer, E, P> g)
					throws CompilationError.CompositionTypecheckException {
				// nothing. Just assume it to be true. The user should be responsible.
			}
		}

		public Pos getPos() {
			return pos;
		}

		private Pos pos;
		private final LexUnicodeSpecification<N, G> spec;
		private final ArrayList<Node> nodes = new ArrayList<>();
		private Pos hoarePos;
		private RangedGraph<Pos, Integer, E, P> hoareAssertion;

		public LexPipeline<N, G> append(RangedGraph<Pos, Integer, E, P> g)
				throws CompilationError.CompositionTypecheckException {
			nodes.add(new AutomatonNode(g, spec));
			if (hoareAssertion != null) {
				assert hoarePos != null;
				Pair<Pos, Pos> counterexample = spec.isSubsetNondeterministic(hoareAssertion, g);
				if (counterexample != null) {
					throw new CompilationError.CompositionTypecheckException(counterexample.l(), counterexample.r());
				}
			}
			hoarePos = null;
			hoareAssertion = null;
			return this;
		}

		public LexPipeline<N, G> append(Function<IntSeq, IntSeq> f) {
			nodes.add(new ExternalNode(f));
			hoarePos = null;
			hoareAssertion = null;
			return this;
		}

		public LexPipeline<N, G> appendLanguage(Pos pos, RangedGraph<Pos, Integer, E, P> g)
				throws CompilationError.CompositionTypecheckException {
			nodes.get(nodes.size() - 1).typecheckOutput(pos, g);
			hoareAssertion = g;
			hoarePos = pos;
			return this;
		}

		public String evaluate(String input) {
			final IntSeq out = evaluate(new IntSeq(input));
			return out == null ? null : out.toUnicodeString();
		}

		public IntSeq evaluate(IntSeq input) {
			for (Node node : nodes) {
				if (input == null)
					break;
				input = node.evaluate(input);
			}
			return input;
		}
	}

	/**
	 * @param name should not contain the @ sign as it is already implied by this
	 *             methods
	 */
	public LexPipeline<N, G> getPipeline(String name) {
		return pipelines.get(name);
	}

	@Override
	public LexPipeline<N, G> makeNewPipeline() {
		return new LexPipeline<N, G>(this);
	}

	@Override
	public void registerNewPipeline(Pos pos, LexPipeline<N, G> pipeline, String name)
			throws CompilationError.DuplicateFunction {
		final LexPipeline<N, G> prev = pipelines.put(name, pipeline);
		if (prev != null) {
			throw new CompilationError.DuplicateFunction(prev.pos, pipeline.pos, '@' + name);
		}
	}

	@Override
	public LexPipeline<N, G> appendAutomaton(Pos pos, LexPipeline<N, G> lexPipeline, G g)
			throws CompilationError.CompositionTypecheckException, CompilationError.WeightConflictingFinal, WeightConflictingToThirdState {
		if (eagerMinimisation)
			pseudoMinimize(g);
		final RangedGraph<Pos, Integer, E, P> optimal = optimiseGraph(g);
		checkStrongFunctionality(optimal);
		return lexPipeline.append(optimal);
	}

	@Override
	public LexPipeline<N, G> appendExternalFunction(Pos pos, LexPipeline<N, G> lexPipeline, String funcName,
			List<Pair<IntSeq, IntSeq>> args) {
		return lexPipeline.append(externalPipelineFunction.make(funcName, args));
	}

	@Override
	public LexPipeline<N, G> appendLanguage(Pos pos, LexPipeline<N, G> lexPipeline, G g) throws CompilationError {
		RangedGraph<Pos, Integer, E, P> optimal = optimiseGraph(g);
		testDeterminism(pos.toString(), optimal);
		return lexPipeline.appendLanguage(pos, optimal);
	}

	@Override
	public LexPipeline<N, G> appendPipeline(Pos pos, LexPipeline<N, G> lexPipeline, String nameOfOtherPipeline)
			throws CompilationError {
		final LexPipeline<N, G> other = pipelines.get(nameOfOtherPipeline);
		if (other == null) {
			throw new CompilationError.MissingFunction(pos, '@' + nameOfOtherPipeline);
		}
		return lexPipeline.appendAll(other);
	}

	public G loadDict(Specification.NullTermIter<Pair<IntSeq, IntSeq>> dict, Pos state)
			throws CompilationError.AmbiguousDictionary {
		return loadDict(dict, state, (in, out1, out2) -> {
			throw new CompilationError.AmbiguousDictionary(in, out1, out2);
		});
	}

	/**
	 * size isEpsilon (weight out)? (transtionNumber (from to target weight out
	 * )^transtionNumber)^size initNumber (from to target weight out )^initNumber
	 * (source outWeight outStr)*
	 **/
	public void compressBinary(G g, DataOutputStream out) throws IOException {
		final LinkedHashMap<N, Integer> vertexToIndex = new LinkedHashMap<>();
		g.collectVertices(true,(N n) -> {
			class Ref {
				boolean computed = false;
			}
			final Ref ref = new Ref();
			vertexToIndex.computeIfAbsent(n, k -> {
				ref.computed = true;
				return vertexToIndex.size();
			});
			return ref.computed;
		}, v -> null, (n, e) -> null);
		out.writeInt(vertexToIndex.size());// size
		final P eps = g.getEpsilon();
		if (eps == null) {
			out.writeByte(0); // isEpsilon
		} else {
			out.writeByte(1); // isEpsilon
			out.writeInt(eps.weight);// weight
			out.writeUTF(eps.out.toUnicodeString());// out
		}
		for (Entry<N, Integer> vertexIdx : vertexToIndex.entrySet()) {
			final int transitionNumber = g.size(vertexIdx.getKey());
			out.writeInt(transitionNumber);// transitionNumber
			for (Entry<E, N> transition : (Iterable<Entry<E, N>>) () -> g.iterator(vertexIdx.getKey())) {
				final int targetIdx = vertexToIndex.get(transition.getValue());
				final int from = transition.getKey().fromExclusive;
				final int to = transition.getKey().toInclusive;
				final int weight = transition.getKey().weight;
				out.writeInt(from);// from
				out.writeInt(to);// to
				out.writeInt(targetIdx);// target
				out.writeInt(weight);// weight
				out.writeUTF(transition.getKey().out.toUnicodeString());// out
			}

		}
		final int initNumber = g.allInitialEdges().size();
		out.writeInt(initNumber);
		for (Entry<E, N> inVertex : (Iterable<Entry<E, N>>) () -> g.iterateInitialEdges()) {
			final N vertex = inVertex.getValue();
			final int idx = vertexToIndex.get(vertex);
			final E edge = inVertex.getKey();
			final int from = edge.fromExclusive;
			final int to = edge.toInclusive;
			final int initWeight = edge.weight;
			out.writeInt(from); // from
			out.writeInt(to); // to
			out.writeInt(idx); // target
			out.writeInt(initWeight);// weight
			out.writeUTF(edge.out.toUnicodeString());// out
		}
		for (Entry<N, Integer> vertexIdx : vertexToIndex.entrySet()) {
			final N vertex = vertexIdx.getKey();
			final int idx = vertexIdx.getValue();
			final P finEdge = g.getFinalEdge(vertex);
			if (finEdge != null) {
				final int finalWeight = finEdge.weight;
				out.writeInt(idx); // source
				out.writeInt(finalWeight);// weight
				out.writeUTF(finEdge.out.toUnicodeString());// out
			}
		}
	}

	public G decompressBinary(Pos meta, DataInputStream in) throws IOException {
		final G g = createEmptyGraph();
		final int size = in.readInt();// size
		final ArrayList<N> indexToVertex = Specification.filledArrayListFunc(size, i -> g.create(meta));
		final boolean isEpsilon = in.readBoolean();
		if (isEpsilon) {
			final int epsWeight = in.readInt();// weight
			final IntSeq epsOut = new IntSeq(in.readUTF());// out
			final P eps = createPartialEdge(epsOut, epsWeight);
			g.setEpsilon(eps);
		}
		for (N vertex : indexToVertex) {
			final int transitionNumber = in.readInt();
			for (int i = 0; i < transitionNumber; i++) {
				final int from = in.readInt();// from
				final int to = in.readInt();// to
				final int targetIdx = in.readInt();// target
				final int weight = in.readInt();// weight
				final IntSeq out = new IntSeq(in.readUTF());// out
				final E edge = createFullEdge(from, to, createPartialEdge(out, weight));
				g.add(vertex, edge, indexToVertex.get(targetIdx));
			}

		}
		final int initNumber = in.readInt();
		for (int i = 0; i < initNumber; i++) {
			final int from = in.readInt(); // from
			final int to = in.readInt(); // to
			final int idx = in.readInt(); // target
			final N initState = indexToVertex.get(idx);
			final int initWeight = in.readInt();// weight
			final IntSeq out = new IntSeq(in.readUTF());// out
			final E edge = createFullEdge(from, to, createPartialEdge(out, initWeight));
			g.addInitialEdge(initState, edge);
		}
		while (in.available() > 0) {
			final int idx = in.readInt(); // source
			final N vertex = indexToVertex.get(idx);
			final int finalWeight = in.readInt();// weight
			final IntSeq out = new IntSeq(in.readUTF());// out
			final P edge = createPartialEdge(out, finalWeight);
			g.setFinalEdge(vertex, edge);
		}
		return g;
	}

	public G subtract(G lhs, G rhs) {
		return subtract(optimiseGraph(lhs), optimiseGraph(rhs));
	}

	/**
	 * Subtracts one transducer from another. It performs language difference on the
	 * input languages. The output language stays the same as in the lhs automaton.
	 * In other words, if left automaton accepts and right one doesn't, then the the
	 * output of left automaton is printed. If both automata accepts then no output
	 * is printed. If left automaton doesn't accept then no output is printed.
	 */
	public G subtract(RangedGraph<Pos, Integer, E, P> lhs, RangedGraph<Pos, Integer, E, P> rhs) {
		final Pair<G, HashMap<IntPair, ? extends StateProduct<N>>> g = product(lhs, rhs, (lv, rv) -> lv,
				(fromExclusive, toInclusive, le, re) -> le == null ? null
						: new E(fromExclusive, toInclusive, le.out, le.weight),
				(finL, finR) -> finR == null ? finL : null);
		final HashSet<N> states = new HashSet<>(g.r().size());
		for (StateProduct<N> state : g.r().values()) {
			states.add(state.product());
		}
		trim(g.l(), states, states::contains);
		return g.l();
	}

	public G compose(G lhs, G rhs, Pos pos) {
		class Ref {
			int maxWeightRhs = Integer.MIN_VALUE;
		}
		final Ref ref = new Ref();
		final RangedGraph<Pos, Integer, E, P> rhsOptimal = optimiseGraph(rhs, n -> null, (e, n) -> {
			if (n.weight > ref.maxWeightRhs)
				ref.maxWeightRhs = n.weight;
			return null;
		});
		return compose(lhs, rhsOptimal, ref.maxWeightRhs, pos);
	}

	public G compose(G lhs, RangedGraph<Pos, Integer, E, P> rhs, int maxRhsWeight, Pos pos) {
		assert isStronglyFunctional(rhs, rhs.initial) == null : isStronglyFunctional(rhs, rhs.initial) + " " + rhs;
		return compose(lhs, rhs, pos, IntSeq::iterator, (l, r) -> l * maxRhsWeight + r,
				(prev, rhsTransTaken, lhsOutSymbol) -> {
					assert rhsTransTaken != null;
					if (rhsTransTaken.edge == null) {// sink
						return Pair.of(weightNeutralElement(), prev.r());
					}
					final IntSeq outPrev = prev.r();
					final IntSeq outNext = rhsTransTaken.edge.out;
					final int[] outJoined = new int[outPrev.size() + outNext.size()];
					for (int i = 0; i < outPrev.size(); i++)
						outJoined[i] = outPrev.get(i);
					for (int i = 0; i < outNext.size(); i++)
						outJoined[outPrev.size() + i] = outNext.get(i).equals(reflect()) ? lhsOutSymbol
								: outNext.get(i);
					return Pair.of(rhsTransTaken.edge.weight, new IntSeq(outJoined));
				}, (p, initial) -> {
					final BacktrackingHead head = evaluate(rhs, initial, p.out.iterator());
					return head == null ? null : Pair.of(head.finalEdge.weight, head.collect(p.out, minimal()));
				}, (a, b) -> {
					if (a == null)
						return b;
					if (b == null)
						return a;
					return a.l() > b.l() ? a : b;// this assumes that both automata are strongly functional
				});
	}

	public void inverse(G g) throws CompilationError {
		inverse(g, Pos.NONE, IntSeq::new, IntSeq::iteratorReversed, p -> p.out, p -> p.weight,
				(sourceState, conflictingEdges) -> {
					Pair<N, P> firstHighestWeightState = null;
					Pair<N, P> secondHighestWeightState = null;
					int firstHighestWeightValue = Integer.MIN_VALUE;
					int secondHighestWeightValue = Integer.MIN_VALUE;
					for (Pair<N, P> stateAndWeight : conflictingEdges) {
						final P fin = stateAndWeight.r();
						if (fin.weight >= firstHighestWeightValue) {
							secondHighestWeightValue = firstHighestWeightValue;
							secondHighestWeightState = firstHighestWeightState;
							firstHighestWeightValue = fin.weight;
							firstHighestWeightState = stateAndWeight;
						}
					}
					if (firstHighestWeightState != null && secondHighestWeightValue == firstHighestWeightValue) {
						assert secondHighestWeightState != null;
						throw new CompilationError.AmbiguousAcceptingState(g.getState(sourceState),
								g.getState(firstHighestWeightState.l()), g.getState(secondHighestWeightState.l()),
								firstHighestWeightState.r(), secondHighestWeightState.r());

					}
					return firstHighestWeightState.r();
				}, new InvertionErrorCallback<N, E, P, IntSeq>() {
					@Override
					public void doubleReflectionOnOutput(N vertex, E edge) throws CompilationError {
						throw new CompilationError.DoubleReflectionOnOutput(g.getState(vertex), edge);
					}

					@Override
					public void rangeWithoutReflection(N target, E edge) throws CompilationError {
						throw new CompilationError.RangeWithoutReflection(g.getState(target), edge);
					}

					@Override
					public void epsilonTransitionCycle(N state, IntSeq output, IntSeq output1) throws CompilationError {
						throw new CompilationError.EpsilonTransitionCycle(g.getState(state), output, output1);
					}
				});
	}

	/**
	 * Makes all transitions return the exact same output as is their input. If
	 * input is a single symbol, then the output is the same symbol. If input is a
	 * range, then output is reflected (output is the
	 * {@link Specification#minimal()} element)
	 */
	public void identity(G g) {
		final IntSeq REFLECT = new IntSeq(reflect());
		mutateEdges(g, edge -> {
			if (edge.fromExclusive + 1 == edge.toInclusive) {
				edge.out = new IntSeq(edge.toInclusive);
			} else {
				edge.out = REFLECT;
			}
		});
		g.mutateAllFinalEdges((fin, edge) -> fin.out = IntSeq.Epsilon);
		if(g.getEpsilon()!=null)g.getEpsilon().out = IntSeq.Epsilon;
	}

	/**
	 * Sets output of all edges to empty string
	 */
	public void clearOutput(G g) {
		mutateEdges(g, edge -> {
			edge.out = IntSeq.Epsilon;
		});
		g.mutateAllFinalEdges((fin, edge) -> fin.out = IntSeq.Epsilon);
		if(g.getEpsilon()!=null)g.getEpsilon().out = IntSeq.Epsilon;
	}



	public AdvAndDelState<Integer, IntQueue> areEquivalent(RangedGraph<?, Integer, E, P> lhs, RangedGraph<?, Integer, E, P> rhs) {
		return areEquivalent(lhs,rhs,e -> IntQueue.asQueue(e.getOut()), p -> IntQueue.asQueue(p.getOut()), IntQueue::new);
	}

	public <V> RangedGraph<V,Integer,E,P> compileOSTIA(OSTIA.State init,
													   Function<Integer, Integer> indexToSymbol,
													   Function<IntSeq, V> shortestAsMeta ) {
		return compileOSTIA(init,indexToSymbol,(in,out)->new E(in-1,in,out,0),i->new IntSeq(i).mapLinear(indexToSymbol),shortestAsMeta);
	}
	public G compileIntermediateOSTIA(OSTIA.State init,
													   Function<Integer, Integer> indexToSymbol,
													   Function<IntSeq, Pos> shortestAsMeta ) {
		return compileIntermediateOSTIA(init,indexToSymbol,(in,out)->new E(in-1,in,out,0), IntSeq::new,shortestAsMeta);
	}
}
