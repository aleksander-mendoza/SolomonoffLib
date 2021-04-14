package net.alagris.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

public interface Pipeline<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> extends StackElem<V, In, E, P, N, G> {

    /**
     * Size of tuple
     */
    int size();

    V meta();


    class Automaton<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final Specification.RangedGraph<V, In, E, P> g;
        public final V meta;

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs,
                                                BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            assert inputs.size() == 1 : inputs;
            Seq<In> output = specs.evaluate(g, inputs.get(0));
            if (output == null) return null;
            inputs.set(0, output);
            return inputs;
        }

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

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs,
                                                BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            assert inputs.size() == 1;
            if (runtime) {
                if (!specs.accepts(g, inputs.get(0).iterator())) {
                    return null;
                }
            }
            return inputs;
        }

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

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs,
                                                BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            assert inputs.size() == 1;
            final Seq<In> out = f.apply(inputs.get(0));
            if (out == null) return null;
            inputs.set(0, out);
            return inputs;
        }

        public External(V meta, Function<Seq<In>, Seq<In>> f) {
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

    class AlternativeSecondBranch<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G> {

        private final ArrayList<Seq<In>> inputs;
        private final Pipeline<V, In, E, P, N, G> rhs;

        public AlternativeSecondBranch(ArrayList<Seq<In>> inputs, Pipeline<V, In, E, P, N, G> rhs) {
            this.inputs = new ArrayList<>(inputs);
            this.rhs = rhs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) {
                return rhs.eval(specs, stack, this.inputs, callback);
            } else {
                return inputs;
            }
        }
    }

    class Alternative<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> lhs, rhs;

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs,
                                                BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            stack.push(new AlternativeSecondBranch<>(inputs, rhs));//rhs is evaluated second
            stack.push(lhs);//lhs is evaluated first
            return inputs;
        }

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

    class TupleMerge<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G> {

        private final ArrayList<Seq<In>> lhsOutputs;

        public TupleMerge(ArrayList<Seq<In>> lhsOutputs) {
            this.lhsOutputs = lhsOutputs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            lhsOutputs.addAll(inputs);
            return lhsOutputs;
        }
    }

    class TupleSecondBranch<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G> {

        private final ArrayList<Seq<In>> inputs;
        private final Pipeline<V, In, E, P, N, G> rhs;

        public TupleSecondBranch(ArrayList<Seq<In>> inputs, Pipeline<V, In, E, P, N, G> rhs) {
            this.inputs = new ArrayList<>(inputs);
            this.rhs = rhs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            stack.push(new TupleMerge<>(inputs));
            stack.push(rhs);
            return this.inputs;
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

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            final int lSize = lhs.size();
            final int rSize = rhs.size();
            final ArrayList<Seq<In>> rInput = new ArrayList<>(rSize);
            final List<Seq<In>> sub = inputs.subList(lSize, inputs.size());
            rInput.addAll(sub);
            sub.clear();
            stack.push(new TupleSecondBranch<>(rInput, rhs));
            stack.push(lhs);
            assert inputs.size() == lSize;
            assert rInput.size() == rSize;
            return inputs;
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

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            stack.push(rhs);//rhs is evaluated second
            stack.push(lhs);//lhs is evaluated first
            return inputs;
        }
    }

    class Join<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G> {

        private final Seq<In> outputSeparator;

        public Join(Seq<In> outputSeparator) {
            this.outputSeparator = outputSeparator;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            int len = (inputs.size() - 1) * outputSeparator.size();//number of separators
            for (Seq<In> seq : inputs) {
                len += seq.size();
            }
            final In[] arr = (In[]) new Object[len];
            inputs.get(0).copyTo(0, arr);
            int offset = inputs.get(0).size();
            for (int i = 1; i < inputs.size(); i++) {
                outputSeparator.copyTo(offset, arr);
                offset += outputSeparator.size();
                final Seq<In> seq = inputs.get(i);
                assert seq != null;
                seq.copyTo(offset, arr);
                offset += seq.size();
            }
            assert offset == arr.length;
            inputs.clear();
            inputs.add(Seq.wrap(arr));
            return inputs;
        }
    }

    class Submatch<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final HashMap<In, Pipeline<V, In, E, P, N, G>> submatchHandler;

        public Submatch(V meta, HashMap<In, Pipeline<V, In, E, P, N, G>> submatchHandler) {
            this.meta = meta;
            this.submatchHandler = submatchHandler;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public V meta() {
            return meta;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
            if (inputs == null) return null;
            assert inputs.size() == 1;
            final Seq<In> out = specs.submatch(inputs.get(0), (group, in) -> {
                Pipeline<V, In, E, P, N, G> p = submatchHandler.get(group);
                if (p == null) return in;
                return Pipeline.eval(specs, p, Seq.wrap(in), callback);
            });
            if (out == null) return null;
            inputs.set(0, out);
            return inputs;
        }
    }


    public static final int BINARY_MARKER_END = 0;
    public static final int BINARY_MARKER_AUTOMATON = 1;
    public static final int BINARY_MARKER_ALTERNATIVE = 2;
    public static final int BINARY_MARKER_COMPOSITION = 3;
    public static final int BINARY_MARKER_TUPLE = 4;
    public static final int BINARY_MARKER_SUBMATCH = 5;
    public static final int BINARY_MARKER_SUBMATCH_ADD_TO_PREVIOUS = 6;
    public static final int BINARY_MARKER_ASSERTION = 7;
//    public static final int BINARY_MARKER_EXTERNAL = 7;

    interface Read<X>{
        X read() throws IOException;
    }
    interface Write<X>{
        void write(X x) throws IOException;
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> void
    compressBinaryPipeline(Pipeline<V, In, E, P, N, G> pipeline, DataOutputStream out, Write<In> writeIn,
                           Write<Specification.RangedGraph<V, In, E, P>> writeAutomaton) throws IOException {
        final HashMap<Specification.RangedGraph<V, In, E, P>, Integer> automata = foldAutomata(pipeline, new HashMap<>(), (map, aut) -> {
            map.computeIfAbsent(aut.g, r -> map.size());
            return map;
        });
        out.writeInt(automata.size());
        for (Map.Entry<Specification.RangedGraph<V, In, E, P>, Integer> e : automata.entrySet()) {
            out.writeInt(e.getValue());
            writeAutomaton.write(e.getKey());
        }
        class PipeOrMarker {
            final int marker;
            final In in;
            final Pipeline<V, In, E, P, N, G> p;

            public PipeOrMarker(Pipeline<V, In, E, P, N, G> pipeline) {
                p = pipeline;
                in = null;
                marker = -1;
            }

            public PipeOrMarker(int m) {
                marker = m;
                p = null;
                in = null;
            }

            public PipeOrMarker(In in, int marker) {
                this.marker = marker;
                this.in = in;
                p = null;
            }
        }
        final Stack<PipeOrMarker> stack = new Stack<>();
        stack.push(new PipeOrMarker(pipeline));
        while (!stack.isEmpty()) {
            final PipeOrMarker p = stack.pop();
            if (p.marker > -1) {
                out.writeInt(p.marker);
                if (p.in != null) {
                    writeIn.write(p.in);
                }
            } else if (p.p instanceof Automaton) {
                final Automaton<V, In, E, P, N, G> a = (Automaton<V, In, E, P, N, G>) p.p;
                final int idx = automata.get(a.g);
                out.writeInt(BINARY_MARKER_AUTOMATON);
                out.writeInt(idx);
            } else if (p.p instanceof Alternative) {
                final Alternative<V, In, E, P, N, G> alt = (Alternative<V, In, E, P, N, G>) p.p;
                stack.push(new PipeOrMarker(BINARY_MARKER_ALTERNATIVE));
                stack.push(new PipeOrMarker(alt.lhs));
                stack.push(new PipeOrMarker(alt.rhs));
            } else if (p.p instanceof Assertion) {
                stack.push(new PipeOrMarker(BINARY_MARKER_ASSERTION));
            } else if (p.p instanceof Composition) {
                final Composition<V, In, E, P, N, G> alt = (Composition<V, In, E, P, N, G>) p.p;
                stack.push(new PipeOrMarker(BINARY_MARKER_COMPOSITION));
                stack.push(new PipeOrMarker(alt.lhs));
                stack.push(new PipeOrMarker(alt.rhs));
            } else if (p.p instanceof Tuple) {
                final Tuple<V, In, E, P, N, G> alt = (Tuple<V, In, E, P, N, G>) p.p;
                stack.push(new PipeOrMarker(BINARY_MARKER_TUPLE));
                stack.push(new PipeOrMarker(alt.lhs));
                stack.push(new PipeOrMarker(alt.rhs));
            } else if (p.p instanceof External) {
                throw new IllegalArgumentException("Cannot export pipeline that uses external functions");
            } else if (p.p instanceof Submatch) {
                final Submatch<V, In, E, P, N, G> alt = (Submatch<V, In, E, P, N, G>) p.p;
                int marker = BINARY_MARKER_SUBMATCH;
                for (Map.Entry<In, Pipeline<V, In, E, P, N, G>> e : alt.submatchHandler.entrySet()) {
                    final In in = e.getKey();
                    stack.push(new PipeOrMarker(in, marker));
                    stack.push(new PipeOrMarker(e.getValue()));
                    marker = BINARY_MARKER_SUBMATCH_ADD_TO_PREVIOUS;
                }
            }
        }
        out.writeInt(BINARY_MARKER_END);
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> Pipeline<V, In, E, P, N, G>
    decompressBinaryPipeline(DataInputStream in, V meta, Read<In> readIn,
                             Read<Specification.RangedGraph<V, In, E, P>> readAutomaton) throws IOException {
        final int automataSize = in.readInt();
        final ArrayList<Specification.RangedGraph<V, In, E, P>> automata = Util.filledArrayList(automataSize, null);
        for (int i = 0; i < automataSize; i++) {
            final int automatonIndex = in.readInt();
            final Specification.RangedGraph<V, In, E, P> aut = readAutomaton.read();
            automata.set(automatonIndex, aut);
        }
        assert Util.forall(automata, Objects::nonNull);
        final Stack<Pipeline<V, In, E, P, N, G>> stack = new Stack<>();
        while (true) {
            final int next = in.readInt();
            switch (next) {
                case BINARY_MARKER_ASSERTION:{
                    stack.push(new Assertion<>(null,meta,false));
                    break;
                }
                case BINARY_MARKER_END:
                    assert stack.size()==1;
                    return stack.pop();
                case BINARY_MARKER_AUTOMATON: {
                    final int autIdx = in.readInt();
                    stack.push(new Automaton<>(automata.get(autIdx), meta));
                    break;
                }
                case BINARY_MARKER_ALTERNATIVE: {
                    stack.push(new Alternative<>(meta, stack.pop(), stack.pop()));
                    break;
                }
                case BINARY_MARKER_COMPOSITION: {
                    stack.push(new Composition<>(meta, stack.pop(), stack.pop()));
                    break;
                }
                case BINARY_MARKER_TUPLE: {
                    stack.push(new Tuple<>(meta, stack.pop(), stack.pop()));
                    break;
                }
                case BINARY_MARKER_SUBMATCH: {
                    final In submatchGroup = readIn.read();
                    final Pipeline<V, In, E, P, N, G> p = stack.pop();
                    final Submatch<V, In, E, P, N, G> sub = new Submatch<>(meta, new HashMap<>());
                    sub.submatchHandler.put(submatchGroup, p);
                    stack.push(sub);
                    break;
                }
                case BINARY_MARKER_SUBMATCH_ADD_TO_PREVIOUS: {
                    final In submatchGroup = readIn.read();
                    final Pipeline<V, In, E, P, N, G> p = stack.pop();
                    ((Submatch<V, In, E, P, N, G>) stack.peek()).submatchHandler.put(submatchGroup, p);
                    break;
                }
                default:
                    throw new IOException("Corrupted file (unexpected marker " + next + ")");
            }
        }
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>, Y> Y
    foldAutomata(Pipeline<V, In, E, P, N, G> pipeline, Y initial, BiFunction<Y, Automaton<V, In, E, P, N, G>, Y> fold) {
        final Stack<Pipeline<V, In, E, P, N, G>> stack = new Stack<>();
        stack.push(pipeline);
        while (!stack.isEmpty()) {
            final Pipeline<V, In, E, P, N, G> p = stack.pop();
            if (p instanceof Automaton) {
                initial = fold.apply(initial, (Automaton<V, In, E, P, N, G>) p);
            } else if (p instanceof Alternative) {
                final Alternative<V, In, E, P, N, G> alt = (Alternative<V, In, E, P, N, G>) p;
                stack.push(alt.lhs);
                stack.push(alt.rhs);
            } else if (p instanceof Composition) {
                final Composition<V, In, E, P, N, G> alt = (Composition<V, In, E, P, N, G>) p;
                stack.push(alt.lhs);
                stack.push(alt.rhs);
            } else if (p instanceof Tuple) {
                final Tuple<V, In, E, P, N, G> alt = (Tuple<V, In, E, P, N, G>) p;
                stack.push(alt.lhs);
                stack.push(alt.rhs);
            } else if (p instanceof Submatch) {
                final Submatch<V, In, E, P, N, G> alt = (Submatch<V, In, E, P, N, G>) p;
                for (Pipeline<V, In, E, P, N, G> e : alt.submatchHandler.values()) {
                    stack.push(e);
                }
            }
        }
        return initial;
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> Seq<In> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, Seq<In> inputs) {
        return eval(specs, pipeline, inputs, (i, o) -> {
        });
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> Seq<In> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, Seq<In> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
        final ArrayList<Seq<In>> o = eval(specs, pipeline, Util.singeltonArrayList(inputs), callback);
        if (o == null) return null;
        assert o.size() == 1;
        return o.get(0);
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, ArrayList<Seq<In>> inputs) {
        return eval(specs, pipeline, inputs, (i, o) -> {
        });
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, ArrayList<Seq<In>> inputs, BiConsumer<StackElem<V, In, E, P, N, G>, ArrayList<Seq<In>>> callback) {
        /**This custom stack implementation allows for more efficient execution when there are millions of pipelines
         * stacked together. If it was implemented naively as recursive function, then Java stack would blow up*/
        final Stack<StackElem<V, In, E, P, N, G>> stack = new Stack<>();
        stack.push(pipeline);
        while (!stack.isEmpty()) {
            final StackElem<V, In, E, P, N, G> p = stack.pop();
            inputs = p.eval(specs, stack, inputs, callback);
            callback.accept(p, inputs);
        }
        return inputs;
    }


}
