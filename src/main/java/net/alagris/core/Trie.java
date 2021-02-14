package net.alagris.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Trie<In, Out> {
    public final HashMap<In, Trie<In, Out>> children;
    public Out value;
    public Trie(){
        this(1);
    }
    public Trie(int capacity){
        children  = new HashMap<>(capacity);
    }
    public Trie<In,Out> copy(){
        final Trie<In,Out> c = new Trie<>();
        c.value = value;
        for(Map.Entry<In, Trie<In, Out>> e:children.entrySet()){
            c.children.put(e.getKey(),e.getValue().copy());
        }
        return c;
    }
    public  Trie<In, Out> get(Iterable<In> l){
        Trie<In, Out> node = this;
        for (In symbol : l) {
            node = node.children.get(symbol);
            if(node==null)return null;
        }
        return node;
    }
    public  Trie<In, Out> getOrCreate(Iterable<In> l){
        Trie<In, Out> node = this;
        for (In symbol : l) {
            node = node.children.computeIfAbsent(symbol, k -> new Trie<>());
        }
        return node;
    }
    public  Out put(Iterable<In> l, Out r){
        Trie<In, Out> node = getOrCreate(l);
        final Out prev = node.value;
        node.value = r;
        return prev;
    }


    public static <In,Out extends Iterable<In>> ArrayList<In> replaceAll(Seq<In> input, Trie<In,Out> dict) {
        final ArrayList<In> ints = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Trie<In,Out> iter = dict;
            Trie<In,Out> furthestMatch = null;
            int furthestMatchIdx = i;
            for (int j = i; j < input.size(); j++) {
                final In symbol = input.get(j);
                iter = iter.children.get(symbol);
                if (iter == null) break;
                if (iter.value != null) {
                    furthestMatchIdx = j;
                    furthestMatch = iter;
                    assert furthestMatch != dict;
                }
            }
            if (furthestMatch != null) {
                for (In j : furthestMatch.value) ints.add(j);
                i = furthestMatchIdx;
            } else {
                ints.add(input.get(i));
            }

        }
        return ints;
    }
}