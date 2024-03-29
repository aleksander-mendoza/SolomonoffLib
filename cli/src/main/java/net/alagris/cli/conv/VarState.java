package net.alagris.cli.conv;

import net.alagris.core.Util;

public enum VarState {
    LAZY("@"),
    NONE("K"),
    INV("I"),
    IDENTITY("D"),
    CLEAR_OUTPUT("C"),
    INV_THEN_CLEAR_OUTPUT("A"),
    INV_THEN_IDENTITY("F"),
    INV_THEN_CLEAR_OUTPUT_THEN_INV("B"),
    CLEAR_OUTPUT_THEN_INV("E"),
    AUTOMATON_BECAME_EPSILON(null);

    static{
        assert Util.unique(VarState.values(),v->v.encodingPrefix);
        assert Util.unique(VarState.values(),v->v.encodingPrefix==null?null:v.encodingPrefixChar());
    }
    public final String encodingPrefix;

    public char encodingPrefixChar(){
        return encodingPrefix.charAt(0);
    }

    private VarState(String encodingPrefix) {
        this.encodingPrefix = encodingPrefix;
    }

    public VarMeta actOn(VarMeta meta) {
        switch (this) {
            case CLEAR_OUTPUT:
                return new VarMeta(meta.readsInput, false, 1);
            case CLEAR_OUTPUT_THEN_INV:
                return new VarMeta(false, meta.readsInput, 1);
            case INV:
                return new VarMeta(meta.producesOutput, meta.readsInput, meta.compositionHeight);
            case INV_THEN_CLEAR_OUTPUT:
                return new VarMeta(meta.producesOutput, false, 1);
            case INV_THEN_CLEAR_OUTPUT_THEN_INV:
                return new VarMeta(false, meta.producesOutput, 1);
            case NONE:
                return new VarMeta(meta.readsInput, meta.producesOutput, meta.compositionHeight);
            case AUTOMATON_BECAME_EPSILON:
                return new VarMeta(false, false, 1);
            case IDENTITY:
                return new VarMeta(meta.readsInput, meta.readsInput, 1);
            case INV_THEN_IDENTITY:
                return new VarMeta(meta.producesOutput, meta.producesOutput, 1);
            default:
                throw new IllegalStateException(this + " is missing");
        }
    }
    public VarState clear() {
        switch (this) {
            case IDENTITY:
            case NONE:
                return VarState.CLEAR_OUTPUT;
            case INV_THEN_IDENTITY:
            case INV:
                return VarState.INV_THEN_CLEAR_OUTPUT;
            case CLEAR_OUTPUT:
            case INV_THEN_CLEAR_OUTPUT:
                return this;
            case INV_THEN_CLEAR_OUTPUT_THEN_INV:
            case CLEAR_OUTPUT_THEN_INV:
            case AUTOMATON_BECAME_EPSILON:
                return VarState.AUTOMATON_BECAME_EPSILON;
            default:
                throw new IllegalStateException(this + " missing");
        }
    }
    public VarState identity() {
        switch (this) {
            case CLEAR_OUTPUT:
            case IDENTITY:
            case NONE:
                return VarState.IDENTITY;
            case INV_THEN_IDENTITY:
            case INV_THEN_CLEAR_OUTPUT:
            case INV:
                return VarState.INV_THEN_IDENTITY;
            case CLEAR_OUTPUT_THEN_INV:
            case INV_THEN_CLEAR_OUTPUT_THEN_INV:
            case AUTOMATON_BECAME_EPSILON:
                return VarState.AUTOMATON_BECAME_EPSILON;
            default:
                throw new IllegalStateException(this + " missing");
        }
    }
    public VarState inv() {
        switch (this) {
            case IDENTITY:
                return VarState.IDENTITY;
            case INV_THEN_IDENTITY:
                return VarState.INV_THEN_IDENTITY;
            case CLEAR_OUTPUT:
                return VarState.CLEAR_OUTPUT_THEN_INV;
            case CLEAR_OUTPUT_THEN_INV:
                return VarState.CLEAR_OUTPUT;
            case INV:
                return VarState.NONE;
            case INV_THEN_CLEAR_OUTPUT:
                return VarState.INV_THEN_CLEAR_OUTPUT_THEN_INV;
            case INV_THEN_CLEAR_OUTPUT_THEN_INV:
                return VarState.INV_THEN_CLEAR_OUTPUT;
            case NONE:
                return VarState.INV;
            case AUTOMATON_BECAME_EPSILON:
                return VarState.AUTOMATON_BECAME_EPSILON;
            default:
                throw new IllegalStateException(this + " missing");
        }
    }

    public Kolmogorov actOn(Kolmogorov kolm) {
        switch (this) {
            case IDENTITY:
                return kolm.identity();
            case INV_THEN_IDENTITY:
                return kolm.inv().identity();
            case AUTOMATON_BECAME_EPSILON:
                return Atomic.EPSILON;
            case CLEAR_OUTPUT:
                return kolm.clearOutput();
            case CLEAR_OUTPUT_THEN_INV:
                return kolm.clearOutput().inv();
            case INV:
                return kolm.inv();
            case INV_THEN_CLEAR_OUTPUT:
                return kolm.inv().clearOutput();
            case INV_THEN_CLEAR_OUTPUT_THEN_INV:
                return kolm.inv().clearOutput().inv();
            case NONE:
                return kolm;
            default:
                throw new IllegalArgumentException(this+" missing!");

        }
    }
}
