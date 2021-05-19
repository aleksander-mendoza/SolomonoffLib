package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.ExternalFunctionsFromSolomonoff;

import java.util.Objects;

public class OSTIAFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
        implements LearningFramework<Pair<OSTIA.State, IntEmbedding>, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer, IntSeq,N, G> {

    private final LexUnicodeSpecification<N, G> specs;
    private final boolean compress;
    /**@param compress - if true, then OSTIA will run as a minimisation algorithm,
     *                  rather than inference algorithm. In other words, it will
     *                  exactly preserve the recognised formal language*/
    public OSTIAFramework(LexUnicodeSpecification<N, G> specs, boolean compress) {
        this.specs = specs;
        this.compress = compress;
    }

    @Override
    public Pair<OSTIA.State, IntEmbedding> makeHypothesis(FuncArg.Informant<G,IntSeq> text) {
        return ExternalFunctionsFromSolomonoff.inferOSTIA(compress?text.filterOutNegative():text, compress);
    }

    @Override
    public boolean testHypothesis(Pair<OSTIA.State, IntEmbedding> hypothesis, Pair<IntSeq, IntSeq> newUnseenExample) {
        return Objects.equals(OSTIAState.run(hypothesis.l(),hypothesis.r(), newUnseenExample.l()), newUnseenExample.r());
    }

    @Override
    public G compileHypothesis(Pair<OSTIA.State, IntEmbedding> hypothesis) {
        return specs.convertCustomGraphToIntermediate(OSTIAState.asGraph(specs, hypothesis.l(), hypothesis.r()::retrieve, x -> Pos.NONE));
    }

    @Override
    public Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> optimiseHypothesis(Pair<OSTIA.State, IntEmbedding> hypothesis) {
        return specs.convertCustomGraphToRanged(OSTIAState.asGraph(specs, hypothesis.l(), hypothesis.r()::retrieve, x -> Pos.NONE), LexUnicodeSpecification.E::getToInclusive);
    }
}
