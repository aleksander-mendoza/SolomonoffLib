package net.alagris.cli;


import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.core.OSTIA;
import net.alagris.core.Pos;
import net.alagris.lib.*;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;


@CommandLine.Command(subcommands = {CLI.Learning.class, CLI.InteractiveRepl.class})
public class CLI {


    @CommandLine.Command(name = "learn")
    public static class Learning<N, G extends IntermediateGraph<Pos, E, P, N>> implements Callable<Integer> {

        private final HashMap<String, LearningFramework<?, Pos, E, P, Integer, N, G>> frameworks;

        @CommandLine.Parameters(index = "0", description = "Algorithm to use")
        private String algorithm;

        @CommandLine.Option(names = {"-i"}, description = "path to input file")
        private File input;

        public Learning(HashMap<String, LearningFramework<?, Pos, E, P, Integer, N, G>> frameworks) {
            this.frameworks = frameworks;
        }

        public <H> int learn(LearningFramework<H, Pos, E, P, Integer, N, G> framework) {
            return 0;
        }

        @Override
        public Integer call() throws Exception {
            if (!frameworks.containsKey(algorithm)) {
                System.err.println("Unrecognized learning algorithm '" + algorithm + "'! Available options are "+frameworks.keySet());
                return 1;
            }
            return learn(frameworks.get(algorithm));
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

    public static <N, G extends IntermediateGraph<Pos, E, P, N>>
    HashMap<String, LearningFramework<?, Pos, E, P, Integer, N, G>> makeFrameworks(Solomonoff<N, G> solomonoff) {
        final HashMap<String, LearningFramework<?, Pos, E, P, Integer, N, G>> frameworks = new HashMap<>();
        frameworks.put("rpni", LearnLibCompatibility.asLearningFramework(solomonoff.specs, LearnLibCompatibility::rpni));
        frameworks.put("rpni_edsm", LearnLibCompatibility.asLearningFramework(solomonoff.specs, LearnLibCompatibility::rpniEDSM));
        frameworks.put("rpni_mdl", LearnLibCompatibility.asLearningFramework(solomonoff.specs, LearnLibCompatibility::rpniMDL));
        frameworks.put("rpni_mealy", LearnLibCompatibility.asLearningFrameworkMealy(solomonoff.specs));
        frameworks.put("ostia", OSTIA.asLearningFramework(solomonoff.specs));
        return frameworks;
    }

    interface Supplier<X> {
        X get() throws Exception;
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> int cli(Supplier<HashMap<String, LearningFramework<?, Pos, E, P, Integer, N, G>>> frameworks, String[] args) {


        return new CommandLine(new CLI(), new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> aClass) throws Exception {
                if (aClass == Learning.class) {
                    return (K)new Learning<N,G>(frameworks.get());
                }else if(aClass==InteractiveRepl.class){
                    return (K) new InteractiveRepl();
                }
                return null;
            }
        }).execute(args);

    }

    public static void main(String[] args) {
        System.exit(cli(()->makeFrameworks(new ArrayBacked(Config.config())),args));
    }

}
