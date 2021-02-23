package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolIdentity implements Kolmogorov {

    @Override
    public int compositionHeight() {
        return 1;
    }

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
    }

    final Kolmogorov lhs;
    final boolean readsInput;

    @Override
    public Kolmogorov clearOutput() {
        return lhs.clearOutput();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public boolean producesOutput() {
        return readsInput();
    }

    @Override
    public boolean readsInput() {
        return readsInput;
    }

    public KolIdentity(Kolmogorov lhs) {
        this.lhs = lhs;
        readsInput = lhs.readsInput();
        assert producesOutput() || compositionHeight() == 1;
        assert compositionHeight() > 0;
    }

    @Override
    public Stacked toSolomonoff(VarQuery query) {
        final Stacked s = lhs.toSolomonoff(query);
        return s.identity(query);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return lhs.representative(variableAssignment);
    }

    @Override
    public Kolmogorov inv() {
        return this;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subLhs == lhs)
            return this;
        return new KolIdentity(subLhs);
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("identity[");
        lhs.toString(sb);
        sb.append("]");

    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }
}
