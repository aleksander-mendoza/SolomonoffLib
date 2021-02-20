package net.alagris.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IntEmbedding {
    public final int[] toOriginal;
    public final HashMap<Integer,Integer> toEmbedding;

    public IntEmbedding(HashMap<Integer, Integer> toEmbedding) {
        this.toEmbedding = toEmbedding;
        toOriginal = new int[toEmbedding.size()];
        for (Map.Entry<Integer, Integer> e : toEmbedding.entrySet()) {
            toOriginal[e.getValue()] = e.getKey();
        }
    }
    public IntEmbedding(int[] toOriginal, HashMap<Integer, Integer> toEmbedding) {
        this.toOriginal = toOriginal;
        this.toEmbedding = toEmbedding;
    }
    public static <M extends Map<Integer, Integer>> M inferAlphabet(Iterator<Pair<IntSeq, IntSeq>> informant,
                                     M symbolToUniqueIndex) {
        while (informant.hasNext()) {
            for (int symbol : informant.next().l()) {
                symbolToUniqueIndex.computeIfAbsent(symbol, s -> symbolToUniqueIndex.size());
            }
        }
        return symbolToUniqueIndex;
    }
    public IntEmbedding(int... chars) {
        toEmbedding = new HashMap<Integer,Integer>();
        toOriginal = chars;
        for(int i=0;i<chars.length;i++){
            toEmbedding.put(chars[i],i);
        }
    }
    public IntEmbedding(Iterator<Pair<IntSeq, IntSeq>> informant) {
        this(inferAlphabet(informant, new HashMap<>()));
    }

    public Integer embed(int symbol){
        return toEmbedding.get(symbol);
    }

    public int retrieve(int embedded){
        return toOriginal[embedded];
    }

    public int size() {
        return toOriginal.length;
    }

}
