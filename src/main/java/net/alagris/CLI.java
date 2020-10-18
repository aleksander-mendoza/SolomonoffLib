package net.alagris;

import de.learnlib.algorithms.rpni.BlueFringeRPNIDFA;
import de.learnlib.api.algorithm.PassiveLearningAlgorithm;
import net.alagris.LexUnicodeSpecification.E;
import net.alagris.LexUnicodeSpecification.P;
import net.alagris.LexUnicodeSpecification.Var;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
		    final IntSeq out = run(name,new IntSeq(input));
		    return out==null?null:out.toUnicodeString();
        }
		public IntSeq run(String name, IntSeq input) {
			return specs.evaluate(getOptimisedTransducer(name), input);

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

		public RangedGraph<Pos, Integer, E, P> getOptimisedTransducer(String name) {
			return specs.borrowVariable(name).getOptimal();
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
		public OptimisedHashLexTransducer(boolean eagerMinimisation, ExternalPipelineFunction externalPipelineFunction)
				throws CompilationError {
			super(new HashMapIntermediateGraph.LexUnicodeSpecification(eagerMinimisation, externalPipelineFunction));
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source, boolean eagerMinimisation,
				ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(eagerMinimisation, externalPipelineFunction);
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
		public OptimisedHashLexTransducer(String source, boolean eagerMinimisation,
				ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
			this(CharStreams.fromString(source), eagerMinimisation, externalPipelineFunction);
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(CharStream source, boolean eagerMinimisation) throws CompilationError {
			this(source, eagerMinimisation, makeEmptyExternalPipelineFunction());
		}

		/**
		 * @param eagerMinimisation This will cause automata to be minimized as soon as
		 *                          they are parsed/registered (that is, the
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize}
		 *                          will be automatically called from
		 *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#introduceVariable})
		 */
		public OptimisedHashLexTransducer(String source, boolean eagerMinimisation) throws CompilationError {
			this(source, eagerMinimisation, makeEmptyExternalPipelineFunction());
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
			return LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.getFirst(), alphAndMealy.getSecond(),
					s -> pos, (in, out) -> spec.createFullEdgeOverSymbol(in, spec.createPartialEdge(new IntSeq(out), 0)), s -> new P(IntSeq.Epsilon, 0));
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
            while(iter.hasNext()){
                composed = spec.compose(composed,iter.next(),pos);
            }
            return composed;
        });
    }

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalStringFile(
			LexUnicodeSpecification<N, G> spec) {
		spec.registerExternalFunction("stringFile", (pos, text) -> {
			if (text.size() != 1)
				throw new CompilationError.IllegalInformantSize(text, 1);
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(text.get(0).getFirst().toUnicodeString())))) {
				return spec.loadDict(() -> {
					try {
						final String line = in.readLine();
						if(line==null)return null;
						final int tab = line.indexOf('\t');
						return Pair.of(
								new IntSeq(line.subSequence(0, tab)), 
								new IntSeq(line.subSequence(tab+1, line.length())));
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
			try (FileInputStream stream = new FileInputStream(text.get(0).getFirst().toUnicodeString())) {
				return spec.decompressBinary(pos, new DataInputStream(stream));
			} catch (IOException e) {
				throw new CompilationError.ParseException(pos, e);
			}
		});
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> G dfaToIntermediate(LexUnicodeSpecification<N, G> spec,
			Pos pos, Pair<Alphabet<Integer>, DFA<?, Integer>> alphAndDfa) {
		return LearnLibCompatibility.dfaToIntermediate(spec, alphAndDfa.getFirst(), alphAndDfa.getSecond(), s -> pos,
                spec::fullNeutralEdgeOverSymbol, s -> spec.partialNeutralEdge());
	}

	public static void main(String[] args) throws IOException, CompilationError {
		if (args.length != 1) {
			System.err.println("Provide one path to file with source code!");
			System.exit(-1);
		}

		final OptimisedHashLexTransducer optimised = new OptimisedHashLexTransducer(
				System.getenv("NO_MINIMIZATION") == null, makeEmptyExternalPipelineFunction());

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
		System.out.println("Typechecking took " + (System.currentTimeMillis() - typecheckingBegin) + " miliseconds");
		System.out.println(
				"All loaded correctly! Total time " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");

		try (final Scanner sc = new Scanner(System.in)) {
			while (sc.hasNextLine()) {
				final String line = sc.nextLine();
				final int space = line.indexOf(' ');
				final String firstWord = line.substring(0, space);
				final String remaining = line.substring(space + 1);
				switch (firstWord) {
				case ":size": {
					RangedGraph<Pos, Integer, E, P> r = optimised.getOptimisedTransducer(remaining);
					System.out.println(r == null ? "No such function!" : r.size());
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
