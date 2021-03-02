package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import org.antlr.v4.runtime.CharStreams;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

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
        	if(args.startsWith("@")) {
        		Pipeline<Pos, Integer, E, P, N, G> r = compiler.getPipeline(args.substring(1));
        		int sumtotal = Pipeline.foldAutomata(r, 0, (sum,auto)->{
        			final int size = auto.g.size();
        			logs.accept(auto.meta+" has "+size+" states");
        			return sum+size;
        		});
	            return r == null ? "No such pipeline!" : String.valueOf(sumtotal);
        	}else {
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
            final long evaluationBegin = System.currentTimeMillis();
            final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
            if (graph == null)
                return "Transducer '" + transducerName + "' not found!";
            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
            final IntSeq output = compiler.specs.evaluate(graph, input);
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
    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replRun() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 2);
            if (parts.length != 2)
                return "Two arguments required 'transducerName' and 'transducerInput' but got " + Arrays.toString(parts);
            final String pipelineName = parts[0].trim();
            if (!pipelineName.startsWith("@")) {
                return "Pipeline names must start with @";
            }
            final String pipelineInput = parts[1].trim();
            final long evaluationBegin = System.currentTimeMillis();
            final Pipeline<Pos, Integer, E, P, N, G> pipeline = compiler
                    .getPipeline(pipelineName.substring(1));
            if (pipeline == null)
                return "Pipeline '" + pipelineName + "' not found!";
            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(pipelineInput);
            final Seq<Integer> output = Pipeline.eval(compiler.specs, pipeline, input,(pipe,out)->{
                if(pipe instanceof Pipeline.Automaton){
                    debug.accept(((Pipeline.Automaton<Pos, Integer, E, P, N, G>) pipe).meta() +":"+out);
                }
            });
            final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
            debug.accept("Took " + evaluationTook + " miliseconds");
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
            if("true".equals(args)){
                logOrDebug.setEnabled(true);
            }else if("false".equals(args)){
                logOrDebug.setEnabled(false);
            }else if(args==null||args.isEmpty()){
                return logOrDebug.isEnabled()?"Debug output is verbose":"Debug output is silenced";
            }else{
                return "Specify 'true' to enable or 'false' to disable verbose debug logs";
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replUnset() {
        return (compiler, logs, debug, args) -> {
            if(args.startsWith("@")){
                if (compiler.specs.pipelines.remove(args) == null) {
                    debug.accept("No such pipeline?");
                }
            }else {
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
            if("pipelines".equals(args)){
                compiler.specs.pipelines.clear();
            }else {
                compiler.specs.variableAssignments.clear();
            }
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
            if(transducer==null){
                return "Transducer not found";
            }
            if (mode.equals("of_size")) {
                final int sampleSize = param;
                compiler.specs.generateRandomSampleOfSize(transducer, sampleSize, RAND, (backtrack, finalState) -> {
                    final LexUnicodeSpecification.BacktrackingHead head = new LexUnicodeSpecification.BacktrackingHead(
                            backtrack, transducer.getFinalEdge(finalState));
                    final IntSeq in = head.randMatchingInput(RAND);
                    final IntSeq out = compiler.specs.collect(head,in);
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
                            final IntSeq out = compiler.specs.collect(head,in);
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
