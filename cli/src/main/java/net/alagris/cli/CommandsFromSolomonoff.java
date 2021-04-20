package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.lib.Solomonoff;
import org.antlr.v4.runtime.CharStreams;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;

public class CommandsFromSolomonoff {

    private CommandsFromSolomonoff() {
    }


    final static Random RAND = new Random();

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replLoad() {
        return (compiler, log, debug, args) -> {
            final long parsingBegin = System.currentTimeMillis();
            compiler.parse(CharStreams.fromFileName(args.trim()));
            debug.accept("Took " + (System.currentTimeMillis() - parsingBegin) + " milliseconds");
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

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replEval(boolean tabular) {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", tabular?3:2);
            if (parts.length != (tabular?3:2))
                return (tabular?"Three arguments required 'bufferSize', 'transducerName' and 'transducerInput' but got ":
                        "Two arguments required 'transducerName' and 'transducerInput' but got ")
                        + Arrays.toString(parts);
            final String transducerName = parts[tabular?1:0].trim();
            final String transducerInput = parts[tabular?2:1].trim();
            final Function<IntSeq,Seq<Integer>> eval;
            if (transducerName.startsWith("@")) {
                final Pipeline<Pos, Integer, E, P, N, G> pip = compiler.getPipeline(transducerName.substring(1));
                if (pip == null)
                    return "Pipeline '" + transducerName + "' not found!";
                if(tabular) {
                    final int bufferSize = Integer.parseInt(parts[0].trim());
                    final byte[] stateToIndex = new byte[Pipeline.foldAutomata(pip,0,(max,aut)->Math.max(max,aut.g.size()))];
                    final int[] outputBuffer = new int[bufferSize];
                    eval = input -> compiler.specs.evaluateTabular(pip, input, stateToIndex, outputBuffer);
                }else {
                    eval = input -> compiler.specs.evaluate(pip, input);
                }
            } else {
                final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
                if (graph == null)
                    return "Transducer '" + transducerName + "' not found!";
                if(tabular) {
                    final int bufferSize = Integer.parseInt(parts[0].trim());
                    final byte[] stateToIndex = new byte[graph.size()];
                    final int[] outputBuffer = new int[bufferSize];
                    eval = input ->  compiler.specs.evaluateTabularReturnRef(graph, stateToIndex, outputBuffer, graph.initial, input);
                }else {
                    eval = input -> compiler.specs.evaluate(graph, input);
                }
            }
            if(transducerInput.startsWith("'")||transducerInput.startsWith("<")){
                final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
                final long evaluationBegin = System.currentTimeMillis();
                final Seq<Integer> out = eval.apply(input);
                final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
                debug.accept("Evaluation took " + evaluationTook + " milliseconds");
                return out == null ? "No match!" : IntSeq.toStringLiteral(out);
            }else if(transducerInput.equals("stdin")){
                evalInLoop(debug, logs, eval,new BufferedReader(new InputStreamReader(System.in)));
                return null;
            }else {
                try(BufferedReader sc = new BufferedReader(new FileReader(transducerInput))){
                    evalInLoop(debug,logs,eval,sc);
                }
                return null;
            }

        };
    }



    private static void evalInLoop(java.util.function.Consumer<String> debug,java.util.function.Consumer<String> logs, Function<IntSeq, Seq<Integer>> eval, BufferedReader sc) throws IOException {
        final long evaluationBegin = System.currentTimeMillis();
        long timeSums = 0;
        int lineNo = 0;
            String line;
            while((line=sc.readLine())!=null && !Thread.interrupted()) {
                lineNo++;
                final long lineEvaluationBegin = System.currentTimeMillis();
                final String output = IntSeq.toUnicodeString(eval.apply(new IntSeq(line)));
                timeSums += System.currentTimeMillis() - lineEvaluationBegin;
                if(output!=null)logs.accept(output);
            }
        final long totalTime = System.currentTimeMillis() - evaluationBegin;
        debug.accept("Took " +totalTime + " milliseconds ("+(totalTime-timeSums)+" was consumed by I/O, "+timeSums+" was spent on evaluation). Number of lines: "+lineNo);
//        long evaluationTook = 0;
//        final long evaluationBeginIO = System.currentTimeMillis();
//        while(sc.hasNextLine() && !Thread.interrupted()){
//            final long evaluationBegin = System.currentTimeMillis();
//            final String out = IntSeq.toUnicodeString(eval.apply(new IntSeq(sc.nextLine())));
//            evaluationTook += System.currentTimeMillis() - evaluationBegin;
//            System.out.println(out);
//        }
//        final long evaluationEndIO = System.currentTimeMillis() - evaluationBeginIO - evaluationTook;
//        debug.accept("Evaluation took " + evaluationTook + " milliseconds + spent "+(evaluationEndIO)+" on I/O");
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replParse() {
        return (compiler, logs, debug, args) -> {
            compiler.parse(CharStreams.fromString(args));
            return null;
        };
    }
    public enum View {
        intermediate,ranged
    }
    public enum Type {
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

    private static <N, G extends IntermediateGraph<Pos, E, P, N>> Util.DOTProvider
    visualize(Solomonoff<N,G> compiler,
              IntSeq input,
              LexUnicodeSpecification.Var<N, G> tr,
              boolean intermediate,
              Type t,
              HashMap<Integer, LexUnicodeSpecification.BacktrackingNode> superposition) throws CompilationError {
        final EpsilonLabeler partialLabeler = (weight, out, sb) -> {
            if (t.weights) {
                sb.append(":");
                sb.append(weight);
            }
            if (out!=null) {
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
            partialLabeler.label(e.weight, t.edgeOutputs?e.getOut():null, sb);
            Util.escape(sb, '"', '\\');
            sb.insert(0, "label=\"");
            sb.append("\"");
            return sb.toString();
        };
        final VertexLabeler vertexLabeler = (stateFin, stateMeta, stateIndex) -> {
            final StringBuilder sb = new StringBuilder();
            if(superposition!=null) {

                if (t.location) {
                    sb.append("(at ").append(stateMeta).append(") ");
                }
                if(!superposition.containsKey(stateIndex)) {
                    sb.append("∅");
                }else{
                    final LexUnicodeSpecification.BacktrackingNode b = superposition.get(stateIndex);
                    final IntSeq out = compiler.specs.collect(new LexUnicodeSpecification.BacktrackingHead(b,compiler.specs.partialNeutralEdge()),input);
                    sb.append(IntSeq.toStringLiteral(out));
                }
            }else{
                if (t.location) {
                    sb.append("(at ").append(stateMeta).append(")");
                } else {
                    sb.append(stateIndex);
                }
                if (stateFin != null) {
                    partialLabeler.label(stateFin.weight, t.stateOutputs?stateFin.getOut():null, sb);
                }
            }
            Util.escape(sb, '"', '\\');
            sb.insert(0, "label=\"");
            sb.append("\"");
            if (stateFin != null) {
                sb.append(",peripheries=2");
            }
            return sb.toString();
        };
        if (intermediate) {
            return os -> compiler.specs.exportDOT(tr.graph, os,
                    (i, n) -> vertexLabeler.label(tr.graph.getFinalEdge(n), tr.graph.getState(n), i),
                    e -> edgeLabeler.label(e.getFromExclusive(), e.getToInclusive(), e), p -> {
                        final StringBuilder sb = new StringBuilder();
                        partialLabeler.label(p.weight, t.edgeOutputs?p.out:null, sb);
                        Util.escape(sb, '"', '\\');
                        sb.insert(0, "label=\"");
                        sb.append("\"");
                        return sb.toString();
                    }
            );
        } else  {
            final Specification.RangedGraph<Pos, Integer, E, P> g = compiler.specs.getOptimised(tr);
            return os -> compiler.specs.exportDOTRanged(g, os,
                    (i) -> vertexLabeler.label(g.getFinalEdge(i), g.state(i), i)
                    , edgeLabeler);
        }
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
                final String[] ARGS = new String[]{"type","view","input"};
                for(String arg:ARGS) {
                    if (parts[i].startsWith(arg+"=")) {
                        final String prev = opts.put(arg, parts[i].substring(arg.length()+1));
                        if (prev != null) {
                            return "Parameter "+arg+" is set twice!";
                        }
                    }
                }
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
            final String inputStr = opts.get("input");
            final IntSeq input = inputStr==null?null:new IntSeq(inputStr);
            final String type = opts.getOrDefault("type", "lwsubfst");
            final View view;
            try {
                view = View.valueOf(opts.getOrDefault("view", "intermediate"));
            }catch(IllegalArgumentException e){
                return "Unknown view " + opts.get("view") + "! Expected ranged or intermediate!";
            }
            final Type t = Util.find(Type.values(), a -> a.name().equals(type));
            if (t == null) return "Unknown type " + type + "! Expected one of " + Arrays.toString(Type.values());

            if (parts[1].endsWith(".gif")) {
                if(input==null){
                    return "Input is required in order to produce gif! Specify input= parameter!";
                }
                if (view!=View.ranged){
                    return "Input can only be evaluated on ranged automata! Specify view=ranged";
                }
                final Specification.RangedGraph<Pos, Integer, E, P> g = compiler.specs.getOptimised(tr);
                HashMap<Integer, LexUnicodeSpecification.BacktrackingNode> thisSuperposition = new HashMap<>(),nextSuperposition = new HashMap<>();
                thisSuperposition.put(g.initial,null);
                final File snapshot = new File(path.getPath()+".part");
                Util.exportPNG(snapshot, visualize(compiler, null, tr, false, t, null));
                BufferedImage img = ImageIO.read(snapshot);
                try(ImageOutputStream output = new FileImageOutputStream(path);
                    GifSequenceWriter writer = new GifSequenceWriter(output, img.getType(), 1000, true)) {
                    writer.writeToSequence(img);
                    for (int i=0;i<input.size();i++) {
                        if (thisSuperposition.isEmpty()) break;
                        final int in = input.at(i);
                        Util.exportPNG(snapshot, visualize(compiler, input.sub(0,i), tr, false, t, thisSuperposition));
                        img = ImageIO.read(snapshot);
                        writer.writeToSequence(img);
                        compiler.specs.deltaSuperposition(g, in, thisSuperposition, nextSuperposition);
                        final HashMap<Integer, LexUnicodeSpecification.BacktrackingNode> tmp = thisSuperposition;
                        thisSuperposition = nextSuperposition;
                        nextSuperposition = tmp;
                        nextSuperposition.clear();
                    }
                    Util.exportPNG(snapshot, visualize(compiler, input, tr, false, t, thisSuperposition));
                    img = ImageIO.read(snapshot);
                    writer.writeToSequence(img);
                }finally{
                    snapshot.delete();
                }
            }else {
                final Util.DOTProvider writer;
                if (view==View.intermediate) {
                    if(input!=null){
                        return "Cannot evaluate input on intermediate automata! Specify view=ranged instead!";
                    }
                    writer = visualize(compiler, null, tr, true, t, null);
                } else if (view == View.ranged) {
                    final HashMap<Integer, LexUnicodeSpecification.BacktrackingNode> superposition;
                    if(input==null){
                        superposition = null;
                    }else {
                        final Specification.RangedGraph<Pos, Integer, E, P> g = compiler.specs.getOptimised(tr);
                        superposition = compiler.specs.deltaSuperpositionTransitiveFromInitial(g,g.initial,input.iterator(),new HashMap<>(),new HashMap<>());
                    }
                    writer = visualize(compiler, input, tr, false, t, superposition);
                }else{
                    return "Unexpected view "+view;
                }

                if (parts[1].endsWith(".dot")) {
                    try (OutputStreamWriter f = new OutputStreamWriter(new FileOutputStream(path))) {
                        writer.writeDOT(f);
                    }
                } else if (parts[1].endsWith(".svg")) {
                    Util.exportSVG(path, writer);
                } else if (parts[1].endsWith(".png")) {
                    Util.exportPNG(path, writer);
                } else if (parts[1].equals("stdout")) {
                    writer.writeDOT(System.out);
                } else {
                    return "Illegal format " + parts[1] + "! Expected path to .dot, .svg or .png file or stdout!";
                }
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
                return "Three arguments required 'transducerName', 'groupIndex' and 'transducerInput' but got "
                        + Arrays.toString(parts);
            final String transducerName = parts[0].trim();
            final int groupMarker = compiler.specs.groupIndexToMarker(Integer.parseInt(parts[1].trim()));
            final String transducerInput = parts[2].trim();
            final IntSeq input = ParserListener.parseCodepointOrStringLiteral(transducerInput);
            if (transducerName.startsWith("@")) {
                return "Use @extractGroup!['groupIndex'] to extract submatches from pipelines";
            }
            final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
            if (graph == null)
                return "Transducer '" + transducerName + "' not found!";
            final long evaluationBegin = System.currentTimeMillis();
            final Seq<Integer> output = compiler.specs.submatchSingleGroup(graph, input, groupMarker);
            final long evaluationTook = System.currentTimeMillis() - evaluationBegin;
            debug.accept("Took " + evaluationTook + " milliseconds");
            return output == null ? "No match!" : IntSeq.toStringLiteral(output);
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replSubmatchFile() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split("\\s+", 3);
            if (parts.length != 3)
                return "Three arguments required 'transducerName', 'groupIndex' and 'filePath' but got "
                        + Arrays.toString(parts);
            final String transducerName = parts[0].trim();
            final int groupMarker = compiler.specs.groupIndexToMarker(Integer.parseInt(parts[1].trim()));
            final String filePath = parts[2].trim();
            if (transducerName.startsWith("@")) {
                return "Use @extractGroup!['groupIndex'] to extract submatches from pipelines";
            }
            final Specification.RangedGraph<Pos, Integer, E, P> graph = compiler.getOptimisedTransducer(transducerName);
            if (graph == null)
                return "Transducer '" + transducerName + "' not found!";
            final long evaluationBegin = System.currentTimeMillis();
            long timeSums = 0;
            int lineNo = 0;

            try(BufferedReader sc = new BufferedReader(new FileReader(filePath))){
                String line;
                while((line=sc.readLine())!=null) {
                    lineNo++;
                    final long lineEvaluationBegin = System.currentTimeMillis();
                    final Seq<Integer> output = compiler.specs.submatchSingleGroup(graph, new IntSeq(line), groupMarker);
                    timeSums += System.currentTimeMillis() - lineEvaluationBegin;
                    if(output!=null)logs.accept(IntSeq.toUnicodeString(output));
                }
            }
            final long totalTime = System.currentTimeMillis() - evaluationBegin;
            debug.accept("Took " +totalTime + " milliseconds ("+(totalTime-timeSums)+" was consumed by I/O, "+timeSums+" was spent on submatch extraction). Number of lines: "+lineNo);
            return null;
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
            final String[] parts = args.split(" ",2);
            if(parts.length!=2){
                return "Expected arguments 'transducerName'/'pipeName' and 'filePath'";
            }
            if(parts[0].startsWith("@")){
                final Pipeline<Pos, Integer, E, P, N, G> g = compiler.getPipeline(parts[0].substring(1));
                try (FileOutputStream f = new FileOutputStream(parts[1].trim())) {
                    compiler.specs.compressBinaryPipeline(g, new DataOutputStream(f));
                    return null;
                }
            }else{
                final LexUnicodeSpecification.Var<N, G> g = compiler.getTransducer(parts[0]);
                try (FileOutputStream f = new FileOutputStream(parts[1].trim())) {
                    compiler.specs.compressBinary(g.graph, new DataOutputStream(f));
                    return null;
                }
            }
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replImport() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.split(" ",2);
            if(parts.length!=2){
                return "Expected arguments 'transducerName'/'pipeName' and 'filePath'";
            }
            if(parts[0].startsWith("@")){
                final String name = parts[0].substring(1);
                if(compiler.getPipeline(name)!=null){
                    return parts[0]+" is already defined";
                }
                try (FileInputStream f = new FileInputStream(parts[1].trim())) {
                    final Pipeline<Pos, Integer, E, P, N, G> g = compiler.specs.decompressBinaryPipeline(Pos.NONE, new DataInputStream(f));
                    compiler.specs.registerNewPipeline(g,name);
                    return null;
                }
            }else{
                if(compiler.getTransducer(parts[0])!=null){
                    return parts[0]+" is already defined";
                }
                try (FileInputStream f = new FileInputStream(parts[1].trim())) {
                    final G g = compiler.specs.decompressBinary(Pos.NONE, new DataInputStream(f));
                    compiler.specs.introduceVariable(parts[0],Pos.NONE,g,0,false);
                    return null;
                }
            }
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replVerbose(ToggleableConsumer<String> logOrDebug) {
        return (compiler, logs, debug, args) -> {
            args = args.trim();
            if ("true".equals(args)) {
                logOrDebug.setEnabled(true);
            } else if ("false".equals(args)) {
                logOrDebug.setEnabled(false);
            } else if (args.isEmpty()) {
                return logOrDebug.isEnabled() ? "Debug output is verbose" : "Debug output is silenced";
            } else {
                return "Specify 'true' to enable or 'false' to disable verbose debug logs";
            }
            return null;
        };
    }

    static <N, G extends IntermediateGraph<Pos, E, P, N>> ReplCommand<N, G, String> replUnset() {
        return (compiler, logs, debug, args) -> {
            args = args.trim();
            if (args.startsWith("@")) {
                if (compiler.specs.pipelines.remove(args.substring(1)) == null) {
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
