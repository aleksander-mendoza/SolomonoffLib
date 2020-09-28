package net.alagris;

import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

/**
 * Specifications for parsing. You can use this to customise parsing. For
 * instance, the standard parsing procedure takes string literals and produces
 * sequences of integers <tt>S&lt;Integer&gt;</tt>. It's possible, though it use
 * one's own custom notation of literals and parse complex
 * numbers/matrices/algebraic words/any Java objects.
 */
public interface ParseSpecs<V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>> {

    /**
     * @return graph that should be substituted for a given
     * variable identifier or null if no such graph is known
     */
    public GMeta<V,E,P,N, G> varAssignment(String varId);

    public void registerVar(GMeta<V,E,P,N, G> g) throws CompilationError;

    public Specification<V, E, P, A, O, W, N, G> specification();

    default G copyVarAssignment(String var) {
        GMeta<V,E,P,N, G> g = varAssignment(var);
        return g == null ? null : specification().deepClone(g.graph);
    }

    public G externalFunctionOnText(Pos pos,String functionName, List<String> args) throws CompilationError.UndefinedExternalFunc;
    public G externalFunctionOnInformant(Pos pos,String functionName, List<Pair<String,String>> args) throws CompilationError.UndefinedExternalFunc;



}