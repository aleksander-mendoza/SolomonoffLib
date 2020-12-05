package net.alagris;

import net.alagris.LexUnicodeSpecification.*;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.github.jamm.MemoryMeter;

import java.io.*;
import java.util.Iterator;
import java.util.Scanner;

import static net.alagris.LexUnicodeSpecification.*;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class CLI {

	public static class OptimisedLexTransducer<N, G extends IntermediateGraph<Pos, E, P, N>> {
		public final LexUnicodeSpecification<N, G> specs;
		final ParserListener<LexPipeline<N, G>, Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> parser;

		public OptimisedLexTransducer(LexUnicodeSpecification<N, G> specs) throws CompilationError {
			this.specs = specs;
			addExternalRPNI(specs);
			addExternalRPNI_EDSM(specs);
			addExternalRPNI_EMDL(specs);
			addExternalRPNI_Mealy(specs);
			addExternalDict(specs);
			addExternalImport(specs);
			addExternalStringFile(specs);
			addExternalCompose(specs);
			addExternalInverse(specs);
			addExternalSubtract(specs);
			addExternalClearOutput(specs);
			addExternalIdentity(specs);
			parser = specs.makeParser();
			parser.addDotAndHashtag();
		}

		public void parse(CharStream source) throws CompilationError {
			parser.parse(source);
		}

		public void checkStrongFunctionality() throws CompilationError {
			for (Var<N, G> var : specs.variableAssignments.values()) {
				specs.checkStrongFunctionality(specs.getOptimised(var));
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

		public void visualize(String id) {
			LearnLibCompatibility.visualize(getTransducer(id).graph, Pos.NONE, Pos.NONE);
		}

		public RangedGraph<Pos, Integer, E, P> getOptimalTransducer(String name) {
			final Var<N, G> v = specs.borrowVariable(name);
			return v==null?null: v.getOptimal();
		}

		public RangedGraph<Pos, Integer, E, P> getOptimisedTransducer(String name) throws CompilationError.WeightConflictingToThirdState {
			final Var<N, G> v = specs.borrowVariable(name);
			return v==null?null: specs.getOptimised(v);
		}

		/**
		 * @param name should not contain the @ sign as it is already implied by this
		 *             methods
		 */
		public LexPipeline<N, G> getPipeline(String name) {
			return specs.getPipeline(name);
		}
	}

	public static class OptimisedHashLexTransducer
			extends OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> {

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(boolean eagerMinimisation, int minimalSymbol,int maximalSymbol,ExternalPipelineFunction externalPipelineFunction)
				throws CompilationError {
			super(new HashMapIntermediateGraph.LexUnicodeSpecification(eagerMinimisation,minimalSymbol,maximalSymbol, externalPipelineFunction));
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source, int minimalSymbol,int maximalSymbol,boolean eagerMinimisation,
				ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(eagerMinimisation, minimalSymbol,maximalSymbol,externalPipelineFunction);
			parse(source);
			checkStrongFunctionality();
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(String source,int minimalSymbol,int maximalSymbol, boolean eagerMinimisation,
				ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(CharStreams.fromString(source),minimalSymbol,maximalSymbol, eagerMinimisation, externalPipelineFunction);
		}
		
		public OptimisedHashLexTransducer(int minimalSymbol,int maximalSymbol) throws CompilationError {
			this(true, minimalSymbol,maximalSymbol,makeEmptyExternalPipelineFunction());
		}
		
		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source,int minimalSymbol,int maximalSymbol, boolean eagerMinimisation) throws CompilationError {
			this(source, minimalSymbol,maximalSymbol,eagerMinimisation, makeEmptyExternalPipelineFunction());
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(String source, int minimalSymbol,int maximalSymbol,boolean eagerMinimisation) throws CompilationError {
			this(source, minimalSymbol,maximalSymbol,eagerMinimisation, makeEmptyExternalPipelineFunction());
		}
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
			return LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.l(), alphAndMealy.r(),
					s -> pos,
					(in, out) -> spec.createFullEdgeOverSymbol(in, spec.createPartialEdge(new IntSeq(out), 0)),
					s -> new P(IntSeq.Epsilon, 0));
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
			return spec.subtract(automata.get(0),automata.get(1));
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

	public static void main(String[] args) throws IOException, CompilationError {
		if (args.length != 1) {
			System.err.println("Provide one path to file with source code!");
			System.exit(-1);
		}
		final OptimisedHashLexTransducer optimised = new OptimisedHashLexTransducer(
				System.getenv("NO_MINIMIZATION") == null,0,Integer.MAX_VALUE, makeEmptyExternalPipelineFunction());
		if (System.getenv("MODE").equals("Thrax")) {
//			final ThraxParser<?, ?> parser = ThraxParser.parse(new File(args[0]),CharStreams.fromFileName(args[0]), optimised.specs);
//			System.out.println(parser.toSolomonoff());
		} else {
			
			final long parsingBegin = System.currentTimeMillis();
			optimised.parse(CharStreams.fromFileName(args[0]));
			System.out.println("Parsing took " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
			final long optimisingBegin = System.currentTimeMillis();
			System.out.println("Optimising took " + (System.currentTimeMillis() - optimisingBegin) + " miliseconds");
			final long ambiguityCheckingBegin = System.currentTimeMillis();
			optimised.checkStrongFunctionality();
			System.out.println(
					"Checking ambiguity " + (System.currentTimeMillis() - ambiguityCheckingBegin) + " miliseconds");
			final long typecheckingBegin = System.currentTimeMillis();
			System.out
					.println("Typechecking took " + (System.currentTimeMillis() - typecheckingBegin) + " miliseconds");
			System.out.println(
					"All loaded correctly! Total time " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
			final MemoryMeter meter = new MemoryMeter();
			try (final Scanner sc = new Scanner(System.in)) {
				while (sc.hasNextLine()) {
					final String line = sc.nextLine();
					final int space = line.indexOf(' ');
					final String firstWord;
					final String remaining;
					if (space >= 0) {
						firstWord = line.substring(0, space);
						remaining = line.substring(space + 1);
					} else {
						firstWord = line;
						remaining = "";
					}
					switch (firstWord) {
					case ":ls": {
						System.out.println(optimised.specs.variableAssignments.keySet());
						break;
					}
					case ":size": {
						RangedGraph<Pos, Integer, E, P> r = optimised.getOptimisedTransducer(remaining);
						System.out.println(r == null ? "No such function!" : r.size());
						break;
					}
					case ":mem": {
						final RangedGraph<Pos, Integer, E, P> r = optimised.getOptimisedTransducer(remaining);
						if (r == null) {
							System.out.println("No such function!");
						} else {
							final long size = +meter.measureDeep(r.indexToState) + meter.measureDeep(r.accepting)
									+ meter.measure(r.graph);
							System.out.println(size + " bytes");
						}
						break;
					}
					case ":export": {
						Var<net.alagris.HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> g = optimised
								.getTransducer(remaining);
						try (FileOutputStream f = new FileOutputStream(remaining + ".star")) {
							optimised.specs.compressBinary(g.graph, new DataOutputStream(new BufferedOutputStream(f)));
						} catch (IOException e) {
							e.printStackTrace();
						}
						break;
					}
					default:
						if (firstWord.startsWith(":")) {
							System.out.println("Unknown command!");
						} else {
							final long evaluationBegin = System.currentTimeMillis();
							final IntSeq output = optimised.run(firstWord, new IntSeq(remaining));
							final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
							System.out.println(output);
							System.out.println(output.toUnicodeString());
							System.out.println("Took " + evaluationTook + " miliseconds");
						}
						break;
					}
				}
			}
		}
	}
}
