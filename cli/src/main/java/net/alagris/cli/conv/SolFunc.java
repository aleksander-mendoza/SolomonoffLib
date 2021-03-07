package net.alagris.cli.conv;

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
    public <Y> Y walk(SolWalker<Y> walker){
        for(Solomonoff s:args){
            final Y y = s.walk(walker);
            if(y!=null)return y;
        }
        return null;
    }
    @Override
    public int validateSubmatches(VarQuery query) {
        int max = -1;
        for(Solomonoff s:args){
            max = Math.max(s.validateSubmatches(query),max);
        }
        return max;
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(id).append("![");
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
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        sb.append(id);
        final Weights out;
        if (args.length > 0) {
            sb.append("![");
            out = args[0].toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
            sb.append("]");
            for (int i = 1; i < args.length; i++) {
                sb.append("![");
                out.minMax(args[1].toStringAutoWeightsAndAutoExponentials(sb, usagesLeft));
                sb.append("]");
            }
        } else {
            out = Weights.str();
        }
        int diff = -100;
        sb.append(diff);
        out.addPost(diff);
        return out;
    }

}
