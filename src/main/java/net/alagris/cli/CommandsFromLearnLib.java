package net.alagris.cli;

import net.alagris.core.CompilationError;
import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import net.alagris.lib.LearnLibCompatibility;

import java.io.File;

public class CommandsFromLearnLib {
    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> ReplCommand<N, G, String> replVisualize() {
        return (compiler, logs, debug, args) -> {
            LearnLibCompatibility.visualize(compiler.getOptimisedTransducer(args));
            return null;
        };
    }

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> ReplCommand<N, G, String> replExportDOT() {
        return (compiler, logs, debug, args) -> {
            final String[] parts = args.trim().split("\\s+");
            if(parts.length!=2){
                return "Expected two arguments: 'transducerName' and 'outputFilePath'";
            }
            LearnLibCompatibility.exportDOT(compiler.getOptimisedTransducer(parts[0]),new File(parts[1]));
            return null;
        };
    }
}
