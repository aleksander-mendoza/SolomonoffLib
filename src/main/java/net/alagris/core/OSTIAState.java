package net.alagris.core;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface OSTIAState<E, S extends OSTIAState<E, S>> {
    enum Kind {
        UNKNOWN, ACCEPTING, REJECTING
    }

    Kind getKind();

    void setKind(Kind kind);

    E transition(int symbol);

    boolean isKnown(E edge);

    void setKnown(E edge, boolean isKnown);

    IntQueue getOutput(E edge);

    void setOutput(E edge, IntQueue out);

    public IntQueue getStateOutput();

    public void setStateOutput(IntQueue out);

    S getTarget(E edge);

    E edgeConstructor();

    void setChild(int symbol, E edge);

    /**
     * The IntQueue is consumed and should not be reused after calling this method.
     */
    public void pushback(IntQueue out);


    public static <E, S extends OSTIAState<E, S>> void buildPttOnward(S ptt, IntSeq input, final IntSeq out) {
        S pttIter = ptt;
        int outputIdx = 0;

        for (final int symbol : input) {//input index
            final E edge;
            if (pttIter.transition(symbol) == null) {
                edge = pttIter.edgeConstructor();
                if (out == null) {
                    pttIter.setKnown(edge, false);
                } else {
                    pttIter.setOutput(edge, IntQueue.asQueue(out, outputIdx, out.size()));
                    pttIter.setKnown(edge, true);
                    outputIdx = out.size();
                }
                pttIter.setChild(symbol, edge);
            } else {
                edge = pttIter.transition(symbol);
                if (out != null) {
                    if (pttIter.isKnown(edge)) {
                        IntQueue commonPrefixEdge = pttIter.getOutput(edge);
                        IntQueue commonPrefixEdgePrev = null;
                        int outPrefixIdx = outputIdx;
                        while (commonPrefixEdge != null && outPrefixIdx < out.size() &&
                                commonPrefixEdge.value == out.at(outPrefixIdx)) {
                            outPrefixIdx++;
                            commonPrefixEdgePrev = commonPrefixEdge;
                            commonPrefixEdge = commonPrefixEdge.next;
                        }
                        /*
                        informant=x
                        edge.out=y
                        ->
                        informant=lcp(x,y)^-1 x
                        edge=lcp(x,y)
                        pushback=lcp(x,y)^-1 y
                        */
                        if (commonPrefixEdgePrev == null) {
                            pttIter.setOutput(edge, null);
                        } else {
                            commonPrefixEdgePrev.next = null;
                        }
                        pttIter.getTarget(edge).pushback(commonPrefixEdge);
                        outputIdx = outPrefixIdx;
                    } else {
                        pttIter.setOutput(edge, IntQueue.asQueue(out, outputIdx, out.size()));
                        pttIter.setKnown(edge, true);
                        outputIdx = out.size();
                    }
                }
            }
            pttIter = pttIter.getTarget(edge);
        }
        if (pttIter.getKind() == OSTIAState.Kind.ACCEPTING) {
            if (out==null) {
                throw new IllegalArgumentException("For input '" + input + "' the state output is '" + pttIter.getStateOutput() +
                        "' but training sample tells to reject");
            }
            if (!IntQueue.equals(pttIter.getStateOutput(), out.sub(outputIdx))) {
                throw new IllegalArgumentException("For input '" + input + "' and output '" + out + "' the state output is '" + pttIter.getStateOutput() +
                        "' but training sample has remaining suffix '" + out.sub(outputIdx) + '\'');
            }
        } else if (pttIter.getKind() == OSTIAState.Kind.REJECTING) {
            if (out!=null) {
                throw new IllegalArgumentException("For input '" + input + "' the state rejects but training sample " +
                        "has remaining suffix '" + pttIter.getStateOutput() +
                        "'");
            }
        } else {
            assert pttIter.getKind() == OSTIAState.Kind.UNKNOWN;
            pttIter.setKind(out == null ? OSTIAState.Kind.REJECTING : OSTIAState.Kind.ACCEPTING);
            pttIter.setStateOutput(out == null ? null : IntQueue.asQueue(out, outputIdx, out.size()));
        }
    }

}
