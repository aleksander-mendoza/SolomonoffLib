package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.lib.Solomonoff;
import org.antlr.v4.runtime.CharStreams;
import org.jline.terminal.MouseEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Repl<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> {

    public static final String PREFIX = "/";
    public static final String COMMENT = "//";

    private static class CmdMeta<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>, Result> {
        final ReplCommand<N, G, Result> cmd;
        final String help;
        final String template;


        private CmdMeta(ReplCommand<N, G, Result> cmd, String help, String template) {
            this.cmd = cmd;
            this.help = help;
            this.template = template;
        }
    }

    public final HashMap<String, CmdMeta<N, G, String>> commands = new HashMap<>();
    public final Solomonoff<N, G> compiler;

    public ReplCommand<N, G, String> registerCommand(String name, String help, String template, ReplCommand<N, G, String> cmd) {
        final CmdMeta<N, G, String> prev = commands.put(name, new CmdMeta<>(cmd, help, template));
        return prev == null ? null : prev.cmd;
    }

    public static final String LOAD = "load";
    public static final String PIPES = "pipes";
    public static final String TRACE = "trace";
    public static final String SUBMATCH = "submatch";
    public static final String SUBMATCH_FILE = "submatch_file";
    public static final String MEM = "mem";
    public static final String VERBOSE = "verbose";
    public static final String LS = "ls";
    public static final String SIZE = "size";
    public static final String IS_DET = "is_det";
    public static final String IS_FUNC = "is_func";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    public static final String EVAL = "eval";
    public static final String UNSET = "unset";
    public static final String UNSET_ALL = "unset_all";
    public static final String FUNCS = "funcs";
    public static final String RAND_SAMPLE = "rand_sample";
    public static final String VIS = "vis";
    public static final String EXIT = "exit";


    public Repl(Solomonoff<N, G> compiler) {
        this.compiler = compiler;
        registerCommand(EXIT, "Exits REPL", "", (a, b, d, c) -> "");
        registerCommand("?", "Prints help", "", (a, b, d, args) -> {
            args = args.trim();
            if (args.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Repl.CmdMeta<N, G, String>> cmd : commands.entrySet()) {
                    sb.append(PREFIX).append(cmd.getKey()).append(" ").append(cmd.getValue().template).append("\n    ").append(cmd.getValue().help).append('\n');
                }
                return sb.toString();
            } else {
                final Repl.CmdMeta<N, G, String> cmd = commands.get(args);
                return cmd.help + ". Usage:\n        " + cmd.template + '\n';
            }
        });
        registerCommand(LOAD, "Loads source code from file", "[FILE]", CommandsFromSolomonoff.replLoad());
        registerCommand(PIPES, "Lists all currently defined pipelines", "", CommandsFromSolomonoff.replListPipes());
        registerCommand(TRACE, "Runs pipeline for the given input and traces outputs produced at each stage", "[ID] [STRING]", CommandsFromSolomonoff.replTrace());
        registerCommand(SUBMATCH, "Extracts all submatches for a specific group", "[ID] [STRING] [GROUP]", CommandsFromSolomonoff.replSubmatch());
        registerCommand(SUBMATCH_FILE, "Extracts all submatches for a file", "[ID] [STRING] [FILE]", CommandsFromSolomonoff.replSubmatchFile());
        registerCommand("", "Feeds given string to the compiler. This is only useful when making one-liners in Bash scripts but its pointless to run from within REPL console.", "[CODE]", CommandsFromSolomonoff.replParse());
        registerCommand(MEM, "Shows RAM memory usage of transducer. This requires running with -javaagent.", "[ID]", CommandsFromJamm.replMem());
        registerCommand(LS, "Lists all currently defined transducers", "", CommandsFromSolomonoff.replList());
        registerCommand(SIZE, "Size of transducer is the number of its states", "[ID]", CommandsFromSolomonoff.replSize());
        registerCommand("equal",
                "Tests if two DETERMINISTIC transducers are equal. Does not work with nondeterministic ones!", "[ID] [ID]",
                CommandsFromSolomonoff.replEqual());
        registerCommand(IS_DET, "Tests whether transducer is deterministic", "[ID]", CommandsFromSolomonoff.replIsDeterministic());
        registerCommand(IS_FUNC, "Tests whether transducer is functional", "[ID]", CommandsFromSolomonoff.replIsFunctional());
        registerCommand(EXPORT, "Exports transducer or pipeline to binary file", "[ID] [FILE_PATH]",
                CommandsFromSolomonoff.replExport());
        registerCommand(IMPORT, "Imports transducer or pipeline from binary file and assigns it to a new variable name", "[ID] [FILE_PATH]",
                CommandsFromSolomonoff.replImport());
        registerCommand(EVAL, "Evaluates transducer (or entire pipeline) on requested input", "[ID] [STRING]", CommandsFromSolomonoff.replEval());
        registerCommand(UNSET, "Removes a previously defined variable (if exists)", "[ID]", CommandsFromSolomonoff.replUnset());
        registerCommand(UNSET_ALL, "Removes all previously defined variable (if any)", "", CommandsFromSolomonoff.replUnsetAll());
        registerCommand(FUNCS, "Lists all available external functions. Those starting with @ are pipeline functions.", "", CommandsFromSolomonoff.replFuncs());
        registerCommand(RAND_SAMPLE, "Generates random sample of input:output pairs produced by ths transducer", "[ID] [of_size/of_length] [NUM]",
                CommandsFromSolomonoff.replRandSample());
        registerCommand(VIS, "Visualizes transducer as a graph. It may export it either as DOT " +
                "format or as SVG., depending on whether the OUTPUT_FILE ends in .dot or .svg. Exporting as SVG requires that graphviz is installed " +
                "and that 'dot' executable is on the PATH. It's also possible to specify 'stdout' as OUTPUT_FILE and then the DOT format will be printed to console. " +
                "Prefixing the OUTPUT_FILE with 'file:' will cause produced file to be automatically opened in the default browser for convenience. " +
                "User may specify type and view of produced graph. Type of " +
                "automaton is one of "+Arrays.toString(MouseEvent.Type.values())+" where fsa stands for finite state acceptor" +
                "(outputs are not shown), fst stands for finite state transducer (outputs are shown), wfsa and wfst" +
                "are their weighted counterparts (so weights are shown), moore is the Moore automaton (only state outputs are shown)" +
                "subfst is the subsequential transducer (outputs of edges as well as state outputs are shown)," +
                "prefix l stands for location in source code (each state is labelled with corresponding line and column in source code)." +
                "The default type is lwsubfst which displays all information there is and doesn't skip anything. View argument " +
                "(either 'intermediate' or 'ranged') decides whether to use intermediate (mutable) graphs produced by Glushkov construction or the optimised (immutable) graphs with sorted" +
                "ranges of edges.", "[ID] [OUTPUT_FILE] type=[TYPE] view=[VIEW]", CommandsFromSolomonoff.replVisualize());

    }

    public String run(String line, Consumer<String> log, Consumer<String> debug) throws Exception {
        if (line.startsWith(COMMENT)) return null;
        if (line.startsWith(PREFIX)) {
            final int space = line.indexOf(' ');
            final String firstWord;
            final String remaining;
            if (space >= 0) {
                firstWord = line.substring(1, space);
                remaining = line.substring(space + 1);
            } else {
                firstWord = line.substring(1);
                remaining = "";
            }
            final CmdMeta<N, G, String> cmd = commands.get(firstWord);
            if(cmd==null){
                return "Unrecognized command "+firstWord;
            }
            return cmd.cmd.run(compiler, log, debug, remaining);
        } else {
            final long begin = System.currentTimeMillis();
            compiler.parse(CharStreams.fromString(line));
            debug.accept("Took " + (System.currentTimeMillis() - begin) + " miliseconds");
            return null;
        }

    }

    /**returns true if /exit command was called*/
    public boolean runMultiline(Consumer<String> log, Consumer<String> debug, String input) throws Exception {
        int from = 0;
        int to = Util.indexOf(input,from,PREFIX.charAt(0));
        if(0<to) {
            final String in = input.substring(from, to);
            if(in.trim().equals(PREFIX+EXIT))return true;
            final String out = run(in, log, debug);
            if (out != null) {
                System.out.println(out);
            }
        }
        from = to;
        while(from<input.length()){
            to = Util.indexOf(input,from+1,'/');
            if(from+1<to) {
                final String in = input.substring(from, to);
                if(in.trim().equals(PREFIX+EXIT))return true;
                final String out = run(in, log, debug);
                if (out != null) {
                    System.out.println(out);
                }
            }
            from = to;
        }
        return false;
    }

}
