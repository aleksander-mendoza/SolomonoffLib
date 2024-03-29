package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolDiff implements Kolmogorov {
    final Kolmogorov lhs, rhs;

    @Override
    public int compositionHeight() {
        return 1;
    }

    final boolean producesOutput, readsInput;

    @Override
    public Kolmogorov clearOutput() {
        return new KolDiff(lhs.clearOutput(), rhs.clearOutput());
    }

    @Override
    public boolean producesOutput() {
        return producesOutput;
    }

    @Override
    public boolean readsInput() {
        return readsInput;
    }

    public KolDiff(Kolmogorov lhs, Kolmogorov rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        producesOutput = lhs.producesOutput();
        readsInput = lhs.readsInput();
        assert lhs.compositionHeight() == 1;
        assert rhs.compositionHeight() == 1;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        final Solomonoff ls = lhs.toSolomonoff(query);
        final Solomonoff rs = rhs.toSolomonoff(query);
        assert rs.validateSubmatches(query)==-1;
        return new SolFunc(new Solomonoff[]{ls,rs}, "subtractNondet");
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return null;
    }

    @Override
    public Kolmogorov inv() {
        return new KolInv(this);
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subRhs = rhs.substitute(argMap);
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subRhs == rhs && subLhs == lhs)
            return this;
        return Optimise.diff(subLhs, subRhs);
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
        sb.append(" - ");
        if (rhs.precedence() < precedence()) {
            sb.append("(");
            rhs.toString(sb);
            sb.append(")");
        } else {
            rhs.toString(sb);
        }
    }

    @Override
    public int precedence() {
        return 1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
        rhs.forEachVar(variableAssignment);
    }
    @Override
    public Kolmogorov identity() {
        return Optimise.diff(lhs.identity(),rhs);
    }
}
