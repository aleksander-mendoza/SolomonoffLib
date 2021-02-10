package net.alagris.core;

import java.util.List;

public interface ExternalFunction<G> {
    G call(Pos pos, List<Pair<IntSeq, IntSeq>> text) throws CompilationError;
}
