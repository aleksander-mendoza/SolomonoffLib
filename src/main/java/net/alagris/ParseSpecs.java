package net.alagris;

import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Specifications for parsing. You can use this to customise parsing. For
 * instance, the standard parsing procedure takes string literals and produces
 * sequences of integers <tt>S&lt;Integer&gt;</tt>. It's possible, though it use
 * one's own custom notation of literals and parse complex
 * numbers/matrices/algebraic words/any Java objects.
 */
public interface ParseSpecs<M, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>> {
    /**
     * this function takes parsing context and produced meta-information that should
     * be associated with given AST node. It can be used to obtain line number which
     * would later be useful for debugging and printing meaningful error messages.
     */
    public M metaInfoGenerator(TerminalNode parseNode);

    public M metaInfoGenerator(ParserRuleContext parseNode);

    public M metaInfoNone();

    /**
     * it takes terminal node associated with particular string literal that will be
     * used to build Product node in AST.
     */
    public O parseStr(TerminalNode parseNode) throws CompilationError;

    /**
     * Parses weights. In the source code weights are denoted with individual
     * integers. You may parse them to something else than numbers if you want.
     */
    public W parseW(TerminalNode parseNode) throws CompilationError;

    /**
     * Parses ranges. In the source code ranges are denoted with pairs of unicode
     * codepoints. You may parse them to something else if you want.
     */
    public Pair<A, A> parseRange(int codepointFrom, int codepointTo);

    /**
     * The largest range of values associated with the . dot
     */
    Pair<A, A> dot();

    /**
     * The special value associated with the # symbol
     */
    A hashtag();

    /**
     * @return graph that should be substituted for a given
     * variable identifier or null if no such graph is known
     */
    public GMeta<V,E,P,N, G> varAssignment(String varId);

    public void registerVar(GMeta<V,E,P,N, G> g) throws CompilationError.DuplicateFunction;

    public Specification<V, E, P, A, O, W, N, G> specification();

    P epsilonUnion(@NonNull P eps1, @NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

    P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;

    V stateBuilder(M meta);

    default G copyVarAssignment(String var) {
        GMeta<V,E,P,N, G> g = varAssignment(var);
        return g == null ? null : specification().deepClone(g.graph);
    }



}