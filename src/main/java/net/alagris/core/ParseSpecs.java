package net.alagris.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Specifications for parsing. You can use this to customise parsing. For
 * instance, the standard parsing procedure takes string literals and produces
 * sequences of integers <tt>S&lt;Integer&gt;</tt>. It's possible, though it use
 * one's own custom notation of literals and parse complex
 * numbers/matrices/algebraic words/any Java objects.
 */
public interface ParseSpecs<Var, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>> {

    O singletonOutput(A in);
    G getGraph(Var variable);
    Pos getDefinitionPos(Var variable);
    String getName(Var variable);
    Specification.RangedGraph<V, A, E, P> getOptimised(Var variable) throws CompilationError;



    /**
     * @return graph that should be substituted for a given
     * variable identifier or null if no such graph is known (either it was not defined or it was defined and later consumed)
     */
    public Var consumeVariable(String varId);

    Iterator<Var> iterateVariables();

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

    public G externalFunction(Pos pos, String functionName, ArrayList<FuncArg<G, O>> args) throws CompilationError;

    public Function<Seq<A>,Seq<A>> externalPipeline(Pos pos, String functionName, List<Pair<O, O>> args) throws CompilationError;

    /**
     * @param name should not contain the @ sign as it is already implied by this methods
     */
    void registerNewPipeline(Pipeline<V,A, E, P,N,G> pipeline, String name) throws CompilationError;

    Pipeline<V,A, E, P,N,G> getPipeline( String name);


}