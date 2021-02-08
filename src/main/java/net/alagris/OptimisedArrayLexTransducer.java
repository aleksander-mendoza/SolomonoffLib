package net.alagris;

public class OptimisedArrayLexTransducer
        extends OptimisedLexTransducer<ArrayIntermediateGraph.N<Pos, LexUnicodeSpecification.E>, ArrayIntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P>> {

    public OptimisedArrayLexTransducer(Config config) throws CompilationError {
        super(new ArrayIntermediateGraph.LexUnicodeSpecification(config));
    }

}
