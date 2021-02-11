package net.alagris.lib;

import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;

import net.alagris.core.*;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class ExternalFunctionsFromSolomonoff {

    private ExternalFunctionsFromSolomonoff() {
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalDict(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("dict", (pos, args) -> spec.loadDict(NullTermIter.fromIterable(FuncArg.unaryInformantFunction(pos, args)), pos));
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


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> Pair<OSTIA.State, int[]> inferOSTIA(Iterable<Pair<IntSeq, IntSeq>> text) {
        final HashMap<Integer, Integer> symbolToIndex = new HashMap<>();
        inferAlphabet(text.iterator(), symbolToIndex);
        final int[] indexToSymbol = new int[symbolToIndex.size()];
        for (Map.Entry<Integer, Integer> e : symbolToIndex.entrySet()) {
            indexToSymbol[e.getValue()] = e.getKey();
        }
        final Iterator<Pair<IntSeq, IntSeq>> mapped = mapSymbolsToIndices(text.iterator(),
                symbolToIndex);
        final OSTIA.State ptt = OSTIA.buildPtt(symbolToIndex.size(), mapped);
        OSTIA.ostia(ptt);
        return Pair.of(ptt, indexToSymbol);
    }


    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalOSTIA(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostia", (pos, text) -> {
            final Pair<OSTIA.State, int[]> result = inferOSTIA(FuncArg.unaryInformantFunction(pos, text));
            return spec.convertCustomGraphToIntermediate(OSTIA.asGraph(spec, result.l(), i -> result.r()[i], x -> pos));
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
            if (automata.size() <= 1)
                throw new CompilationError.IllegalOperandsNumber(pos, automata, 2);
            final Iterator<FuncArg<G, IntSeq>> iter = automata.iterator();
            G composed = FuncArg.expectAutomaton(pos, iter.next());
            while (iter.hasNext()) {
                composed = spec.compose(composed, FuncArg.expectAutomaton(pos, iter.next()), pos);
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
            if (automata.size() != 2)
                throw new CompilationError.IllegalOperandsNumber(pos, automata, 2);
            return spec.subtract(FuncArg.expectAutomaton(pos, automata.get(0)), FuncArg.expectAutomaton(pos, automata.get(1)));
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalSubtractNondet(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("subtractNondet", (pos, automata) -> {
            if (automata.size() != 2)
                throw new CompilationError.IllegalOperandsNumber(pos, automata, 2);
            return spec.subtractNondet(FuncArg.expectAutomaton(pos, automata.get(0)), FuncArg.expectAutomaton(pos, automata.get(1)));
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalStringFile(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("stringFile", (pos, text) -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(IntSeq.toUnicodeString(FuncArg.unaryInformantFunction(pos, text).get(0).l()))))) {
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
            try (FileInputStream stream = new FileInputStream(IntSeq.toUnicodeString(FuncArg.unaryInformantFunction(pos, text).get(0).l()))) {
                return spec.decompressBinary(pos, new DataInputStream(stream));
            } catch (IOException e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }


}
