package net.alagris;

import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class CLI {

    public static class OptimisedLexTransducer{
        final HashMapIntermediateGraph.LexUnicodeSpecification specs = new HashMapIntermediateGraph.LexUnicodeSpecification();
        final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> graph;

        public OptimisedLexTransducer(String sourceCode, String functionName) throws CompilationError {
            final Compiler.Funcs<Pos, Integer, LexUnicodeSpecification.IntSeq, Integer> funcs = specs.parse(CharStreams.fromString(sourceCode));
            specs.compile(funcs);
            specs.checkStrongFunctionality(funcs);
            specs.typecheck(funcs);
            graph = specs.optimiseVar(functionName);
        }

        public String run(String input){
            return specs.evaluate(graph,input);
        }
    }



    public static void main(String[] args) throws IOException, CompilationError {
        if(args.length!=0){
            System.err.println("Provide one path to file with source code!");
            System.exit(-1);
        }
        final HashMapIntermediateGraph.LexUnicodeSpecification specs = new HashMapIntermediateGraph.LexUnicodeSpecification();
        final Compiler.Funcs<Pos, Integer, LexUnicodeSpecification.IntSeq, Integer> funcs = specs.parse(CharStreams.fromFileName(args[0]));
        specs.compile(funcs);
        specs.checkStrongFunctionality(funcs);
        specs.typecheck(funcs);

        final HashMap<String,Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P>>
                optimal = new HashMap<>();

        final Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()){
            final String line = sc.nextLine();
            final int space = line.indexOf(' ');
            final String function = line.substring(0,space);
            final String input = line.substring(space+1);
            final Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> f =
                    optimal.computeIfAbsent(function,specs::optimiseVar);
            final String output = specs.evaluate(f,input);
            System.out.println(output);
        }
    }
}
