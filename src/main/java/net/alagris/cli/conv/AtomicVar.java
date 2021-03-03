package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class AtomicVar extends EncodedID implements Kolmogorov, Atomic, Solomonoff {
    /**
     * Meta information about the original variable
     */
    public final VarMeta meta;

    @Override
    public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
        return argMap.get(encodeID());
    }

    @Override
    public int compositionHeight() {
        return state.actOn(meta).compositionHeight;
    }

    @Override
    public void forEachVar(Consumer<AtomicVar> variableAssignment) {
        variableAssignment.accept(this);
    }

    @Override
    public boolean equals(Object o) {
        boolean equals = super.equals(o);
        assert !equals || !(o instanceof AtomicVar) || Objects.equals(meta, ((AtomicVar) o).meta):this+" == "+o+", "+this.state+"=="+((AtomicVar) o).state+", "+meta+" != "+((AtomicVar) o).meta;
        return equals;
    }

    @Override
    public Kolmogorov clearOutput() {
        final VarState newState = state.clear();
        return newState == VarState.AUTOMATON_BECAME_EPSILON ? EPSILON : (newState == state ? this : new AtomicVar(this, newState));

    }

    public AtomicVar(String id, VarState state, Kolmogorov referenced) {
        this(id, state, new VarMeta(referenced));
    }

    private AtomicVar(AtomicVar other, VarState newState) {
        this(other.id, newState, other.meta);
    }

    /**
     * literal constructor
     */
    private AtomicVar(String id, VarState state, VarMeta meta) {
        super(id, state);
        this.meta = meta;
    }

    @Override
    public Kolmogorov inv() {
        final VarState newState = state.inv();
        return newState == VarState.AUTOMATON_BECAME_EPSILON ? EPSILON : (newState == state ? this : new AtomicVar(this, newState));
    }

    @Override
    public Solomonoff toSolomonoff(VarQuery query) {
        query.variableAssignment(this);// just a callback. Do nothing with it
        return this;
    }
    @Override
    public <Y> Y walk(SolWalker<Y> walker) {
        return walker.atomicVar(this);
    }
    @Override
    public int validateSubmatches(VarQuery query) {
        Solomonoff sol = query.variableDefinitions(this);
        assert sol!=null:this;
        return sol.validateSubmatches(query);
    }
    @Override
    public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment) {
        return variableAssignment.apply(this).representative(variableAssignment);
    }

    @Override
    public boolean producesOutput() {
        return state.actOn(meta).producesOutput;
    }

    @Override
    public boolean readsInput() {
        return state.actOn(meta).readsInput;
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void toString(StringBuilder sb) {
        sb.append(encodeID());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, SolStringifier usagesLeft) {

        final StringifierMeta usages = usagesLeft.usagesLeft(this);
        assert usages.usagesLeft > 0;
        if (--usages.usagesLeft > 0) {
            sb.append("!!");
        }
        sb.append(encodeID());
        assert usages.weights != null : id+"\n\n"+sb;
        return new Weights(usages.weights);
    }


}
