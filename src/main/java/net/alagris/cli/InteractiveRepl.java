package net.alagris.cli;


import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.lib.*;
import picocli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


@CommandLine.Command(name = "repl")
public class InteractiveRepl implements Callable<Integer> {


    @CommandLine.Option(names = {"-b", "--backed-by"}, description = "array, hash")
    private String backedBy = "array";

    @CommandLine.Option(names = {"--silent"})
    private boolean silent = false;


    public <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    int run(Solomonoff<N, G> compiler) throws IOException {
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
        repl.registerCommand("verbose", "Prints additional debug logs", "", CommandsFromSolomonoff.replVerbose(debug));
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


    public static void main(String[] args) {
        System.exit(new CommandLine(new InteractiveRepl()).execute(args));
    }

}
