package net.alagris;

import java.util.*;
import java.util.function.*;

public class Util {



    /**Just like List.removeIf but this one is */
    static <X> void removeTail(List<X> list, int desiredLength) {
        while (list.size() > desiredLength) {
            list.remove(list.size() - 1);
        }
    }

    static <X> ArrayList<X> filledArrayList(int size, X defaultElement) {
        return filledArrayListFunc(size, i -> defaultElement);
    }

    static <X> ArrayList<X> filledArrayListFunc(int size, Function<Integer, X> defaultElement) {
        ArrayList<X> arr = new ArrayList<>(size);
        for (int i = 0; i < size; i++) arr.add(defaultElement.apply(i));
        return arr;
    }

    static <X> ArrayList<X> concatArrayLists(List<X> a, List<X> b) {
        ArrayList<X> arr = new ArrayList<>(a.size() + b.size());
        arr.addAll(a);
        arr.addAll(b);
        return arr;
    }

    static <X, Y> Y fold(Collection<X> a, Y init, BiFunction<X, Y, Y> b) {
        for (X x : a) {
            init = b.apply(x, init);
        }
        return init;
    }

    /**
     * returns new size of list
     */
    static <X> int shiftDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        if (sortedArray.size() > 1) {
            int i = 0;
            for (int j = 1; j < sortedArray.size(); j++) {
                X prev = sortedArray.get(i);
                X curr = sortedArray.get(j);
                if (areEqual.test(prev, curr)) {
                    mergeEqual.accept(prev, curr);
                } else {
                    i++;
                    sortedArray.set(i, curr);
                }
            }
            return i + 1;
        }
        return sortedArray.size();
    }

    /**
     * removes duplicates in an array. First the array is sorted adn then all the equal elements
     * are merged into one. You can specify when elements are equal and how to merge them
     */
    static <X> void removeDuplicates(List<X> sortedArray, BiPredicate<X, X> areEqual, BiConsumer<X, X> mergeEqual) {
        removeTail(sortedArray, shiftDuplicates(sortedArray, areEqual, mergeEqual));

    }


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

    /**
     * Searches a collection for an element that satisfies some predicate
     */
    public static <T> T find(Collection<T> c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return t;
        return null;
    }

    public static <T> boolean forall(Collection<T> c, Predicate<T> pred) {
        for (T t : c) if (!pred.test(t)) return false;
        return true;
    }

    public static <T> boolean exists(Collection<T> c, Predicate<T> pred) {
        for (T t : c) if (pred.test(t)) return true;
        return false;
    }

    public static <X> ArrayList<X> singeltonArrayList(X x) {
        ArrayList<X> a = new ArrayList<>(1);
        a.add(x);
        return a;
    }


    public static <X,Y extends List<X>> Y addAllIfAbsent(Y a,List<X> b){
        for(X s:b){
            if(!a.contains(s))a.add(s);
        }
        return a;
    }
}
