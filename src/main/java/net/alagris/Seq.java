package net.alagris;

/**
 * Generic interface for representing immutable sequences of symbols
 */
public interface Seq<X> extends Iterable<X>{
    int size();

    X get(int i);

    public default boolean isEmpty() {
        return size() == 0;
    }
}