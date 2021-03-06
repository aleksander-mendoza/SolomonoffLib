package net.alagris.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;

/**
 * Iterator that returns null when end is reached
 */
public interface NullTermIter<X> {
    X next();



    static <X> NullTermIter<X> fromIterable(Iterable<X> iter) {
        return fromIter(iter.iterator());
    }

    static <X> NullTermIter<X> fromIter(Iterator<X> iter) {
        return () -> iter.hasNext() ? iter.next() : null;
    }

    static <X, Y> NullTermIter<Y> fromIterableMapped(Iterable<X> iter, Function<X, Y> map) {
        return fromIter(iter.iterator(), map);
    }

    static <X, Y> NullTermIter<Y> fromIter(Iterator<X> iter, Function<X, Y> map) {
        return () -> iter.hasNext() ? map.apply(iter.next()) : null;
    }

    static <X> ArrayList<X> collect(NullTermIter<X> iter, int capacity) {
        final ArrayList<X> arr = new ArrayList<>(capacity);
        X x;
        while ((x = iter.next()) != null) {
            arr.add(x);
        }
        return arr;
    }
}
