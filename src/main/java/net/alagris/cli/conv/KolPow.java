package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Concatenates exactly n times
 */
public class KolPow implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov lhs;
    final int power;
    final boolean producesOutput, readsInput;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
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

    public KolPow(Kolmogorov lhs, int power) {
        this.lhs = lhs;
        this.power = power;
        assert power >= 0;
        producesOutput = lhs.producesOutput();
        readsInput = lhs.readsInput();
        compositionHeight = power == 0 ? 1 : lhs.compositionHeight();
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subLhs == lhs)
            return this;
        return new KolPow(subLhs, power);
    }

    @Override
    public Stacked toSolomonoff(VarQuery query) {
        final Stacked s = lhs.toSolomonoff(query);
        return s.power(power,query);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        final IntSeq one = lhs.representative(variableAssignment);
        return one == null ? null : one.pow(power);
    }

    @Override
    public Kolmogorov inv() {
        return new KolPow(lhs.inv(), power);
    }

    @Override
    public int precedence() {
        return 4;
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
        sb.append("^").append(power);
    }

    @Override
    public Kolmogorov clearOutput() {
        return new KolPow(lhs.clearOutput(), power);
    }
}
