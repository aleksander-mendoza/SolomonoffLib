package net.alagris;

import net.alagris.CompilationError.WeightConflictingToThirdState;
import net.alagris.LexUnicodeSpecification.*;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.*;
import java.util.*;

import static net.alagris.LexUnicodeSpecification.*;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class OptimisedLexTransducer<N, G extends IntermediateGraph<Pos, E, P, N>> {
    public final LexUnicodeSpecification<N, G> specs;
    public final ParserListener<Var<N, G>, Pos, E, P, Integer, IntSeq, Integer, N, G> listener;
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
        addExternalCompress(specs);
        addExternalInverse(specs);
        addExternalSubtract(specs);
        addExternalSubtractNondet(specs);
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
        checkStrongFunctionalityOfVariables();
    }

    public void checkStrongFunctionalityOfVariables() throws CompilationError {
        for (Var<N, G> var : specs.variableAssignments.values()) {
            specs.checkFunctionality(specs.getOptimised(var), var.pos);
        }
    }

    public String run(String name, String input) {
        final IntSeq out = run(name, new IntSeq(input));
        return out == null ? null : IntSeq.toUnicodeString(out);
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
    public Pipeline<Pos,Integer,E,P,N, G> getPipeline(String name) {
        return specs.getPipeline(name);
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
        spec.registerExternalFunction("rpniEdsm",
                (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniEDSM(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EMDL(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpniMdl",
                (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniMDL(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_Mealy(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpniMealy", (pos, text) -> {
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalCompress(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalOperation("compress", (pos, automata) -> {
            if (automata.size() != 1)
                throw new CompilationError.IllegalOperandsNumber(automata, 1);
            final G g = automata.get(0);
            spec.pseudoMinimize(pos,g);
            return g;
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalSubtractNondet(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalOperation("subtractNondet", (pos, automata) -> {
            if (automata.size() != 2)
                throw new CompilationError.IllegalOperandsNumber(automata, 2);
            return spec.subtractNondet(automata.get(0), automata.get(1));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalLongerMatchesHigherWeights(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalOperation("longerMatchesHigherWeights", (pos, automata) -> {
            if (automata.size() != 1)
                throw new CompilationError.IllegalOperandsNumber(automata, 1);
            final G g = automata.get(0);
            final int[] weight = new int[]{spec.weightNeutralElement()};
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
                for (Map.Entry<E, N> outgoing : g.outgoing(vertex)) {
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
                        maxStates = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MIN_INPUT)) {
                        minInputExcl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MAX_INPUT)) {
                        maxInputIncl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MIN_LEN_OUTPUT)) {
                        minOutputLenIncl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MAX_LEN_OUTPUT)) {
                        maxOutputLenExcl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MIN_OUTPUT)) {
                        minOutputExcl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MAX_OUTPUT)) {
                        maxOutputIncl = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(MAX_TRANS)) {
                        maxTrans = Integer.parseInt(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(PARTIALITY)) {
                        partiality = Double.parseDouble(IntSeq.toUnicodeString(t.r()));
                    } else if (t.l().equals(RAND_SEED)) {
                        randomSeed = Long.parseLong(IntSeq.toUnicodeString(t.r()));
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
//    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalDeterminize(
//            LexUnicodeSpecification<N, G> spec) {
//        spec.registerExternalOperation("determinize", (pos, automata) -> {
//            if (automata.size() != 1)
//                throw new CompilationError.IllegalOperandsNumber(automata, 2);
//            final G g = automata.get(0);
//            final RangedGraph<Pos, Integer, E, P> r = spec.optimiseGraph(g);
//            spec.reduceEdges(pos,r);
//            final RangedGraph<Pos, Integer, E, P> d = spec.powerset(r);
//
//            return d;
//        });
//    }
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
                    new InputStreamReader(new FileInputStream(IntSeq.toUnicodeString(text.get(0).l()))))) {
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
            try (FileInputStream stream = new FileInputStream(IntSeq.toUnicodeString(text.get(0).l()))) {
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


    public static class OptimisedHashLexTransducer
            extends OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>> {

        public OptimisedHashLexTransducer(Config config) throws CompilationError {
            super(new HashMapIntermediateGraph.LexUnicodeSpecification(config));
        }

    }

    public static class OptimisedArrayLexTransducer
            extends OptimisedLexTransducer<ArrayIntermediateGraph.N<Pos, E>, ArrayIntermediateGraph<Pos, E, P>> {

        public OptimisedArrayLexTransducer(Config config) throws CompilationError {
            super(new ArrayIntermediateGraph.LexUnicodeSpecification(config));
        }

    }

}
