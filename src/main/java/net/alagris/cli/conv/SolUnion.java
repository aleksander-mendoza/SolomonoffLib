package net.alagris.cli.conv;

import java.util.function.Consumer;
import java.util.function.Function;

public class SolUnion implements Solomonoff {
    final Solomonoff lhs, rhs;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public SolUnion(Solomonoff lhs, Solomonoff rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        assert !(rhs instanceof SolUnion);
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
        sb.append("|");
        if (rhs.precedence() < precedence()) {
            sb.append("(");
            rhs.toString(sb);
            sb.append(")");
        } else {
            rhs.toString(sb);
        }
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<EncodedID, StringifierMeta> usagesLeft) {
        final Weights lw;
        if (lhs.precedence() < precedence()) {
            sb.append("(");
            lw = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            sb.append(")");
        } else {
            lw = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
        }
        sb.append("|");
        final Weights rw;
        if (rhs.precedence() < precedence()) {
            sb.append("(");
            rw = rhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            sb.append(")");
        } else {
            rw = rhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
        }
        final int diff = lw.inferUnionFavourRight(rw);
        if (diff != 0) {
            sb.append(" ").append(diff);
        }
        return lw;
    }

    @Override
    public void countUsages(Consumer<AtomicVar> countUsage) {
        lhs.countUsages(countUsage);
        rhs.countUsages(countUsage);
    }
}
