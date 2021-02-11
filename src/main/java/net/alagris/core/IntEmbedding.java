package net.alagris.core;

import java.util.HashMap;

public class IntEmbedding {
    public final int[] toOriginal;
    public final HashMap<Integer,Integer> toEmbedding;

    public IntEmbedding(int[] toOriginal, HashMap<Integer, Integer> toEmbedding) {
        this.toOriginal = toOriginal;
        this.toEmbedding = toEmbedding;
    }

    public int embed(int symbol){
        return toEmbedding.get(symbol);
    }

    public int retrieve(int embedded){
        return toOriginal[embedded];
    }

}
