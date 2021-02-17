package net.alagris.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Pipeline<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> extends StackElem<V, In, E, P, N, G> {

    /**
     * Size of tuple
     */
    int size();

    V meta();


    class Automaton<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G>{
        public final Specification.RangedGraph<V, In, E, P> g;
        public final V meta;

        @Override
        public <Out,W>  ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                     Stack<StackElem<V, In, E, P, N, G>> stack,
                                                     ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            assert inputs.size()==1:inputs;
            Seq<In> output = specs.evaluate(g, inputs.get(0));
            if(output==null)return null;
            inputs.set(0,output);
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
        public <Out,W>  ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            assert inputs.size()==1;
            if(runtime){
                if(!specs.accepts(g,inputs.get(0).iterator())){
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
        public <Out,W>  ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            assert inputs.size()==1;
            final Seq<In> out = f.apply(inputs.get(0));
            if(out==null)return null;
            inputs.set(0,out);
            return inputs;
        }
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

    class AlternativeSecondBranch<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G>{

        private final ArrayList<Seq<In>> inputs;
        private final Pipeline<V, In, E, P, N, G> rhs;

        public AlternativeSecondBranch(ArrayList<Seq<In>> inputs, Pipeline<V, In, E, P, N, G> rhs) {
            this.inputs = new ArrayList<>(inputs);
            this.rhs = rhs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null) {
                return rhs.eval(specs, stack, this.inputs);
            }else{
                return inputs;
            }
        }
    }

    class Alternative<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> lhs, rhs;
        @Override
        public <Out,W>  ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs,
                                                Stack<StackElem<V, In, E, P, N, G>> stack,
                                                ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            stack.push(new AlternativeSecondBranch<>(inputs,rhs));//rhs is evaluated second
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

    class TupleMerge<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G>{

        private final ArrayList<Seq<In>> lhsOutputs;

        public TupleMerge(ArrayList<Seq<In>> lhsOutputs) {
            this.lhsOutputs = lhsOutputs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            lhsOutputs.addAll(inputs);
            return lhsOutputs;
        }
    }

    class TupleSecondBranch<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements StackElem<V, In, E, P, N, G>{

        private final ArrayList<Seq<In>> inputs;
        private final Pipeline<V, In, E, P, N, G> rhs;

        public TupleSecondBranch(ArrayList<Seq<In>> inputs, Pipeline<V, In, E, P, N, G> rhs) {
            this.inputs = new ArrayList<>(inputs);
            this.rhs = rhs;
        }

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
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
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            final int lSize = lhs.size();
            final int rSize = rhs.size();
            final ArrayList<Seq<In>> rInput = new ArrayList<>(rSize);
            final List<Seq<In>> sub = inputs.subList(lSize,inputs.size());
            rInput.addAll(sub);
            sub.clear();
            stack.push(new TupleSecondBranch<>(rInput,rhs));
            stack.push(lhs);
            assert inputs.size()==lSize;
            assert rInput.size()==rSize;
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
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
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
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            int len = (inputs.size()-1)*outputSeparator.size();//number of separators
            for(Seq<In> seq : inputs){
                len+= seq.size();
            }
            final In[] arr = (In[]) new Object[len];
            inputs.get(0).copyTo(0,arr);
            int offset=inputs.get(0).size();
            for(int i=1;i<inputs.size();i++){
                outputSeparator.copyTo(offset,arr);
                offset+=outputSeparator.size();
                final Seq<In> seq = inputs.get(i);
                assert seq!=null;
                seq.copyTo(offset,arr);
                offset+=seq.size();
            }
            assert offset==arr.length;
            inputs.clear();
            inputs.add(Seq.wrap(arr));
            return inputs;
        }
    }
    class Split<V, In, E, P, N, G extends IntermediateGraph<V, E, P, N>> implements Pipeline<V, In, E, P, N, G> {
        public final V meta;
        public final Pipeline<V, In, E, P, N, G> tuple;
        public final In inputSeparator;
        public final Seq<In> outputSeparator;

        public Split(V meta, Pipeline<V, In, E, P, N, G> tuple,In inputSeparator, Seq<In> outputSeparator) {
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

        @Override
        public <Out, W> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Stack<StackElem<V, In, E, P, N, G>> stack, ArrayList<Seq<In>> inputs) {
            if(inputs==null)return null;
            assert inputs.size()==1;
            final int expectedNumberOfSplits = tuple.size();
            final Seq<In> in = inputs.remove(0);
            inputs.ensureCapacity(expectedNumberOfSplits);
            for(int offset=0;offset<in.size();){
                int end = in.indexOf(offset,inputSeparator);
                assert offset<=end;
                inputs.add(in.sub(offset,end));
                offset = end+1;
            }
            if(inputs.size()!=expectedNumberOfSplits){
                return null;
            }
            stack.push(new Join<>(outputSeparator));
            stack.push(tuple);
            return inputs;
        }
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>,Y> Y 
    forEachAutomaton(Pipeline<V, In, E, P, N, G> pipeline, Function<Automaton<V, In, E, P, N, G>,Y> callback) {
    	final Stack<Pipeline<V, In, E, P, N, G>> stack = new Stack<>();
    	stack.push(pipeline);
    	while(!stack.isEmpty()) {
    		final Pipeline<V, In, E, P, N, G> p = stack.pop();
    		if(p instanceof Automaton) {
    			Y y = callback.apply((Automaton<V, In, E, P, N, G>) p);
    			if(y!=null)return y;
    		}else if(p instanceof Alternative){
    			final Alternative<V, In, E, P, N, G> alt = (Alternative<V, In, E, P, N, G> ) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}else if(p instanceof Composition){
    			final Composition<V, In, E, P, N, G> alt = (Composition<V, In, E, P, N, G>) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}else if(p instanceof Tuple){
    			final Tuple<V, In, E, P, N, G> alt = (Tuple<V, In, E, P, N, G> ) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}
    	}
    	return null;
    }
    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>,Y> Y 
    foldAutomata(Pipeline<V, In, E, P, N, G> pipeline,Y initial, BiFunction<Y,Automaton<V, In, E, P, N, G>,Y> fold) {
    	final Stack<Pipeline<V, In, E, P, N, G>> stack = new Stack<>();
    	stack.push(pipeline);
    	while(!stack.isEmpty()) {
    		final Pipeline<V, In, E, P, N, G> p = stack.pop();
    		if(p instanceof Automaton) {
    			initial = fold.apply(initial,(Automaton<V, In, E, P, N, G>) p);
    		}else if(p instanceof Alternative){
    			final Alternative<V, In, E, P, N, G> alt = (Alternative<V, In, E, P, N, G> ) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}else if(p instanceof Composition){
    			final Composition<V, In, E, P, N, G> alt = (Composition<V, In, E, P, N, G>) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}else if(p instanceof Tuple){
    			final Tuple<V, In, E, P, N, G> alt = (Tuple<V, In, E, P, N, G> ) p;
    			stack.push(alt.lhs);
    			stack.push(alt.rhs);
    		}
    	}
    	return initial;
    }
    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> Seq<In> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, Seq<In> inputs) {
        final ArrayList<Seq<In>> o = eval(specs,pipeline, Util.singeltonArrayList(inputs));
        if(o==null)return null;
        assert o.size()==1;
        return o.get(0);
    }

    static <V, In, Out, W, E, P, N, G extends IntermediateGraph<V, E, P, N>> ArrayList<Seq<In>> eval(Specification<V, E, P, In, Out, W, N, G> specs, Pipeline<V, In, E, P, N, G> pipeline, ArrayList<Seq<In>> inputs) {
        /**This custom stack implementation allows for more efficient execution when there are millions of pipelines
         * stacked together. If it was implemented naively as recursive function, then Java stack would blow up*/
        final Stack<StackElem<V, In, E, P, N, G>> stack = new Stack<>();
        stack.push(pipeline);
        while (!stack.isEmpty()) {
            final StackElem<V, In, E, P, N, G> p = stack.pop();
            inputs = p.eval(specs,stack,inputs);
        }
        return inputs;
    }


}
