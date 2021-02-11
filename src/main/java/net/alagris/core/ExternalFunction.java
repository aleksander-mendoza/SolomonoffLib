package net.alagris.core;

import java.util.ArrayList;
import java.util.List;

public interface ExternalFunction<G> {
    G call(Pos pos, ArrayList<FuncArg<G, IntSeq>> args) throws CompilationError;
}
