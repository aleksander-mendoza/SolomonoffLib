package net.alagris.lib;

import net.alagris.core.ArrayIntermediateGraph;
import net.alagris.core.CompilationError;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;

public class ArrayBacked
        extends Solomonoff<ArrayIntermediateGraph.N<Pos, LexUnicodeSpecification.E>, ArrayIntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P>> {

    public ArrayBacked(Config config) throws CompilationError {
        super(new ArrayIntermediateGraph.LexUnicodeSpecification(config),config);
    }

}
