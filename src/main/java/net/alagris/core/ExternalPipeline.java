package net.alagris.core;

import java.util.List;
import java.util.function.Function;

public interface ExternalPipeline<G> {
    Function<Seq<Integer>,Seq<Integer>> make(Pos pos, List<FuncArg<G,IntSeq>> text) throws CompilationError;
}
