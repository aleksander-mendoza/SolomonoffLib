package net.alagris.cli;


import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import org.jline.builtins.Nano;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
        final LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(parser)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .variable(LineReader.LIST_MAX, 100)
                .variable(LineReader.HISTORY_FILE, Paths.get("history"))
                .history(history)
                .build();
        final String prompt = ">";
        while (true) {
            try {
                String line = reader.readLine(prompt);
                repl.run(line, log, debug);
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
