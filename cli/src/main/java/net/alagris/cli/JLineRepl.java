package net.alagris.cli;


import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import net.alagris.core.Util;
import org.jline.builtins.Nano;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.console.impl.SystemHighlighter;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class JLineRepl {


    private JLineRepl() {
    }


    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    void loopInTerminal(Repl<N, G> repl, Consumer<String> log, Consumer<String> debug) throws IOException {

        final DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedBracket(DefaultParser.Bracket.ANGLE, DefaultParser.Bracket.ROUND, DefaultParser.Bracket.SQUARE);
        parser.setEofOnUnclosedQuote(true);
        parser.setEscapeChars(null);
        parser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*");    // change default regex to support shell commands

        final Terminal terminal = TerminalBuilder.terminal();
        if (terminal.getWidth() == 0 || terminal.getHeight() == 0) {
            terminal.setSize(new Size(120, 40));   // hard coded terminal size when redirecting
        }
        Thread executeThread = Thread.currentThread();

        terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
        final DefaultHistory history = new DefaultHistory();
        final Completer completer = new Completer() {
            @Override
            public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
                final int words = parsedLine.words().size();
                if (words > 1 && parsedLine.words().get(0).startsWith(Repl.PREFIX)) {
                    final String prefix = parsedLine.word();
                    switch (parsedLine.words().get(0)) {
                    
                        case Repl.PREFIX + "load": {
                            files(list, prefix);
                            return;
                        }
                        case Repl.PREFIX + "size":
                        case Repl.PREFIX + "mem": {
                        	vars(list, prefix, repl);
                            pipes(list, prefix);
                            return;
                        }
                        case Repl.PREFIX + "run": {
                            pipes(list, prefix);
                            return;
                        }
                        case Repl.PREFIX + "exportDOT":
                            if (parsedLine.wordIndex() == 1) {
                                vars(list, prefix, repl);
                            } else if (parsedLine.wordIndex() == 2) {
                                files(list, prefix);
                                list.add(new Candidate(prefix + ".dot"));
                            }
                            return;
                        case Repl.PREFIX + "unset_all": {
                            list.add(new Candidate("pipelines"));
                            vars(list, prefix, repl);
                            pipes(list, prefix);
                            return;
                        }
                        case Repl.PREFIX + "rand_sample":
                            if (parsedLine.wordIndex() == 1) {
                                vars(list, prefix, repl);
                            } else if (parsedLine.wordIndex() == 2) {
                                list.add(new Candidate("of_size"));
                                list.add(new Candidate("of_length"));
                            }
                            return;
                        default:
                            vars(list, prefix, repl);
                            return;

                    }
                } else {
                    final String prefix = parsedLine.word();

                    if (prefix.startsWith(Repl.PREFIX)) {
                        final String noSlash = prefix.substring(1);
                        for (String cmd : repl.commands.keySet()) {
                            if (cmd.startsWith(noSlash)) {
                                list.add(new Candidate(Repl.PREFIX + cmd));
                            }
                        }
                        return;
                    }
                    if (prefix.startsWith("@")) {
                        pipes(list, prefix);
                        return;
                    }
                    if (prefix.startsWith("!!")) {
                        final String noCopy = prefix.substring(2);
                        for (String var : repl.compiler.specs.variableAssignments.keySet()) {
                            if (var.startsWith(noCopy)) {
                                list.add(new Candidate("!!" + var));
                            }
                        }
                        return;
                    }
                    vars(list, prefix, repl);
                }
            }

            private void pipes(List<Candidate> list, String prefix) {
                final String noAt = prefix.startsWith("@") ? prefix.substring(1) : prefix;
                for (String pip : repl.compiler.specs.pipelines.keySet()) {
                    if (pip.startsWith(noAt)) {
                        list.add(new Candidate("@" + pip));
                    }
                }
            }

            private void files(List<Candidate> list, String prefix) {
                final String[] files = new File(".").list((file, name) -> name.endsWith(".mealy") && name.startsWith(prefix));
                if (files != null) {
                    for (String name : files) {
                        list.add(new Candidate(name));
                    }
                }
            }

            private <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> void vars(List<Candidate> list, String prefix, Repl<N, G> repl) {
                for (String var : repl.compiler.specs.variableAssignments.keySet()) {
                    if (var.startsWith(prefix)) {
                        list.add(new Candidate(var));
                    }
                }
            }
        };

        final Nano.SyntaxHighlighter highlighter = Nano.SyntaxHighlighter.build("classpath:/syntax_highlighter.nanorc");
        final LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(new Highlighter() {
                    @Override
                    public AttributedString highlight(LineReader lineReader, String s) {
                        return highlighter.highlight(s);
                    }

                    @Override
                    public void setErrorPattern(Pattern pattern) {

                    }

                    @Override
                    public void setErrorIndex(int i) {

                    }
                })
                .parser(parser)
                .completer(completer)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .variable(LineReader.LIST_MAX, 100)
                .variable(LineReader.HISTORY_FILE, Paths.get("history"))
                .history(history)
                .build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);

        final String prompt = ">";

        while (true) {
            try {
                String line = reader.readLine(prompt);
                if (line.trim().equals(Repl.PREFIX + "exit")) break;

                String out = repl.run(line, log, debug);
                if (out != null) terminal.writer().println(out);
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
