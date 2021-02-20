package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.ExternalFunctionsFromLearnLib;
import net.alagris.lib.LearnLibCompatibility;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.words.Alphabet;

import java.util.function.Function;

public class RPNIFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> implements
        LearningFramework<Pair<Alphabet<Integer>, DFA<?, Integer>>, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer, IntSeq, N, G> {

    private final LexUnicodeSpecification<N, G> specs;
    private final Function<FuncArg.Informant<?,IntSeq>, Pair<Alphabet<Integer>, DFA<?, Integer>>> learn;

    public RPNIFramework(LexUnicodeSpecification<N, G> specs, Function<FuncArg.Informant<?,IntSeq>, Pair<Alphabet<Integer>, DFA<?, Integer>>> learn) {
        this.specs = specs;
        this.learn = learn;
    }

    @Override
    public Pair<Alphabet<Integer>, DFA<?, Integer>> makeHypothesis(FuncArg.Informant<G,IntSeq> text) {
        return learn.apply(text);
    }

    @Override
    public boolean testHypothesis(Pair<Alphabet<Integer>, DFA<?, Integer>> hypothesis, Pair<IntSeq, IntSeq> newUnseenExample) {
        if (hypothesis.r().accepts(newUnseenExample.l())) {
            return newUnseenExample.r() != null;
        } else {
            return newUnseenExample.r() == null;
        }
    }

    @Override
    public G compileHypothesis(Pair<Alphabet<Integer>, DFA<?, Integer>> hypothesis) {
        return ExternalFunctionsFromLearnLib.dfaToIntermediate(specs, Pos.NONE, hypothesis);
    }

    @Override
    public Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P>
    optimiseHypothesis(Pair<Alphabet<Integer>, DFA<?, Integer>> hypothesis) {
        return specs.convertCustomGraphToRanged(LearnLibCompatibility.asGraph(specs, hypothesis.r(), hypothesis.l(), o -> Pos.NONE), LexUnicodeSpecification.E::getToExclsuive);
    }
}
