package net.alagris.cli.conv;

import net.alagris.core.Pipeline;

import java.util.function.Consumer;
import java.util.function.Function;

public class SolConcat implements Solomonoff {
    final Solomonoff lhs, rhs;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public SolConcat(Solomonoff lhs, Solomonoff rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        assert !(rhs instanceof SolConcat);
    }
    @Override
    public <Y> Y walk(SolWalker<Y> walker){
        final Y y = lhs.walk(walker);
        if(y!=null)return y;
        return rhs.walk(walker);
    }
    @Override
    public int precedence() {
        return 1;
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
        if (!(rhs instanceof SolProd))
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
    public int validateSubmatches( VarQuery query) {
        return Math.max(lhs.validateSubmatches(query), rhs.validateSubmatches(query));
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        final Weights lw;
        if (lhs.precedence() < precedence()) {
            sb.append("(");
            lw = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            sb.append(")");
        } else {
            lw = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
        }

        final StringBuilder rsb = new StringBuilder();
        final Weights rw;
        if (rhs.precedence() < precedence()) {
            rsb.append("(");
            rw = rhs.toStringAutoWeightsAndAutoExponentials(rsb, usagesLeft);
            rsb.append(")");
        } else {
            rw = rhs.toStringAutoWeightsAndAutoExponentials(rsb, usagesLeft);
        }
        final int diff = lw.inferConcatFavourLoopback(rw);
        if (diff != 0) {
            sb.append(" ").append(diff).append(" ");
        } else {
            if (!(rhs instanceof SolProd))
                sb.append(" ");
        }
        sb.append(rsb);
        return lw;
    }

}
