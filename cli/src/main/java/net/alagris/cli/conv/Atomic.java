package net.alagris.cli.conv;

import net.alagris.core.IntSeq;
import net.alagris.core.NullTermIter;
import net.alagris.core.Specification;
import net.alagris.core.Specification.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Atomic {


    static interface Set extends Kolmogorov, Atomic, Church {
        ArrayList<Range<Integer, Boolean>> ranges();

    }

    static interface Str extends Kolmogorov, Solomonoff, Atomic, Church {
        IntSeq str();

        default public int precedence() {
            return Integer.MAX_VALUE;
        }

        @Override
        public default void forEachVar(Consumer<AtomicVar> variableAssignment) {
        }

        @Override
        default public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
            return str();
        }

        @Override
        default public Solomonoff toSolomonoff(VarQuery query) {
            return this;
        }

        @Override
        default public Kolmogorov inv() {
            return Optimise.prod(this);
        }

        @Override
        default public <Y> Y walk(SolWalker<Y> walker) {
            return null;
        }

        @Override
        default int validateSubmatches(VarQuery query) {
            return -1;
        }

        @Override
        default public boolean producesOutput() {
            return false;
        }

        @Override
        public default Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
            return this;
        }

        @Override
        default PushedBack toKolmogorov(TranslationQueries resolveFreeVariable) {
            return PushedBack.wrap(this);
        }

        @Override
        public default Church substituteCh(Function<ChVar, Church> argMap) {
            return this;
        }

        @Override
        default public void toString(StringBuilder sb) {
            sb.append(IntSeq.toStringLiteral(str()));
        }

        @Override
        public default Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,
                                                                      SolStringifier usagesLeft) {
            sb.append(IntSeq.toStringLiteral(str()));
            if (str().isEmpty())
                return Weights.eps();
            return Weights.str();
        }

        @Override
        default Kolmogorov clearOutput() {
            return this;
        }
    }

    public static final Str EPSILON = new AtomicStr(IntSeq.Epsilon);
    public static final IntSeq REFLECT_STR = new IntSeq(Kolmogorov.SPECS.minimal());
    public static final SolProd REFLECT_PROD = new SolProd(REFLECT_STR);
    public static final ArrayList<Range<Integer, Boolean>> DOT = Kolmogorov.SPECS.makeSingletonRanges(true, false,
            Kolmogorov.SPECS.minimal(), Kolmogorov.SPECS.mid());

    public static ArrayList<Range<Integer, Boolean>> composeSets(ArrayList<Range<Integer, Boolean>> lhs,
                                                                 ArrayList<Range<Integer, Boolean>> rhs, BiFunction<Boolean, Boolean, Boolean> f) {
        final NullTermIter<Specification.RangeImpl<Integer, Boolean>> i = Kolmogorov.SPECS.zipTransitionRanges(
                NullTermIter.fromIterable(lhs), NullTermIter.fromIterable(rhs),
                (from, to, lhsTran, rhsTran) -> new Specification.RangeImpl<>(to, f.apply(lhsTran, rhsTran)));
        final ArrayList<Range<Integer, Boolean>> ranges = new ArrayList<>(lhs.size() + rhs.size());
        Specification.RangeImpl<Integer, Boolean> next;
        Specification.RangeImpl<Integer, Boolean> prev = null;
        while ((next = i.next()) != null) {
            if (prev != null && !prev.edges().equals(next.edges())) {
                ranges.add(prev);
            }
            prev = next;
        }
        assert prev != null;
        assert prev.input().equals(Kolmogorov.SPECS.maximal()) : prev.input() + "==" + Kolmogorov.SPECS.maximal();
        ranges.add(prev);
        if (ranges.size() == 2 && ranges.get(0).input().equals(255) && ranges.get(0).edges()) {
            return DOT;
        } else {
            return ranges;
        }
    }


    public static int min(ArrayList<Range<Integer, Boolean>> ranges) {
        assert Kolmogorov.SPECS.isFullSigmaCovered(ranges);
        if (ranges.get(0).edges()) {
            return Kolmogorov.SPECS.successor(Kolmogorov.SPECS.minimal());
        } else if (ranges.size() > 1) {
            assert ranges.get(1).edges() : ranges;
            return Kolmogorov.SPECS.successor(ranges.get(0).input());
        }
        return Kolmogorov.SPECS.minimal();
    }

    public static boolean isEmpty(ArrayList<Range<Integer, Boolean>> ranges) {
        return Kolmogorov.SPECS.minimal().equals(min(ranges));
    }
}
