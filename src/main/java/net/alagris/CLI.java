package net.alagris;

import de.learnlib.algorithms.rpni.BlueFringeRPNIDFA;
import de.learnlib.api.algorithm.PassiveLearningAlgorithm;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.commons.util.Pair;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
            return specs.evaluate(getOptimisedTransducer(name), input);

        }

        public Var<N, G> getTransducer(String id) {
            //Parsing is already over, so the user might as well mutate it and nothing bad will happen
            //All variables that were meant to be used as building blocks for other transducers have
            //already been either copied or consumed. In the worst case, user might just try to get consumed
            //variable and get null.
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
        spec.registerExternalFunction("dict", (pos, text) -> spec.loadDict(text.iterator(), pos));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni", (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpni(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EDSM(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_edsm", (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniEDSM(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EMDL(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_mdl", (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniMDL(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_Mealy(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_mealy", (pos, text) -> {
            Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> alphAndMealy = LearnLibCompatibility
                    .rpniMealy(text);
            return LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.getFirst(), alphAndMealy.getSecond(),
                    s -> pos, (in, out) -> new E(in, in, new IntSeq(out), 0), s -> new P(IntSeq.Epsilon, 0));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalImport(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("import", (pos, text) -> {
            if (text.size() != 1) throw new CompilationError.IllegalInformantSize(text, 1);
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
                in -> new E(in, in, IntSeq.Epsilon, 0), s -> new P(IntSeq.Epsilon, 0));
    }

    public static void main(String[] args) throws IOException, CompilationError {
        if (args.length != 1) {
            System.err.println("Provide one path to file with source code!");
            System.exit(-1);
        }

        final OptimisedHashLexTransducer optimised = new OptimisedHashLexTransducer(
                System.getenv("EAGER_MINIMIZATION") != null, makeEmptyExternalPipelineFunction());


        final long parsingBegin = System.currentTimeMillis();
        optimised.parse(CharStreams.fromFileName(args[0]));
        System.out.println("Parsing took " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
        final long optimisingBegin = System.currentTimeMillis();
        System.out.println("Optimising took " + (System.currentTimeMillis() - optimisingBegin) + " miliseconds");
        final long ambiguityCheckingBegin = System.currentTimeMillis();
        optimised.checkStrongFunctionality();
        System.out.println("Checking ambiguity " + (System.currentTimeMillis() - ambiguityCheckingBegin) + " miliseconds");
        final long typecheckingBegin = System.currentTimeMillis();
        System.out.println("Typechecking took " + (System.currentTimeMillis() - typecheckingBegin) + " miliseconds");
        System.out.println("All loaded correctly! Total time " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");

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
                    default:
                        if (firstWord.startsWith(":")) {
                            System.out.println("Unknown command!");
                        } else {
                            final long evaluationBegin = System.currentTimeMillis();
                            final String output = optimised.run(firstWord, remaining);
                            final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
                            System.out.println(output);
                            System.out.println("Took " + evaluationTook + " miliseconds");
                        }
                        break;
                }
            }
        }
    }
}
