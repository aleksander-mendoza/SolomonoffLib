package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.ExternalFunctionsFromSolomonoff;

import java.util.Objects;

public abstract class OSTIAAbstractFramework<C,N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
        implements LearningFramework<Pair<OSTIAArbitraryOrder.State<C>, IntEmbedding>, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer,IntSeq, N, G> {

    protected final LexUnicodeSpecification<N, G> specs;
    protected final OSTIAArbitraryOrder.ScoringFunction<C> scoring;
    protected final OSTIAArbitraryOrder.MergingPolicy<C> policy;

    public OSTIAAbstractFramework(LexUnicodeSpecification<N, G> specs,OSTIAArbitraryOrder.ScoringFunction<C> scoring, OSTIAArbitraryOrder.MergingPolicy<C> policy
                                    ) {
        this.specs = specs;
        this.scoring = scoring;
        this.policy = policy;
    }

    @Override
    public boolean testHypothesis(Pair<OSTIAArbitraryOrder.State<C>, IntEmbedding> hypothesis, Pair<IntSeq, IntSeq> newUnseenExample) {
        return Objects.equals(OSTIAArbitraryOrder.run(hypothesis.l(), Util.mapIterLazy(newUnseenExample.l().iterator(),hypothesis.r()::embed)), newUnseenExample.r());
    }

    @Override
    public G compileHypothesis(Pair<OSTIAArbitraryOrder.State<C>, IntEmbedding> hypothesis) {
        return specs.convertCustomGraphToIntermediate(OSTIAState.asGraph(specs, hypothesis.l(), hypothesis.r()::retrieve, x -> Pos.NONE));
    }

    @Override
    public Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P> optimiseHypothesis(Pair<OSTIAArbitraryOrder.State<C>, IntEmbedding> hypothesis) {
        return specs.convertCustomGraphToRanged(OSTIAState.asGraph(specs, hypothesis.l(), hypothesis.r()::retrieve, x -> Pos.NONE), LexUnicodeSpecification.E::getToExclsuive);
    }

    public static class OSTIAMaxOverlapFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> extends OSTIAAbstractFramework<OSTIAArbitraryOrder.StatePTT,N, G>{
        public OSTIAMaxOverlapFramework(LexUnicodeSpecification<N, G> specs, OSTIAArbitraryOrder.ScoringFunction<OSTIAArbitraryOrder.StatePTT> scoring, OSTIAArbitraryOrder.MergingPolicy<OSTIAArbitraryOrder.StatePTT> policy) {
            super(specs, scoring, policy);
        }


        @Override
        public Pair<OSTIAArbitraryOrder.State<OSTIAArbitraryOrder.StatePTT>, IntEmbedding> makeHypothesis(FuncArg.Informant<G, IntSeq> text) {
            return ExternalFunctionsFromSolomonoff.inferOSTIAMaxOverlap(text, scoring,policy);
        }
    }

    public static class OSTIAMaxDeepOverlapFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> extends OSTIAAbstractFramework<Void,N, G>{

        public OSTIAMaxDeepOverlapFramework(LexUnicodeSpecification<N, G> specs, OSTIAArbitraryOrder.ScoringFunction<Void> scoring, OSTIAArbitraryOrder.MergingPolicy<Void> policy) {
            super(specs, scoring, policy);
        }

        @Override
        public Pair<OSTIAArbitraryOrder.State<Void>, IntEmbedding> makeHypothesis(FuncArg.Informant<G, IntSeq> text) {
            return ExternalFunctionsFromSolomonoff.inferOSTIAMaxDeepOverlap(text, scoring,policy);
        }
    }
}
