package net.alagris.cli.conv;

import java.util.HashSet;

public class StringifierMeta {
    int usagesLeft;
    Weights weights;
    HashSet<Integer> usedGroupIndices;

    public StringifierMeta(int usagesLeft) {
        this.usagesLeft = usagesLeft;
    }

    public StringifierMeta increment() {
        usagesLeft++;
        return this;
    }
}
