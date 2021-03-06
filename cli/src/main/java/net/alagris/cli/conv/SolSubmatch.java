package net.alagris.cli.conv;

public class SolSubmatch implements Solomonoff {
    final Solomonoff nested;
    final int groupIndex;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public SolSubmatch(Solomonoff nested,int groupIndex) {
        this.nested = nested;
        this.groupIndex = groupIndex;
    }

    @Override
    public <Y> Y walk(SolWalker<Y> walker){
        final Y y = walker.submatch(this);
        if(y!=null)return y;
        return nested.walk(walker);
    }
    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(groupIndex).append("{");
        nested.toString(sb);
        sb.append("}");
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {
        usagesLeft.useSubmatch(groupIndex);
        sb.append(groupIndex).append("{");
        Weights w = nested.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
        sb.append("}");
        return w;
    }
    @Override
    public int validateSubmatches( VarQuery query) {
        int maxGroup = nested.validateSubmatches(query);
        assert groupIndex>maxGroup:groupIndex+" "+maxGroup+"\n"+this;
        return groupIndex;
    }
}
