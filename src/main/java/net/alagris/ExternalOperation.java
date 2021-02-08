package net.alagris;

import java.util.List;

public interface ExternalOperation<G> {
    G call(Pos pos, List<G> text) throws CompilationError;
}
