package net.alagris.core.learn;

import net.alagris.core.*;
import net.alagris.lib.LearnLibCompatibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public interface LearningFramework<H, V, E, P, In, O, N, G extends IntermediateGraph<V, E, P, N>> {

    /**
     * This function implements some passive learning algorithm
     */
    H makeHypothesis(FuncArg.Informant<G, O> text);

    boolean testHypothesis(H hypothesis, Pair<O, O> newUnseenExample);

    G compileHypothesis(H hypothesis);

    Specification.RangedGraph<V, In, E, P> optimiseHypothesis(H hypothesis);


    public static final String[] ALGORITHMS = {"rpni","rpni_edsm","rpni_mdl","rpni_mealy","ostia","ostia_max_overlap"};

    public static <N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    LearningFramework<?, Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, Integer, IntSeq, N, G> forID(String id, LexUnicodeSpecification<N, G> specs) {
        switch (id) {
            case "rpni":
                return new RPNIFramework<N, G>(specs, LearnLibCompatibility::rpni);
            case "rpni_edsm":
                return new RPNIFramework<>(specs, LearnLibCompatibility::rpniEDSM);
            case "rpni_mdl":
                return new RPNIFramework<>(specs, LearnLibCompatibility::rpniMDL);
            case "rpni_mealy":
                return new RPNIMealyFramework<>(specs);
            case "ostia":
                return new OSTIAFramework<>(specs);
//            case "ostia_in_out_one_to_one_max_overlap":
//                return new OSTIAMaxOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_GREEDY());
            case "ostia_max_overlap":
                return new OSTIAAbstractFramework.OSTIAMaxOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_GREEDY());
            case "ostia_max_deep_overlap":
                return new OSTIAAbstractFramework.OSTIAMaxDeepOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_DEEP_OVERLAP(), OSTIAArbitraryOrder.POLICY_GREEDY());
            case "ostia_max_compatible_inputs":
                return new OSTIAAbstractFramework.OSTIAMaxOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_COMPATIBLE_INPUTS, OSTIAArbitraryOrder.POLICY_GREEDY());
            case "ostia_max_compatible":
                return new OSTIAAbstractFramework.OSTIAMaxOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_COMPATIBLE, OSTIAArbitraryOrder.POLICY_GREEDY());
            case "ostia_conservative":
                return new OSTIAAbstractFramework.OSTIAMaxOverlapFramework<>(specs,OSTIAArbitraryOrder.SCORING_MAX_OVERLAP, OSTIAArbitraryOrder.POLICY_THRESHOLD(1));
        }
        throw new IllegalArgumentException("Unexpected algorithm "+id+"! Choose one of "+ Arrays.toString(ALGORITHMS));
    }


    static <H, V, E, P, In, O, N, G extends IntermediateGraph<V, E, P, N>> G activeLearning(
            LearningFramework<H, V, E, P, In, O, N, G> framework,
            FuncArg.Informant<G, O> examplesSeenSoFar,
            LazyDataset<Pair<O, O>> dataset,
            int batchSize,
            Consumer<String> log) throws Exception {
        final ArrayList<Pair<O, O>> counterexamples = new ArrayList<>();
        int iteration = 1;
        final long beginInit = System.currentTimeMillis();
        H hypothesis = framework.makeHypothesis(examplesSeenSoFar);
        log.accept("Learning iteration "+iteration+" took "+(System.currentTimeMillis()-beginInit)+" miliseconds and used "+examplesSeenSoFar.size()+" examples");
        int totalNumberOfExamples;
        try {
            outer:
            while (true) {
                totalNumberOfExamples = 0;
                boolean hadCounterexample = false;
                Pair<O, O> newExample;
                dataset.begin();
                while ((newExample = dataset.next()) != null) {
                    if (Thread.interrupted()) break outer;
                    if (!framework.testHypothesis(hypothesis, newExample)) {
                        hadCounterexample = true;
                        counterexamples.add(newExample);
                        log.accept("New counterexample " + newExample);
                        if (counterexamples.size() >= batchSize) {
                            examplesSeenSoFar.addAll(counterexamples);
                            counterexamples.clear();
                            iteration++;
                            final long begin = System.currentTimeMillis();
                            hypothesis = framework.makeHypothesis(examplesSeenSoFar);
                            log.accept("Learning iteration " + iteration + " took " + (System.currentTimeMillis() - begin) + " miliseconds and used " + examplesSeenSoFar.size() + " examples");
                        }
                    }
                    totalNumberOfExamples++;
                }
                if (counterexamples.size() > 0) {
                    examplesSeenSoFar.addAll(counterexamples);
                    counterexamples.clear();
                    iteration++;
                    final long begin = System.currentTimeMillis();
                    hypothesis = framework.makeHypothesis(examplesSeenSoFar);
                    log.accept("Learning iteration " + iteration + " took " + (System.currentTimeMillis() - begin) + " miliseconds and used " + examplesSeenSoFar.size() + " examples");
                }
                if (!hadCounterexample) {//all examples passed correctly. Nothing more to learn
                    break;
                }
            }
            final int trainingExamples = examplesSeenSoFar.size();
            log.accept("Learning used " + trainingExamples + " example(s)");
            log.accept("Validated against " + (totalNumberOfExamples - trainingExamples) + " remaining example(s)");
            log.accept("Total number of examples is " + totalNumberOfExamples);
            final float percentage = ((float) examplesSeenSoFar.size()) / ((float) totalNumberOfExamples);
            log.accept("Learning used " + percentage * 100f + "% of examples");
            if (percentage > 0.7f) {
                log.accept("The model might have a very large generalization error");
            }
            if (percentage > 0.5f) {
                log.accept("You should consider providing more examples");
            }
            return framework.compileHypothesis(hypothesis);
        }finally {
            dataset.close();
        }
    }

}
