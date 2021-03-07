package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolConcat implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov lhs, rhs;
    final boolean producesOutput, readsInput;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
        rhs.forEachVar(variableAssignment);
    }

    @Override
    public Kolmogorov clearOutput() {
        return new KolConcat(lhs.clearOutput(), rhs.clearOutput());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public boolean producesOutput() {
        return producesOutput;
    }

    @Override
    public boolean readsInput() {
        return readsInput;
    }

    public KolConcat(Kolmogorov lhs, Kolmogorov rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        producesOutput = lhs.producesOutput() || rhs.producesOutput();
        readsInput = lhs.readsInput() || rhs.readsInput();
        compositionHeight = Math.max(lhs.compositionHeight(), rhs.compositionHeight());
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        Solomonoff ls = lhs.toSolomonoff(query);
        Solomonoff rs = rhs.toSolomonoff(query);
        return Optimise.concat(ls,rs);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return IntSeq.concatOpt(lhs.representative(variableAssignment), rhs.representative(variableAssignment));
    }

    @Override
    public Kolmogorov inv() {
        return Optimise.concat(lhs.inv(), rhs.inv());
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subRhs = rhs.substitute(argMap);
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subRhs == rhs && subLhs == lhs)
            return this;
        return Optimise.concat(subLhs, subRhs);
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
        sb.append(" ");
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
        return 3;
    }

    @Override
    public Kolmogorov identity() {
        return Optimise.concat(lhs.identity(),rhs.identity());
    }
}
