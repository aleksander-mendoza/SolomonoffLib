package net.alagris.cli;


import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import net.alagris.lib.ArrayBacked;
import net.alagris.lib.Config;
import net.alagris.lib.HashMapBacked;
import net.alagris.lib.Solomonoff;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;


public class CLI {

    @CommandLine.Command(name = "learn")
    public static class Learning implements Callable<Integer> {

        @CommandLine.Parameters(index = "0", description = "Algorithm to use")
        private String algorithm;

        @CommandLine.Option(names = {"-i"},description = "path to input file")
        private File input;

        @Override
        public Integer call() throws Exception {
            switch (algorithm){
                case "rpni":

                    return 0;
                default:
                    System.err.println("Unrecognized learning algorithm '"+algorithm+"'");
                    return 1;
            }

        }
    }

    @CommandLine.Command(name = "repl")
    public static class InteractiveRepl implements Callable<Integer> {


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
    }


    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}
