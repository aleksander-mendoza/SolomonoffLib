package net.alagris.cli;

import net.alagris.cli.conv.Compiler;
import net.alagris.cli.conv.ThraxParser;
import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;
import org.antlr.v4.runtime.CharStreams;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "convert", description = "Convert Thrax to Solomonoff")
public class Convert<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Path to original Thrax file")
    private String thraxFile;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Path to produced Solomonoff file")
    private String solFile;

    @CommandLine.Option(names = {"-t", "--transducer"}, description = "Transducer t oconvert")
    private String transducerName;

    @Override
    public Integer call() throws Exception {
        final ThraxParser<N, G> tp = ThraxParser.parse(new File(thraxFile), CharStreams.fromFileName(thraxFile));
        if (transducerName != null) {
            final HashSet<String> toExport = tp.fileImportHierarchy.peek().export;
            toExport.clear();
            toExport.add(transducerName);
        }
        final String sol = Compiler.compileSolomonoff(false, true, tp);
        if (solFile != null) {
            try (PrintWriter pw = new PrintWriter(solFile)) {
                pw.print(sol);
            }
        }else{
            System.out.println(sol);
        }
        return 0;
    }
}