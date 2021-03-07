package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolInv implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov lhs;
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

    public KolInv(Kolmogorov lhs) {
        this.lhs = lhs;
        producesOutput = lhs.readsInput();
        readsInput = lhs.producesOutput();
        compositionHeight = lhs.compositionHeight();
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        final Solomonoff s = lhs.toSolomonoff(query);
        assert s.validateSubmatches(query)==-1;
        return new SolFunc(new Solomonoff[]{s}, "inverse");
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return null;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subLhs == lhs)
            return this;
        return new KolInv(subLhs);
    }

    @Override
    public Kolmogorov inv() {
        return lhs;
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
        sb.append("^-1");
    }

    @Override
    public Kolmogorov clearOutput() {
        return new KolClearOutput(lhs);
    }

    @Override
    public Kolmogorov identity() {
        return new KolIdentity(this);
    }
}
