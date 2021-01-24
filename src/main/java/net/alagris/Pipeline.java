package net.alagris;

import java.util.Stack;
import java.util.function.Function;

public interface Pipeline<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> {

    /**
     * Size of tuple
     */
    int size();

    V meta();


    class Automaton<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final Specification.RangedGraph<V, In, E, P> g;
        public final V meta;

        public Automaton(Specification.RangedGraph<V, In, E, P> g, V meta) {
            this.g = g;
            this.meta = meta;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class Assertion<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final Specification.RangedGraph<V, In, E, P> g;
        public final V meta;
        public final boolean runtime;

        public Assertion(Specification.RangedGraph<V, In, E, P> g, V meta, boolean runtime) {
            this.g = g;
            this.meta = meta;
            this.runtime = runtime;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class External<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final Function<Seq<In>, Seq<In>> f;
        public final V meta;

        public External(V meta, Function< Seq<In>, Seq<In>> f) {
            this.f = f;
            this.meta = meta;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class Alternative<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> lhs, rhs;

        /**
         * Evaluation is  most efficient when lhs is never instance of Alternative
         */
        public Alternative(V meta, Pipeline<V, In, E, P, N, G> lhs, Pipeline<V, In, E, P, N, G> rhs) {
            this.meta = meta;
            assert lhs.size() == rhs.size() : lhs.size() + " " + rhs.size();
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public int size() {
            return lhs.size();
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class Tuple<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> lhs, rhs;
        public final int size;

        /**
         * Evaluation is  most efficient when lhs is never instance of Tuple
         */
        public Tuple(V meta, Pipeline<V, In, E, P, N, G> lhs, Pipeline<V, In, E, P, N, G> rhs) {
            this.meta = meta;
            this.lhs = lhs;
            this.rhs = rhs;
            size = lhs.size() + rhs.size();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class Composition<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> lhs, rhs;

        /**
         * Evaluation is  most efficient when lhs is never instance of Composition
         */
        public Composition(V meta, Pipeline<V, In, E, P, N, G> lhs, Pipeline<V, In, E, P, N, G> rhs) {
            this.meta = meta;
            this.lhs = lhs;
            this.rhs = rhs;
            assert lhs.size() == rhs.size() : lhs.size() + " " + rhs.size();
        }

        @Override
        public int size() {
            return lhs.size();
        }

        @Override
        public V meta() {
            return meta;
        }
    }

    class Split<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> tuple;
        public final IntSeq inputSeparator;
        public final IntSeq outputSeparator;

        public Split(V meta, Pipeline<V, In, E, P, N, G> tuple, IntSeq inputSeparator, IntSeq outputSeparator) {
            this.meta = meta;
            this.tuple = tuple;
            this.inputSeparator = inputSeparator;
            this.outputSeparator = outputSeparator;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public V meta() {
            return meta;
        }
    }


    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> Seq<In> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, Seq<In> input) {
        /**This custom stack implementation allows for more efficient execution when there are millions of pipelines
         * stacked together. If it was implemented naively as recursive function, then Java stack would blow up*/
        final Stack<Pipeline<V, In, E, P, N, G>> stack = new Stack<>();
        stack.push(pipeline);
        while (!stack.isEmpty()) {
            if (input == null) return null;
            final Pipeline<V, In, E, P, N, G> p = stack.pop();
            if (p instanceof Assertion) {
                final Assertion<V, In, E, P, N, G> a = (Assertion<V, In, E, P, N, G>) p;
                if (a.runtime) {

                }
            } else if (p instanceof Composition) {
                final Composition<V, In, E, P, N, G> a = (Composition<V, In, E, P, N, G>) p;
                stack.push(a.rhs);//rhs is evaluated second
                stack.push(a.lhs);//lhs is evaluated first
            } else if (p instanceof Automaton) {
                final Automaton<V, In, E, P, N, G> a = (Automaton<V, In, E, P, N, G>) p;
                input = specs.evaluate(a.g, input);
            } else if (p instanceof Alternative) {
                final Alternative<V, In, E, P, N, G> a = (Alternative<V, In, E, P, N, G>) p;
                final Seq<In> lOut = eval(specs, a.lhs, input);
                if (lOut == null) {
                    stack.push(a.rhs);
                } else {
                    input = lOut;
                }
            } else if (p instanceof Tuple) {
                final Tuple<V, In, E, P, N, G> a = (Tuple<V, In, E, P, N, G>) p;

            } else if (p instanceof External) {
                final External<V, In, E, P, N, G> a = (External<V, In, E, P, N, G>) p;
                input = a.f.apply(input);
            } else {
                throw new ClassCastException(p.getClass() + " is not a recognized pipeline");
            }
        }
        return input;
    }


}
