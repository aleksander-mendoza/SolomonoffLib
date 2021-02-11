package net.alagris.lib;

import net.alagris.core.*;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;

import java.io.*;
import java.util.*;

public class ExternalFunctionsFromLearnLib {


    private ExternalFunctionsFromLearnLib(){}

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpni",
                (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpni(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EDSM(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpniEdsm",
                (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniEDSM(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_EMDL(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpniMdl",
                (pos, text) -> dfaToIntermediate(spec, pos, LearnLibCompatibility.rpniMDL(text)));
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> void addExternalRPNI_Mealy(
            LexUnicodeSpecification<N, G> spec) {
        spec.registerExternalFunction("rpniMealy", (pos, text) -> {
            Pair<Alphabet<Integer>, MealyMachine<?, Integer, ?, Integer>> alphAndMealy = LearnLibCompatibility
                    .rpniMealy(text);
            G g = LearnLibCompatibility.mealyToIntermediate(spec, alphAndMealy.l(), alphAndMealy.r(),
                    IntSeq::new,
                    s -> pos);
            return g;
        });
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> G dfaToIntermediate(LexUnicodeSpecification<N, G> spec,
                                                                                     Pos pos, Pair<Alphabet<Integer>, DFA<?, Integer>> alphAndDfa) {
        return LearnLibCompatibility.dfaToIntermediate(spec, alphAndDfa.l(), alphAndDfa.r(), s -> pos);
    }


}
