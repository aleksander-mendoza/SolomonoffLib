package net.alagris;

import net.alagris.CompilationError.WeightConflictingToThirdState;
import net.alagris.LexUnicodeSpecification.*;
import net.alagris.LexUnicodeSpecification.LexPipeline.AutomatonNode;
import net.alagris.LexUnicodeSpecification.LexPipeline.Node;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.visualization.Visualization;
import net.automatalib.words.Alphabet;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.NoViableAltException;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static net.alagris.LexUnicodeSpecification.*;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class OptimisedLexTransducer<N, G extends IntermediateGraph<Pos, E, P, N>> {
	public final LexUnicodeSpecification<N, G> specs;
	public final ParserListener<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> listener;
	public final SolomonoffGrammarParser parser;

	public OptimisedLexTransducer(LexUnicodeSpecification<N, G> specs) throws CompilationError {
		this.specs = specs;
		addExternalRPNI(specs);
		addExternalRPNI_EDSM(specs);
		addExternalRPNI_EMDL(specs);
		addExternalRPNI_Mealy(specs);
		addExternalDict(specs);
		addExternalImport(specs);
		addExternalStringFile(specs);
		addExternalDropEpsilon(specs);
		addExternalCompose(specs);
		addExternalInverse(specs);
		addExternalSubtract(specs);
		addExternalLongerMatchesHigherWeights(specs);
		addExternalReweight(specs);
		addExternalRandom(specs);
		addExternalClearOutput(specs);
		addExternalIdentity(specs);
		addExternalOSTIA(specs);
		listener = specs.makeParser();
		listener.addDotAndHashtag();
		parser = ParserListener.makeParser(null);
	}

	public void setInput(CharStream source) {
		parser.setTokenStream(new CommonTokenStream(new SolomonoffGrammarLexer(source)));
	}

	public void parse(CharStream source) throws CompilationError {
		setInput(source);
		listener.runCompiler(parser);
	}

	public void parseREPL(CharStream source) throws CompilationError {
		setInput(source);
		listener.runREPL(parser);
	}

	public void checkStrongFunctionality() throws CompilationError {
		checkStrongFunctionalityOfPipelines();
		checkStrongFunctionalityOfVariables();
	}

	public void checkStrongFunctionalityOfVariables() throws CompilationError {
		for (Var<N, G> var : specs.variableAssignments.values()) {
			specs.checkStrongFunctionality(specs.getOptimised(var), var.pos);
		}
	}

	public void checkStrongFunctionalityOfPipelines() throws CompilationError {
		for (LexPipeline<N, G> pipes : specs.pipelines.values()) {
			for (Node pipe : pipes.nodes) {
				if (pipe instanceof AutomatonNode) {
					AutomatonNode n = (AutomatonNode) pipe;
					specs.checkStrongFunctionality(n.g, n.pos);
				}
			}
		}
	}

	public String run(String name, String input) {
		final IntSeq out = run(name, new IntSeq(input));
		return out == null ? null : out.toUnicodeString();
	}

	public IntSeq run(String name, IntSeq input) {
		return specs.evaluate(getOptimalTransducer(name), input);
	}

	public Var<N, G> getTransducer(String id) {
		// Parsing is already over, so the user might as well mutate it and nothing bad
		// will happen
		// All variables that were meant to be used as building blocks for other
		// transducers have
		// already been either copied or consumed. In the worst case, user might just
		// try to get consumed
		// variable and get null.
		return specs.borrowVariable(id);
	}

	public void visualize(String id) throws WeightConflictingToThirdState {
		LearnLibCompatibility.visualize(getOptimisedTransducer(id));
	}

	public RangedGraph<Pos, Integer, E, P> getOptimalTransducer(String name) {
		final Var<N, G> v = specs.borrowVariable(name);
		return v == null ? null : v.getOptimal();
	}

	public RangedGraph<Pos, Integer, E, P> getOptimisedTransducer(String name)
			throws CompilationError.WeightConflictingToThirdState {
		final Var<N, G> v = specs.borrowVariable(name);
		return v == null ? null : specs.getOptimised(v);
	}

	/**
	 * @param name should not contain the @ sign as it is already implied by this
	 *             methods
	 */
	public LexPipeline<N, G> getPipeline(String name) {
		return specs.getPipeline(name);
	}

	public static ExternalPipelineFunction makeEmptyExternalPipelineFunction() {
		return (a, b) -> s -> s;
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalDict(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("dict", (pos, text) -> spec.loadDict(Specification.fromIterable(text), pos));
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("rpni",
				(pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpni(text)));
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EDSM(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("rpni_edsm",
				(pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniEDSM(text)));
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EMDL(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("rpni_mdl",
				(pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniMDL(text)));
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_Mealy(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("rpni_mealy", (pos, text) -> {
			Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> alphAndMealy = LearnLibCompatibility
					.rpniMealy(text);
			G g = LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.l(), alphAndMealy.r(), s -> pos,
					(in, out) -> spec.createFullEdgeOverSymbol(in, spec.createPartialEdge(new IntSeq(out), 0)),
					s -> new P(IntSeq.Epsilon, 0));
			return g;
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalOSTIA(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("ostia", (pos, text) -> {
			final HashMap<Integer, Integer> symbolToIndex = new HashMap<>();
			LearnLibCompatibility.inferAlphabet(text.iterator(), symbolToIndex);
			final int[] indexToSymbol = new int[symbolToIndex.size()];
			for (Map.Entry<Integer, Integer> e : symbolToIndex.entrySet()) {
				indexToSymbol[e.getValue()] = e.getKey();
			}
			final Iterator<Pair<IntSeq, IntSeq>> mapped = LearnLibCompatibility.mapSymbolsToIndices(text.iterator(),
					symbolToIndex);
			final OSTIA.State ptt = OSTIA.buildPtt(symbolToIndex.size(), mapped);
			OSTIA.ostia(ptt);
			return spec.compileIntermediateOSTIA(ptt, i -> indexToSymbol[i], x -> pos);
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalInverse(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("inverse", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 1);
			spec.inverse(automata.get(0));
			return automata.get(0);
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalCompose(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("compose", (pos, automata) -> {
			if (automata.size() <= 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 2);
			final Iterator<G> iter = automata.iterator();
			G composed = iter.next();
			while (iter.hasNext()) {
				composed = spec.compose(composed, iter.next(), pos);
			}
			return composed;
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalSubtract(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("subtract", (pos, automata) -> {
			if (automata.size() != 2)
				throw new CompilationError.IllegalOperandsNumber(automata, 2);
			return spec.subtract(automata.get(0), automata.get(1));
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalLongerMatchesHigherWeights(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("longerMatchesHigherWeights", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 1);
			final G g = automata.get(0);
			final int[] weight = new int[] { spec.weightNeutralElement() };
			final N init = g.makeUniqueInitialState(Pos.NONE);
			spec.collect(false, g, init, new HashSet<>(), n -> {
				final P fin = g.getFinalEdge(n);
				if (fin != null)
					fin.weight = weight[0]++;
				return null;
			}, (n, e) -> null);
			g.useStateOutgoingEdgesAsInitial(init);
			return g;
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalReweight(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("reweight", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 1);
			final G g = automata.get(0);
			final HashSet<N> vertices = g.collectVertexSet(new HashSet<>(), n -> {
				g.setColor(n, new ArrayList<E>());
				return null;
			}, (n, e) -> null);
			for (Map.Entry<E, N> init : (Iterable<Map.Entry<E, N>>) g::iterateInitialEdges) {
				ArrayList<E> incoming = (ArrayList<E>) g.getColor(init.getValue());
				incoming.add(init.getKey());
			}
			for (N vertex : vertices) {
				for (Map.Entry<E, N> outgoing : (Iterable<Map.Entry<E, N>>) () -> g.iterator(vertex)) {
					final ArrayList<E> incoming = (ArrayList<E>) g.getColor(outgoing.getValue());
					incoming.add(outgoing.getKey());
				}
			}
			for (N vertex : vertices) {
				final ArrayList<E> incoming = (ArrayList<E>) g.getColor(vertex);
				incoming.sort(Comparator.comparingInt(E::getWeight));
				int weight = spec.weightNeutralElement();
				for (E edge : incoming) {
					edge.weight = weight++;
				}
				g.setColor(vertex, null);
			}
			return g;
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalDropEpsilon(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("dropEpsilon", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 1);
			final G g = automata.get(0);
			g.setEpsilon(null);
			return g;
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRandom(
			LexUnicodeSpecification<N, G> spec) {
		final IntSeq MAX_STATES = new IntSeq("maxStates");
		final IntSeq MIN_INPUT = new IntSeq("minInputExcl");
		final IntSeq MAX_INPUT = new IntSeq("maxInputIncl");
		final IntSeq MIN_OUTPUT = new IntSeq("minOutputExcl");
		final IntSeq MAX_OUTPUT = new IntSeq("maxOutputIncl");
		final IntSeq MIN_LEN_OUTPUT = new IntSeq("minOutputLenIncl");
		final IntSeq MAX_LEN_OUTPUT = new IntSeq("maxOutputLenExcl");
		final IntSeq MAX_TRANS = new IntSeq("maxOutgoingTransitions");
		final IntSeq PARTIALITY = new IntSeq("partialityFactor");
		final IntSeq RAND_SEED = new IntSeq("randomSeed");
		spec.registerExternalFunction("randomDFA", (pos, text) -> {
			int maxStates = 20;
			int minInputExcl = 'a';
			int maxInputIncl = 'c';
			int minOutputExcl = 'a';
			int maxOutputIncl = 'c';
			int minOutputLenIncl = 0;
			int maxOutputLenExcl = 4;
			int maxTrans = 5;
			long randomSeed = System.currentTimeMillis();
			double partiality = 0;
			for (Pair<IntSeq, IntSeq> t : text) {
				if (t.r() != null) {
					if (t.l().equals(MAX_STATES)) {
						maxStates = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MIN_INPUT)) {
						minInputExcl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MAX_INPUT)) {
						maxInputIncl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MIN_LEN_OUTPUT)) {
						minOutputLenIncl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MAX_LEN_OUTPUT)) {
						maxOutputLenExcl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MIN_OUTPUT)) {
						minOutputExcl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MAX_OUTPUT)) {
						maxOutputIncl = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(MAX_TRANS)) {
						maxTrans = Integer.parseInt(t.r().toUnicodeString());
					} else if (t.l().equals(PARTIALITY)) {
						partiality = Double.parseDouble(t.r().toUnicodeString());
					} else if (t.l().equals(RAND_SEED)) {
						randomSeed = Long.parseLong(t.r().toUnicodeString());
					}
				}
			}
			final Random rnd = new Random(randomSeed);
			final int minInput = minInputExcl;
			final int maxInput = maxInputIncl;
			final int minOutput = minOutputExcl;
			final int maxOutput = maxOutputIncl;
			final int minOutputLen = minOutputLenIncl;
			final int maxOutputLen = maxOutputLenExcl;
			return spec.randomDeterministic(maxStates, maxTrans, partiality,
					() -> minInput + 1 + rnd.nextInt(maxInput - minInput),
					(fromExclusive, toInclusive) -> new E(fromExclusive, toInclusive,
							IntSeq.rand(minOutputLen, maxOutputLen, minOutput + 1, maxOutput + 1, rnd), 0),
					() -> Pair.of(new P(IntSeq.rand(minOutputLen, maxOutputLen, minOutput + 1, maxOutput + 1, rnd), 0),
							pos),
					rnd);
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalIdentity(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("identity", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 2);
			spec.identity(automata.get(0));
			return automata.get(0);
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalClearOutput(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalOperation("clearOutput", (pos, automata) -> {
			if (automata.size() != 1)
				throw new CompilationError.IllegalOperandsNumber(automata, 2);
			spec.clearOutput(automata.get(0));
			return automata.get(0);
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalStringFile(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("stringFile", (pos, text) -> {
			if (text.size() != 1)
				throw new CompilationError.IllegalInformantSize(text, 1);
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(text.get(0).l().toUnicodeString())))) {
				return spec.loadDict(() -> {
					try {
						final String line = in.readLine();
						if (line == null)
							return null;
						final int tab = line.indexOf('\t');
						return Pair.of(new IntSeq(line.subSequence(0, tab)),
								new IntSeq(line.subSequence(tab + 1, line.length())));
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				}, pos);
			} catch (IOException e) {
				throw new CompilationError.ParseException(pos, e);
			}
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalImport(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("import", (pos, text) -> {
			if (text.size() != 1)
				throw new CompilationError.IllegalInformantSize(text, 1);
			try (FileInputStream stream = new FileInputStream(text.get(0).l().toUnicodeString())) {
				return spec.decompressBinary(pos, new DataInputStream(stream));
			} catch (IOException e) {
				throw new CompilationError.ParseException(pos, e);
			}
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> G dfaToIntermediate(LexUnicodeSpecification<N, G> spec,
			Pos pos, Pair<Alphabet<Integer>, DFA<?, Integer>> alphAndDfa) {
		return LearnLibCompatibility.dfaToIntermediate(spec, alphAndDfa.l(), alphAndDfa.r(), s -> pos,
				spec::fullNeutralEdgeOverSymbol, s -> spec.partialNeutralEdge());
	}

	private final static Random RAND = new Random();

	public interface ReplCommand<Result> {
		Result run(OptimisedHashLexTransducer compiler, Consumer<String> log, Consumer<String> debug, String args);
	}

	public static final ReplCommand<String> REPL_LOAD = (compiler, log, debug, args) -> {
		try {
			final long parsingBegin = System.currentTimeMillis();
			compiler.parse(CharStreams.fromFileName(args));
			debug.accept("Parsing took " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
			final long optimisingBegin = System.currentTimeMillis();
			debug.accept("Optimising took " + (System.currentTimeMillis() - optimisingBegin) + " miliseconds");
//            final long ambiguityCheckingBegin = System.currentTimeMillis();
//            compiler.checkStrongFunctionality();
//            debug.accept("Checking ambiguity " + (System.currentTimeMillis() - ambiguityCheckingBegin) + " miliseconds");
			final long typecheckingBegin = System.currentTimeMillis();
			debug.accept("Typechecking took " + (System.currentTimeMillis() - typecheckingBegin) + " miliseconds");
			debug.accept("Total time " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
			return null;
		} catch (CompilationError | IOException e) {
			return e.toString();
		}
	};

	public static final ReplCommand<String> REPL_LIST = (compiler, logs, debug,
			args) -> compiler.specs.variableAssignments.keySet().toString();
	public static final ReplCommand<String> REPL_SIZE = (compiler, logs, debug, args) -> {
		try {
			RangedGraph<Pos, Integer, E, P> r = compiler.getOptimisedTransducer(args);
			return r == null ? "No such function!" : String.valueOf(r.size());
		} catch (CompilationError e) {
			return e.toString();
		}
	};
	public static final ReplCommand<String> REPL_EVAL = (compiler, logs, debug, args) -> {
		try {
			final String[] parts = args.split("\\s+", 2);
			if (parts.length != 2)
				return "Two arguments required 'transducerName' and 'transducerInput' but got "
						+ Arrays.toString(parts);
			final String transducerName = parts[0].trim();
			final String transducerInput = parts[1].trim();
			final long evaluationBegin = System.currentTimeMillis();
			final RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
			if (graph == null)
				return "Transducer '" + transducerName + "' not found!";
			final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
			final IntSeq output = compiler.specs.evaluate(graph, input);
			final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
			debug.accept("Took " + evaluationTook + " miliseconds");
			return output == null ? "No match!" : output.toStringLiteral();
		} catch (WeightConflictingToThirdState e) {
			return e.toString();
		}
	};
	public static final ReplCommand<String> REPL_RUN = (compiler, logs, debug, args) -> {
		final String[] parts = args.split("\\s+", 2);
		if (parts.length != 2)
			return "Two arguments required 'transducerName' and 'transducerInput' but got " + Arrays.toString(parts);
		final String pipelineName = parts[0].trim();
		if (!pipelineName.startsWith("@")) {
			return "Pipeline names must start with @";
		}
		final String pipelineInput = parts[1].trim();
		final long evaluationBegin = System.currentTimeMillis();
		final LexPipeline<net.alagris.HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> pipeline = compiler
				.getPipeline(pipelineName.substring(1));
		if (pipeline == null)
			return "Pipeline '" + pipelineName + "' not found!";
		final IntSeq input = ParserListener.parseCodepointOrStringLiteral(pipelineInput);
		final IntSeq output = pipeline.evaluate(input);
		final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
		debug.accept("Took " + evaluationTook + " miliseconds");
		return output == null ? "No match!" : output.toStringLiteral();
	};
	public static final ReplCommand<String> REPL_EXPORT = (compiler, logs, debug, args) -> {
		Var<net.alagris.HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = compiler
				.getTransducer(args);
		try (FileOutputStream f = new FileOutputStream(args + ".star")) {
			compiler.specs.compressBinary(g.graph, new DataOutputStream(new BufferedOutputStream(f)));
			return null;
		} catch (IOException e) {
			return e.toString();
		}
	};

	public static final ReplCommand<String> REPL_IS_DETERMINISTIC = (compiler, logs, debug, args) -> {
		try {
			RangedGraph<Pos, Integer, E, P> r = compiler.getOptimisedTransducer(args);
			if (r == null)
				return "No such function!";
			return r.isDeterministic() == null ? "true" : "false";
		} catch (CompilationError e) {
			return e.toString();
		}
	};
	public static final ReplCommand<String> REPL_LIST_PIPES = (compiler, logs, debug, args) -> {
		return Specification.fold(compiler.specs.pipelines.keySet(), new StringBuilder(),
				(pipe, sb) -> sb.append("@").append(pipe).append(", ")).toString();
	};
	public static final ReplCommand<String> REPL_EQUAL = (compiler, logs, debug, args) -> {
		try {
			final String[] parts = args.split("\\s+", 2);
			if (parts.length != 2)
				return "Two arguments required 'transducerName' and 'transducerInput' but got "
						+ Arrays.toString(parts);
			final String transducer1 = parts[0].trim();
			final String transducer2 = parts[1].trim();
			RangedGraph<Pos, Integer, E, P> r1 = compiler.getOptimisedTransducer(transducer1);
			RangedGraph<Pos, Integer, E, P> r2 = compiler.getOptimisedTransducer(transducer2);
			if (r1 == null)
				return "No such transducer '" + transducer1 + "'!";
			if (r2 == null)
				return "No such transducer '" + transducer2 + "'!";
			final AdvAndDelState<Integer, IntQueue> counterexample = compiler.specs.areEquivalent(r1, r2);
			if (counterexample == null)
				return "true";
			return "false";
		} catch (CompilationError e) {
			return e.toString();
		}
	};
	public static final ReplCommand<String> REPL_RAND_SAMPLE = (compiler, logs, debug, args) -> {
		try {
			final String[] parts = args.split("\\s+", 4);
			if (parts.length != 3) {
				return "Three arguments required: 'transducerName', 'mode' and 'size'";
			}
			final String transducerName = parts[0].trim();
			final String mode = parts[1];
			final int param = Integer.parseInt(parts[2].trim());
			final RangedGraph<Pos, Integer, E, P> transducer = compiler.getOptimisedTransducer(transducerName);
			if (mode.equals("of_size")) {
				final int sampleSize = param;
				compiler.specs.generateRandomSampleOfSize(transducer, sampleSize, RAND, (backtrack, finalState) -> {
					final LexUnicodeSpecification.BacktrackingHead head = new LexUnicodeSpecification.BacktrackingHead(
							backtrack, transducer.getFinalEdge(finalState));
					final IntSeq in = head.randMatchingInput(RAND);
					final IntSeq out = head.collect(in, compiler.specs.minimal());
					logs.accept(in.toStringLiteral() + ":" + out.toStringLiteral());
				}, x -> {
				});
				return null;
			} else if (mode.equals("of_length")) {
				final int maxLength = param;
				compiler.specs.generateRandomSampleBoundedByLength(transducer, maxLength, 10, RAND,
						(backtrack, finalState) -> {
							final LexUnicodeSpecification.BacktrackingHead head = new LexUnicodeSpecification.BacktrackingHead(
									backtrack, transducer.getFinalEdge(finalState));
							final IntSeq in = head.randMatchingInput(RAND);
							final IntSeq out = head.collect(in, compiler.specs.minimal());
							logs.accept(in.toStringLiteral() + ":" + out.toStringLiteral());
						}, x -> {
						});
				return null;
			} else {
				return "Choose one of the generation modes: 'of_size' or 'of_length'";
			}

		} catch (WeightConflictingToThirdState | NumberFormatException e) {
			return e.toString();
		}
	};
	public static final ReplCommand<String> REPL_VISUALIZE = (compiler, logs, debug, args) -> {
		try {
			compiler.visualize(args);
			return null;
		} catch (CompilationError e) {
			return e.toString();
		}
	};

	public static class OptimisedHashLexTransducer
			extends OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> {

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(boolean eagerMinimisation, boolean eagerCopy, boolean eagerFunctionalityChecks,int minimalSymbol,
				int maximalSymbol, ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			super(new HashMapIntermediateGraph.LexUnicodeSpecification(eagerMinimisation, eagerCopy, eagerFunctionalityChecks,minimalSymbol,
					maximalSymbol, externalPipelineFunction));
		}

		public OptimisedHashLexTransducer(boolean eagerMinimisation, boolean eagerCopy, int minimalSymbol,
				int maximalSymbol, ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(eagerMinimisation, eagerCopy,true, minimalSymbol, maximalSymbol, externalPipelineFunction);
		}

		public OptimisedHashLexTransducer(boolean eagerMinimisation, int minimalSymbol, int maximalSymbol,
				ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(eagerMinimisation, false, minimalSymbol, maximalSymbol, externalPipelineFunction);
		}
		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation, boolean eagerCopy, ExternalPipelineFunction externalPipelineFunction)
				throws CompilationError {
			this(eagerMinimisation, eagerCopy, minimalSymbol, maximalSymbol, externalPipelineFunction);
			parse(source);
//            checkStrongFunctionality();
		}

		public OptimisedHashLexTransducer(int minimalSymbol, int maximalSymbol, boolean eagerMinimisation,
				boolean eagerCopy) throws CompilationError {
			this(eagerMinimisation, eagerCopy, minimalSymbol, maximalSymbol, makeEmptyExternalPipelineFunction());
		}
		public OptimisedHashLexTransducer(int minimalSymbol, int maximalSymbol, boolean eagerMinimisation,
				boolean eagerCopy, boolean eagerFunctionalityChecks) throws CompilationError {
			this(eagerMinimisation, eagerCopy, eagerFunctionalityChecks, minimalSymbol, maximalSymbol, makeEmptyExternalPipelineFunction());
		}
		public OptimisedHashLexTransducer(CharStream source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation, ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(source, minimalSymbol, maximalSymbol, eagerMinimisation, false, externalPipelineFunction);
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(String source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation, boolean eagerCopy, ExternalPipelineFunction externalPipelineFunction)
				throws CompilationError {
			this(CharStreams.fromString(source), minimalSymbol, maximalSymbol, eagerMinimisation, eagerCopy,
					externalPipelineFunction);
		}

		public OptimisedHashLexTransducer(int minimalSymbol, int maximalSymbol) throws CompilationError {
			this(minimalSymbol, maximalSymbol, true);
		}

		public OptimisedHashLexTransducer(int minimalSymbol, int maximalSymbol, boolean eagerMinimisation)
				throws CompilationError {
			this(eagerMinimisation, minimalSymbol, maximalSymbol, makeEmptyExternalPipelineFunction());
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation, boolean eagerCopy) throws CompilationError {
			this(source, minimalSymbol, maximalSymbol, eagerMinimisation, eagerCopy,
					makeEmptyExternalPipelineFunction());
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(String source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation, boolean eagerCopy) throws CompilationError {
			this(source, minimalSymbol, maximalSymbol, eagerMinimisation, eagerCopy,
					makeEmptyExternalPipelineFunction());
		}

		public OptimisedHashLexTransducer(String source, int minimalSymbol, int maximalSymbol,
				boolean eagerMinimisation) throws CompilationError {
			this(source, minimalSymbol, maximalSymbol, eagerMinimisation, false);
		}
	}

	public static class Repl {
		private static class CmdMeta<Result> {
			final ReplCommand<Result> cmd;
			final String help;

			private CmdMeta(ReplCommand<Result> cmd, String help) {
				this.cmd = cmd;
				this.help = help;
			}
		}

		private final HashMap<String, CmdMeta<String>> commands = new HashMap<>();
		private final OptimisedHashLexTransducer compiler;

		public ReplCommand<String> registerCommand(String name, String help, ReplCommand<String> cmd) {
			final CmdMeta<String> prev = commands.put(name, new CmdMeta<>(cmd, help));
			return prev == null ? null : prev.cmd;
		}

		public Repl(OptimisedHashLexTransducer compiler) {
			this.compiler = compiler;
			registerCommand("exit", "Exits REPL", (a, b, d, c) -> "");
			registerCommand("load", "Loads source code from file", REPL_LOAD);
			registerCommand("pipes", "Lists all currently defined pipelines", REPL_LIST_PIPES);
			registerCommand("run", "Runs pipeline for the given input", REPL_RUN);
			registerCommand("ls", "Lists all currently defined transducers", REPL_LIST);
			registerCommand("size", "Size of transducer is the number of its states", REPL_SIZE);
			registerCommand("equal",
					"Tests if two DETERMINISTIC transducers are equal. Does not work with nondeterministic ones!",
					REPL_EQUAL);
			registerCommand("is_det", "Tests whether transducer is deterministic", REPL_IS_DETERMINISTIC);
			registerCommand("export", "Exports transducer to STAR (Subsequential Transducer ARchie) binary file",
					REPL_EXPORT);
			registerCommand("eval", "Evaluates transducer on requested input", REPL_EVAL);
			registerCommand("rand_sample", "Generates random sample of input:output pairs produced by ths transducer",
					REPL_RAND_SAMPLE);
			registerCommand("vis", "Visualizes transducer as a graph", REPL_VISUALIZE);
		}

		public String run(String line, Consumer<String> log, Consumer<String> debug) {
			if (line.startsWith(":")) {
				final int space = line.indexOf(' ');
				final String firstWord;
				final String remaining;
				if (space >= 0) {
					firstWord = line.substring(1, space);
					remaining = line.substring(space + 1);
				} else {
					firstWord = line.substring(1);
					remaining = "";
				}
				if (firstWord.startsWith("?")) {
					final String noQuestionmark = firstWord.substring(1);
					if (noQuestionmark.isEmpty()) {
						final StringBuilder sb = new StringBuilder();
						for (Map.Entry<String, CmdMeta<String>> cmd : commands.entrySet()) {
							final String name = cmd.getKey();
							sb.append(":").append(name).append("\t").append(cmd.getValue().help).append("\n");
						}
						return sb.toString();
					} else {
						final CmdMeta<String> cmd = commands.get(noQuestionmark);
						return cmd.help;
					}
				} else {
					final CmdMeta<String> cmd = commands.get(firstWord);
					return cmd.cmd.run(compiler, log, debug, remaining);
				}

			} else {
				try {
					compiler.parseREPL(CharStreams.fromString(line));
					return null;
				} catch (CompilationError | NoViableAltException | EmptyStackException e) {
					return e.toString();
				}
			}
		}
	}
}
