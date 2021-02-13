package net.alagris.core;

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


    public  Out add(Iterable<In> l, Out r){
        Trie<In, Out> node = this;
        for (In symbol : l) {
            node = node.children.computeIfAbsent(symbol, k -> new Trie<>());
        }

        final Out prev = node.value;
        node.value = r;
        return prev;
    }

}