package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.function.Consumer;
import java.util.function.Function;

public class SolProd implements Solomonoff {
    final IntSeq output;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
    @Override
    public <Y> Y walk(SolWalker<Y> walker){
        return null;
    }
    public SolProd(IntSeq output) {
        this.output = output;
    }

    @Override
    public int precedence() {
        return 3;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(":").append(IntSeq.toStringLiteral(output));
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        sb.append(":").append(IntSeq.toStringLiteral(output));
        return Weights.eps();
    }
    @Override
    public int validateSubmatches( VarQuery query) {
        return -1;
    }
}
