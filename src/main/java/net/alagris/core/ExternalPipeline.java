package net.alagris.core;

import java.util.List;
import java.util.function.Function;

public interface ExternalPipeline {
    Function<Seq<Integer>,Seq<Integer>> make(Pos pos, List<Pair<IntSeq, IntSeq>> text) throws CompilationError;
}
