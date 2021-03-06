package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolClearOutput implements Kolmogorov {
    @Override
    public int compositionHeight() {
        return 1;
    }

    final Kolmogorov lhs;
    final boolean readsInput;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
    }

    @Override
    public Kolmogorov clearOutput() {
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public boolean producesOutput() {
        return false;
    }

    @Override
    public boolean readsInput() {
        return readsInput;
    }

    public KolClearOutput(Kolmogorov lhs) {
        this.lhs = lhs;
        readsInput = lhs.readsInput();
        assert producesOutput() || compositionHeight() == 1;
        assert compositionHeight() > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        return new SolFunc(new Solomonoff[]{lhs.toSolomonoff(query)}, "clearOutput");
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return lhs.representative(variableAssignment);
    }

    @Override
    public Kolmogorov inv() {
        return new KolProd(this);
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subLhs == lhs)
            return this;
        return new KolClearOutput(subLhs);
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append("clearOutput[");
        lhs.toString(sb);
        sb.append("]");

    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }
}
