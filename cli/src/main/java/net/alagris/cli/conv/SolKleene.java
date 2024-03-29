package net.alagris.cli.conv;

public class SolKleene implements Solomonoff {
    final Solomonoff lhs;
    final char type;

    public SolKleene(Solomonoff lhs, char type) {
        this.lhs = lhs;
        this.type = type;
    }
    @Override
    public <Y> Y walk(SolWalker<Y> walker){
        return lhs.walk(walker);
    }
    @Override
    public int validateSubmatches( VarQuery query) {
        return lhs.validateSubmatches(query);
    }
    @Override
    public int precedence() {
        return 2;
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

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        final Weights w;
        if (lhs.precedence() <= precedence()) {
            sb.append("(");
            w = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            sb.append(")");
        } else {
            w = lhs.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
        }
        if (type == '*') {
            final int diff = w.inferKleeneFavourLoopback();
            if (diff != 0) {
                sb.append(' ').append(diff);
            }
            sb.append('*');
        } else if (type == '+') {
            final int diff = w.inferKleeneOneOrMoreFavourLoopback();
            if (diff != 0) {
                sb.append(' ').append(diff);
            }
            sb.append('+');
        } else if (type == '?') {
            final int diff = w.inferKleeneOptional();
            if (diff != 0) {
                sb.append(' ').append(diff);
            }
            sb.append('?');
        }
        return w;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

}
