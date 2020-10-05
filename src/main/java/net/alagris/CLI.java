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
        final ParserListener<LexPipeline<N,G>, Pos, E, P, Integer, IntSeq, Integer, N, G> parser;
        final ArrayList<ParserListener.Type<Pos, E, P, N, G>> types = new ArrayList<>();
        final HashMap<String, Specification.RangedGraph<Pos, Integer, E, P>> optimised = new HashMap<>();

        public OptimisedLexTransducer(LexUnicodeSpecification<N, G> specs) throws CompilationError {
            this.specs = specs;
            addExternalRPNI(specs);
            addExternalRPNI_EDSM(specs);
            addExternalRPNI_EMDL(specs);
            addExternalRPNI_Mealy(specs);
            parser = specs.makeParser(types);
            parser.addDotAndHashtag();
        }

        public void parse(CharStream source) throws CompilationError {
            parser.parse(source);
        }

        public void pseudoMinimise() throws CompilationError {
            for (GMeta<Pos, E, P, N, G> graph : specs.variableAssignments.values()) {
                specs.pseudoMinimize(graph.graph);
            }
        }

        public void optimise() throws CompilationError {
            for (GMeta<Pos, E, P, N, G> graph : specs.variableAssignments.values()) {
                final Specification.RangedGraph<Pos, Integer, E, P> optimal = specs.optimiseGraph(graph.graph);
                specs.reduceEdges(optimal);
                optimised.put(graph.name, optimal);
            }
        }

        public void checkStrongFunctionality() throws CompilationError {
            for (RangedGraph<Pos, Integer, E, P> optimal : optimised.values()) {
                specs.checkStrongFunctionality(optimal);
            }
        }

        public void typecheck() throws CompilationError {
            for (ParserListener.Type<Pos, E, P, N, G> type : types) {
                final Specification.RangedGraph<Pos, Integer, E, P> optimal = optimised.get(type.name);
                final Specification.RangedGraph<Pos, Integer, E, P> lhs = specs.optimiseGraph(type.lhs);
                final Specification.RangedGraph<Pos, Integer, E, P> rhs = specs.optimiseGraph(type.rhs);
                final Pos graphPos = specs.varAssignment(type.name).pos;
                specs.typecheck(type.name, graphPos, type.meta,type.constructor, optimal, lhs, rhs);
            }
        }

        public String run(String name, String input) {
            return specs.evaluate(optimised.get(name), input);
        }

        public GMeta<Pos, E, P, N, G> getTransducer(String id) {
            return specs.varAssignment(id);
        }

        public void visualize(String id) {
            LearnLibCompatibility.visualize(getTransducer(id).graph,Pos.NONE,Pos.NONE);
        }

        public RangedGraph<Pos, Integer, E, P> getOptimisedTransducer(String id) {
            return optimised.get(id);
        }

        /**@param name should not contain the @ sign as it is already implied by this methods*/
        public LexPipeline<N,G> getPipeline(String name) {
            return specs.getPipeline(name);
        }
    }


    public static class OptimisedHashLexTransducer extends
            OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> {

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#registerVar})
         */
        public OptimisedHashLexTransducer(boolean eagerMinimisation, ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
            super(new HashMapIntermediateGraph.LexUnicodeSpecification(eagerMinimisation, externalPipelineFunction));
        }

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#registerVar})
         */
        public OptimisedHashLexTransducer(CharStream source, boolean eagerMinimisation,
                                          ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
            this(eagerMinimisation, externalPipelineFunction);
            parse(source);
            optimise();
            checkStrongFunctionality();
            typecheck();
        }

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#registerVar})
         */
        public OptimisedHashLexTransducer(String source, boolean eagerMinimisation,
                                          ExternalPipelineFunction externalPipelineFunction) throws CompilationError {
            this(CharStreams.fromString(source), eagerMinimisation, externalPipelineFunction);
        }

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#registerVar})
         */
        public OptimisedHashLexTransducer(CharStream source, boolean eagerMinimisation) throws CompilationError {
            this(source, eagerMinimisation, makeEmptyExternalPipelineFunction());
        }

        /**
         * @param eagerMinimisation This will cause automata to be minimized as soon as they are parsed/registered (that is, the {@link HashMapIntermediateGraph.LexUnicodeSpecification#pseudoMinimize} will be automatically called from
         *                          {@link HashMapIntermediateGraph.LexUnicodeSpecification#registerVar})
         */
        public OptimisedHashLexTransducer(String source, boolean eagerMinimisation) throws CompilationError {
            this(source, eagerMinimisation, makeEmptyExternalPipelineFunction());
        }
    }

    public static ExternalPipelineFunction makeEmptyExternalPipelineFunction() {
        return (a, b) -> s -> s;
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI(LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni", (pos, text) -> {
            return dfaToIntermediate(spec, pos, LearnLibCompatibility.rpni(text));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EDSM(LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_edsm", (pos, text) -> {
            return dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniEDSM(text));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EMDL(LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_mdl", (pos, text) -> {
            return dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniMDL(text));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_Mealy(LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni_mealy", (pos, text) -> {
            Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> alphAndMealy = LearnLibCompatibility.rpniMealy(text);
            return LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.getFirst(), alphAndMealy.getSecond(), s -> pos, (in, out) -> new E(in, in, new IntSeq(out),0),s->new P(IntSeq.Epsilon,0));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> G dfaToIntermediate(LexUnicodeSpecification<N, G> spec, Pos pos, Pair<Alphabet<Integer>, DFA<?, Integer>> alphAndDfa) {
        return LearnLibCompatibility.dfaToIntermediate(spec, alphAndDfa.getFirst(), alphAndDfa.getSecond(), s -> pos, in -> new E(in, in, IntSeq.Epsilon, 0), s -> new P(IntSeq.Epsilon, 0));
    }

    public static void main(String[] args) throws IOException, CompilationError {
        if (args.length != 1) {
            System.err.println("Provide one path to file with source code!");
            System.exit(-1);
        }
        final OptimisedHashLexTransducer optimised = new OptimisedHashLexTransducer(
                CharStreams.fromFileName(args[0]), true);
        System.out.println("All loaded correctly!");
        final Scanner sc = new Scanner(System.in);
        while (sc.hasNextLine()) {
            final String line = sc.nextLine();
            final int space = line.indexOf(' ');
            final String function = line.substring(0, space);
            final String input = line.substring(space + 1);
            final String output = optimised.run(function, input);
            System.out.println(output);
        }
    }
}
