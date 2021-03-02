package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class KolKleene implements Kolmogorov {
    final int compositionHeight;

    @Override
    public int compositionHeight() {
        return compositionHeight;
    }

    final Kolmogorov lhs;
    final char type;
    final boolean producesOutput, readsInput;

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        lhs.forEachVar(variableAssignment);
    }

    @Override
    public Kolmogorov clearOutput() {
        return new KolKleene(lhs.clearOutput(), type);
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

    public KolKleene(Kolmogorov lhs, char type) {
        this.lhs = lhs;
        this.type = type;
        assert type == '*' || type == '?' || type == '+';
        producesOutput = lhs.producesOutput();
        readsInput = lhs.readsInput();
        compositionHeight = lhs.compositionHeight();
        assert producesOutput() || compositionHeight == 1;
        assert compositionHeight > 0;
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        final Solomonoff s = lhs.toSolomonoff(query);
        return Optimise.kleene(s, type);
    }

    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return IntSeq.Epsilon;
    }

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        final Kolmogorov subLhs = lhs.substitute(argMap);
        if (subLhs == lhs)
            return this;
        return new KolKleene(subLhs, type);
    }

    @Override
    public Kolmogorov inv() {
        return new KolKleene(lhs.inv(), type);
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
        sb.append(type);
    }


}
