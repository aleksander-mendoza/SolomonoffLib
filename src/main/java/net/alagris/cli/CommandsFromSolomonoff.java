package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import org.antlr.v4.runtime.CharStreams;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class CommandsFromSolomonoff {

    private CommandsFromSolomonoff() {
    }


    final static Random RAND = new Random();

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replLoad() {
        return (compiler, log, debug, args) -> {
            final long parsingBegin = System.currentTimeMillis();
            compiler.parse(CharStreams.fromFileName(args.trim()));
            debug.accept("Took " + (System.currentTimeMillis() - parsingBegin) + " miliseconds");
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replList() {
        return (compiler, logs, debug,
                args) -> compiler.specs.variableAssignments.keySet().toString();
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replSize() {
        return (compiler, logs, debug, args) -> {
            args = args.trim();
            if (args.startsWith("@")) {
                Pipeline<Pos, Integer, E, P, N, G> r = compiler.getPipeline(args.substring(1));
                int sumtotal = Pipeline.foldAutomata(r, 0, (sum, auto) -> {
                    final int size = auto.g.size();
                    logs.accept(auto.meta + " has " + size + " states");
                    return sum + size;
                });
                return r == null ? "No such pipeline!" : String.valueOf(sumtotal);
            } else {
                Specification.RangedGraph<Pos, Integer, E, P> r = compiler.getOptimisedTransducer(args);
                return r == null ? "No such function!" : String.valueOf(r.size());
            }
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replEval() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 2);
            if (parts.length != 2)
                return "Two arguments required 'transducerName' and 'transducerInput' but got "
                        + Arrays.toString(parts);
            final String transducerName = parts[0].trim();
            final String transducerInput = parts[1].trim();
            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
            final Seq<Integer> output;
            final long evaluationBegin;
            if (transducerName.startsWith("@")) {
                final Pipeline<Pos, Integer, E, P, N, G> pip = compiler.getPipeline(transducerName);
                if (pip == null)
                    return "Pipeline '" + transducerName + "' not found!";
                evaluationBegin = System.currentTimeMillis();
                output = compiler.specs.evaluate(pip, input);
            } else {
                final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
                if (graph == null)
                    return "Transducer '" + transducerName + "' not found!";
                evaluationBegin = System.currentTimeMillis();
                output = compiler.specs.evaluate(graph, input);
            }
            final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
            debug.accept("Took " + evaluationTook + " miliseconds");
            return output == null ? "No match!" : IntSeq.toStringLiteral(output);
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replParse() {
        return (compiler, logs, debug, args) -> {
            compiler.parse(CharStreams.fromString(args));
            return null;
        };
    }

    private enum Type {
        fsa(false, false, false, false),
        moore(false, false, false, true),
        fst(false, false, true, false),
        wfsa(false, true, false, false),
        wfst(false, true, true, false),
        wmoore(false, true, false, true),
        subfst(false, false, true, true),
        wsubfst(false, true, true, true),
        lfsa(true, false, false, false),
        lmoore(true, false, false, true),
        lfst(true, false, true, false),
        lwfsa(true, true, false, false),
        lwfst(true, true, true, false),
        lwmoore(true, true, false, true),
        lsubfst(true, false, true, true),
        lwsubfst(true, true, true, true);

        private final boolean weights;
        private final boolean edgeOutputs;
        private final boolean stateOutputs;
        private final boolean location;

        Type(boolean location, boolean weights, boolean edgeOutputs, boolean stateOutputs) {
            this.location = location;
            this.weights = weights;
            this.edgeOutputs = edgeOutputs;
            this.stateOutputs = stateOutputs;
        }
    }

    private interface VertexLabeler {
        String label(P stateFin, Pos stateMeta, int stateIndex);
    }

    private interface EpsilonLabeler {
        void label(int weight, IntSeq out, StringBuilder sb);
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replVisualize() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.trim().split("\\s+");
            if (parts.length < 2)
                return "Not enough arguments! Required 'transducerName' and 'outputFile'!";
            if (parts[0].startsWith("@"))
                return "Pipelines cannot be visualized!";
            final LexUnicodeSpecification.Var<N, G> tr = compiler.getTransducer(parts[0]);
            if (tr == null) return "No such transducer: " + parts[0];
            final HashMap<String, String> opts = new HashMap<>();
            for (int i = 2; i < parts.length; i++) {
                if (parts[i].startsWith("type=")) {
                    final String prev = opts.put("type", parts[i].substring("type=".length()));
                    if (prev != null) {
                        return "Parameter type is set twice!";
                    }
                } else if (parts[i].startsWith("view=")) {
                    final String prev = opts.put("view", parts[i].substring("view=".length()));
                    if (prev != null) {
                        return "Parameter view is set twice!";
                    }
                }
            }
            final String type = opts.getOrDefault("type", "lwsubfst");
            final String view = opts.getOrDefault("view", "intermediate");
            final Type t = Util.find(Type.values(), a -> a.name().equals(type));
            if (t == null) return "Unknown type " + type + "! Expected one of " + Arrays.toString(Type.values());
            final Util.DOTProvider writer;
            final EpsilonLabeler partialLabeler = (weight, out, sb) -> {
                if (t.weights) {
                    sb.append(":");
                    sb.append(weight);
                }
                if (t.edgeOutputs) {
                    if (t.weights) {
                        sb.append(" ");
                    } else {
                        sb.append(":");
                    }
                    sb.append(IntSeq.toStringLiteral(out));
                }
            };
            final Specification.EdgeLabeler<Integer, E> edgeLabeler = (from, to, e) -> {
                final StringBuilder sb = new StringBuilder();
                IntSeq.appendRange(sb, from + 1, to);
                partialLabeler.label(e.weight, e.getOut(), sb);
                Util.escape(sb, '"', '\\');
                sb.insert(0, "label=\"");
                sb.append("\"");
                return sb.toString();
            };
            final VertexLabeler vertexLabeler = (stateFin, stateMeta, stateIndex) -> {
                final StringBuilder sb = new StringBuilder();
                if (t.location) {
                    sb.append("(at ").append(stateMeta).append(")");
                } else {
                    sb.append(stateIndex);
                }
                if (stateFin != null) {
                    partialLabeler.label(stateFin.weight, stateFin.getOut(), sb);
                }
                Util.escape(sb, '"', '\\');
                sb.insert(0, "label=\"");
                sb.append("\"");
                if (stateFin != null) {
                    sb.append(",peripheries=2");
                }
                return sb.toString();
            };
            if (view.equals("intermediate")) {
                writer = os -> compiler.specs.exportDOT(tr.graph, os,
                        (i, n) -> vertexLabeler.label(tr.graph.getFinalEdge(n), tr.graph.getState(n), i),
                        e -> edgeLabeler.label(e.getFromExclusive(), e.getToInclusive(), e), p -> {
                            final StringBuilder sb = new StringBuilder();
                            partialLabeler.label(p.weight, p.out, sb);
                            Util.escape(sb, '"', '\\');
                            sb.insert(0, "label=\"");
                            sb.append("\"");
                            return sb.toString();
                        }
                );
            } else if (view.equals("ranged")) {
                final Specification.RangedGraph<Pos, Integer, E, P> g = compiler.specs.getOptimised(tr);
                writer = os -> compiler.specs.exportDOTRanged(g, os,
                        (i) -> vertexLabeler.label(g.getFinalEdge(i), g.state(i), i)
                        , edgeLabeler);
            } else {
                return "Unknown view " + view + "! Expected ranged or intermediate!";
            }
            final File path;
            boolean openInBrowser;
            if (parts[1].startsWith("file:")) {
                path = new File(parts[1].substring("file:".length()));
                openInBrowser = true;
            } else {
                path = new File(parts[1]);
                openInBrowser = false;
            }
            if (parts[1].endsWith(".dot")) {
                try (OutputStreamWriter f = new OutputStreamWriter(new FileOutputStream(path))) {
                    writer.writeDOT(f);
                }
            } else if (parts[1].endsWith(".svg")) {
                Util.exportSVG(path, writer);
            } else if (parts[1].equals("stdout")) {
                writer.writeDOT(System.out);
            } else {
                return "Illegal format " + parts[1] + "! Expected path to .dot or .svg file or stdout!";
            }
            if (openInBrowser) {
                Util.openInBrowser(path);
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replSubmatch() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 3);
            if (parts.length != 3)
                return "Three arguments required 'transducerName', 'transducerInput' and 'groupIndex' but got "
                        + Arrays.toString(parts);
            final String transducerName = parts[0].trim();
            final String transducerInput = parts[1].trim();
            final int groupMarker = compiler.specs.groupIndexToMarker(Integer.parseInt(parts[2].trim()));
            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
            if (transducerName.startsWith("@")) {
                return "Use @extractGroup!['groupIndex'] to extract submatches from pipelines";
            }
            final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
            if (graph == null)
                return "Pipeline '" + transducerName + "' not found!";
            final long evaluationBegin = System.currentTimeMillis();
            final Seq<Integer> output = compiler.specs.submatchSingleGroup(graph, input, groupMarker);
            final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
            debug.accept("Took " + evaluationTook + " miliseconds");
            return output == null ? "No match!" : IntSeq.toStringLiteral(output);
        };
    }


    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replTrace() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 2);
            if (parts.length != 2)
                return "Two arguments required 'transducerName' and 'transducerInput' but got " + Arrays.toString(parts);
            final String pipelineName = parts[0].trim();
            if (!pipelineName.startsWith("@")) {
                return "Pipeline names must start with @";
            }
            final String pipelineInput = parts[1].trim();
            final Pipeline<Pos, Integer, E, P, N, G> pipeline = compiler
                    .getPipeline(pipelineName.substring(1));
            if (pipeline == null)
                return "Pipeline '" + pipelineName + "' not found!";

            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(pipelineInput);
            final Seq<Integer> output = Pipeline.eval(compiler.specs, pipeline, input, (pipe, out) -> {
                if (pipe instanceof Pipeline.Automaton) {
                    logs.accept(((Pipeline.Automaton<Pos, Integer, E, P, N, G>) pipe).meta() + ":" + out);
                }
            });
            return output == null ? "No match!" : IntSeq.toStringLiteral(output);
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replExport() {
        return (compiler, logs, debug, args) -> {
            LexUnicodeSpecification.Var<N, G> g = compiler
                    .getTransducer(args);
            try (FileOutputStream f = new FileOutputStream(args + ".star")) {
                compiler.specs.compressBinary(g.graph, new DataOutputStream(f));
                return null;
            }
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replVerbose(ToggleableConsumer<String> logOrDebug) {
        return (compiler, logs, debug, args) -> {
            if ("true".equals(args)) {
                logOrDebug.setEnabled(true);
            } else if ("false".equals(args)) {
                logOrDebug.setEnabled(false);
            } else if (args == null || args.isEmpty()) {
                return logOrDebug.isEnabled() ? "Debug output is verbose" : "Debug output is silenced";
            } else {
                return "Specify 'true' to enable or 'false' to disable verbose debug logs";
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replUnset() {
        return (compiler, logs, debug, args) -> {
            if (args.startsWith("@")) {
                if (compiler.specs.pipelines.remove(args) == null) {
                    debug.accept("No such pipeline?");
                }
            } else {
                if (compiler.specs.variableAssignments.remove(args) == null) {
                    debug.accept("No such variable?");
                }
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replUnsetAll() {
        return (compiler, logs, debug, args) -> {
            args = args.trim();
            if ("pipelines".equals(args)) {
                compiler.specs.pipelines.clear();
            } else {
                compiler.specs.variableAssignments.clear();
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replFuncs() {
        return (compiler, logs, debug, args) -> {
            compiler.specs.externalFunc.keySet().forEach(logs);
            compiler.specs.externalPips.keySet().forEach(i -> logs.accept("@" + i));
            return null;
        };
    }


    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replIsDeterministic() {
        return (compiler, logs, debug, args) -> {
            Specification.RangedGraph<Pos, Integer, E, P> r = compiler.getOptimisedTransducer(args);
            if (r == null)
                return "No such function!";
            return r.isDeterministic() == null ? "true" : "false";
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replIsFunctional() {
        return (compiler, logs, debug, args) -> {
            Specification.RangedGraph<Pos, Integer, E, P> r = compiler.getOptimisedTransducer(args);
            if (r == null)
                return "No such function!";
            final Specification.FunctionalityCounterexample<E, P, Pos> weightConflictingTranitions = compiler.specs.isFunctional(r, r.initial);
            if (weightConflictingTranitions != null) {
                if (weightConflictingTranitions instanceof Specification.FunctionalityCounterexampleFinal) {
                    Specification.FunctionalityCounterexampleFinal<E, P, ?> c = (Specification.FunctionalityCounterexampleFinal<E, P, ?>) weightConflictingTranitions;
                    return c.getMessage(args);
                } else {
                    Specification.FunctionalityCounterexampleToThirdState<E, P, ?> c = (Specification.FunctionalityCounterexampleToThirdState<E, P, ?>) weightConflictingTranitions;
                    return c.getMessage(args);
                }
            }
            return "Automaton is strongly functional!";
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replListPipes() {
        return (compiler, logs, debug, args) -> {
            return Util.fold(compiler.specs.pipelines.keySet(), new StringBuilder(),
                    (pipe, sb) -> sb.append("@").append(pipe).append(", ")).toString();
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replEqual() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 2);
            if (parts.length != 2)
                return "Two arguments required 'transducerName' and 'transducerInput' but got "
                        + Arrays.toString(parts);
            final String transducer1 = parts[0].trim();
            final String transducer2 = parts[1].trim();
            Specification.RangedGraph<Pos, Integer, E, P> r1 = compiler.getOptimisedTransducer(transducer1);
            Specification.RangedGraph<Pos, Integer, E, P> r2 = compiler.getOptimisedTransducer(transducer2);
            if (r1 == null)
                return "No such transducer '" + transducer1 + "'!";
            if (r2 == null)
                return "No such transducer '" + transducer2 + "'!";
            final Specification.AdvAndDelState<Integer, IntQueue> counterexample = compiler.specs.areEquivalent(r1, r2);
            if (counterexample == null)
                return "true";
            return "false";
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replRandSample() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 4);
            if (parts.length != 3) {
                return "Three arguments required: 'transducerName', 'mode' and 'size'";
            }
            final String transducerName = parts[0].trim();
            final String mode = parts[1];
            final int param = Integer.parseInt(parts[2].trim());
            final Specification.RangedGraph<Pos, Integer, E, P> transducer = compiler.getOptimisedTransducer(transducerName);
            if (transducer == null) {
                return "Transducer not found";
            }
            if (mode.equals("of_size")) {
                final int sampleSize = param;
                compiler.specs.generateRandomSampleOfSize(transducer, sampleSize, RAND, (backtrack, finalState) -> {
                    final LexUnicodeSpecification.BacktrackingHead head = new LexUnicodeSpecification.BacktrackingHead(
                            backtrack, transducer.getFinalEdge(finalState));
                    final IntSeq in = head.randMatchingInput(RAND);
                    final IntSeq out = compiler.specs.collect(head, in);
                    logs.accept(IntSeq.toStringLiteral(in) + ":" + IntSeq.toStringLiteral(out));
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
                            final IntSeq out = compiler.specs.collect(head, in);
                            logs.accept(IntSeq.toStringLiteral(in) + ":" + IntSeq.toStringLiteral(out));
                        }, x -> {
                        });
                return null;
            } else {
                return "Choose one of the generation modes: 'of_size' or 'of_length'";
            }
        };
    }


}
