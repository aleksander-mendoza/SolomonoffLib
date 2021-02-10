package net.alagris.cli;

import net.alagris.core.CompilationError;
import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import net.alagris.lib.LearnLibCompatibility;

public class CommandsFromLearnLib {
    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> ReplCommand<N, G, String> replVisualize() {
        return (compiler, logs, debug, args) -> {
            LearnLibCompatibility.visualize(compiler.getOptimisedTransducer(args));
            return null;
        };
    }
}
