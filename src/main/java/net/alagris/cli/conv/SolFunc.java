package net.alagris.cli.conv;

import java.util.function.Consumer;
import java.util.function.Function;

public class SolFunc implements Solomonoff {
    final Solomonoff[] args;
    final String id;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public SolFunc(Solomonoff[] args, String id) {
        this.args = args;
        this.id = id;
    }

    @Override
    public int precedence() {
        return 3;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(id).append("[");
        if (args.length > 0) {
            args[0].toString(sb);
            for (int i = 1; i < args.length; i++) {
                sb.append(",");
                args[1].toString(sb);
            }
        }
        sb.append("]");
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<EncodedID, StringifierMeta> usagesLeft) {
        sb.append(id).append("[");
        final Weights out;
        if (args.length > 0) {
            out = args[0].toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            for (int i = 1; i < args.length; i++) {
                sb.append(",");
                out.minMax(args[1].toStringAutoWeightsAndAutoExponentials(sb, usagesLeft));
            }
        } else {
            out = Weights.str();
        }
        sb.append("]");
        int diff = -100;
        sb.append(diff);
        out.addPost(diff);
        return out;
    }

    @Override
    public void countUsages(Consumer<AtomicVar> countUsage) {
        for (Solomonoff arg : args) {
            arg.countUsages(countUsage);
        }
    }
}
