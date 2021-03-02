package net.alagris.cli.conv;

import net.alagris.core.IntSeq;
import net.alagris.core.Pair;
import net.alagris.core.Specification;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class AtomicSet implements Atomic.Set, Solomonoff {
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public int compositionHeight() {
        return 1;
    }

    final ArrayList<Specification.Range<Integer, Boolean>> ranges;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        return this;
    }

    @Override
    public Church substituteCh(Function<ChVar, Church> argMap) {
        return this;
    }

    @Override
    public PushedBack toKolmogorov(TranslationQueries resolveFreeVariable) {
        return PushedBack.wrap(this);
    }

    public AtomicSet(ArrayList<Specification.Range<Integer, Boolean>> ranges) {
        this.ranges = ranges;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        return this;
    }
    @Override
    public <Y> Y walk(SolWalker<Y> walker){
        return null;
    }

    @Override
    public int validateSubmatches(VarQuery query){
        return -1;
    }

    public List<Pair.IntPair> inclusiveRangePairs() {
        assert SPECS.isFullSigmaCovered(ranges);
        int fromExclusive;
        int i;
        if (ranges.get(0).edges()) {
            i = 0;
            fromExclusive = SPECS.minimal();
        } else if (ranges.size() > 1) {
            fromExclusive = ranges.get(0).input();
            i = 1;
        } else {
            return Collections.emptyList();
        }
        Specification.Range<Integer, Boolean> range = ranges.get(i);
        assert range.edges() : ranges;
        int toInclusive = range.input();
        ArrayList<Pair.IntPair> inclusiveRanges = new ArrayList<>();
        inclusiveRanges.add(Pair.of(fromExclusive + 1, toInclusive));
        for (i++; i < ranges.size(); i++) {
            Specification.Range<Integer, Boolean> rangeExcluded = ranges.get(i);
            fromExclusive = rangeExcluded.input();
            assert !rangeExcluded.edges() : ranges;
            i++;
            if (i == ranges.size()) {
                assert SPECS.maximal().equals(fromExclusive) : ranges;
                break;
            }
            range = ranges.get(i);
            assert range.edges() : ranges;
            toInclusive = range.input();
            inclusiveRanges.add(Pair.of(fromExclusive + 1, toInclusive));
        }
        return inclusiveRanges;
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        return toStringCommon(sb);
    }
    private Weights toStringCommon(StringBuilder sb) {
        List<Pair.IntPair> inclusiveRanges = inclusiveRangePairs();
        if (inclusiveRanges.isEmpty()) {
            sb.append("[]");
            return Weights.empty();
        }
        boolean isPrintable = true;
        for (Pair.IntPair pair : inclusiveRanges) {
            if (!IntSeq.isPrintableChar(pair.l) || !IntSeq.isPrintableChar(pair.r)) {
                isPrintable = false;
                break;
            }
        }
        if (isPrintable) {
            sb.append('[');
            for (Pair.IntPair pair : inclusiveRanges) {
                IntSeq.appendRangeNoBrackets(sb, pair.l, pair.r);
            }
            sb.append(']');
        } else {
            sb.append("<[");
            Iterator<Pair.IntPair> i = inclusiveRanges.iterator();
            while (i.hasNext()) {
                Pair.IntPair pair = i.next();
                if (pair.l == pair.r) {
                    sb.append(Integer.toUnsignedString(pair.l));
                } else {
                    sb.append(Integer.toUnsignedString(pair.l));
                    sb.append('-');
                    sb.append(Integer.toUnsignedString(pair.r));
                }
                if (i.hasNext()) {
                    sb.append(' ');
                }
            }
            sb.append("]>");
        }
        return Weights.str();
    }

    @Override
    public ArrayList<Specification.Range<Integer, Boolean>> ranges() {
        return ranges;
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        final int min = Atomic.min(ranges);
        return SPECS.minimal().equals(min) ? null : new IntSeq(min);
    }

    @Override
    public Kolmogorov inv() {
        return Optimise.prod(this);
    }

    @Override
    public boolean producesOutput() {
        return false;
    }

    @Override
    public boolean readsInput() {
        return !Atomic.isEmpty(ranges);
    }

    @Override
    public void toString(StringBuilder sb) {
        toStringCommon(sb);
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Kolmogorov clearOutput() {
        return this;
    }
}
