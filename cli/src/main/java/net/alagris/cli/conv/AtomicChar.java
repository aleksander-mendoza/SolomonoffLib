package net.alagris.cli.conv;

import net.alagris.core.IntSeq;
import net.alagris.core.Specification;

import java.util.ArrayList;

public class AtomicChar implements Atomic.Set, Atomic.Str {
    final int character;
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    @Override
    public Kolmogorov identity() {
        return new KolRefl(this);
    }

    @Override
    public int compositionHeight() {
        return 1;
    }

    @Override
    public boolean readsInput() {
        return true;
    }

    public AtomicChar(int character) {
        this.character = character;
    }

    @Override
    public IntSeq str() {
        return new IntSeq(character);
    }

    @Override
    public ArrayList<Specification.Range<Integer, Boolean>> ranges() {
        return SPECS.makeSingletonRanges(true, false, character - 1, character);
    }

}
