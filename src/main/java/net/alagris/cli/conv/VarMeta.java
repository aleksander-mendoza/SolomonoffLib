package net.alagris.cli.conv;

import java.util.Objects;

public class VarMeta {
    /**
     * true if automaton reads anything else than epsilon
     */
    final boolean readsInput;
    /**
     * true if automaton produces anything else than epsilon
     */
    final boolean producesOutput;

    /**Height of composition*/
    final int compositionHeight;

    public VarMeta(boolean readsInput, boolean producesOutput, int compositionHeight) {
        this.readsInput = readsInput;
        this.producesOutput = producesOutput;
        this.compositionHeight = compositionHeight;
    }

    public VarMeta(Kolmogorov kolm) {
        this.readsInput = kolm.readsInput();
        this.producesOutput = kolm.producesOutput();
        this.compositionHeight = kolm.compositionHeight();
    }

    @Override
    public String toString() {
        return "VarMeta{" +
                "readsInput=" + readsInput +
                ", producesOutput=" + producesOutput +
                ", compositionHeight=" + compositionHeight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        VarMeta varMeta = (VarMeta) o;
        return readsInput == varMeta.readsInput &&
                producesOutput == varMeta.producesOutput &&
                compositionHeight == varMeta.compositionHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(readsInput, producesOutput, compositionHeight);
    }
}
