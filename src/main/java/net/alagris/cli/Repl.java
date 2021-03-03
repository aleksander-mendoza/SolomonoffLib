package net.alagris.cli;

import net.alagris.core.*;
import net.alagris.lib.Solomonoff;
import org.antlr.v4.runtime.CharStreams;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Repl<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> {

    public static final String PREFIX = "/";

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

    public Repl(Solomonoff<N, G> compiler) {
        this.compiler = compiler;
        registerCommand("exit", "Exits REPL", "", (a, b, d, c) -> "");
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
        registerCommand("load", "Loads source code from file", "[FILE]", CommandsFromSolomonoff.replLoad());
        registerCommand("pipes", "Lists all currently defined pipelines", "", CommandsFromSolomonoff.replListPipes());
        registerCommand("run", "Runs pipeline for the given input", "[ID] [STRING]", CommandsFromSolomonoff.replRun());
        registerCommand("trace", "Runs pipeline for the given input and traces outputs produced at each stage", "[ID] [STRING]", CommandsFromSolomonoff.replTrace());
        registerCommand("", "Feeds given string to the compiler. This is only useful when making one-liners in Bash scripts but its pointless to run from within REPL console.", "[CODE]", CommandsFromSolomonoff.replParse());
        registerCommand("mem", "Shows RAM memory usage of transducer. This requires running with -javaagent.", "[ID]", CommandsFromJamm.replMem());
        registerCommand("ls", "Lists all currently defined transducers", "", CommandsFromSolomonoff.replList());
        registerCommand("size", "Size of transducer is the number of its states", "[ID]", CommandsFromSolomonoff.replSize());
        registerCommand("equal",
                "Tests if two DETERMINISTIC transducers are equal. Does not work with nondeterministic ones!", "[ID] [ID]",
                CommandsFromSolomonoff.replEqual());
        registerCommand("is_det", "Tests whether transducer is deterministic", "[ID]", CommandsFromSolomonoff.replIsDeterministic());
        registerCommand("is_func", "Tests whether transducer is functional", "[ID]", CommandsFromSolomonoff.replIsFunctional());
        registerCommand("export", "Exports transducer to STAR (Subsequential Transducer ARchie) binary file", "[ID]",
                CommandsFromSolomonoff.replExport());
        registerCommand("eval", "Evaluates transducer on requested input", "[ID] [STRING]", CommandsFromSolomonoff.replEval());
        registerCommand("unset", "Removes a previously defined variable (if exists)", "[ID]", CommandsFromSolomonoff.replUnset());
        registerCommand("unset_all", "Removes all previously defined variable (if any)", "", CommandsFromSolomonoff.replUnsetAll());
        registerCommand("funcs", "Lists all available external functions", "", CommandsFromSolomonoff.replFuncs());
        registerCommand("rand_sample", "Generates random sample of input:output pairs produced by ths transducer", "[ID] [of_size/of_length] [NUM]",
                CommandsFromSolomonoff.replRandSample());
        registerCommand("vis", "Visualizes transducer as a graph", "[ID]", CommandsFromLearnLib.replVisualize());
        registerCommand("exportDOT", "Export transducer as a DOT graph", "[ID] [FILE]", CommandsFromLearnLib.replExportDOT());
    }

    public String run(String line, Consumer<String> log, Consumer<String> debug) throws Exception {
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
        int to = Util.indexOf(input,from,'/');
        if(0<to) {
            final String in = input.substring(from, to);
            if(in.trim().equals("/exit"))return true;
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
                if(in.trim().equals("/exit"))return true;
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
