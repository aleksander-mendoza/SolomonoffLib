package net.alagris;


import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Transducer regular expressions (may) have distinct input <tt>In</tt> and
 * output <tt>Out</tt> alphabets. The expressions can also be weighted with
 * weights <tt>W</tt>. Additionally to every node of syntax tree some
 * meta-information <tt>M</tt> may be attached (like for instance, type of
 * expression or line in source code)
 */
public abstract class Regex<M, In, Out, W> {

    private final M meta;

    private Regex(M meta) {
        this.meta = meta;
    }

    public M getMeta() {
        return meta;
    }

    public interface CompilationSpecs<M, V, E, P, In, Out, W, N , G extends IntermediateGraph<V, E, P, N>> {

        /**@return graph that should be substituted for a given
         * variable identifier or null if no such graph is known */
        public G varAssignment(String varId);

        public Specification<V, E, P, In, Out, W, N, G> specification();

        P epsilonUnion(@NonNull  P eps1,@NonNull P eps2) throws IllegalArgumentException, UnsupportedOperationException;

        P epsilonKleene(@NonNull P eps) throws IllegalArgumentException, UnsupportedOperationException;

        V stateBuilder(M meta);

        default G copyVarAssignment(String var) {
            G g = varAssignment(var);
            return g == null ? null : specification().deepClone(g);
        }


    }


    /**
     * Substitutes all variables for precompiled automata graphs and performs Glushkov's construction
     */
    public abstract <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
            CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError;

    public static final class Union<M, In, Out, W> extends Regex<M, In, Out, W> {
        public final Regex<M, In, Out, W> lhs, rhs;

        public Union(M meta, Regex<M, In, Out, W> lhs, Regex<M, In, Out, W> rhs) {
            super(meta);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G l = lhs.compile(specs);
            G r = rhs.compile(specs);
            try {
                return specs.specification().union(l, r, specs::epsilonUnion);
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                throw new CompilationError.ParseException(this, e);
            }
        }
    }

    public static final class Concat<M, In, Out, W> extends Regex<M, In, Out, W> {
        public final Regex<M, In, Out, W> lhs, rhs;

        public Concat(M meta, Regex<M, In, Out, W> lhs, Regex<M, In, Out, W> rhs) {
            super(meta);
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G l = lhs.compile(specs);
            G r = rhs.compile(specs);
            return specs.specification().concat(l, r);
        }

    }

    public static final class Product<M, In, Out, W> extends Regex<M, In, Out, W> {
        public final Regex<M, In, Out, W> nested;
        public final Out output;

        public Product(M meta, Regex<M, In, Out, W> nested, Out output) {
            super(meta);
            this.nested = nested;
            this.output = output;
        }

        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G n = nested.compile(specs);
            return specs.specification().rightActionOnGraph(n, specs.specification().partialOutputEdge(output));
        }

    }

    public static final class WeightBefore<M, In, Out, W> extends Regex<M, In, Out, W> {

        public final Regex<M, In, Out, W> nested;
        public final W weight;

        public WeightBefore(M meta, Regex<M, In, Out, W> nested, W weight) {
            super(meta);
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return " " + weight + " "
                    + (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString());
        }
        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G n = nested.compile(specs);
            return specs.specification().leftActionOnGraph(specs.specification().partialWeightedEdge(weight), n);
        }

    }

    public static final class WeightAfter<M, In, Out, W> extends Regex<M, In, Out, W> {

        public final Regex<M, In, Out, W> nested;
        public final W weight;

        public WeightAfter(M meta, Regex<M, In, Out, W> nested, W weight) {
            super(meta);
            this.nested = nested;
            this.weight = weight;
        }

        @Override
        public String toString() {
            return (nested instanceof Union || nested instanceof Concat ? "(" + nested + ")" : nested.toString()) + " "
                    + weight + " ";
        }
        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G n = nested.compile(specs);
            return specs.specification().rightActionOnGraph(n, specs.specification().partialWeightedEdge(weight));
        }
    }

    public static final class Kleene<M, In, Out, W> extends Regex<M, In, Out, W> {
        public final Regex<M, In, Out, W> nested;

        public Kleene(M meta, Regex<M, In, Out, W> nested) {
            super(meta);
            this.nested = nested;
        }
        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G n = nested.compile(specs);
            try {
                return specs.specification().kleene(n, specs::epsilonKleene);
            }catch (IllegalArgumentException | UnsupportedOperationException e){
                throw new CompilationError.ParseException(this,e);
            }
        }
    }

    public static final class Atomic<M, In, Out, W> extends Regex<M, In, Out, W> {
        final private In from;
        final private In to;

        public Atomic(M meta, In singleton) {
            this(meta, singleton, singleton);
        }

        public Atomic(M meta, In from, In to) {
            super(meta);
            this.from = from;
            this.to = to;
        }

        public In getFrom() {
            return from;
        }

        public In getTo() {
            return to;
        }
        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            return specs.specification().atomicRangeGraph(from,specs.stateBuilder(getMeta()),to);
        }
    }

    public static class Epsilon<M, In, Out, W> extends Regex<M, In, Out, W> {

        public Epsilon(M meta) {
            super(meta);
        }

        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            return specs.specification().atomicEpsilonGraph();
        }
    }

    public static class Var<M, In, Out, W> extends Regex<M, In, Out, W> {
        final String id;

        public Var(M meta, String id) {
            super(meta);
            this.id = id;
        }

        @Override
        public <V, E, P, N , G extends IntermediateGraph<V, E, P, N>> G compile(
                CompilationSpecs<M, V, E, P, In, Out, W, N, G> specs) throws CompilationError {
            G g = specs.copyVarAssignment(id);
            if(g==null){
                throw new CompilationError.ParseException(this,new IllegalArgumentException("Variable '"+id+"' not found!"));
            }else{
                return g;
            }
        }
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> eps(M meta) {
        return new Epsilon<>(meta);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> concat(M meta, Regex<M, In, Out, W> lhs,
                                                              Regex<M, In, Out, W> rhs) {
        return new Concat<>(meta, lhs, rhs);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> union(M meta, Regex<M, In, Out, W> lhs,
                                                             Regex<M, In, Out, W> rhs) {
        return new Union<>(meta, lhs, rhs);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> kleene(M meta, Regex<M, In, Out, W> nested) {
        return new Kleene<>(meta, nested);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> product(M meta, Regex<M, In, Out, W> nested, Out out) {
        return new Product<>(meta, nested, out);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> weightAfter(M meta, Regex<M, In, Out, W> nested, W w) {
        return new WeightAfter<>(meta, nested, w);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> weightBefore(M meta, W w, Regex<M, In, Out, W> nested) {
        return new WeightBefore<>(meta, nested, w);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> atomic(M meta, In from, In to) {
        return new Atomic<M, In, Out, W>(meta, from, to);
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> atomic(M meta, In symbol) {
        return new Atomic<M, In, Out, W>(meta, symbol);
    }
    public static <M, In, Out, W> Regex<M, In, Out, W> fromString(M meta, Iterable<In> string) {
        return fromString(meta,string.iterator());
    }
    public static <M, In, Out, W> Regex<M, In, Out, W> fromString(M meta, Iterator<In> string) {
        if(string.hasNext()){
            Regex<M, In, Out, W> concatenated = atomic(meta, string.next());
            while(string.hasNext()){
                concatenated = concat(meta,concatenated,atomic(meta,string.next()));
            }
            return concatenated;
        }else{
            return eps(meta);
        }
    }

    public static <M, In, Out, W> Regex<M, In, Out, W> var(M meta, String id) {
        return new Var<M, In, Out, W>(meta, id);
    }


}
