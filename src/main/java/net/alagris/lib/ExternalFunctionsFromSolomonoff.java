package net.alagris.lib;

import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;

import net.alagris.core.*;
import net.alagris.core.learn.*;

import java.io.*;
import java.util.*;

public class ExternalFunctionsFromSolomonoff {

    private ExternalFunctionsFromSolomonoff() {
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalDict(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("dict", (pos, args) -> spec.loadDict(NullTermIter.fromIterable(FuncArg.unaryInformantFunction(pos, args)), pos));
    }


    /**
     * Consumes informant
     */
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> Pair<OSTIA.State, IntEmbedding> inferOSTIA(Iterable<Pair<IntSeq, IntSeq>> text) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        final OSTIA.State ptt = OSTIA.buildPtt(e, text.iterator());
        OSTIA.ostia(ptt);
        return Pair.of(ptt, e);
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> Pair<OSTIA.State, IntEmbedding> inferOSTIAWithDomain(Iterable<Pair<IntSeq, IntSeq>> text,
                                                                                                                      Specification<?, E, ?, Integer, ?, ?, ?, ?> specs,
                                                                                                                      Specification.RangedGraph<?, Integer, E, ?> domain) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        final OSTIA.State ptt = OSTIA.buildPtt(e, text.iterator());
        OSTIAWithDomain.ostiaWithDomain(ptt, e, specs, domain);
        return Pair.of(ptt, e);
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalOSTIA(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostia", (pos, text) -> {
            final Pair<OSTIA.State, IntEmbedding> result = inferOSTIA(FuncArg.unaryInformantFunction(pos, text));
            // text is consumed
            return spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalOSTIAWithDomain(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaWithDomain", (pos, text) -> {
            if (text.size() > 2) {
                throw new CompilationError.TooManyOperands(pos, text, 2);
            }
            final Specification.RangedGraph<Pos, Integer, E, P> domain = spec.powerset(spec.optimiseGraph(FuncArg.expectAutomaton(pos, 0, text)));
            final FuncArg.Informant<G, IntSeq> informant = FuncArg.expectInformant(pos, 1, text);

            final Pair<OSTIA.State, IntEmbedding> result = inferOSTIAWithDomain(informant, spec, domain);
            // text is consumed
            return spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
        });
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalInverse(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("inverse", (pos, automata) -> {
            G g = FuncArg.unaryAutomatonFunction(pos, automata);
            spec.inverse(g);
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalCompose(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("compose", (pos, automata) -> {
            G composed = spec.compose(FuncArg.expectAutomaton(pos, 0, automata),
                    FuncArg.expectAutomaton(pos, 1, automata), pos);
            for (int i = 2; i < automata.size(); i++) {
                composed = spec.compose(composed, FuncArg.expectAutomaton(pos, i, automata), pos);
            }
            return composed;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalCompress(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("compress", (pos, automata) -> {
            final G g = FuncArg.unaryAutomatonFunction(pos, automata);
            spec.pseudoMinimize(pos, g);
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalSubtract(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("subtract", (pos, automata) -> {
            if (automata.size() > 2)
                throw new CompilationError.TooManyOperands(pos, automata, 2);
            return spec.subtract(FuncArg.expectAutomaton(pos, 0, automata), FuncArg.expectAutomaton(pos, 1, automata));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalSubtractNondet(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("subtractNondet", (pos, automata) -> {
            if (automata.size() > 2)
                throw new CompilationError.TooManyOperands(pos, automata, 2);
            return spec.subtractNondet(FuncArg.expectAutomaton(pos, 0, automata), FuncArg.expectAutomaton(pos, 1, automata));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalLongerMatchesHigherWeights(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("longerMatchesHigherWeights", (pos, automata) -> {
            final G g = FuncArg.unaryAutomatonFunction(pos, automata);
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
        spec.registerExternalFunction("reweight", (pos, automata) -> {
            final G g = FuncArg.unaryAutomatonFunction(pos, automata);
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
        spec.registerExternalFunction("dropEpsilon", (pos, automata) -> {
            final G g = FuncArg.unaryAutomatonFunction(pos, automata);
            g.setEpsilon(null);
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRandom(
            LexUnicodeSpecification<N, G> spec) {
        final String MAX_STATES = "maxStates";
        final String MIN_INPUT = "minInputExcl";
        final String MAX_INPUT = "maxInputIncl";
        final String MIN_OUTPUT = "minOutputExcl";
        final String MAX_OUTPUT = "maxOutputIncl";
        final String MIN_LEN_OUTPUT = "minOutputLenIncl";
        final String MAX_LEN_OUTPUT = "maxOutputLenExcl";
        final String MAX_TRANS = "maxOutgoingTransitions";
        final String PARTIALITY = "partialityFactor";
        final String RAND_SEED = "randomSeed";
        spec.registerExternalFunction("randomDFA", (pos, text) -> {

            final HashMap<String, String> args = FuncArg.parseArgsFromInformant(pos, FuncArg.unaryInformantFunction(pos, text),
                    MAX_STATES, "20",
                    MIN_INPUT, "a",
                    MAX_INPUT, "c",
                    MIN_OUTPUT, "a",
                    MAX_OUTPUT, "c",
                    MIN_LEN_OUTPUT, "0",
                    MAX_LEN_OUTPUT, "4",
                    MAX_TRANS, "5",
                    PARTIALITY, "0",
                    RAND_SEED, String.valueOf(System.currentTimeMillis()));
            final int maxStates = Integer.parseInt(args.get(MAX_STATES));
            final int minInputExcl = args.get(MIN_INPUT).codePointAt(0);
            final int maxInputIncl = args.get(MAX_INPUT).codePointAt(0);
            final int minOutputExcl = args.get(MIN_OUTPUT).codePointAt(0);
            final int maxOutputIncl = args.get(MAX_OUTPUT).codePointAt(0);
            final int minOutputLenIncl = Integer.parseInt(args.get(MIN_LEN_OUTPUT));
            final int maxOutputLenExcl = Integer.parseInt(args.get(MAX_LEN_OUTPUT));
            final int maxTrans = Integer.parseInt(args.get(MAX_TRANS));
            final long randomSeed = Long.parseLong(args.get(RAND_SEED));
            final double partiality = Double.parseDouble(args.get(PARTIALITY));
            final Random rnd = new Random(randomSeed);
            return spec.randomDeterministic(maxStates, maxTrans, partiality,
                    () -> minInputExcl + 1 + rnd.nextInt(maxInputIncl - minInputExcl),
                    (fromExclusive, toInclusive) -> new E(fromExclusive, toInclusive,
                            IntSeq.rand(minOutputLenIncl, maxOutputLenExcl, minOutputExcl + 1, maxOutputIncl + 1, rnd), 0),
                    () -> Pair.of(new P(IntSeq.rand(minOutputLenIncl, maxOutputLenExcl, minOutputExcl + 1, maxOutputIncl + 1, rnd), 0),
                            pos),
                    rnd);
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalIdentity(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("identity", (pos, automata) -> {
            G g = FuncArg.unaryAutomatonFunction(pos, automata);
            spec.identity(g);
            return g;
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
        spec.registerExternalFunction("clearOutput", (pos, automata) -> {
            G g = FuncArg.unaryAutomatonFunction(pos, automata);
            spec.clearOutput(g);
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalStringFile(LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("stringFile", (pos, text) -> {
            final FuncArg.Informant<G, IntSeq> args = FuncArg.unaryInformantFunction(pos, text);
            final HashMap<String, String> parsedArgs = FuncArg.parseArgsFromInformant(pos, args, "path", null, "separator", "\t", "header", "false", "inputColumn", "0", "outputColumn", "1");
            final String path = parsedArgs.get("path");
            final String separatorStr = parsedArgs.get("separator");
            if (separatorStr.length() != 1) {
                throw new CompilationError.ParseException(pos, "Separator must be a single character!");
            }
            final char sep = separatorStr.charAt(0);
            final boolean header = Boolean.parseBoolean(parsedArgs.get("header"));


            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path)))) {
                final int inColIdx;
                final int outColIdx;
                final String inCol = parsedArgs.get("inputColumn");
                final String outCol = parsedArgs.get("outputColumn");
                if (header) {
                    final String[] headerLine = Util.split(in.readLine(), sep);
                    inColIdx = Util.indexOf(headerLine, 0, s -> s.equals(inCol));
                    outColIdx = outCol == null ? -1 : Util.indexOf(headerLine, 0, s -> s.equals(outCol));
                    if (inColIdx >= headerLine.length)
                        throw new CompilationError.ParseException(pos, "Could not find column named " + inCol);
                    if (outColIdx >= headerLine.length)
                        throw new CompilationError.ParseException(pos, "Could not find column named " + outCol);
                } else {
                    inColIdx = Integer.parseInt(inCol);
                    outColIdx = outCol == null ? -1 : Integer.parseInt(outCol);
                }
                return spec.loadDict(() -> {
                    try {
                        final String line = in.readLine();
                        if (line == null)
                            return null;
                        final String[] parts = Util.split(line, sep);
                        if (inColIdx >= parts.length || outColIdx >= parts.length) {
                            throw new RuntimeException(new CompilationError.ParseException(pos, "Not enough columns at row: " + line));
                        }
                        return Pair.of(new IntSeq(parts[inColIdx]), outColIdx==-1?IntSeq.Epsilon:new IntSeq(parts[outColIdx]));
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
            try (FileInputStream stream = new FileInputStream(IntSeq.toUnicodeString(FuncArg.unaryInformantFunction(pos, text).get(0).l()))) {
                return spec.decompressBinary(pos, new DataInputStream(stream));
            } catch (IOException e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding>
    inferOSTIAMaxOverlap(Iterable<Pair<IntSeq, IntSeq>> text,
                         OSTIAArbitraryOrder.ScoringFunction<OSTIAArbitraryOrder.StatePTT> scoring,
                         OSTIAArbitraryOrder.MergingPolicy<OSTIAArbitraryOrder.StatePTT> policy) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT> ptt = OSTIAArbitraryOrder.buildPtt(e, text.iterator());
        OSTIAArbitraryOrder.buildSamplePtt(ptt);
        ptt = OSTIAArbitraryOrder.ostia(ptt, scoring, policy, OSTIAArbitraryOrder.StatePTT::add);
        return Pair.of(ptt, e);
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> Pair<OSTIAArbitraryOrder.State<Void>, IntEmbedding>
    inferOSTIAMaxDeepOverlap(Iterable<Pair<IntSeq, IntSeq>> text,
                             OSTIAArbitraryOrder.ScoringFunction<Void> scoring,
                             OSTIAArbitraryOrder.MergingPolicy<Void> policy) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        OSTIAArbitraryOrder.State<Void> ptt = OSTIAArbitraryOrder.buildPtt(e, text.iterator());
        ptt = OSTIAArbitraryOrder.ostia(ptt, scoring, policy, (a, b) -> null);
        return Pair.of(ptt, e);
    }


    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxOverlap(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxOverlap", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxCompatible(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxCompatible", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_COMPATIBLE, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxDeepOverlap(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxDeepOverlap", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<Void>, IntEmbedding> result = inferOSTIAMaxDeepOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_DEEP_OVERLAP(), OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxCompatibleInputs(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxCompatibleInputs", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_COMPATIBLE_INPUTS, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxCompatibleInputsAndOutputs(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxCompatibleInputsAndOutputs", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_COMPATIBLE_INPUTS_AND_OUTPUTS, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAConservative(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaConservative", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_THRESHOLD(1));
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAInOutOneToOne(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaInOutOneToOne", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = inferOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }


    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalActiveLearningFromDataset(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("activeLearningFromDataset", (pos, args) -> {
            final FuncArg.Informant<G, IntSeq> text = FuncArg.expectInformant(pos, 0, args);
            final String ALGORITHM = "algorithm";
            final String SEPARATOR = "separator";
            final String INPUT_FILE = "datasetPath";
            final String FIRST_BATCH_COUNT = "firstBatchCount";
            final String USED_EXAMPLES_DEST_FILE = "usedExamplesDestFile";
            final String BATCH_SIZE = "batchSize";

            final HashMap<String, String> params = FuncArg.parseArgsFromInformant(pos, text,
                    ALGORITHM, "ostia",
                    INPUT_FILE, null,
                    FIRST_BATCH_COUNT, "-1",
                    BATCH_SIZE, "1",
                    SEPARATOR, "\t",
                    USED_EXAMPLES_DEST_FILE, null);
            final String algorithm = params.get(ALGORITHM);
            final String inputFile = params.get(INPUT_FILE);
            final String usedExamplesOutputFile = params.get(USED_EXAMPLES_DEST_FILE);
            final String separator = params.get(SEPARATOR);
            final int firstBatchCount = Integer.parseInt(params.get(FIRST_BATCH_COUNT));
            final int batchSize = Integer.parseInt(params.get(BATCH_SIZE));
            final LearningFramework<?, Pos, E, P, Integer, IntSeq, N, G> framework = LearningFramework.forID(algorithm, spec);
            final FuncArg.Informant<G, IntSeq> examplesSeenSoFar = new FuncArg.Informant<>();
            final ArrayList<LazyDataset<Pair<IntSeq, IntSeq>>> datasets = new ArrayList<>();
            if (args.size() > 1) {
                datasets.add(LazyDataset.from(FuncArg.expectInformant(pos, 1, args)));
            }
            if (inputFile != null) {
                for (String path : inputFile.split(";", 0)) {
                    final File f = new File(path);
                    if (!f.isFile()) {
                        System.err.println("File " + f + " does not exist!");
                        continue;
                    }
                    if (path.endsWith(".py")) {
                        datasets.add(LazyDataset.loadDatasetFromPython(f, separator));
                    } else {
                        datasets.add(LazyDataset.loadDatasetFromFile(f, separator));
                    }
                }

            }
            final LazyDataset<Pair<IntSeq, IntSeq>> dataset = LazyDataset.combine(datasets);
            try {
                copyFirstN(firstBatchCount, dataset, examplesSeenSoFar);
                G g = LearningFramework.activeLearning(framework, examplesSeenSoFar, dataset, batchSize, System.out::println);
                if (usedExamplesOutputFile != null) {
                    try (PrintWriter pw = new PrintWriter(usedExamplesOutputFile)) {
                        for (Pair<IntSeq, IntSeq> ex : examplesSeenSoFar) {
                            pw.print(IntSeq.toUnicodeString(ex.l()));
                            pw.print(separator);
                            pw.println(IntSeq.toUnicodeString(ex.r()));
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return g;
            } catch (Exception e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }


    private static <X> void copyFirstN(int n, LazyDataset<X> it, List<X> l) throws Exception {
        it.begin();
        try {
            for (int i = 0; i < n; i++) {
                final X x = it.next();
                if (x == null) return;
                l.add(x);
            }
        } finally {
            it.close();
        }
    }
}
