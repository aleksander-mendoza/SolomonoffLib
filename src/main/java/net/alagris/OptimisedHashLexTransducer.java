package net.alagris;

public class OptimisedHashLexTransducer
        extends OptimisedLexTransducer<HashMapIntermediateGraph.N<Pos, LexUnicodeSpecification.E>, HashMapIntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P>> {

    public OptimisedHashLexTransducer(Config config) throws CompilationError {
        super(new HashMapIntermediateGraph.LexUnicodeSpecification(config));
    }

}
