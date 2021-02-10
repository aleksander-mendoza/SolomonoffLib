package net.alagris.core;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * Generic interface for representing immutable sequences of symbols
 */
public interface Seq<X> extends Iterable<X>{
    int size();

    X get(int i);

    public default boolean isEmpty() {
        return size() == 0;
    }

    default int indexOf(int offsetInclusive, X o) {
        int i = offsetInclusive;
        while (i < size())
            if (Objects.equals(get(i),o))
                return i;
            else
                i++;
        return size();
    }
    static <X> Seq<X> empty(){
        return wrap((X[])new Object[0]);
    }
    default void copyTo(int offset,X[] arr){
        for(int i=0;i<size();i++){
            arr[offset+i] = get(i);
        }
    }
    static <X> Seq<X> wrap(X[] arr){
        return new Seq<X>() {
            @Override
            public int size() {
                return arr.length;
            }

            @Override
            public X get(int i) {
                return arr[i];
            }

            @Override
            public Iterator<X> iterator() {
                return new Iterator<X>() {
                    int i = 0;
                    @Override
                    public boolean hasNext() {
                        return i<arr.length;
                    }

                    @Override
                    public X next() {
                        return arr[i++];
                    }
                };
            }

            @Override
            public String toString() {
                return Arrays.toString(arr);
            }
        };
    }
    default Seq<X> sub(int offsetInclusive, int endIndexExclusive){
        if(offsetInclusive>=endIndexExclusive)return empty();
        final X[] arr =(X[]) new Object[endIndexExclusive-offsetInclusive];
        for(int i=offsetInclusive;i<endIndexExclusive;i++){
            arr[i-offsetInclusive] = get(i);
        }
        return wrap(arr);
    }
}