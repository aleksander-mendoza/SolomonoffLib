package net.alagris.core.learn;

import net.alagris.core.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface OSTIAState<E, S extends OSTIAState<E, S>> {
    static <E,S extends OSTIAState<E,S>> ArrayList<S> indexAllStates(S transducer, BiConsumer<Integer,S> callback) {
        final ArrayList<S> states = new ArrayList<>();
        int stackHeight = 0;
        states.add(transducer);
        while (stackHeight < states.size()) {
            final S s = states.get(stackHeight);
            callback.accept(stackHeight,s);
            stackHeight++;
            for (int i=0;i<s.transitionCount();i++) {
                final E e = s.transition(i);
                if (e != null) {
                    assert !states.contains(s.getTarget(e));//initially transducer is a tree
                    states.add(s.getTarget(e));
                }
            }
        }
        return states;
    }

    enum Kind {
        UNKNOWN, ACCEPTING, REJECTING
    }

    Kind getKind();

    void setKind(Kind kind);

    E transition(int symbol);

    boolean isKnown(E edge);

    void setKnown(E edge, boolean isKnown);

    IntQueue getOutput(E edge);

    int transitionCount();

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


    public static <E, S extends OSTIAState<E, S>> void buildPttOnward(S ptt, IntEmbedding alph, IntSeq input, final IntSeq out) {
        S pttIter = ptt;
        int outputIdx = 0;

        for (int symbol : input) {//input index
            symbol = alph.embed(symbol);
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


    public static <E, S extends OSTIAState<E, S>> @Nullable IntSeq run(S init, IntEmbedding alph, Iterable<Integer> input) {
        return run(init, alph, input.iterator());
    }

    public static <E, S extends OSTIAState<E, S>> @Nullable IntSeq run(S init, IntEmbedding alph,Iterator<Integer> input) {
        final List<Integer> output = new ArrayList<>();
        S iter = init;
        while (input.hasNext()) {
            final Integer j = input.next();
            if (j == null) return null;
            final Integer i = alph.embed(j);
            if (i==null) return null;
            final E edge = iter.transition(i);
            if (edge == null) {
                return null;
            }
            IntQueue q = iter.getOutput(edge);
            iter = iter.getTarget(edge);
            while (q != null) {
                output.add(q.value);
                q = q.next;
            }
        }
        if (iter.getKind() != OSTIAState.Kind.ACCEPTING) {
            return null;
        }
        IntQueue q = iter.getStateOutput();
        while (q != null) {
            output.add(q.value);
            q = q.next;
        }
        int[] arr = new int[output.size()];
        for (int i = 0; i < output.size(); i++) {
            arr[i] = output.get(i);
        }
        return new IntSeq(arr);
    }



    public static <T, S extends OSTIAState<T,S>, V, N, G extends IntermediateGraph<Pos, LexUnicodeSpecification.E, LexUnicodeSpecification.P, N>>
    Specification.CustomGraph<S, Integer, LexUnicodeSpecification.E, LexUnicodeSpecification.P, V> asGraph(LexUnicodeSpecification<N, G> specs,
                                                                                                           S transducer,
                                                                                                           Function<Integer, Integer> indexToSymbol,
                                                                                                           Function<S, V> shortestAsMeta) {
        return asGraph(specs, transducer, indexToSymbol, (in, out) -> new LexUnicodeSpecification.E(in - 1, in, out, 0), IntSeq::new, shortestAsMeta);
    }

    public static <T,S extends OSTIAState<T,S>, V, E, P, In, Out, W, N, G extends IntermediateGraph<?, E, P, N>>
    Specification.CustomGraph<S, Integer, E, P, V> asGraph(Specification<?, E, P, In, Out, W, N, G> specs,
                                                           S transducer,
                                                           Function<Integer, In> indexToSymbol,
                                                           BiFunction<In, Out, E> fullEdge,
                                                           Function<IntQueue, Out> convertOutput,
                                                           Function<S, V> shortestAsMeta) {
        return new Specification.CustomGraph<S, Integer, E, P, V>() {

            @Override
            public S init() {
                return transducer;
            }

            @Override
            public P stateOutput(S state) {
                if (state.getKind() == OSTIAState.Kind.ACCEPTING) {
                    final Out fin = convertOutput.apply(state.getStateOutput());
                    return specs.createPartialEdge(fin, specs.weightNeutralElement());
                }
                return null;
            }

            @Override
            public Iterator<Integer> outgoing(S state) {
                return new Iterator<Integer>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < state.transitionCount();
                    }

                    @Override
                    public Integer next() {
                        final Integer out = state.transition(i) == null ? null : i;
                        i++;
                        return out;
                    }
                };
            }

            @Override
            public S target(S state, Integer transition) {
                return state.getTarget(state.transition(transition));
            }

            @Override
            public E edge(S state, Integer transition) {
                final Out out = convertOutput.apply(state.getOutput(state.transition(transition)));
                final In in = indexToSymbol.apply(transition);
                return fullEdge.apply(in, out);
            }

            @Override
            public V meta(S state) {
                return shortestAsMeta.apply(state);
            }
        };
    }

}
