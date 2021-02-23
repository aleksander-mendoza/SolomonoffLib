package net.alagris.cli.conv;

import net.alagris.core.IntSeq;

public class AtomicStr implements Atomic.Str {
    final IntSeq str;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public AtomicStr(IntSeq str) {
        this.str = str;
    }

    @Override
    public IntSeq str() {
        return str;
    }

    @Override
    public int compositionHeight() {
        return 1;
    }

    @Override
    public boolean readsInput() {
        return !str.isEmpty();
    }

}
