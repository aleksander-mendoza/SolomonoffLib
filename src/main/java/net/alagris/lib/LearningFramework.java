package net.alagris.lib;

import net.alagris.core.IntSeq;
import net.alagris.core.IntermediateGraph;
import net.alagris.core.Pair;
import net.alagris.core.Specification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public interface LearningFramework<H, V, E, P, In, N, G extends IntermediateGraph<V, E, P, N>> {

    /**This function implements some passive learning algorithm*/
    H makeHypothesis(List<Pair<IntSeq, IntSeq>> text);

    boolean testHypothesis(H hypothesis, Pair<IntSeq, IntSeq> newUnseenExample);

    G compileHypothesis(H hypothesis);
    Specification.RangedGraph<V,In,E,P> optimiseHypothesis(H hypothesis);


}
