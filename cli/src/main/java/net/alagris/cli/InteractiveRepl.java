package net.alagris.cli;


import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.lib.*;
import org.antlr.v4.runtime.CharStreams;
import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(name = "repl", description = "Open interactive Solomonoff REPL console")
public class InteractiveRepl implements Callable<Integer> {

    @CommandLine.Option(names = {"-f", "--file"}, description = "load contents of file")
    private String file = null;

    @CommandLine.Option(names = {"-e", "--exec"}, description = "execute specific command on start")
    private String exec = null;

    @CommandLine.Option(names = {"-b", "--backed-by"}, description = "array, hash")
    private String backedBy = "array";

    @CommandLine.Option(names = {"--silent"})
    private boolean silent = false;


    public <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    int run(Solomonoff<N, G> compiler) throws Exception {
        final Repl<N, G> repl = new Repl<>(compiler);
        final ToggleableConsumer<String> debug = new ToggleableConsumer<String>() {
            boolean enabled = !silent;

            @Override
            public boolean isEnabled() {
                return enabled;
            }

            @Override
            public void setEnabled(boolean value) {
                enabled = value;
            }

            @Override
            public void accept(String s) {
                if (enabled) System.err.println(s);
            }
        };
        repl.registerCommand(Repl.VERBOSE, "Prints additional debug logs", "", CommandsFromSolomonoff.replVerbose(debug));
        if(file!=null){
            repl.compiler.parse(CharStreams.fromFileName(file));
        }
        if(exec!=null){
            if(repl.runMultiline(System.out::println, debug,exec)){
                return 0;
            }
        }

        JLineRepl.loopInTerminal(repl, System.out::println, debug);
        return 0;
    }

    @Override
    public Integer call() throws Exception {
        final Config config = new Config();
        switch (backedBy) {
            case "array":
                return run(new ArrayBacked(config));
            case "hash":
                return run(new HashMapBacked(config));
            default:
                System.err.println("Invalid value '" + backedBy + "'! Should be wither 'array' or 'hash'");
                return 1;
        }

    }


}
