package net.alagris;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static net.alagris.LexUnicodeSpecification.*;

/**
 * Simple implementation of command-line interface for the compiler
 */
public class CLI {

    public static class OptimisedLexTransducer<N, G extends IntermediateGraph<Pos, E, P, N>>{
        final LexUnicodeSpecification<N, G> specs;
        final ParserListener<Pos, E, P, Integer, IntSeq,Integer, N, G> parser;
        final ArrayList<ParserListener.Type<Pos, E, P, N, G>> types = new ArrayList<>();
        final HashMap<String,Specification.RangedGraph<Pos, Integer, E, P>> optimised = new HashMap<>();

        public OptimisedLexTransducer(LexUnicodeSpecification<N, G> specs) throws CompilationError {
            this.specs = specs;
            parser = specs.makeParser(types);
            parser.addDotAndHashtag();
        }
        public void parse(CharStream source) throws CompilationError {
            parser.parse(source);
        }
        public void pseudoMinimise() throws CompilationError {
            for(GMeta<Pos, E, P, N, G> graph:specs.variableAssignments.values()){
                specs.pseudoMinimize(graph.graph);
            }
        }
        public void optimise() throws CompilationError {
            for(GMeta<Pos, E, P, N, G> graph:specs.variableAssignments.values()){
                final Specification.RangedGraph<Pos, Integer, E, P> optimal = specs.optimiseGraph(graph.graph);
                optimised.put(graph.name,optimal);
            }
        }
        public void reduceEdges() throws CompilationError {
            for(RangedGraph<Pos, Integer, E, P> optimal:optimised.values()){
                specs.reduceEdges(optimal);
            }
        }
        public void checkStrongFunctionality() throws CompilationError {
            for(RangedGraph<Pos, Integer, E, P> optimal:optimised.values()){
                specs.checkStrongFunctionality(optimal);
            }
        }
        public void typecheck() throws CompilationError {
            for(ParserListener.Type<Pos, E, P, N, G> type:types){
                final Specification.RangedGraph<Pos, Integer, E, P> optimal = optimised.get(type.name);
                final Specification.RangedGraph<Pos, Integer, E, P> lhs = specs.optimiseGraph(type.lhs);
                final Specification.RangedGraph<Pos, Integer, E, P> rhs = specs.optimiseGraph(type.rhs);
                final Pos graphPos = specs.varAssignment(type.name).pos;
                specs.typecheck(type.name,graphPos,type.meta,optimal,lhs,rhs);
            }
        }
        public String run(String name,String input){
            return specs.evaluate(optimised.get(name),input);
        }
    }


    public static class OptimisedHashLexTransducer extends
            OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, E>, HashMapIntermediateGraph<Pos, E, P>>{

        public OptimisedHashLexTransducer(boolean eagerMinimisation) throws CompilationError {
            super(new HashMapIntermediateGraph.LexUnicodeSpecification(eagerMinimisation));
        }
        public OptimisedHashLexTransducer(CharStream source,boolean eagerMinimisation,boolean reduceEdges) throws CompilationError {
            this(eagerMinimisation);
            parse(source);
//            if(minimise)pseudoMinimise();
            optimise();
            if(reduceEdges)reduceEdges();
            checkStrongFunctionality();
            typecheck();
        }
        public OptimisedHashLexTransducer(String source,boolean eagerMinimisation,boolean reduceEdges) throws CompilationError {
            this(CharStreams.fromString(source), eagerMinimisation,reduceEdges);
        }
    }
    public static void main(String[] args) throws IOException, CompilationError {
        if(args.length!=1){
            System.err.println("Provide one path to file with source code!");
            System.exit(-1);
        }
        final OptimisedHashLexTransducer optimised = new OptimisedHashLexTransducer(
                CharStreams.fromFileName(args[0]), true,true);
        System.out.println("All loaded correctly!");
        final Scanner sc = new Scanner(System.in);
        while(sc.hasNextLine()){
            final String line = sc.nextLine();
            final int space = line.indexOf(' ');
            final String function = line.substring(0,space);
            final String input = line.substring(space+1);
            final String output = optimised.run(function,input);
            System.out.println(output);
        }
    }
}
