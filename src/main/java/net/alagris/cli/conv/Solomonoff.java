package net.alagris.cli.conv;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Solomonoff {

    public void countUsages(Consumer<AtomicVar> countUsage);

    int precedence();

    void toString(StringBuilder sb);

    /**
     * prints to string but with automatic inference of lexicographic weights and
     * automatically prefixing variables with exponentials whenever copies are
     * necessary. Returns the highest weight that was appended to the regular
     * expression.
     *
     * @return max and min weight of any outgoing transition (according to
     * Glushkov's construction)
     */
    Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<EncodedID, StringifierMeta> usagesLeft);



}