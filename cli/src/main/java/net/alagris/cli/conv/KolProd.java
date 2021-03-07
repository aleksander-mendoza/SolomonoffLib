package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolProd implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov rhs;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        rhs.forEachVar(variableAssignment);
    }

    @Override
    public Kolmogorov clearOutput() {
        return Atomic.EPSILON;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public KolProd(Kolmogorov rhs) {
        assert !rhs.producesOutput();
        this.rhs = rhs;
        compositionHeight = 1;
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        return new SolProd(rhs.representative(query::variableAssignment));
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return IntSeq.Epsilon;
    }

    @Override
    public Kolmogorov inv() {
        assert !rhs.producesOutput();
        return rhs;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subRhs = rhs.substitute(argMap);
        if (subRhs == rhs)
            return this;
        return Optimise.prod(subRhs);
    }

    @Override
    public boolean producesOutput() {
        return true;
    }

    @Override
    public boolean readsInput() {
        return false;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(":");
        if (!(rhs instanceof Atomic)) {
            sb.append("(");
            rhs.toString(sb);
            sb.append(")");
        } else {
            rhs.toString(sb);
        }
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Kolmogorov identity() {
        return Atomic.EPSILON;
    }
}
