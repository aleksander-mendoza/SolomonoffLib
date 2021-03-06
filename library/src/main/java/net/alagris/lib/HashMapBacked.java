package net.alagris.lib;

import net.alagris.core.CompilationError;
import net.alagris.core.HashMapIntermediateGraph;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.core.Pos;

public class HashMapBacked
        extends Solomonoff<HashMapIntermediateGraph.N<Pos, LexUnicodeSpecification.E>, HashMapIntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P>> {

    public HashMapBacked(Config config) throws CompilationError {
        super(new HashMapIntermediateGraph.LexUnicodeSpecification(config),config);
    }

}
