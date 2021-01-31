package net.alagris;

import java.util.*;
import java.util.function.Function;

public class Util {




    /**
     * Yields undefined behaviour if underlying structure is mutated. If you need to
     * edit the structure of transitions, then to it in a new separate copy
     */
    static <X> List<X> lazyConcatImmutableLists(List<X> lhs, List<X> rhs) {
        return new AbstractList<X>() {
            /**this should not change*/
            final int offset = lhs.size();

            @Override
            public Iterator<X> iterator() {
                return new Iterator<X>() {
                    boolean finishedLhs = false;
                    Iterator<X> lhsIter = lhs.iterator();
                    Iterator<X> rhsIter = rhs.iterator();

                    @Override
                    public boolean hasNext() {
                        if (finishedLhs) return rhsIter.hasNext();
                        return lhsIter.hasNext() || rhsIter.hasNext();
                    }

                    @Override
                    public X next() {
                        if (finishedLhs) return rhsIter.next();
                        if (lhsIter.hasNext()) return lhsIter.next();
                        finishedLhs = true;
                        return rhsIter.next();
                    }
                };
            }

            @Override
            public X get(int index) {
                return index < offset ? lhs.get(index) : rhs.get(index - offset);
            }

            @Override
            public int size() {
                return offset + lhs.size();
            }
        };
    }

    /**
     * This is a utility function that can take some list of transitions. If the list is null,
     * then a sink transition is implied. Sink transition is represented by null
     */
    public static <Y> List<Y> listOrSingletonWithNull(List<Y> list) {
        return list.isEmpty() ? Collections.singletonList(null) : list;
    }

    public static <X, Y> List<Y> mapListLazy(List<X> list, Function<X, Y> f) {
        return new AbstractList<Y>() {
            @Override
            public Y get(int index) {
                return f.apply(list.get(index));
            }

            @Override
            public int size() {
                return list.size();
            }
        };
    }

    public static Iterator<Integer> iterNumbers(int initInclusive, int endExclusive){
        return new Iterator<Integer>() {
            int i=initInclusive;
            @Override
            public boolean hasNext() {
                return i<endExclusive;
            }

            @Override
            public Integer next() {
                return i++;
            }
        };
    }
    public static <X> Iterator<X> iterArray(X[] array){
        return new Iterator<X>() {
            int i=0;
            @Override
            public boolean hasNext() {
                return i<array.length;
            }

            @Override
            public X next() {
                return array[i++];
            }
        };
    }
}
