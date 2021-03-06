package net.alagris.cli.conv;

public interface Solomonoff {

    interface SolWalker<Y>{
        Y atomicVar(AtomicVar var);
        Y submatch(SolSubmatch sub);
    }

    public <Y> Y walk(SolWalker<Y> walker);

    /**Returns maximal group index contained in the expression*/
    public int validateSubmatches(VarQuery query);

    int precedence();

    void toString(StringBuilder sb);


    interface SolStringifier{
        StringifierMeta usagesLeft(EncodedID id);
        void useSubmatch(int groupIndex);
    }
    /**
     * prints to string but with automatic inference of lexicographic weights and
     * automatically prefixing variables with exponentials whenever copies are
     * necessary. Returns the highest weight that was appended to the regular
     * expression.
     *
     * @return max and min weight of any outgoing transition (according to
     * Glushkov's construction)
     */
    Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,SolStringifier usagesLeft);



}