package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.ExternalFunctionsFromSolomonoff;

import java.util.Objects;

public class OSTIAFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
        implements LearningFramework<Pair<OSTIA.State, IntEmbedding>, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer, IntSeq,N, G> {

    private final LexUnicodeSpecification<N, G> specs;

    public OSTIAFramework(LexUnicodeSpecification<N, G> specs) {
        this.specs = specs;
    }

    @Override
    public Pair<OSTIA.State, IntEmbedding> makeHypothesis(FuncArg.Informant<G,IntSeq> text) {
        return ExternalFunctionsFromSolomonoff.inferOSTIA(text);
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
