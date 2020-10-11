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
public interface ParseSpecs<Pipeline,Var, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>> {

    G getGraph(Var variable);
    Pos getDefinitionPos(Var variable);
    String getName(Var variable);
    Specification.RangedGraph<V, A, E, P> getOptimised(Var variable) throws CompilationError;



    /**
     * @return graph that should be substituted for a given
     * variable identifier or null if no such graph is known (either it was not defined or it was defined and later consumed)
     */
    public Var consumeVariable(String varId);

    /**
     * Introduction of new variable into context of linear logic.
     */
    Var introduceVariable(String name, Pos pos, G graph, boolean alwaysCopy) throws CompilationError;

    /**
     * Get a copy of variable without consuming it. This corresponds to exponential operator in linear logic.
     *
     * @return null if no such variable is available at this point (either it was not defined or it was defined and later consumed)
     */
    Var copyVariable(String var);

    /**
     * You can get direct access to variable without consuming it but you have to promise that you won't mutate it.
     *
     * @return null if no such variable is available at this point (either it was not defined or it was defined and later consumed)
     */
    Var borrowVariable(String var);

    Specification<V, E, P, A, O, W, N, G> specification();

    void typecheckProduct(Pos pos, String funcName, G in, G out) throws CompilationError;

    void typecheckFunction(Pos pos, String funcName, G in, G out) throws CompilationError;

    public G externalFunction(Pos pos, String functionName, List<Pair<O, O>> args) throws CompilationError;

    public G externalOperation(Pos pos, String functionName, List<G> args) throws CompilationError;

    Pipeline makeNewPipeline();

    /**
     * @param name should not contain the @ sign as it is already implied by this methods
     */
    void registerNewPipeline(Pos pos, Pipeline pipeline, String name) throws CompilationError;

    Pipeline appendAutomaton(Pos pos, Pipeline pipeline, G g) throws CompilationError;

    Pipeline appendLanguage(Pos pos, Pipeline pipeline, G g) throws CompilationError;

    Pipeline appendExternalFunction(Pos pos, Pipeline pipeline, String funcName, List<Pair<O, O>> args) throws CompilationError;

    Pipeline appendPipeline(Pos pos, Pipeline pipeline, String nameOfOtherPipeline) throws CompilationError;


}