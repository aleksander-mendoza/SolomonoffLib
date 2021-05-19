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
        spec.registerExternalFunction("dict", (pos, args) -> spec.loadDict(NullTermIter.fromIterable(FuncArg.unaryInformantFunction(pos, args)), pos,(File)null));
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> Pair<OSTIACompress.State, IntEmbedding> compressOSTIA(Iterable<Pair<IntSeq, IntSeq>> text) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        final OSTIACompress.State ptt = OSTIACompress.buildPtt(e, text.iterator());
        OSTIACompress.ostia(ptt);
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalOSTIACompress(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaCompress", (pos, text) -> {
            final Pair<OSTIACompress.State, IntEmbedding> result = compressOSTIA(FuncArg.unaryInformantFunction(pos, text));
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
            if(g.getEpsilon()!=null){
                g.getEpsilon().weight = weight[0]++;
            }
            spec.collectVerticesOfGraphToSet(false, g, new HashSet<>(), n -> {
                final P fin = g.getFinalEdge(n);
                if (fin != null)
                    fin.weight = weight[0]++;
                return null;
            }, (n, e) -> null);
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

            final HashMap<String, ArrayList<String>> args = FuncArg.parseArgsFromInformant(pos, FuncArg.unaryInformantFunction(pos, text),
                    MAX_STATES,
                    MIN_INPUT,
                    MAX_INPUT,
                    MIN_OUTPUT,
                    MAX_OUTPUT,
                    MIN_LEN_OUTPUT,
                    MAX_LEN_OUTPUT,
                    MAX_TRANS,
                    PARTIALITY,
                    RAND_SEED);
            final int maxStates = FuncArg.getExpectSingleInt(pos,args,MAX_STATES,20);
            final int minInputExcl = FuncArg.getExpectSingleCodepoint(pos,args,MIN_INPUT,'a');
            final int maxInputIncl = FuncArg.getExpectSingleCodepoint(pos,args,MAX_INPUT, 'c');
            final int minOutputExcl = FuncArg.getExpectSingleCodepoint(pos,args,MIN_OUTPUT,'a');
            final int maxOutputIncl = FuncArg.getExpectSingleCodepoint(pos,args,MAX_OUTPUT,'c');
            final int minOutputLenIncl = FuncArg.getExpectSingleInt(pos,args,MIN_LEN_OUTPUT,0);
            final int maxOutputLenExcl = FuncArg.getExpectSingleInt(pos,args,MAX_LEN_OUTPUT,4);
            final int maxTrans = FuncArg.getExpectSingleInt(pos,args,MAX_TRANS,5);
            final long randomSeed = FuncArg.getExpectSingleLong(pos,args,RAND_SEED,System.currentTimeMillis());
            final double partiality = FuncArg.getExpectSingleDouble(pos,args,PARTIALITY,0);
            final Random rnd = new Random(randomSeed);
            return spec.randomDeterministicWithRanges(maxStates, maxTrans, partiality,
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
            final HashMap<String, ArrayList<String>> parsedArgs = FuncArg.parseArgsFromInformant(pos, args, "path", "separator", "header", "inputColumn", "outputColumn");
            final File path = pos.resolveRelative(FuncArg.getExpectSingleString(pos,parsedArgs,"path",null));
            final String separatorStr = FuncArg.getExpectSingleString(pos,parsedArgs,"separator","\t");
            if (separatorStr.length() != 1) {
                throw new CompilationError.ParseException(pos, "Separator must be a single character!");
            }
            final char sep = separatorStr.charAt(0);
            final boolean header = FuncArg.getExpectSingleBoolean(pos,parsedArgs,"header", false);


            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path)))) {
                final int inColIdx;
                final int outColIdx;
                final String inCol = FuncArg.getExpectSingleString(pos,parsedArgs,"inputColumn", "0");
                final String outCol = FuncArg.getExpectSingleString(pos,parsedArgs,"outputColumn","1");
                if (header) {
                    final String[] headerLine = Util.split(in.readLine(), sep);
                    inColIdx = Util.indexOf(headerLine, 0, s -> s.equals(inCol));
                    outColIdx = outCol == null ? -1 : Util.indexOf(headerLine, 0, s -> s.equals(outCol));
                    if (inColIdx >= headerLine.length)
                        throw new CompilationError.ParseException(pos, "Could not find column named " + inCol+" in "+path);
                    if (outColIdx >= headerLine.length)
                        throw new CompilationError.ParseException(pos, "Could not find column named " + outCol+" in "+path);
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
                            throw new RuntimeException(new CompilationError.ParseException(pos, "Not enough columns at row '" + line+"' in file "+path));
                        }
                        return Pair.of(new IntSeq(parts[inColIdx]), outColIdx==-1?IntSeq.Epsilon:new IntSeq(parts[outColIdx]));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }, pos,path);
            } catch (IOException e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalImport(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("import", (pos, text) -> {
            final String path = IntSeq.toUnicodeString(FuncArg.unaryInformantFunction(pos, text).get(0).l());
            try (FileInputStream stream = new FileInputStream(pos.resolveRelative(path))) {
                return spec.decompressBinary(pos, new DataInputStream(stream));
            } catch (IOException e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalImportATT(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("importATT", (pos, text) -> {
            try{
                final String path = IntSeq.toUnicodeString(FuncArg.unaryInformantFunction(pos, text).get(0).l());
                return spec.importATT(pos.resolveRelative(path),' ');
            } catch (IOException e) {
                throw new CompilationError.ParseException(pos, e);
            }
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalParseATT(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("parseATT", (pos, text) -> {
            try{
                return spec.importATT(pos.getFile(),FuncArg.unaryInformantFunction(pos, text),' ');
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

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding>
    compressOSTIAMaxOverlap(Iterable<Pair<IntSeq, IntSeq>> text,
                         OSTIAArbitraryOrder.ScoringFunction<OSTIAArbitraryOrder.StatePTT> scoring,
                         OSTIAArbitraryOrder.MergingPolicy<OSTIAArbitraryOrder.StatePTT> policy) {
        final IntEmbedding e = new IntEmbedding(text.iterator());
        OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT> ptt = OSTIAArbitraryOrder.buildPtt(e, text.iterator());
        OSTIAArbitraryOrder.buildSamplePtt(ptt);
        OSTIAState.setAllUnknownStatesAs(ptt, OSTIAState.Kind.REJECTING); // this is the crucial part
        ptt = OSTIAArbitraryOrder.ostia(ptt, scoring, policy, OSTIAArbitraryOrder.StatePTT::add);
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

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxOverlapCompress(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxOverlapCompress", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> result = compressOSTIAMaxOverlap(FuncArg.unaryInformantFunction(pos, text), OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_GREEDY());
            final G g = spec.convertCustomGraphToIntermediate(OSTIAState.asGraph(spec, result.l(), result.r()::retrieve, x -> pos));
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalOSTIAMaxDeepOverlapCompress(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("ostiaMaxDeepOverlapCompress", (pos, text) -> {
            final Pair<OSTIAArbitraryOrder.State<Void>, IntEmbedding> result = inferOSTIAMaxDeepOverlap(FuncArg.unaryInformantFunction(pos, text).filterOutNegative(), OSTIAArbitraryOrder.SCORING_DEEP_COMPRESS(), OSTIAArbitraryOrder.POLICY_GREEDY());
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

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalReverse(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("reverse", (pos, text) -> IntSeq::reverse);
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalUppercase(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("uppercase", (pos, text) -> IntSeq::uppercase);
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalLowercase(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("lowercase", (pos, text) -> IntSeq::lowercase);
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalExtractGroup(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("extractGroup", (pos, text) -> {

            final int groupIdx = Integer.parseInt(IntSeq.toUnicodeString(FuncArg.expectInformant(pos,0,text).get(0).l()));
            final int groupMarker = spec.groupIndexToMarker(groupIdx);
            if(text.size()>1){
                final G g = FuncArg.expectReference(pos,1,text);
                final Specification.RangedGraph<Pos, Integer, E, P> r = spec.optimiseGraph(g);
                return str->spec.submatchSingleGroup(r,str,groupMarker);
            }else{
                return str->spec.submatchSingleGroup(str,groupMarker);
            }

        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalAdd(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("add", (pos, text) -> {
            final int num = Integer.parseInt(IntSeq.toUnicodeString(FuncArg.expectInformant(pos,0,text).get(0).l()));
            return str->IntSeq.map(str,i->i+num);
        });
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalPipelineImport(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalPipe("import", (pos, text) -> {
            final String path = IntSeq.toUnicodeString(FuncArg.expectInformant(pos,0,text).get(0).l());
            try(DataInputStream in = new DataInputStream(new FileInputStream(path))) {
                final Pipeline<Pos, Integer, E, P, N, G> p = spec.decompressBinaryPipeline(pos, in);
                return str->Pipeline.eval(spec,p,str);
            } catch (IOException e) {
                throw new CompilationError(e);
            }
        });
    }


    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void addExternalActiveLearningFromDataset(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("activeLearningFromDataset", (pos, args) -> {
            final FuncArg.Informant<G, IntSeq> text = FuncArg.expectInformant(pos, 0, args);
            final String ALGORITHM = "algorithm";
            final String SEPARATOR = "separator";
            final String INPUT_FILE = "datasetPath";
            final String SCRIPT_ARGS = "arg";
            final String FIRST_BATCH_COUNT = "firstBatchCount";
            final String USED_EXAMPLES_DEST_FILE = "usedExamplesDestFile";
            final String BATCH_SIZE = "batchSize";
            final HashMap<String, ArrayList<String>> params = FuncArg.parseArgsFromInformant(pos, text,
                    ALGORITHM, INPUT_FILE, FIRST_BATCH_COUNT, BATCH_SIZE, SCRIPT_ARGS, SEPARATOR, USED_EXAMPLES_DEST_FILE);
            final String algorithm = FuncArg.getExpectSingleString(pos,params,ALGORITHM,"ostia");
            final String inputFile = FuncArg.getExpectSingleString(pos,params,INPUT_FILE,null);
            final ArrayList<String> scriptArgs = params.getOrDefault(SCRIPT_ARGS,new ArrayList<>(0));
            final String usedExamplesOutputFile = FuncArg.getExpectSingleString(pos,params,USED_EXAMPLES_DEST_FILE,null);
            final String separator = FuncArg.getExpectSingleString(pos,params,SEPARATOR,"\t");
            final int firstBatchCount = FuncArg.getExpectSingleInt(pos,params,FIRST_BATCH_COUNT, 0);
            final int batchSize = FuncArg.getExpectSingleInt(pos,params,BATCH_SIZE,1);
            final LearningFramework<?, Pos, E, P, Integer, IntSeq, N, G> framework = LearningFramework.forID(algorithm, spec);
            final FuncArg.Informant<G, IntSeq> examplesSeenSoFar = new FuncArg.Informant<>();
            final ArrayList<LazyDataset<Pair<IntSeq, IntSeq>>> datasets = new ArrayList<>();
            if (args.size() > 1) {
                datasets.add(LazyDataset.from(FuncArg.expectInformant(pos, 1, args)));
            }
            if (inputFile != null) {
                for (String path : inputFile.split(";", 0)) {
                    final File f = pos.resolveRelative(path);
                    if (!f.isFile()) {
                        System.err.println("File " + f + " does not exist!");
                        continue;
                    }
                    if (path.endsWith(".py")) {
                        datasets.add(LazyDataset.loadDatasetFromPython(f, separator, scriptArgs));
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

    /**set n==-1 if you want to copy everything */
    private static <X> void copyFirstN(int n, LazyDataset<X> it, List<X> l) throws Exception {
        it.begin();
        try {
            for (int i = 0; i != n; i++) {
                final X x = it.next();
                if (x == null) return;
                l.add(x);
            }
        } finally {
            it.close();
        }
    }
}
