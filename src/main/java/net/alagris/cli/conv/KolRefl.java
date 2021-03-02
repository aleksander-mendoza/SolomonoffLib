package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolRefl implements Kolmogorov {
    @Override
    public int compositionHeight() {
        return 1;
    }

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
    }

    final Atomic.Set set;

    @Override
    public Kolmogorov clearOutput() {
        return set;
    }

    @Override
    public boolean producesOutput() {
        return true;
    }

    @Override
    public boolean readsInput() {
        return true;
    }

    public KolRefl(Atomic.Set set) {
        assert set.readsInput();
        this.set = set;
        assert producesOutput() || compositionHeight() == 1;
        assert compositionHeight() > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        Solomonoff s = set.toSolomonoff(query);
        return Optimise.refl(s);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return set.representative(variableAssignment);
    }

    @Override
    public Kolmogorov inv() {
        return this;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        return this;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(":<0>");
        set.toString(sb);
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

}
