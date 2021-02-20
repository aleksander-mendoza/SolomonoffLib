package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.LearnLibCompatibility;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;

import java.util.ArrayList;

public class RPNIMealyFramework<N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>> implements
        LearningFramework<Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>>, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer, IntSeq, N, G> {

    private final LexUnicodeSpecification<N, G> specs;

    public RPNIMealyFramework(LexUnicodeSpecification<N, G> specs) {
        this.specs = specs;
    }

    @Override
    public Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> makeHypothesis(FuncArg.Informant<G,IntSeq> text) {
        return LearnLibCompatibility.rpniMealy(text);
    }

    @Override
    public boolean testHypothesis(Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> hypothesis, Pair<IntSeq, IntSeq> newUnseenExample) {
        ArrayList<Integer> out = new ArrayList<>(newUnseenExample.r().size());
        boolean accepted = hypothesis.r().trace(newUnseenExample.l(), out);
        if (accepted) {
            if (newUnseenExample.r() == null) return false;
            if (newUnseenExample.r().size() != out.size()) return false;
            for (int i = 0; i < out.size(); i++) {
                if (newUnseenExample.r().at(i) != out.get(i)) return false;
            }
            return true;
        } else {
            return newUnseenExample.r() == null;
        }
    }

    @Override
    public G compileHypothesis(Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> hypothesis) {
        return LearnLibCompatibility.mealyToIntermediate(specs, hypothesis.l(), hypothesis.r(), IntSeq::new, s -> Pos.NONE);
    }

    @Override
    public Specification.RangedGraph<Pos, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P>
    optimiseHypothesis(Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> hypothesis) {
        return specs.convertCustomGraphToRanged(LearnLibCompatibility.asGraphMealy(specs, hypothesis.r(), hypothesis.l(), IntSeq::new, o -> Pos.NONE), LexUnicodeSpecification.E::getToExclsuive);
    }
}
