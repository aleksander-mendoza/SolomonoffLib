package net.alagris;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.NoViableAltException;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Repl<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> {
    private static class CmdMeta<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>, Result> {
        final ReplCommand<N, G, Result> cmd;
        final String help;

        private CmdMeta(ReplCommand<N, G, Result> cmd, String help) {
            this.cmd = cmd;
            this.help = help;
        }
    }

    private final HashMap<String, CmdMeta<N, G, String>> commands = new HashMap<>();
    private final OptimisedLexTransducer<N, G> compiler;

    public ReplCommand<N, G, String> registerCommand(String name, String help, ReplCommand<N, G, String> cmd) {
        final CmdMeta<N, G, String> prev = commands.put(name, new CmdMeta<>(cmd, help));
        return prev == null ? null : prev.cmd;
    }

    public Repl(OptimisedLexTransducer<N, G> compiler) {
        this.compiler = compiler;
        registerCommand("exit", "Exits REPL", (a, b, d, c) -> "");
        registerCommand("load", "Loads source code from file", ReplCommand.replLoad());
        registerCommand("pipes", "Lists all currently defined pipelines", ReplCommand.replListPipes());
        registerCommand("run", "Runs pipeline for the given input", ReplCommand.replRun());
        registerCommand("ls", "Lists all currently defined transducers", ReplCommand.replList());
        registerCommand("size", "Size of transducer is the number of its states", ReplCommand.replSize());
        registerCommand("equal",
                "Tests if two DETERMINISTIC transducers are equal. Does not work with nondeterministic ones!",
                ReplCommand.replEqual());
        registerCommand("is_det", "Tests whether transducer is deterministic", ReplCommand.replIsDeterministic());
        registerCommand("export", "Exports transducer to STAR (Subsequential Transducer ARchie) binary file",
                ReplCommand.replExport());
        registerCommand("eval", "Evaluates transducer on requested input", ReplCommand.replEval());
        registerCommand("rand_sample", "Generates random sample of input:output pairs produced by ths transducer",
                ReplCommand.replRandSample());
        registerCommand("vis", "Visualizes transducer as a graph", ReplCommand.replVisualize());
    }

    public String run(String line, Consumer<String> log, Consumer<String> debug) {
        if (line.startsWith(":")) {
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
            if (firstWord.startsWith("?")) {
                final String noQuestionmark = firstWord.substring(1);
                if (noQuestionmark.isEmpty()) {
                    final StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, CmdMeta<N, G, String>> cmd : commands.entrySet()) {
                        final String name = cmd.getKey();
                        sb.append(":").append(name).append("\t").append(cmd.getValue().help).append("\n");
                    }
                    return sb.toString();
                } else {
                    final CmdMeta<N, G, String> cmd = commands.get(noQuestionmark);
                    return cmd.help;
                }
            } else {
                final CmdMeta<N, G, String> cmd = commands.get(firstWord);
                return cmd.cmd.run(compiler, log, debug, remaining);
            }

        } else {
            try {
                compiler.parseREPL(CharStreams.fromString(line));
                return null;
            } catch (CompilationError | NoViableAltException | EmptyStackException e) {
                return e.toString();
            }
        }
    }
}
