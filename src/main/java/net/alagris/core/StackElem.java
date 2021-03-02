package net.alagris.core;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiConsumer;

public interface StackElem<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> {
    /**
     * consumes inputs
     */
    <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                     Stack<StackElem<V, In, E, P, N, G>> stack,
                                     ArrayList<Seq<In>> inputs,
                                     BiConsumer<StackElem<V, In, E, P, N, G>,ArrayList<Seq<In>>> callback);
}