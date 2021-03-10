package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolComp implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov lhs, rhs;
    final boolean readsInput;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
        rhs.forEachVar(variableAssignment);
    }

    @Override
    public Kolmogorov clearOutput() {
        return new KolComp(lhs, rhs.clearOutput());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public boolean producesOutput() {
        return true;
    }

    @Override
    public boolean readsInput() {
        return readsInput;
    }

    public KolComp(Kolmogorov lhs, Kolmogorov rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        readsInput = lhs.readsInput();
        assert !(rhs instanceof KolComp);
        compositionHeight = lhs.compositionHeight() + rhs.compositionHeight();
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        final Solomonoff ls = lhs.toSolomonoff(query);
        final Solomonoff rs = rhs.toSolomonoff(query);
        final int groupIndex = query.introduceAuxiliaryVar(rs);
        return new SolSubmatch(ls,groupIndex);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return null;
    }

    @Override
    public Kolmogorov inv() {
        return Optimise.comp(rhs.inv(), lhs.inv());
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subRhs = rhs.substitute(argMap);
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subRhs == rhs && subLhs == lhs)
            return this;
        return new KolComp(subLhs, subRhs);
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public void toString(StringBuilder sb) {
        if (lhs.precedence() < precedence()) {
            sb.append("(");
            lhs.toString(sb);
            sb.append(")");
        } else {
            lhs.toString(sb);
        }
        sb.append(" @ ");
        if (rhs.precedence() < precedence()) {
            sb.append("(");
            rhs.toString(sb);
            sb.append(")");
        } else {
            rhs.toString(sb);
        }
    }

    @Override
    public Kolmogorov identity() {
        return lhs.identity();
    }
}
