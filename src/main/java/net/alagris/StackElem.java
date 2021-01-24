package net.alagris;

import java.util.ArrayList;
import java.util.Stack;

interface StackElem<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> {
    /**
     * consumes inputs
     */
    <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs);
}